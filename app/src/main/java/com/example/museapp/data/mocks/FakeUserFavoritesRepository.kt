package com.example.museapp.data.mocks

import android.content.Context
import com.example.museapp.data.remote.dto.FavoriteUserAddResponseDto
import com.example.museapp.data.remote.dto.FavoriteUserDto
import com.example.museapp.data.remote.mapper.toDomain
import com.example.museapp.data.util.NetworkResult
import com.example.museapp.domain.model.FavoriteUser
import com.example.museapp.domain.repository.UserFavoritesRepository
import com.example.museapp.util.AssetUtils
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FakeUserFavoritesRepository @Inject constructor(
    private val context: Context
) : UserFavoritesRepository {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val favListType = Types.newParameterizedType(List::class.java, FavoriteUserDto::class.java)
    private val favListAdapter = moshi.adapter<List<FavoriteUserDto>>(favListType)
    private val addWrapperAdapter = moshi.adapter(AddResponseWrapper::class.java)
    private val userDtoAdapter = moshi.adapter(com.example.museapp.data.remote.dto.UserDto::class.java)

    private data class AddResponseWrapper(val data: FavoriteUserAddResponseDto?)

    override suspend fun getMyFavorites(): NetworkResult<List<FavoriteUser>> = withContext(Dispatchers.IO) {
        delay(250)
        val raw = AssetUtils.readJsonAsset(context, "user_favorites_get_response.json") ?: "[]"
        val dtoList = try { favListAdapter.fromJson(raw) ?: emptyList() } catch (_: Throwable) { emptyList() }
        val domain = dtoList.map { dto -> FavoriteUser(id = dto.id, favoriteUser = dto.favoriteUser.toDomain(), createdAt = dto.createdAt) }
        NetworkResult.Success(domain)
    }

    override suspend fun addFavorite(userId: String): NetworkResult<FavoriteUser> = withContext(Dispatchers.IO) {
        delay(200)
        val raw = AssetUtils.readJsonAsset(context, "user_favorites_add_response.json")
        val parsedData = try {
            if (raw != null) addWrapperAdapter.fromJson(raw)?.data else null
        } catch (_: Throwable) { null }

        if (parsedData != null) {
            val userDto = parsedData.favoriteUser ?: parsedData.user
            if (userDto != null) {
                val domainUser = userDto.toDomain()
                val fav = FavoriteUser(id = parsedData.id ?: "fav_$userId", favoriteUser = domainUser, createdAt = parsedData.createdAt)
                return@withContext NetworkResult.Success(fav)
            }
        }

        // fallback construct small json and parse to UserDto -> toDomain()
        val simpleJson = """{ "id":"$userId", "fullName":"Favorited User" }"""
        val userDto = try { userDtoAdapter.fromJson(simpleJson) } catch (_: Throwable) { null }
        val domainUser = userDto?.toDomain() ?: throw IllegalStateException("Unable to build user")
        NetworkResult.Success(FavoriteUser(id = "fav_$userId", favoriteUser = domainUser, createdAt = null))
    }

    override suspend fun removeFavorite(favoriteId: String): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        delay(150)
        NetworkResult.Success(Unit)
    }
}

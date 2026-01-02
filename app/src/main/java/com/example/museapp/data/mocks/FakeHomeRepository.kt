package com.example.museapp.data.mocks

import android.content.Context
import com.example.museapp.data.remote.dto.AdDto
import com.example.museapp.data.remote.dto.UsersApiResponse
import com.example.museapp.data.remote.dto.UserDto
import com.example.museapp.data.remote.mapper.toDomain
import com.example.museapp.data.util.NetworkResult
import com.example.museapp.domain.model.Ad
import com.example.museapp.domain.model.User
import com.example.museapp.domain.repository.HomeRepository
import com.example.museapp.util.AssetUtils
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FakeHomeRepository @Inject constructor(private val context: Context) : HomeRepository {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adDtoType = Types.newParameterizedType(List::class.java, AdDto::class.java)
    private val adAdapter = moshi.adapter<List<AdDto>>(adDtoType)

    // Adapter for UsersApiResponse (wrapper with data.users)
    private val usersApiAdapter = moshi.adapter(UsersApiResponse::class.java)

    override suspend fun getAds(lat: Double?, lng: Double?, radiusKm: Int?): NetworkResult<List<Ad>> = withContext(Dispatchers.IO) {
        delay(300)
        return@withContext try {
            val json = AssetUtils.readJsonAsset(context, "dummy_ads.json") ?: return@withContext NetworkResult.Error(
                message = "Missing dummy_ads.json in assets")
            val dtoList = adAdapter.fromJson(json) ?: emptyList()
            val domainList: List<Ad> = dtoList.map { it.toDomain() }
            NetworkResult.Success(domainList)
        } catch (t: Throwable) {
            NetworkResult.Error(message = t.message ?: "Mock parse error", throwable = t)
        }
    }

    override suspend fun getSkills(): NetworkResult<List<String>> = withContext(Dispatchers.IO) {
        delay(200)
        return@withContext try {
            val json = AssetUtils.readJsonAsset(context, "dummy_ads.json") ?: return@withContext NetworkResult.Error(
                message = "Missing dummy_ads.json in assets")
            val dtoList = adAdapter.fromJson(json) ?: emptyList()
            val set = mutableSetOf<String>()

            dtoList.forEach { adDto ->
                // primarySkill (if present)
                adDto.primarySkill.takeIf { it.isNotBlank() }?.let { set.add(it) }

                // user's interests -> each interest likely has a 'name' field in DTO
                adDto.user?.let { user ->
                    // try to read interests (array of objects with .name)
                    user.interests?.forEach { interest ->
                        // interest.name might be nullable depending on DTO; be defensive
                        val name = when {
                            interest == null -> null
                            // access property 'name' via safe call (Interest DTO should have .name)
                            else -> try { interest::class.members.firstOrNull { it.name == "name" }?.call(interest) as? String } catch (t: Throwable) { null }
                        }
                        name?.takeIf { it.isNotBlank() }?.let { set.add(it) }
                    }

                    // Some DTO variants might expose skills as List<String> (older schema).
                    // Try to access that too in a safe manner using reflection to avoid compile errors.
                    val skillsField = try {
                        user::class.members.firstOrNull { it.name == "skills" }
                    } catch (t: Throwable) { null }

                    if (skillsField != null) {
                        @Suppress("UNCHECKED_CAST")
                        val skillsValue = try { skillsField.call(user) as? List<*> } catch (t: Throwable) { null }
                        skillsValue?.forEach { s ->
                            val str = s as? String
                            str?.takeIf { it.isNotBlank() }?.let { set.add(it) }
                        }
                    }
                }
            }

            NetworkResult.Success(set.toList().sorted())
        } catch (t: Throwable) {
            NetworkResult.Error(message = t.message ?: "Mock parse error", throwable = t)
        }
    }

    // New: getUsers reads dummy_users.json, parses UsersApiResponse -> list<UserDto> -> domain User
    override suspend fun getUsers(page: Int, limit: Int): NetworkResult<List<User>> = withContext(Dispatchers.IO) {
        delay(300)
        return@withContext try {
            val json = AssetUtils.readJsonAsset(context, "dummy_users.json") ?: return@withContext NetworkResult.Error(
                message = "Missing dummy_users.json in assets")
            val resp: UsersApiResponse? = usersApiAdapter.fromJson(json)
            val dtoList: List<UserDto> = resp?.data?.users ?: emptyList()
            val domainList: List<User> = dtoList.map { it.toDomain() }
            NetworkResult.Success(domainList)
        } catch (t: Throwable) {
            NetworkResult.Error(message = t.message ?: "Mock parse error", throwable = t)
        }
    }
}

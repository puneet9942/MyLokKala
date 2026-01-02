package com.example.museapp.data.mocks

import android.content.Context
import com.example.museapp.data.remote.dto.AdDto
import com.example.museapp.data.remote.mapper.toDomain
import com.example.museapp.data.util.NetworkResult
import com.example.museapp.domain.model.Ad
import com.example.museapp.domain.repository.FavoritesRepository
import com.example.museapp.util.AssetUtils
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

class FakeFavoritesRepository @Inject constructor(private val context: Context) : FavoritesRepository {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adDtoType = Types.newParameterizedType(List::class.java, AdDto::class.java)
    private val adAdapter = moshi.adapter<List<AdDto>>(adDtoType)

    private val favSet = ConcurrentHashMap.newKeySet<String>()

    init {
        runCatching {
            val seed = AssetUtils.readJsonAsset(context, "dummy_favorites.json")
            if (!seed.isNullOrBlank()) {
                val jsonAdapter = moshi.adapter<List<String>>(Types.newParameterizedType(List::class.java, String::class.java))
                val ids = jsonAdapter.fromJson(seed)
                ids?.forEach { favSet.add(it) }
            }
        }
    }

    private fun loadAllAds(): List<AdDto> {
        val json = AssetUtils.readJsonAsset(context, "dummy_ads.json") ?: "[]"
        return try {
            adAdapter.fromJson(json) ?: emptyList()
        } catch (t: Throwable) {
            emptyList()
        }
    }

    override suspend fun getFavorites(): NetworkResult<List<Ad>> = withContext(Dispatchers.IO) {
        delay(200)
        return@withContext try {
            val dtoList = loadAllAds()
            val selected = dtoList.filter { favSet.contains(it.id) }.map { it.toDomain() }
            NetworkResult.Success(selected)
        } catch (t: Throwable) {
            NetworkResult.Error(message = t.message ?: "Mock error", throwable = t)
        }
    }

    override suspend fun addFavorite(adId: String): NetworkResult<Unit> {
        delay(150)
        favSet.add(adId)
        return NetworkResult.Success(Unit)
    }

    override suspend fun removeFavorite(adId: String): NetworkResult<Unit> {
        delay(150)
        favSet.remove(adId)
        return NetworkResult.Success(Unit)
    }
}

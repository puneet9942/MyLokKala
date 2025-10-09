package com.example.lokkala.data.mocks

import android.content.Context
import com.example.lokkala.data.remote.dto.AdDto
import com.example.lokkala.data.remote.mapper.toDomain
import com.example.lokkala.data.util.NetworkResult
import com.example.lokkala.domain.model.Ad
import com.example.lokkala.domain.repository.HomeRepository
import com.example.lokkala.util.AssetUtils
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.delay
import javax.inject.Inject

class FakeHomeRepository @Inject constructor(private val context: Context) : HomeRepository {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adDtoType = Types.newParameterizedType(List::class.java, AdDto::class.java)
    private val adAdapter = moshi.adapter<List<AdDto>>(adDtoType)

    override suspend fun getAds(lat: Double?, lng: Double?, radiusKm: Int?): NetworkResult<List<Ad>> {
        delay(300)
        return try {
            val json = AssetUtils.readJsonAsset(context, "dummy_ads.json")
                ?: return NetworkResult.Error(message = "Missing dummy_ads.json in assets")
            val dtoList = adAdapter.fromJson(json) ?: emptyList()
            val domainList: List<Ad> = dtoList.map { it.toDomain() } // explicit List<Ad>
            NetworkResult.Success(domainList)
        } catch (t: Throwable) {
            t.printStackTrace()
            NetworkResult.Error(message = t.message ?: "Mock parse error", throwable = t)
        }
    }

    override suspend fun getSkills(): NetworkResult<List<String>> {
        delay(200)
        return try {
            val json = AssetUtils.readJsonAsset(context, "dummy_ads.json")
                ?: return NetworkResult.Error(message = "Missing dummy_ads.json in assets")
            val dtoList = adAdapter.fromJson(json) ?: emptyList()
            val set = mutableSetOf<String>()
            dtoList.forEach { adDto ->
                adDto.primarySkill.takeIf { it.isNotBlank() }?.let { set.add(it) }
                adDto.user.skills.forEach { s -> if (s.isNotBlank()) set.add(s) }
            }
            NetworkResult.Success(set.toList().sorted())
        } catch (t: Throwable) {
            t.printStackTrace()
            NetworkResult.Error(message = t.message ?: "Mock parse error", throwable = t)
        }
    }
}
package com.example.museapp.data.local.converters

import androidx.room.TypeConverter
import com.example.museapp.data.remote.dto.InterestDto
import com.example.museapp.util.MoshiProvider
import com.squareup.moshi.Types

class RoomConverters {
    private val moshi = MoshiProvider.moshi

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        if (value == null) return null
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        return moshi.adapter<List<String>>(type).toJson(value)
    }

    @TypeConverter
    fun toStringList(json: String?): List<String>? {
        if (json == null) return null
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        return moshi.adapter<List<String>>(type).fromJson(json)
    }

    @TypeConverter
    fun fromInterestList(value: List<InterestDto>?): String? {
        if (value == null) return null
        val type = Types.newParameterizedType(List::class.java, InterestDto::class.java)
        return moshi.adapter<List<InterestDto>>(type).toJson(value)
    }

    @TypeConverter
    fun toInterestList(json: String?): List<InterestDto>? {
        if (json == null) return null
        val type = Types.newParameterizedType(List::class.java, InterestDto::class.java)
        return moshi.adapter<List<InterestDto>>(type).fromJson(json)
    }
}

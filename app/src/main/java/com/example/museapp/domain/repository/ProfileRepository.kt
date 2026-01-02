package com.example.museapp.domain.repository

import android.net.Uri
import com.example.museapp.data.util.NetworkResult
import com.example.museapp.domain.model.User
import com.example.museapp.data.remote.dto.ProfileRequestDto

interface ProfileRepository {
    /**
     * Update profile sending JSON payload and optional files.
     *
     * @param payloadDto JSON payload DTO for the "payload" multipart part
     * @param profilePhoto single profile photo Uri (optional)
     * @param photos multiple photo Uris (optional)
     * @param videos multiple video Uris (optional)
     */
    suspend fun updateProfile(
        payloadDto: ProfileRequestDto,
        profilePhoto: Uri?,
        photos: List<Uri>,
        videos: List<Uri>
    ): NetworkResult<User>

//    /**
//     * Fetch the current user's profile.
//     */
//    suspend fun getProfile(): NetworkResult<User>
}

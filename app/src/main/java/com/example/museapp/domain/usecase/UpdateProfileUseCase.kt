package com.example.museapp.domain.usecase

import android.net.Uri
import android.util.Log
import com.example.museapp.data.remote.dto.ProfileRequestDto
import com.example.museapp.data.util.NetworkResult
import com.example.museapp.domain.model.User
import com.example.museapp.domain.repository.ProfileRepository
import javax.inject.Inject

class UpdateProfileUseCase @Inject constructor(
    private val repository: ProfileRepository
) {// inside UpdateProfileUseCase (suspend operator fun invoke(...) or similar)
suspend operator fun invoke(
    payloadDto: ProfileRequestDto,
    profilePhoto: Uri?,
    photos: List<Uri>,
    videos: List<Uri>
): NetworkResult<User> {
    try {
        Log.d("UpdateProfileUC", "invoke: forwarding to repo")
        val res = repository.updateProfile(payloadDto, profilePhoto, photos, videos)
        Log.d("UpdateProfileUC", "invoke: repo returned: $res")
        return res
    } catch (t: Throwable) {
        Log.e("UpdateProfileUC", "invoke: error", t)
        return NetworkResult.Error(message = t.message ?: "Update Failed", throwable = t)
    }
}

}

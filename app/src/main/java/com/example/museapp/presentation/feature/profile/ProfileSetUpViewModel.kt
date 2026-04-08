package com.example.museapp.presentation.feature.profile

import android.net.Uri
import android.util.Log
import java.time.format.DateTimeFormatter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.museapp.data.auth.dto.VerifyOtpData
import com.example.museapp.data.local.dao.InterestsDao
import com.example.museapp.data.local.dao.ProfileCacheDao
import com.example.museapp.data.local.entity.ProfileCacheEntity
import com.example.museapp.data.remote.dto.CacheUserDto
import com.example.museapp.data.remote.dto.ProfileRequestDto
import com.example.museapp.data.remote.dto.ProfileCacheDto
import com.example.museapp.data.remote.mapper.toProfileRequestDto
import com.example.museapp.data.util.NetworkResult
import com.example.museapp.domain.model.User
import com.example.museapp.domain.usecase.UpdateProfileUseCase
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.OffsetDateTime
import javax.inject.Inject
import kotlin.reflect.KCallable
import kotlin.reflect.full.memberProperties

@HiltViewModel
class ProfileSetupViewModel @Inject constructor(
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val interestDao: InterestsDao,
    private val profileCacheDao: ProfileCacheDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val CACHE_EPOCH_TOLERANCE = 10_000L // 10s

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    // Add near other MutableStateFlow declarations
    private val _userEdited = MutableStateFlow(false)
    val userEdited = _userEdited.asStateFlow()


    private val _profileCacheDto = MutableStateFlow<ProfileCacheDto?>(null)
    val profileCacheDto: StateFlow<ProfileCacheDto?> = _profileCacheDto.asStateFlow()

    // NEW: expose cache epoch so composable can decide which cache is authoritative
    // starts -1 to indicate "unknown / not loaded"
    private val _cacheEpoch = MutableStateFlow<Long>(-1L)
    val cacheEpoch: StateFlow<Long> = _cacheEpoch.asStateFlow()

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val cacheAdapter = moshi.adapter(ProfileCacheDto::class.java)

    // store last-known cache user id so we can upsert the same row later
    private var cachedUserId: String? = null

    private val _availableInterests = MutableStateFlow<List<String>>(emptyList())
    val availableInterests: StateFlow<List<String>> = _availableInterests.asStateFlow()

    private val _cacheDump = MutableStateFlow<String?>(null)
    val cacheDump: StateFlow<String?> = _cacheDump.asStateFlow()

    init {
        // Keep loading available interests
        viewModelScope.launch(ioDispatcher) {
            try {
                interestDao.getAllInterestsFlow()
                    .map { list -> list.mapNotNull { it.name } }
                    .collect { names ->
                        _availableInterests.value = names
                    }
            } catch (_: Throwable) {
            }
        }

        viewModelScope.launch(ioDispatcher) {
            try {
                profileCacheDao.observeLatestProfileCache().collectLatest  { entity ->
                    if (entity == null) return@collectLatest
                    try {
                        val parsed = cacheAdapter.fromJson(entity.json)
                        if (parsed == null) {
                            Log.w("ProfileVM", "observeLatestProfileCache: parsed==null")
                            return@collectLatest
                        }

                        // compute incoming epoch: prefer entity.lastUpdatedMillis, fall back to parsing updatedAt inside DTO
                        val incomingEpochFromEntity = entity.lastUpdatedMillis
                        val incomingIsoFromDto = parsed.data?.let { dd ->
                            readStringPropertyByName(dd, "updatedAt", "updated_at")
                        }
                        val incomingEpochFromIso = parseIsoToEpoch(incomingIsoFromDto)
                        val incomingEpoch = when {
                            incomingEpochFromEntity > 0L -> incomingEpochFromEntity
                            incomingEpochFromIso != null -> incomingEpochFromIso
                            else -> System.currentTimeMillis()
                        }

                        // If incoming is not newer than current epoch, ignore it (prevent stale overwrite)
                        val current = _cacheEpoch.value
                        if (incomingEpoch <= current) {
                            Log.d("ProfileVM", "observeLatestProfileCache: ignoring older cache incomingEpoch=$incomingEpoch vmEpoch=$current")
                            return@collectLatest
                        }

                        // Accept the incoming cache as authoritative
                        _cacheEpoch.value = incomingEpoch
                        _profileCacheDto.value = parsed
                        Log.d("ProfileVM", "observeLatestProfileCache: applying cache epoch=$incomingEpoch id=${parsed.data?.id} name=${parsed.data?.fullName}")
                        // prefill UI on main
                        withContext(Dispatchers.Main) {
                            try {
                                prefillFromCache(parsed)
                            } catch (e: Throwable) {
                                Log.w("ProfileVM", "prefillFromCache failed on Main: ${e.message}")
                            }
                        }
                    } catch (t: Throwable) {
                        Log.w("ProfileVM", "observeLatestProfileCache: failed to parse/apply cache: ${t.message}")
                    }
                }
            } catch (t: Throwable) {
                Log.w("ProfileVM", "observeLatestProfileCache collector failed: ${t.message}")
            }
        }

        // Optional: try to seed cacheEpoch early if you can find a suitable cached row.
        // If you have a deterministic current user id available at VM init, call profileCacheDao.getProfileCacheOnce(userId)
        // and set _profileCacheDto/_cacheEpoch. If you don't have user id here, we rely on prefillFromCache and submitProfile readback to set epoch.
    }

    fun onEvent(event: ProfileEvent) {
        val cur = _state.value
        when (event) {
            is ProfileEvent.NameChanged -> _state.value = cur.copy(name = event.name ?: "")
            is ProfileEvent.DobChanged -> _state.value = cur.copy(dob = event.dob ?: "")
            is ProfileEvent.GenderChanged -> _state.value = cur.copy(gender = event.gender)
            is ProfileEvent.DescriptionChanged -> _state.value = cur.copy(description = event.desc ?: "")
            is ProfileEvent.BiographyChanged -> _state.value = cur.copy(biography = event.bio ?: "")
            is ProfileEvent.MobileChanged -> _state.value = cur.copy(mobile = event.mobile ?: "")

            is ProfileEvent.InterestToggled -> {
                val interestParam = event.interest?.trim() ?: ""
                if (interestParam.isEmpty()) return

                val m = cur.interests.map { it.trim() }.toMutableList()

                val existingIndex = m.indexOfFirst { it.equals(interestParam, ignoreCase = true) }
                if (existingIndex >= 0) {
                    m.removeAt(existingIndex)
                    _state.value = cur.copy(interests = m)
                } else {
                    if (m.size >= 4) {
                        _state.value = cur.copy(error = "You can select up to 4 interests")
                    } else {
                        m.add(interestParam)
                        _state.value = cur.copy(interests = m)
                    }
                }
            }

            is ProfileEvent.CustomInterestChanged -> _state.value = cur.copy(customInterest = event.value)
            is ProfileEvent.AddCustomInterest -> {
                val custom = (cur.customInterest ?: "").trim()
                if (custom.isNotEmpty()) {
                    val m = cur.interests.toMutableList()
                    if (m.size >= 4) {
                        _state.value = cur.copy(error = "You can select up to 4 interests")
                    } else {
                        m.add(custom)
                        val avail = _availableInterests.value.toMutableList()
                        val lower = avail.map { it.lowercase() }
                        if (!lower.contains(custom.lowercase())) {
                            avail.add(custom)
                            _availableInterests.value = avail
                        }
                        _state.value = cur.copy(interests = m, customInterest = null)
                    }
                }
            }

            is ProfileEvent.ProfilePicChanged -> {
                val uri = toUriOrNull(event.uri)
                if (uri != null) _state.value = cur.copy(profilePicUri = uri)
            }
            is ProfileEvent.AddPhotos -> {
                val newUris = event.uris.mapNotNull { toUriOrNull(it) }
                val merged = cur.photos.toMutableList()
                newUris.forEach { if (!merged.contains(it)) merged.add(it) }
                _state.value = cur.copy(photos = merged)
            }
            is ProfileEvent.AddVideos -> {
                val newUris = event.uris.mapNotNull { toUriOrNull(it) }
                val merged = cur.videos.toMutableList()
                newUris.forEach { if (!merged.contains(it)) merged.add(it) }
                _state.value = cur.copy(videos = merged)
            }

            is ProfileEvent.RemovePhoto -> {
                val target = toUriOrNull(event.uri)
                val remaining = cur.photos.filterNot { it == target }
                _state.value = cur.copy(photos = remaining)
            }
            is ProfileEvent.RemoveVideo -> {
                val target = toUriOrNull(event.uri)
                val remaining = cur.videos.filterNot { it == target }
                _state.value = cur.copy(videos = remaining)
            }

            is ProfileEvent.InstaChanged -> _state.value = cur.copy(instaId = event.id)
            is ProfileEvent.TwitterChanged -> _state.value = cur.copy(twitterId = event.id)
            is ProfileEvent.YoutubeChanged -> _state.value = cur.copy(youtubeId = event.id)
            is ProfileEvent.FacebookChanged -> _state.value = cur.copy(facebookId = event.id)

            ProfileEvent.NextStep -> _state.value = cur.copy(step = (cur.step + 1).coerceAtMost(ProfileUiState.MAX_STEP))
            ProfileEvent.PrevStep -> _state.value = cur.copy(step = (cur.step - 1).coerceAtLeast(1))
            ProfileEvent.ClearError -> _state.value = cur.copy(error = null)

            ProfileEvent.Submit -> submitProfile()

            is ProfileEvent.MaxPriceChanged -> _state.value = cur.copy(maxPrice = event.value ?: "")
            is ProfileEvent.MinPriceChanged -> _state.value = cur.copy(minPrice = event.value ?: "")
            is ProfileEvent.SetEventManager -> _state.value = cur.copy(isEventManager = event.isEventManager)
            is ProfileEvent.StandardPriceChanged -> _state.value = cur.copy(standardPrice = event.value ?: "")
            is ProfileEvent.TravelRadiusChanged -> _state.value = cur.copy(travelRadiusKm = event.value ?: "")
            is ProfileEvent.PricingTypeSelected -> _state.value = cur.copy(pricingType = event.type ?: cur.pricingType)
        }
    }

    fun prefillFromVerify(verify: VerifyOtpData?) {
        if (verify == null) return
        val u = verify.user ?: return
        val cur = _state.value
        verify.user?.id?.let { cachedUserId = it }
        _state.value = cur.copy(
            name = u.fullName ?: cur.name,
            dob = u.dob ?: cur.dob,
            gender = u.gender ?: cur.gender,
            description = u.profileDescription ?: cur.description,
            biography = u.bio ?: cur.biography,
            profilePicUri = cur.profilePicUri
        )
    }

    /**
     * Prefill UI state from Room-cached DTO.
     * Use only data.* fields (no fallback to top-level fields).
     * Also attempt to set cacheEpoch from any updatedAt-like property in the cached DTO.
     */
    fun prefillFromCache(cache: ProfileCacheDto?) {
        if (cache == null) return
        try {
            val d = cache.data
            val incomingUpdatedIso: String? = readStringPropertyByName(d, "updatedAt", "updated_at")
            val incomingEpoch = parseIsoToEpoch(incomingUpdatedIso) ?: System.currentTimeMillis()

            // If incoming epoch is not newer than current, ignore
            val current = _cacheEpoch.value
            if (incomingEpoch + CACHE_EPOCH_TOLERANCE <= current) {
                Log.d("ProfileVM", "prefillFromCache() ignoring older cache incomingEpoch=$incomingEpoch vmEpoch=$current name=${d?.fullName}")
                return
            }

            Log.d("ProfileVM", "prefillFromCache() called with cache=${d?.id} name=${d?.fullName} - applying epoch=$incomingEpoch")
            // update authoritative epoch
            _cacheEpoch.value = incomingEpoch

            // set cachedUserId if present
            d?.id?.let { cachedUserId = it }

            val cur = _state.value

            fun formatDob(raw: String?): String? {
                if (raw.isNullOrBlank()) return null
                return try {
                    OffsetDateTime.parse(raw).toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                } catch (_: Exception) {
                    raw
                }
            }

            val fullName: String? = d?.fullName
            val dobRaw: String? = d?.dob
            val formattedDob: String? = formatDob(dobRaw)
            val gender: String? = d?.gender

            val description: String? = d?.profileDescription
            val biography: String? = d?.bio

            val insta: String? = d?.instaId
            val twitter: String? = d?.twitterId
            val youtube: String? = d?.youtubeId
            val facebook: String? = d?.facebookId

            val mobile: String? = d?.phone

            val interestsList: List<String> = run {
                val domain = try { d?.toDomain() } catch (_: Throwable) { null }
                val names = domain?.interests
                    ?.mapNotNull { it.name?.trim()?.takeIf { it.isNotEmpty() } }
                    ?.map { it.trim() }
                    ?.distinctBy { it.lowercase() }
                    ?: emptyList()
                if (names.isEmpty()) cur.interests else names
            }

            val avail = _availableInterests.value.toMutableList()
            interestsList.forEach { ni ->
                if (avail.none { it.equals(ni, ignoreCase = true) }) avail.add(ni)
            }
            _availableInterests.value = avail

            val normalizedInterests = interestsList.map { it.trim() }.filter { it.isNotEmpty() }
            val interestsForState = if (normalizedInterests.isNotEmpty()) normalizedInterests else cur.interests

            val profilePicUri = d?.photo?.let { toUriOrNull(it) } ?: cur.profilePicUri

            val photos: List<Uri> = run {
                val out = ArrayList<Uri>()
                val raw = d?.profilePhotos
                when (raw) {
                    is Collection<*> -> {
                        for (e in raw) {
                            val s = e?.toString()
                            toUriOrNull(s)?.let { out.add(it) }
                        }
                    }
                    is Array<*> -> {
                        for (e in raw) {
                            val s = e?.toString()
                            toUriOrNull(s)?.let { out.add(it) }
                        }
                    }
                    is String -> {
                        toUriOrNull(raw)?.let { out.add(it) }
                    }
                    else -> { /* empty */ }
                }
                out
            }

            val videos: List<Uri> = run {
                val out = ArrayList<Uri>()
                val raw = d?.profileVideos
                when (raw) {
                    is Collection<*> -> {
                        for (e in raw) {
                            val s = e?.toString()
                            toUriOrNull(s)?.let { out.add(it) }
                        }
                    }
                    is Array<*> -> {
                        for (e in raw) {
                            val s = e?.toString()
                            toUriOrNull(s)?.let { out.add(it) }
                        }
                    }
                    is String -> {
                        toUriOrNull(raw)?.let { out.add(it) }
                    }
                    else -> { /* empty */ }
                }
                out
            }

            val pricingType: String? = d?.pricingType
            val standardPrice: String = d?.standardPrice?.toString() ?: cur.standardPrice
            val minPrice: String = d?.priceMin?.toString() ?: cur.minPrice
            val maxPrice: String = d?.priceMax?.toString() ?: cur.maxPrice
            val travelRadius: String? = d?.travelRadius?.toString()

            val isEventManagerFlag: Boolean? = when (val v = d?.isEventManager) {
                is Boolean -> v
                is String -> v.toBooleanStrictOrNull()
                else -> null
            }

            _state.value = cur.copy(
                name = fullName ?: cur.name,
                dob = formattedDob ?: cur.dob,
                gender = gender ?: cur.gender,
                description = description ?: cur.description,
                biography = biography ?: cur.biography,
                profilePicUri = profilePicUri,
                interests = interestsForState,
                photos = if (photos.isNotEmpty()) photos else cur.photos,
                videos = if (videos.isNotEmpty()) videos else cur.videos,
                instaId = insta ?: cur.instaId,
                twitterId = twitter ?: cur.twitterId,
                youtubeId = youtube ?: cur.youtubeId,
                facebookId = facebook ?: cur.facebookId,
                mobile = mobile ?: cur.mobile,
                pricingType = pricingType ?: cur.pricingType,
                standardPrice = standardPrice,
                minPrice = minPrice,
                maxPrice = maxPrice,
                travelRadiusKm = travelRadius ?: cur.travelRadiusKm,
                isEventManager = isEventManagerFlag ?: cur.isEventManager
            )

        } catch (t: Throwable) {
            Log.w("ProfileVM", "prefillFromCache failed: ${t.message}")
        }
    }

    private fun submitProfile() {
        val cur = _state.value
        if (cur.name.isBlank()) {
            _state.value = cur.copy(loading = false, error = "Please enter full name")
            Log.d("ProfileVM", "submitProfile: validation failed - name blank")
            return
        }

        viewModelScope.launch(ioDispatcher) {
            _state.value = cur.copy(loading = true, error = null)
            try {
                val dto: com.example.museapp.data.remote.dto.ProfileRequestDto = cur.toProfileRequestDto()
                val photosUris = cur.photos.mapNotNull { it }
                val videosUris = cur.videos.mapNotNull { it }

                val result = try {
                    updateProfileUseCase(dto, cur.profilePicUri, photosUris, videosUris)
                } catch (e: Throwable) {
                    Log.e("ProfileVM", "submitProfile: updateProfileUseCase threw", e)
                    _state.value = _state.value.copy(loading = false, error = e.message ?: "Network error")
                    return@launch
                }

                when (result) {
                    is NetworkResult.Success -> {
                        Log.d("ProfileVM", "submitProfile: network result=${result.data?.let { "userId=${it.id} fullName=${it.fullName}" } ?: result}")

                        try {
                            val serverUser: User? = result.data
                            Log.d("ProfileVM", "submitProfile: Success - serverUser=${serverUser?.let { "id=${it.id} name=${it.fullName} interests=${it.interests?.size ?: 0}" } ?: "null"}")

                            serverUser?.id?.takeIf { it.isNotBlank() }?.let { cachedUserId = it }

                            val cacheDto = if (serverUser != null) {
                                buildCacheDtoFromUser(serverUser)
                            } else {
                                buildCacheDtoFromState(cur)
                            }

                            val json = cacheAdapter.toJson(cacheDto)

                            // Persist to the canonical single-row cache id used across the repo
                            val userId = com.example.museapp.util.AppConstants.PROFILE_CACHE_USER_ID

                            if (userId.isNullOrBlank()) {
                                Log.w("ProfileVM", "submitProfile: no userId available; skipping local cache upsert")
                            } else {
                                // prefer server-provided updatedAt if present
                                // prefer server-provided updatedAt if present on the DTO, else use current instant
                                val serverIsoFromDto: String? = cacheDto.data?.let { dd ->
                                    readStringPropertyByName(dd, "updatedAt", "updated_at")
                                }

// compute epoch deterministically (same value used in VM and DB)
                                val serverEpoch: Long = try {
                                    serverIsoFromDto?.let { Instant.parse(it).toEpochMilli() } ?: System.currentTimeMillis()
                                } catch (_: Throwable) {
                                    System.currentTimeMillis()
                                }

// set VM authoritative epoch before any observers can emit stale values
                                _cacheEpoch.value = serverEpoch

                                // set VM authoritative epoch before any observers can emit stale values

                                // compute json and serverEpoch as before
                                val entity = ProfileCacheEntity(
                                    userId = userId,
                                    json = json,
                                    lastUpdatedMillis = serverEpoch
                                )

                                // atomic upsert + prune
                                try {
                                    profileCacheDao.upsertAndPrune(entity)
                                    Log.d("upsert", entity.toString())
                                    Log.d("ProfileVM", "submitProfile: upsertAndPrune completed for userId=$userId epoch=$serverEpoch")
                                } catch (e: Throwable) {
                                    Log.w("ProfileVM", "submitProfile: upsertAndPrune failed: ${e.message}")
                                }



                                // read-back immediately and reapply to VM state (so UI shows updated cache)
                                try {
                                    val saved = profileCacheDao.getProfileCacheOnce(userId)
                                    if (saved == null) Log.w("ProfileVM", "submitProfile: persisted entity reported OK but read-back returned null for userId=$userId")

                                    Log.d("upsert1", saved?.toString() ?: "saved==null")
                                    saved?.let { e ->
                                        try {
                                            if (e.lastUpdatedMillis > 0L) {
                                                _cacheEpoch.value = e.lastUpdatedMillis
                                                Log.d("ProfileVM", "submitProfile: cacheEpoch from saved.lastUpdatedMillis=${e.lastUpdatedMillis}")
                                            }
                                        } catch (_: Throwable) { /* ignore */ }

                                        try {
                                            val parsed = cacheAdapter.fromJson(e.json)
                                            if (parsed != null) {
                                                _profileCacheDto.value = parsed
                                                withContext(Dispatchers.Main) {
                                                    try {
                                                        // prefill only if saved epoch is newer than previously applied epoch,
                                                        // but we just set _cacheEpoch above so prefillFromCache will respect that guard.
                                                        prefillFromCache(parsed)
                                                    } catch (inner: Throwable) {
                                                        Log.w("ProfileVM", "submitProfile: prefillFromCache failed on Main: ${inner.message}")
                                                    }
                                                }
                                            }
                                        } catch (inner: Throwable) {
                                            Log.w("ProfileVM", "submitProfile: failed to parse saved cache json: ${inner.message}")
                                        }
                                    }
                                } catch (dbReadErr: Throwable) {
                                    Log.w("ProfileVM", "submitProfile: failed to read back cache: ${dbReadErr.message}")
                                }
                            }
                        } catch (t: Throwable) {
                            Log.w("ProfileVM", "Failed to update local cache: ${t.message}")
                        }
                        _state.value = _state.value.copy(loading = false, error = null)
                    }

                    is NetworkResult.Error -> _state.value = _state.value.copy(loading = false, error = result.message ?: "Profile update failed")
                }
            } catch (t: Throwable) {
                Log.e("ProfileVM", "submitProfile: unexpected", t)
                _state.value = _state.value.copy(loading = false, error = t.message ?: "Unexpected error")
            }
        }
    }

    private fun buildCacheDtoFromUser(user: User): ProfileCacheDto {
        val data = CacheUserDto(
            id = user.id.takeIf { it.isNotBlank() },
            fullName = user.fullName?.takeIf { it.isNotBlank() },
            dob = user.dob,
            gender = user.gender,
            profileDescription = user.profileDescription,
            bio = user.bio,
            instaId = user.instagramId,
            twitterId = user.twitterId,
            youtubeId = user.youtubeId,
            facebookId = user.facebookId,
            phone = user.phone,
            profilePhotos = user.photos ?: emptyList(),
            profileVideos = user.videos ?: emptyList(),
            priceMin = user.priceMin as Long?,
            priceMax = user.priceMax as Long?,
            pricingType = user.pricingType,
            standardPrice = user.standardPrice as Long?,
            travelRadius = user.travelRadius as Int?,
            isEventManager = user.isEventManager,
            interests = user.interests
        )
        return ProfileCacheDto(data = data)
    }


    // Helper: parse common ISO timestamp to epoch millis (defensive)
    private fun parseIsoToEpoch(iso: String?): Long? {
        if (iso.isNullOrBlank()) return null
        return try {
            Instant.parse(iso).toEpochMilli()
        } catch (_: Throwable) {
            null
        }
    }

    private fun anyToLongSafe(v: Any?): Long? = when (v) {
        null -> null
        is Long -> v
        is Int -> v.toLong()
        is Short -> v.toLong()
        is Double -> v.toLong()
        is Float -> v.toLong()
        is Number -> v.toLong()
        is String -> v.toLongOrNull()
        else -> null
    }

    private fun anyToIntSafe(v: Any?): Int? = when (v) {
        null -> null
        is Int -> v
        is Long -> v.toInt()
        is Short -> v.toInt()
        is Double -> v.toInt()
        is Float -> v.toInt()
        is Number -> v.toInt()
        is String -> v.toIntOrNull()
        else -> null
    }

    private fun buildCacheDtoFromState(state: ProfileUiState): ProfileCacheDto {
        val data = CacheUserDto(
            id = cachedUserId ?: state.mobile,
            fullName = state.name.takeIf { it.isNotBlank() },
            dob = state.dob.takeIf { it.isNotBlank() },
            gender = state.gender,
            profileDescription = state.description.takeIf { it.isNotBlank() },
            bio = state.biography.takeIf { it.isNotBlank() },
            instaId = state.instaId,
            twitterId = state.twitterId,
            youtubeId = state.youtubeId,
            facebookId = state.facebookId,
            phone = state.mobile,
            interests = state.interests.toList(),
            profilePhotos = state.photos.map { it.toString() },
            profileVideos = state.videos.map { it.toString() },
            photo = state.profilePicUri?.toString(),
            pricingType = state.pricingType.takeIf { it.isNotBlank() },
            standardPrice = state.standardPrice.takeIf { it.isNotBlank() }?.toLongOrNull(),
            priceMin = state.minPrice.takeIf { it.isNotBlank() }?.toLongOrNull(),
            priceMax = state.maxPrice.takeIf { it.isNotBlank() }?.toLongOrNull(),
            travelRadius = state.travelRadiusKm.takeIf { it.isNotBlank() }?.toIntOrNull(),
            isEventManager = state.isEventManager
        )

        return ProfileCacheDto(data = data)
    }

    private fun toUriOrNull(input: Any?): Uri? {
        return when (input) {
            null -> null
            is Uri -> input
            is String -> try { Uri.parse(input) } catch (_: Exception) { null }
            else -> try { Uri.parse(input.toString()) } catch (_: Exception) { null }
        }
    }

    fun refreshCacheDump() {
        viewModelScope.launch(ioDispatcher) {
            try {
                // read table rows + per-user summary from DAO
                val rows = profileCacheDao.getAllCacheRows()
                val summary = try { profileCacheDao.getCacheSummary() } catch (_: Throwable) { emptyList<com.example.museapp.data.local.dao.ProfileCacheDao.CacheSummary>() }

                val sb = StringBuilder()
                sb.appendLine("profile_cache rows: count=${rows.size}")
                rows.forEachIndexed { idx, r ->
                    sb.appendLine("---- row #${idx + 1} ----")
                    sb.appendLine("userId: ${r.userId}")
                    sb.appendLine("lastUpdatedMillis: ${r.lastUpdatedMillis}")
                    // try to parse DTO for quick human fields (defensive)
                    val parsed = try { cacheAdapter.fromJson(r.json) } catch (_: Throwable) { null }
                    sb.appendLine("dto.id=${parsed?.data?.id} dto.fullName=${parsed?.data?.fullName}")
                    // include a truncated json to avoid huge strings in UI/logs
                    val truncated = if (r.json.length > 2000) r.json.take(2000) + "...(truncated)" else r.json
                    sb.appendLine("json: $truncated")
                }

                sb.appendLine()
                sb.appendLine("per-user summary:")
                summary.forEach { s ->
                    sb.appendLine("user=${s.user_id} count=${s.cnt} latest_epoch=${s.latest_epoch}")
                }

                _cacheDump.value = sb.toString()
            } catch (t: Throwable) {
                _cacheDump.value = "refreshCacheDump failed: ${t.message}"
            }
        }
    }


    private fun readStringPropertyByName(target: Any?, vararg names: String): String? {
        if (target == null) return null
        return try {
            val prop = target::class.memberProperties.firstOrNull { pn ->
                names.any { name -> pn.name.equals(name, ignoreCase = true) }
            }
            (prop as? KCallable<*>)?.call(target) as? String
        } catch (_: Throwable) {
            null
        }
    }
}

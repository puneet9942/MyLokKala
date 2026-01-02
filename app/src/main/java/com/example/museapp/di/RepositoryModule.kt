package com.example.museapp.di

import android.content.Context
import androidx.room.Room
import com.example.museapp.BuildConfig
import com.example.museapp.data.mocks.FakeAuthRepository
import com.example.museapp.data.mocks.FakeFavoritesRepository
import com.example.museapp.data.mocks.FakeHomeRepository
import com.example.museapp.data.remote.ApiService
import com.example.museapp.data.repository.AuthRepositoryImpl
import com.example.museapp.data.repository.FavoritesRepositoryImpl
import com.example.museapp.data.repository.HomeRepositoryImpl
import com.example.museapp.data.store.FavoritesStore
import com.example.museapp.domain.repository.AuthRepository
import com.example.museapp.domain.repository.FavoritesRepository
import com.example.museapp.domain.repository.HomeRepository
import com.example.museapp.domain.repository.InterestsRepository
import com.example.museapp.data.local.dao.InterestsDao
import com.example.museapp.data.local.dao.UserDao
import com.example.museapp.data.repository.FeedbackRepositoryImpl
import com.example.museapp.di.NetworkModule.provideMoshi
import com.example.museapp.domain.repository.FeedbackRepository
import com.example.museapp.util.AppConstants
import com.example.museapp.util.AppConstants.USE_FAKE_REPO
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideProfileRepository(
        @ApplicationContext context: Context,
        api: ApiService,
        userDao: UserDao
    ): com.example.museapp.domain.repository.ProfileRepository =

            // Match the constructor variant in your code that expects (api, context, dispatcher, moshi, userDao)
            com.example.museapp.data.repository.ProfileRepositoryImpl(
                api,
                context,
                Dispatchers.IO,
                provideMoshi(),
                userDao
            )



    @Provides
    @Singleton
    fun provideUserFavoritesRepository(
        @ApplicationContext context: Context,
        api: ApiService
    ): com.example.museapp.domain.repository.UserFavoritesRepository =
        com.example.museapp.data.repository.UserFavoritesRepositoryImpl(api, Dispatchers.IO)


    @Provides
    @Singleton
    fun provideFeedbackRepository(
        @ApplicationContext context: Context,
        api: ApiService
    ): FeedbackRepository = FeedbackRepositoryImpl(api, Dispatchers.IO)

    // provide UserStore
    @Provides
    @Singleton
    fun provideUserStore(@ApplicationContext context: Context): com.example.museapp.data.store.UserStore {
        return com.example.museapp.data.store.UserStore(context)
    }

    // Provide Room database
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): com.example.museapp.data.local.AppDatabase {
        return Room.databaseBuilder(
            context,
            com.example.museapp.data.local.AppDatabase::class.java,
            "museapp-db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideUserDao(db: com.example.museapp.data.local.AppDatabase): com.example.museapp.data.local.dao.UserDao =
        db.userDao()

    @Provides
    @Singleton
    fun provideInterestDao(db: com.example.museapp.data.local.AppDatabase): InterestsDao =
        db.interestsDao()

    @Provides
    @Singleton
    fun provideInterestsRepository(
        api: ApiService,
        dao: InterestsDao
    ): InterestsRepository {
        return com.example.museapp.data.repository.InterestsRepositoryImpl(api, dao)
    }

    /**
     * Provide AuthRepository.
     * NOTE: Accept InterestsRepository and pass it to AuthRepositoryImpl so it can launch background fetch.
     */
    @Provides
    @Singleton
    fun provideAuthRepository(
        @ApplicationContext ctx: Context,
        api: ApiService,
        interestsRepo: com.example.museapp.domain.repository.InterestsRepository,
        tokenStore: com.example.museapp.data.store.TokenStore,
        userStore: com.example.museapp.data.store.UserStore
    ): AuthRepository {
        return if (AppConstants.USE_FAKE_REPO) {
            FakeAuthRepository(ctx, shouldFail = false, networkDelayMs = 150L)
        } else {
            AuthRepositoryImpl(api, interestsRepo, userStore, tokenStore, Dispatchers.IO)
        }
    }

    @Provides
    @Singleton
    fun provideHomeRepository(
        @ApplicationContext context: Context,
        api: ApiService
    ): HomeRepository = if (AppConstants.USE_FAKE_REPO) {
        FakeHomeRepository(context)
    } else {
        HomeRepositoryImpl(api, Dispatchers.IO)
    }

    @Provides
    @Singleton
    fun provideFavoritesStore(@ApplicationContext context: Context): FavoritesStore =
        FavoritesStore(context)

    @Provides
    @Singleton
    fun provideFavoritesRepository(
        @ApplicationContext context: Context,
        api: ApiService
    ): FavoritesRepository = if (AppConstants.USE_FAKE_REPO) {
        FakeFavoritesRepository(context)
    } else {
        FavoritesRepositoryImpl(api, Dispatchers.IO)
    }
}

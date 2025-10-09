package com.example.lokkala.di

import android.content.Context
import com.example.lokkala.BuildConfig
import com.example.lokkala.data.remote.ApiService
import com.example.lokkala.data.repository.AuthRepositoryImpl
import com.example.lokkala.data.mocks.FakeAuthRepository
import com.example.lokkala.data.mocks.FakeHomeRepository
import com.example.lokkala.data.repository.HomeRepositoryImpl
import com.example.lokkala.domain.repository.AuthRepository
import com.example.lokkala.domain.repository.HomeRepository
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
    fun provideAuthRepository(
        @ApplicationContext context: Context,
        api: ApiService
    ): AuthRepository =
        if (BuildConfig.USE_FAKE_REPO) FakeAuthRepository(context)
        else AuthRepositoryImpl(api, Dispatchers.IO)


    @Module
    @InstallIn(SingletonComponent::class)
    object RepositoryModule {

        @Provides
        @Singleton
        fun provideHomeRepository(
            @ApplicationContext context: Context,
            api: ApiService
        ): HomeRepository = if (BuildConfig.USE_FAKE_REPO) {
            FakeHomeRepository(context)
        } else {
            HomeRepositoryImpl(api, Dispatchers.IO)
        }
    }
}
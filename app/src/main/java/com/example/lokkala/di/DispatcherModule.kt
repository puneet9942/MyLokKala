package com.example.lokkala.di


import com.example.lokkala.data.util.DefaultDispatchersProvider
import com.example.lokkala.data.util.DispatchersProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {
    @Provides @Singleton
    fun provideDispatchers(): DispatchersProvider = DefaultDispatchersProvider()
}
package com.example.museapp.di

import android.content.Context
import android.content.SharedPreferences
import com.example.museapp.BuildConfig
import com.example.museapp.data.remote.ApiService
import com.example.museapp.data.store.TokenStore
import com.example.museapp.data.util.AppErrorBroadcaster
import com.example.museapp.data.util.DefaultAppErrorBroadcaster
import com.example.museapp.data.util.DefaultGlobalErrorHandler
import com.example.museapp.data.util.GlobalErrorHandler
import com.example.museapp.network.LocationInjectorInterceptor
import com.example.museapp.util.AppConstants
import com.example.museapp.util.AppContextProvider
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val PREFS_NAME = "museapp_auth_prefs"



    @Provides
    @Singleton
    fun provideSharedPreferences( @ApplicationContext appContext: Context): SharedPreferences {
        // initialize global context holder so static builders can read prefs safely
        AppContextProvider.init(appContext)
        return appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideTokenStore(@ApplicationContext appContext: Context): TokenStore = TokenStore(appContext)

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenStore: TokenStore): AuthInterceptor {
        // If you want to customize exclusions, pass them in constructor
        return AuthInterceptor(tokenStore)
    }

    @Provides
    fun provideNetworkLogger(): NetworkLoggingInterceptor =
        NetworkLoggingInterceptor(enabled = BuildConfig.DEBUG)

    @Provides
    @Singleton
    fun provideLocationInjector(@ApplicationContext appContext: Context): LocationInjectorInterceptor {
        // Minimal constructor: uses appContext and internal cached fallback.
        return LocationInjectorInterceptor(appContext)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        logging: NetworkLoggingInterceptor,
        locationInjector: LocationInjectorInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)   // MUST be before location injector so headers are present
            .addInterceptor(locationInjector)  // inject real location into JSON "location" objects
            .addInterceptor(logging)           // logging last so it logs final outgoing request
            // keep existing timeouts or add any additional configuraton here
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
        .build()



    @Provides @Singleton
    fun provideRetrofit(client: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl(AppConstants.BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides @Singleton
    fun provideApi(retrofit: Retrofit): ApiService = retrofit.create(ApiService::class.java)

    @Provides
    @Singleton
    fun provideAppErrorBroadcaster(): AppErrorBroadcaster = DefaultAppErrorBroadcaster()

    @Provides
    @Singleton
    fun provideGlobalErrorHandler(broadcaster: AppErrorBroadcaster): GlobalErrorHandler =
        DefaultGlobalErrorHandler(broadcaster)
}

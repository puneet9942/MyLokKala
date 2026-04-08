package com.example.museapp.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BackendRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SerpRetrofit
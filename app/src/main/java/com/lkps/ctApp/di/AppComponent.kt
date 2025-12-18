package com.lkps.ctApp.di

import android.content.Context
import com.lkps.ctApp.App
import com.lkps.ctApp.data.repository.NotificationRepository
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AndroidSupportInjectionModule::class,
        AppModule::class,
        NetworkModule::class,
        MainActivityModule::class
    ]
)
interface AppComponent : AndroidInjector<App> {

    fun notificationRepository(): NotificationRepository

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance applicationContext: Context): AppComponent
    }
}
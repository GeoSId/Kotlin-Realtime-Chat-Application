package com.lkps.ctApp

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.lkps.ctApp.di.AppComponent
import com.lkps.ctApp.di.DaggerAppComponent
import dagger.android.AndroidInjector
import dagger.android.support.DaggerApplication

class App : DaggerApplication(), LifecycleObserver {

    private lateinit var _appComponent: AppComponent

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        _appComponent = DaggerAppComponent.factory().create(applicationContext)
        return _appComponent
    }

    companion object {
        lateinit var appContext: Context
            private set

        var tempReceiverId: String? = null
        var receiverId: String? = null

        /**
         * Returns the Dagger AppComponent for accessing dependencies.
         * Should only be used in cases where constructor injection is not possible.
         */
        val appComponent: AppComponent
            get() = (appContext.applicationContext as App)._appComponent
    }

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        appContext = applicationContext
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onMoveToForeground() {
        receiverId = tempReceiverId
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onMoveToBackground() {
        tempReceiverId = receiverId
        receiverId = null
    }
}
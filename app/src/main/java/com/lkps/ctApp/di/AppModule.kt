package com.lkps.ctApp.di

import com.lkps.ctApp.controllers.device.DeviceController
import com.lkps.ctApp.data.remote.FcmApiService
import com.lkps.ctApp.data.repository.NotificationRepository
import com.lkps.ctApp.data.repository.NotificationRepositoryImpl
import com.lkps.ctApp.data.repository.Repository
import com.lkps.ctApp.data.repository.RepositoryImpl
import com.lkps.ctApp.data.source.firebase.FirebaseDaoImpl
import com.lkps.ctApp.di.viewModel.ViewModelModule
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module(includes = [(ViewModelModule::class)])
object AppModule {

    @JvmStatic
    @Singleton
    @Provides
    fun provideRepository(
        firebaseDaoImpl: FirebaseDaoImpl,
        ioDispatcher: CoroutineDispatcher
    ): Repository {
        return RepositoryImpl(firebaseDaoImpl)
    }

    @JvmStatic
    @Singleton
    @Provides
    fun provideNotificationRepository(
        fcmApiService: FcmApiService,
        ioDispatcher: CoroutineDispatcher
    ): NotificationRepository {
        return NotificationRepositoryImpl(fcmApiService, ioDispatcher)
    }

    @JvmStatic
    @Singleton
    @Provides
    fun provideFirebaseDaoImpl(): FirebaseDaoImpl {
        return FirebaseDaoImpl()
    }

    @JvmStatic
    @Singleton
    @Provides
    fun provideIoDispatcher() = Dispatchers.IO

    @JvmStatic
    @Singleton
    @Provides
    fun provideDeviceController(): DeviceController {
        return DeviceController()
    }
}

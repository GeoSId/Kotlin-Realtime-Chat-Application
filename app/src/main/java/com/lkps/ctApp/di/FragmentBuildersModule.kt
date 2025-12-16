package com.lkps.ctApp.di

import com.lkps.ctApp.view.ManagerFragment
import com.lkps.ctApp.view.chatRoom.ChatFragment
import com.lkps.ctApp.view.chatRooms.StartFragment
import com.lkps.ctApp.view.searchUser.SearchFragment
import com.lkps.ctApp.view.userProfile.UsernameFragment

import dagger.Module
import dagger.android.ContributesAndroidInjector

@Suppress("unused")
@Module
abstract class FragmentBuildersModule {
    @ContributesAndroidInjector
    abstract fun contributeChatFragment(): ChatFragment

    @ContributesAndroidInjector
    abstract fun contributeStartFragment(): StartFragment

    @ContributesAndroidInjector
    abstract fun contributeSearchFragment(): SearchFragment

    @ContributesAndroidInjector
    abstract fun contributeUsernameFragment(): UsernameFragment

    @ContributesAndroidInjector
    abstract fun contributeManagerFragment(): ManagerFragment
}

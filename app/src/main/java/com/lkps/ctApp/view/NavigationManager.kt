package com.lkps.ctApp.view

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import com.lkps.ct.R
import com.lkps.ct.databinding.ActivityMainBinding
import com.lkps.ctApp.App
import com.lkps.ctApp.utils.states.FragmentState
import com.lkps.ctApp.view.chatRoom.ChatFragment
import com.lkps.ctApp.view.chatRooms.StartFragment
import com.lkps.ctApp.view.searchUser.SearchFragment
import com.lkps.ctApp.view.userProfile.UsernameFragment

class NavigationManager(activity: AppCompatActivity, val binding: ActivityMainBinding) {

    private val navOptions = NavOptions.Builder().setLaunchSingleTop(true).build()
    private val uriUsername = Uri.parse("atr:fragment_username")
    private val uriChat = Uri.parse("atr:fragment_chat")
    private val uriSearch = Uri.parse("atr:fragment_search")
    private val uriStart = Uri.parse("atr:fragment_start")
    private val uriManager = Uri.parse("atr:fragment_manager")

    object FragmentLabel {
        val USERNAME = UsernameFragment::class.java.simpleName
        val START = StartFragment::class.java.simpleName
        val CHAT = ChatFragment::class.java.simpleName
        val SEARCH = SearchFragment::class.java.simpleName
        val MANAGER = ManagerFragment::class.java.simpleName
    }

    private val doNothing = Any()
    private val navController: NavController

    init {
        navController = activity.findNavController(R.id.nav_host_fragment)
        Navigation.setViewNavController(binding.root, navController)
        addOnDestinationChangedListener(binding.root.findNavController())
    }

    fun onFragmentStateChange(fragmentState: FragmentState) = when (fragmentState) {
        FragmentState.USERNAME -> navigateTo(uriUsername, navOptions)
        FragmentState.START -> navigateTo(uriStart, navOptions)
        FragmentState.CHAT -> navigateTo(uriChat, navOptions)
        FragmentState.MANAGER -> navigateTo(uriManager, navOptions)
        FragmentState.SEARCH -> {

            navigateTo(uriSearch, navOptions)
        }
        else -> throw Exception()
    }

    fun onBack(activity: AppCompatActivity, viewMoDel:FirebaseViewModel) {
        val currentFr =navController.navigateUpOrFinish(activity)
        if(StartFragment::class.java.simpleName.equals(currentFr)){
            viewMoDel.setFragmentState(FragmentState.START)
        }else if (ChatFragment::class.java.simpleName.equals(currentFr)){
            activity.finish()
        }else if (SearchFragment::class.java.simpleName.equals(currentFr)){
            viewMoDel.setFragmentState(FragmentState.START)
        }
    }

    private fun NavController.navigateUpOrFinish(activity: AppCompatActivity): String {
        return if (navigateUp()) {
            val currentDestination :String = this.currentDestination?.label.toString()
            currentDestination
        } else {
            activity.finish()
            ""
        }
    }

    private fun addOnDestinationChangedListener(navController: NavController) {
        navController.addOnDestinationChangedListener(getOnDestinationChangedListener())
    }

    private fun getOnDestinationChangedListener(): NavController.OnDestinationChangedListener {
        return NavController.OnDestinationChangedListener { controller, destination, arguments ->
            onDestinationChanged(destination)
        }
    }

    private fun onDestinationChanged(destination: NavDestination) = when (destination.label) {
        FragmentLabel.CHAT -> doNothing
        else -> App.receiverId = null
    }

    private fun navigateTo(uri: Uri, navOptions: NavOptions) {
        binding.root.findNavController().navigate(uri, navOptions)
    }
}
package com.lkps.ctApp.view

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.doOnAttach
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.auth.auth
import com.google.firebase.Firebase
import com.lkps.ct.R
import com.lkps.ct.databinding.ActivityMainBinding
import com.lkps.ctApp.controllers.device.DeviceController
import com.lkps.ctApp.controllers.locale.LocaleController
import com.lkps.ctApp.data.model.Message
import com.lkps.ctApp.data.source.firebase.FirebaseDaoImpl
import com.lkps.ctApp.utils.Constant.NOTIFICATION_INTENT
import com.lkps.ctApp.utils.IntentManager.galleryAddPic
import com.lkps.ctApp.utils.IntentManager.getFileExtensionFromUri
import com.lkps.ctApp.utils.IntentManager.getTypeFromUri
import com.lkps.ctApp.utils.convertFromString
import com.lkps.ctApp.utils.states.AuthenticationState
import com.lkps.ctApp.utils.states.FragmentState
import dagger.android.support.DaggerAppCompatActivity
import java.util.*
import javax.inject.Inject

class MainActivity : DaggerAppCompatActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: ActivityMainBinding
    private val navigationManager by lazy { NavigationManager(this, binding) }
    private val firebaseVm: FirebaseViewModel by viewModels { viewModelFactory }

    private var waitForResultFromSignIn = false
    private var myMenu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleController.setLocale(resources)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        
        // Initialize navigation manager and start app when view is ready
        binding.root.doOnAttach { 
            navigationManager
            startApp()
        }

        val user = Firebase.auth.currentUser
        user?.let {
            // Name, email address, and profile photo Url
            val name = user.displayName
            val email = user.email
            val photoUrl = user.photoUrl
            // Check if user's email is verified
            val emailVerified = user.isEmailVerified
            // The user's ID, unique to the Firebase project. Do NOT use this value to
            // authenticate with your backend server, if you have one. Use
            // FirebaseUser.getToken() instead.
            val uid = user.uid
        }

        onBackPressedDispatcher.addCallback(this /* lifecycle owner */, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {

            }
        })

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Do custom work here
                    navigationManager.onBack(this@MainActivity, firebaseVm)
                    // if you want onBackPressed() to be called as normal afterwards
                    if (isEnabled) {
                       // isEnabled = false  this disable the backPressed
                    }
                }
            }
        )
    }

    override fun onResume() {
        super.onResume()
        val deviceController = DeviceController()
        deviceController.logOutAll(applicationContext)
    }

    private fun startApp() {
        firebaseVm.fetchConfigs(this)
        observeAuthState()
        observeFragmentState()

        if (intent != null) {
            checkNotificationIntent()
            firebaseVm.setCTIntent(intent)
        }

        supportActionBar?.hide()
        checkGooglePlayServices()
    }

    //IF WE DONT TAKE PUSH MAY THIS IS THE PROBLEM
    private fun checkGooglePlayServices(): Boolean {
        // 1
        val status = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
        // 2
        return if (status != ConnectionResult.SUCCESS) {
            Log.e("MainActivity", "Error")
            // ask user to update google play services and manage the error.
            false
        } else {
            // 3
            Log.i("MainActivity", "Google play services updated")
            true
        }
    }

    private fun observeAuthState() {
        firebaseVm.authenticationState.observe(this) { userWithState ->
            when (userWithState.second) {
                is AuthenticationState.Authenticated -> onAuthenticated()
                is AuthenticationState.Unauthenticated -> onUnauthenticated()
                is AuthenticationState.InvalidAuthentication -> {
                    Log.e("MainActivity", "Invalid authentication state")
                }
            }
        }
    }

    private fun observeFragmentState() {
        firebaseVm.fragmentState.observe(this, Observer {
            it?.let {
                if (it.first == FragmentState.START && it.second) {
                    handleMenu(showBack = false, showMenuOptions = true, menuTitle = resources.getString(R.string.app_name))
                } else if (it.first == FragmentState.CHAT && it.second) {
                    handleMenu(showBack = true, showMenuOptions = false, menuTitle = firebaseVm.clientUserName)
                } else if (it.first == FragmentState.MANAGER && it.second) {
                    handleMenu(showMenuOptions = false)
                } else if (it.first == FragmentState.USERNAME && it.second) {
                    handleMenu(showBack = false, showMenuOptions = false)
                } else if (it.first == FragmentState.SEARCH && it.second) {
                    handleMenu(showBack = true, showMenuOptions = false, menuTitle =  resources.getString(R.string.app_name))
                }
                if (!it.second) return@Observer
                navigationManager.onFragmentStateChange(it.first)
            }
        })
    }

    private fun handleMenu(showBack: Boolean = true, showMenuOptions: Boolean = true, menuTitle: String = resources.getString(R.string.app_name)){
        title = menuTitle
        actionBar?.setDisplayUseLogoEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(showBack)
        actionBar?.setHomeButtonEnabled(showBack)
        myMenu?.findItem(R.id.search)?.isVisible = showMenuOptions
        myMenu?.findItem(R.id.sign_out_menu)?.isVisible = showMenuOptions
        myMenu?.findItem(R.id.manger_menu)?.isVisible = showMenuOptions

    }

    private fun onAuthenticated() {
        firebaseVm.onSignIn()
        supportActionBar?.show()
    }

    private fun onUnauthenticated() {
        if (waitForResultFromSignIn.not()) {
            firebaseVm.onSignOut()
            startSignInActivity()
            supportActionBar?.hide()
            waitForResultFromSignIn = true
        }
    }

    private fun checkNotificationIntent() {
        if (intent.hasExtra(NOTIFICATION_INTENT)) {
            val msg: Message = convertFromString(intent.getStringExtra(NOTIFICATION_INTENT) ?: "")?: return
            val name = msg.name
            if(name == null){
                firebaseVm.setFragmentState(FragmentState.START)
            }else{
                firebaseVm.setReceiverFromPush(msg.senderId, name)
                firebaseVm.setFragmentState(FragmentState.CHAT)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FirebaseDaoImpl.RC_SIGN_IN) {
            when (resultCode) {
                Activity.RESULT_OK -> supportActionBar?.show()
                Activity.RESULT_CANCELED -> finish()
            }
            waitForResultFromSignIn = false
        } else if (requestCode == FirebaseDaoImpl.RC_PHOTO_PICKER && resultCode == Activity.RESULT_OK) {
            var uri = Uri.EMPTY
            var fileExtension = ""
            if(data != null){
                uri =  data.data
                fileExtension = getTypeFromUri(data)
            }else{
                uri = galleryAddPic(this)
                fileExtension = getFileExtensionFromUri(uri)
            }
            firebaseVm.pushFile(uri, fileExtension)
        }
    }

    private fun startSignInActivity() {
        val providers = mutableListOf(
            AuthUI
                .IdpConfig
                .EmailBuilder()
                .build()
        )

        val auth = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setTheme(R.style.LoginTheme)
//            .setIsSmartLockEnabled(false)
            .setAvailableProviders(providers)
            .setLogo(R.drawable.ic_launcher)
        startActivityForResult(auth.build(), FirebaseDaoImpl.RC_SIGN_IN)
        firebaseVm.setFragmentState(FragmentState.START)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        if(myMenu == null) {
            myMenu = menu
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.sign_out_menu -> {
                AuthUI.getInstance().signOut(this)
                DeviceController().deleteDeviceToken(this)
                true
            }
            R.id.manger_menu -> {
                firebaseVm.setFragmentState(FragmentState.MANAGER)
                true
            }
            R.id.search -> {
                firebaseVm.setFragmentState(FragmentState.SEARCH)
                true
            }
            else -> {
                navigationManager.onBack(this@MainActivity, firebaseVm)
                true
            }
        }
    }

    private fun showMessageDialog(
        context: Context?,
        message: String,
        @StringRes positive: Int?,
        positiveListener: DialogInterface.OnClickListener?
    ) {
        val builder = context?.let { AlertDialog.Builder(it, R.style.AlertDialogStyle) }
        builder?.setCancelable(false)
        builder?.setTitle(R.string.app_name)
        builder?.setMessage(message)
        positive?.let { builder?.setPositiveButton(it, positiveListener) }
        builder?.show()
    }
}

package com.lkps.ctApp.view

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.doOnAttach
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.lkps.ct.R
import com.lkps.ct.databinding.ActivityMainBinding
import com.lkps.ctApp.controllers.device.DeviceController
import com.lkps.ctApp.data.model.Message
import com.lkps.ctApp.utils.Constant.NOTIFICATION_INTENT
import com.lkps.ctApp.utils.IntentManager.galleryAddPic
import com.lkps.ctApp.utils.IntentManager.getFileExtensionFromUri
import com.lkps.ctApp.utils.IntentManager.getTypeFromUri
import com.lkps.ctApp.utils.convertFromString
import com.lkps.ctApp.utils.states.AuthenticationState
import com.lkps.ctApp.utils.states.FragmentState
import dagger.android.support.DaggerAppCompatActivity
import javax.inject.Inject

class MainActivity : DaggerAppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var deviceController: DeviceController

    private lateinit var binding: ActivityMainBinding
    private val navigationManager by lazy { NavigationManager(this, binding) }
    private val firebaseVm: FirebaseViewModel by viewModels { viewModelFactory }

    private var waitForResultFromSignIn = false
    private var myMenu: Menu? = null

    // Activity Result API launcher for sign-in
    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        handleSignInResult(result)
    }

    // Activity Result API launcher for photo picker
    private val photoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        handlePhotoPickerResult(result)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        setupBackPressHandler()
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    navigationManager.onBack(this@MainActivity, firebaseVm)
                }
            }
        )
    }

    override fun onResume() {
        super.onResume()
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

    private fun checkGooglePlayServices(): Boolean {
        val status = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
        return if (status != ConnectionResult.SUCCESS) {
            Log.e(TAG, "Google Play Services error")
            false
        } else {
            Log.i(TAG, "Google Play Services updated")
            true
        }
    }

    private fun observeAuthState() {
        firebaseVm.authenticationState.observe(this) { userWithState ->
            when (userWithState.second) {
                is AuthenticationState.Authenticated -> onAuthenticated()
                is AuthenticationState.Unauthenticated -> onUnauthenticated()
                is AuthenticationState.InvalidAuthentication -> {
                    Log.e(TAG, "Invalid authentication state")
                }
            }
        }
    }

    private fun observeFragmentState() {
        firebaseVm.fragmentState.observe(this) { fragmentStatePair ->
            fragmentStatePair?.let { (state, isActive) ->
                if (isActive) {
                    when (state) {
                        FragmentState.START -> handleMenu(
                            showBack = false,
                            showMenuOptions = true,
                            menuTitle = resources.getString(R.string.app_name)
                        )
                        FragmentState.CHAT -> handleMenu(
                            showBack = true,
                            showMenuOptions = false,
                            menuTitle = firebaseVm.clientUserName
                        )
                        FragmentState.MANAGER -> handleMenu(showMenuOptions = false)
                        FragmentState.USERNAME -> handleMenu(
                            showBack = false,
                            showMenuOptions = false
                        )
                        FragmentState.SEARCH -> handleMenu(
                            showBack = true,
                            showMenuOptions = false,
                            menuTitle = resources.getString(R.string.app_name)
                        )
                        else -> { /* No action needed */ }
                    }
                    navigationManager.onFragmentStateChange(state)
                }
            }
        }
    }

    private fun handleMenu(
        showBack: Boolean = true,
        showMenuOptions: Boolean = true,
        menuTitle: String = resources.getString(R.string.app_name)
    ) {
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
        if (!waitForResultFromSignIn) {
            firebaseVm.onSignOut()
            startSignInActivity()
            supportActionBar?.hide()
            waitForResultFromSignIn = true
        }
    }

    private fun checkNotificationIntent() {
        if (intent.hasExtra(NOTIFICATION_INTENT)) {
            val msg: Message = convertFromString(
                intent.getStringExtra(NOTIFICATION_INTENT) ?: ""
            ) ?: return
            val name = msg.name
            if (name == null) {
                firebaseVm.setFragmentState(FragmentState.START)
            } else {
                firebaseVm.setReceiverFromPush(msg.senderId, name)
                firebaseVm.setFragmentState(FragmentState.CHAT)
            }
        }
    }

    private fun handleSignInResult(result: ActivityResult) {
        when (result.resultCode) {
            RESULT_OK -> supportActionBar?.show()
            RESULT_CANCELED -> finish()
        }
        waitForResultFromSignIn = false
    }

    private fun handlePhotoPickerResult(result: ActivityResult) {
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val uri: Uri
            val fileExtension: String

            if (data != null) {
                uri = data.data ?: Uri.EMPTY
                fileExtension = getTypeFromUri(data)
            } else {
                uri = galleryAddPic(this) ?: Uri.EMPTY
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

        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setTheme(R.style.LoginTheme)
            .setAvailableProviders(providers)
            .setLogo(R.drawable.ic_launcher)
            .build()

        signInLauncher.launch(signInIntent)
        firebaseVm.setFragmentState(FragmentState.START)
    }

    /**
     * Launches the photo picker activity using Activity Result API.
     * Call this method from FirebaseViewModel or wherever photo selection is needed.
     */
    fun launchPhotoPicker(intent: android.content.Intent) {
        photoPickerLauncher.launch(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        if (myMenu == null) {
            myMenu = menu
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.sign_out_menu -> {
                AuthUI.getInstance().signOut(this)
                deviceController.deleteDeviceToken(this)
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
}

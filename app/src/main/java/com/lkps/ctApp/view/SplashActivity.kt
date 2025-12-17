package com.lkps.ctApp.view

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.lkps.ct.R
import com.lkps.ctApp.utils.Utility

class SplashActivity : AppCompatActivity() {

    companion object {
        private const val INITIAL_REQUEST = 1337
        private const val MAX_PERMISSION_RETRIES = 2
        
        private fun getRequiredPermissions(): Array<String> {
            val permissions = mutableListOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA
            )
            
            // Android 14+ (API 34) - Use partial media access permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
                permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
                permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
                permissions.add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            // Android 13 (API 33) requires granular media permissions
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
                permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
                permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                // For Android 12 and below, use legacy storage permissions
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    // WRITE_EXTERNAL_STORAGE is only needed below Android 10
                    permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
            
            return permissions.toTypedArray()
        }
        
        /**
         * Check if the app has media access (full or partial on Android 14+)
         */
        private fun hasMediaAccess(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // On Android 14+, either full access OR partial (user-selected) access is acceptable
                val hasFullAccess = ContextCompat.checkSelfPermission(
                    context, 
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED
                
                val hasPartialAccess = ContextCompat.checkSelfPermission(
                    context, 
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                ) == PackageManager.PERMISSION_GRANTED
                
                hasFullAccess || hasPartialAccess
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context, 
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(
                    context, 
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
    }
    
    private var permissionRetryCount = 0
    private var isRequestingPermissions = false
    private var isActivityResumed = false
    private var openedSettingsForPermissions = false
    private val permissionHandler = Handler(Looper.getMainLooper())
    private var pendingPermissionCheck: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Post to ensure activity is fully ready
        permissionHandler.post {
            checkPermissions()
        }
    }
    
    override fun onResume() {
        super.onResume()
        isActivityResumed = true
        
        // Re-check permissions when returning from app settings
        if (openedSettingsForPermissions) {
            openedSettingsForPermissions = false
            permissionRetryCount = 0
            isRequestingPermissions = false
            cancelPendingPermissionCheck()
            
            if (checkHasPermissions()) {
                startApp()
            } else {
                showPermissionDeniedDialog()
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        isActivityResumed = false
        cancelPendingPermissionCheck()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        permissionHandler.removeCallbacksAndMessages(null)
    }

    private fun startApp() {
        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
        finish()
    }

    private fun checkPermissions() {
        if (isRequestingPermissions || !isActivityResumed) {
            return
        }
        
        val requiredPermissions = getRequiredPermissions()
        val deniedNonMediaPermissions = getDeniedPermissions(requiredPermissions)
        val needsMediaPermission = !hasMediaAccess(this)
        
        val allDeniedPermissions = if (needsMediaPermission) {
            deniedNonMediaPermissions + getMediaPermissionsToRequest()
        } else {
            deniedNonMediaPermissions
        }
        
        if (allDeniedPermissions.isNotEmpty()) {
            val shouldShowRationale = allDeniedPermissions.any { permission ->
                ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
            }
            
            if (shouldShowRationale && permissionRetryCount > 0) {
                showPermissionRationaleDialog(allDeniedPermissions)
            } else {
                requestPermissions(allDeniedPermissions)
            }
        } else {
            startApp()
        }
    }
    
    private fun getDeniedPermissions(permissions: Array<String>): Array<String> {
        return permissions.filter { permission ->
            if (isMediaPermission(permission)) {
                false
            } else {
                ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
            }
        }.toTypedArray()
    }
    
    private fun isMediaPermission(permission: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission == Manifest.permission.READ_MEDIA_IMAGES ||
            permission == Manifest.permission.READ_MEDIA_VIDEO ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && 
                permission == Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        } else {
            permission == Manifest.permission.READ_EXTERNAL_STORAGE ||
            permission == Manifest.permission.WRITE_EXTERNAL_STORAGE
        }
    }
    
    private fun getMediaPermissionsToRequest(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            } else {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }
    
    private fun requestPermissions(permissions: Array<String>) {
        isRequestingPermissions = true
        ActivityCompat.requestPermissions(this, permissions, INITIAL_REQUEST)
    }

    private fun checkHasPermissions(): Boolean {
        val nonMediaPermissionsGranted = getDeniedPermissions(getRequiredPermissions()).isEmpty()
        val hasMedia = hasMediaAccess(this)
        return nonMediaPermissionsGranted && hasMedia
    }

    private fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun cancelPendingPermissionCheck() {
        pendingPermissionCheck?.let { 
            permissionHandler.removeCallbacks(it)
            pendingPermissionCheck = null
        }
    }
    
    private fun schedulePermissionCheck() {
        cancelPendingPermissionCheck()
        
        pendingPermissionCheck = Runnable {
            if (isActivityResumed && !isRequestingPermissions) {
                checkPermissions()
            }
            pendingPermissionCheck = null
        }
        
        permissionHandler.postDelayed(pendingPermissionCheck!!, 1500)
    }
    
    private fun showPermissionRationaleDialog(deniedPermissions: Array<String>) {
        val permissionNames = deniedPermissions.mapNotNull { permission ->
            when (permission) {
                Manifest.permission.RECORD_AUDIO -> getString(R.string.permission_microphone)
                Manifest.permission.CAMERA -> getString(R.string.permission_camera)
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE -> getString(R.string.permission_storage)
                else -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        when (permission) {
                            Manifest.permission.READ_MEDIA_IMAGES -> getString(R.string.permission_photos)
                            Manifest.permission.READ_MEDIA_VIDEO -> getString(R.string.permission_videos)
                            Manifest.permission.READ_MEDIA_AUDIO -> getString(R.string.permission_audio)
                            Manifest.permission.POST_NOTIFICATIONS -> getString(R.string.permission_notifications)
                            else -> null
                        }
                    } else {
                        null
                    }
                }
            }
        }.distinct().joinToString(", ")
        
        isRequestingPermissions = true
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.permissions_required_title)
            .setMessage(getString(R.string.permissions_required_message, permissionNames))
            .setPositiveButton(R.string.grant_permissions) { _, _ ->
                isRequestingPermissions = false
                requestPermissions(deniedPermissions)
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                isRequestingPermissions = false
                showPermissionDeniedDialog()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun showPermissionDeniedDialog() {
        isRequestingPermissions = true
        cancelPendingPermissionCheck()
        
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.permissions_denied_title)
            .setMessage(R.string.permissions_denied_message)
            .setPositiveButton(R.string.open_settings) { _, _ ->
                isRequestingPermissions = false
                openAppSettings()
            }
            .setNegativeButton(R.string.exit_app) { _, _ ->
                isRequestingPermissions = false
                finish()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun openAppSettings() {
        openedSettingsForPermissions = true
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        isRequestingPermissions = false
        
        when (requestCode) {
            INITIAL_REQUEST -> {
                if (checkHasPermissions()) {
                    permissionRetryCount = 0
                    startApp()
                } else {
                    permissionRetryCount++
                    
                    val isAndroid14Plus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                    val mediaPermissionDenied = !hasMediaAccess(this)
                    
                    val permanentlyDenied = permissions.any { permission ->
                        val isDenied = !isPermissionGranted(permission)
                        val cantAskAgain = !ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
                        val isVisualUserSelected = isAndroid14Plus && 
                            permission == Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                        isDenied && cantAskAgain && !isVisualUserSelected
                    }
                    
                    val photoPickerDismissed = isAndroid14Plus && mediaPermissionDenied && permissionRetryCount >= 1
                    
                    if (permanentlyDenied || photoPickerDismissed || permissionRetryCount >= MAX_PERMISSION_RETRIES) {
                        showPermissionDeniedDialog()
                    } else {
                        Utility.makeText(this, getString(R.string.permissions_necessary))
                        schedulePermissionCheck()
                    }
                }
            }
        }
    }
}
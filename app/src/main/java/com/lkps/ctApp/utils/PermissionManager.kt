package com.lkps.ctApp.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

object PermissionManager {

    /**
     * Check if camera permission is granted
     */
    fun isCameraPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Constant.PERMISSION_CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if microphone permission is granted
     */
    fun isMicrophonePermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Constant.PERMISSION_RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if storage permissions are granted
     * Handles different permission requirements based on Android version
     */
    fun isStoragePermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ - granular media permissions
            val hasImagesPermission = ContextCompat.checkSelfPermission(
                context,
                Constant.PERMISSION_READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED

            val hasVideoPermission = ContextCompat.checkSelfPermission(
                context,
                Constant.PERMISSION_READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED

            val hasAudioPermission = ContextCompat.checkSelfPermission(
                context,
                Constant.PERMISSION_READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            val hasVisualUserSelectedPermission = ContextCompat.checkSelfPermission(
                context,
                Constant.PERMISSION_READ_MEDIA_VISUAL_USER_SELECTED
            ) == PackageManager.PERMISSION_GRANTED

            // Allow access if at least one media type permission is granted
            hasImagesPermission || hasVideoPermission || hasAudioPermission || hasVisualUserSelectedPermission
        } else {
            // Android 12 and below - legacy storage permission
            ContextCompat.checkSelfPermission(
                context,
                Constant.PERMISSION_READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Get the appropriate storage permissions to request based on Android version
     */
    fun getStoragePermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ - request all granular media permissions
            arrayOf(
                Constant.PERMISSION_READ_MEDIA_IMAGES,
                Constant.PERMISSION_READ_MEDIA_VIDEO,
                Constant.PERMISSION_READ_MEDIA_AUDIO,
                Constant.PERMISSION_READ_MEDIA_VISUAL_USER_SELECTED
            )
        } else {
            // Android 12 and below - request legacy storage permission
            arrayOf(Constant.PERMISSION_READ_EXTERNAL_STORAGE)
        }
    }

    /**
     * Check and request camera permission if needed
     * Returns true if permission is already granted, false if request was launched
     */
    fun checkAndRequestCameraPermission(
        fragment: Fragment,
        permissionLauncher: ActivityResultLauncher<String>,
        onGranted: () -> Unit,
        onDenied: () -> Unit
    ): Boolean {
        return if (isCameraPermissionGranted(fragment.requireContext())) {
            onGranted()
            true
        } else {
            // Launch permission request
            permissionLauncher.launch(Constant.PERMISSION_CAMERA)
            false
        }
    }

    /**
     * Check and request microphone permission if needed
     * Returns true if permission is already granted, false if request was launched
     */
    fun checkAndRequestMicrophonePermission(
        fragment: Fragment,
        permissionLauncher: ActivityResultLauncher<String>,
        onGranted: () -> Unit,
        onDenied: () -> Unit
    ): Boolean {
        return if (isMicrophonePermissionGranted(fragment.requireContext())) {
            onGranted()
            true
        } else {
            // Launch permission request
            permissionLauncher.launch(Constant.PERMISSION_RECORD_AUDIO)
            false
        }
    }

    /**
     * Check and request storage permissions if needed
     * Returns true if permissions are already granted, false if request was launched
     */
    fun checkAndRequestStoragePermission(
        fragment: Fragment,
        multiplePermissionsLauncher: ActivityResultLauncher<Array<String>>,
        onGranted: () -> Unit,
        onDenied: () -> Unit
    ): Boolean {
        return if (isStoragePermissionGranted(fragment.requireContext())) {
            onGranted()
            true
        } else {
            // Launch permission request
            multiplePermissionsLauncher.launch(getStoragePermissions())
            false
        }
    }

    /**
     * Legacy method for backward compatibility - use checkAndRequestCameraPermission instead
     */
    @Deprecated("Use checkAndRequestCameraPermission with pre-registered launcher instead")
    fun requestCameraPermission(
        fragment: Fragment,
        onGranted: () -> Unit,
        onDenied: () -> Unit
    ) {
        if (isCameraPermissionGranted(fragment.requireContext())) {
            onGranted()
            return
        }

        val requestPermissionLauncher = fragment.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                onGranted()
            } else {
                onDenied()
            }
        }

        requestPermissionLauncher.launch(Constant.PERMISSION_CAMERA)
    }

    /**
     * Legacy method for backward compatibility - use checkAndRequestMicrophonePermission instead
     */
    @Deprecated("Use checkAndRequestMicrophonePermission with pre-registered launcher instead")
    fun requestMicrophonePermission(
        fragment: Fragment,
        onGranted: () -> Unit,
        onDenied: () -> Unit
    ) {
        if (isMicrophonePermissionGranted(fragment.requireContext())) {
            onGranted()
            return
        }

        val requestPermissionLauncher = fragment.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                onGranted()
            } else {
                onDenied()
            }
        }

        requestPermissionLauncher.launch(Constant.PERMISSION_RECORD_AUDIO)
    }

    /**
     * Legacy method for backward compatibility - use checkAndRequestStoragePermission instead
     */
    @Deprecated("Use checkAndRequestStoragePermission with pre-registered launcher instead")
    fun requestStoragePermission(
        fragment: Fragment,
        onGranted: () -> Unit,
        onDenied: () -> Unit
    ) {
        if (isStoragePermissionGranted(fragment.requireContext())) {
            onGranted()
            return
        }

        val requestMultiplePermissionsLauncher = fragment.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.values.all { it }
            if (allGranted) {
                onGranted()
            } else {
                onDenied()
            }
        }

        requestMultiplePermissionsLauncher.launch(getStoragePermissions())
    }

    /**
     * Handle permission rationale - check if we should show rationale
     */
    fun shouldShowRationale(activity: Activity, permission: String): Boolean {
        return androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    /**
     * Check if permission is permanently denied (user checked "Don't ask again")
     */
    fun isPermissionPermanentlyDenied(activity: Activity, permission: String): Boolean {
        return !androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(activity, permission) &&
                ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED
    }
}
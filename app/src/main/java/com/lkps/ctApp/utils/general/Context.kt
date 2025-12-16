package com.lkps.ctApp.utils.general

import android.app.Activity
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Context.VIBRATOR_SERVICE
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

fun Context?.isAvailable(): Boolean {
    if (this == null) {
        return false
    } else if (this !is Application) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            if (this is FragmentActivity) {
                return !this.isDestroyed
            } else if (this is Activity) {
                return !this.isDestroyed
            }
        }
    }
    return true
}

fun Context?.hideKeyboard() {
    var view = (this as? AppCompatActivity)?.currentFocus
    if (view == null) view = View(this)
    val imm = this?.getSystemService(Activity.INPUT_METHOD_SERVICE) as? InputMethodManager
            ?: return
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}

fun Context?.hideKeyboard(view: View) {
    val imm = this?.getSystemService(Activity.INPUT_METHOD_SERVICE) as? InputMethodManager
            ?: return
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}

fun Context?.showKeyboard() {
    var view = (this as? AppCompatActivity)?.currentFocus
    if (view == null) view = View(this)
    val imm = this?.getSystemService(Activity.INPUT_METHOD_SERVICE) as? InputMethodManager
            ?: return
    imm.showSoftInput(view, InputMethodManager.SHOW_FORCED)
}

fun Context?.showKeyboard(view: View) {
    val imm = this?.getSystemService(Activity.INPUT_METHOD_SERVICE) as? InputMethodManager
            ?: return
    imm.showSoftInput(view, InputMethodManager.SHOW_FORCED)
}

fun Context?.isKeyboardOpen(): Boolean {
    var view = (this as? AppCompatActivity)?.currentFocus
    if (view == null) view = View(this)
    val imm = this?.getSystemService(Activity.INPUT_METHOD_SERVICE) as? InputMethodManager
            ?: return false
    return imm.isAcceptingText
}

fun Context.getScreenWidth(): Int =
        this.resources.displayMetrics.widthPixels

fun Context.getScreenHeight(): Int =
        this.resources.displayMetrics.heightPixels

fun Activity.updateStatusBarColor(@ColorRes color: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        window?.apply {
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = ContextCompat.getColor(this@updateStatusBarColor, color)
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        window?.apply {
            decorView.apply {
                systemUiVisibility = when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                    else -> View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                }
            }
        }
    }
}

fun Activity.updateNavigationBarColor(@ColorRes color: Int) {
    window?.apply {
        navigationBarColor = ContextCompat.getColor(this@updateNavigationBarColor, color)
    }
}

fun Context.dpTopixel(@DimenRes dimen: Int): Float =
        this.resources.getDimension(dimen) * this.resources.displayMetrics.density

fun Context.pixelTodp(pixel: Float): Float =
        pixel / this.resources.displayMetrics.density

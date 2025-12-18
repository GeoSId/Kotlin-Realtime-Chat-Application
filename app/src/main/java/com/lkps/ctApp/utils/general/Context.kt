package com.lkps.ctApp.utils.general

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager

import androidx.appcompat.app.AppCompatActivity
fun Context?.hideKeyboard() {
    var view = (this as? AppCompatActivity)?.currentFocus
    if (view == null) view = View(this)
    val imm = this?.getSystemService(Activity.INPUT_METHOD_SERVICE) as? InputMethodManager
            ?: return
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}
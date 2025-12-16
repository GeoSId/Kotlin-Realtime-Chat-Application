package com.lkps.ctApp.controllers.device

import android.content.Context
import com.firebase.ui.auth.AuthUI
import java.io.IOException
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging
import com.lkps.ctApp.controllers.shared_preferences.SharedPrefsController
import com.lkps.ctApp.utils.Constant.LOG_OUT_ALL
import com.lkps.ctApp.utils.Constant.LOG_OUT_PATH
import com.lkps.ctApp.utils.Constant.VERSION_PATH
import com.lkps.ct.BuildConfig

class DeviceController {

    //Comments Firebase Controller
    /*Title ---------------------------------- Delete ----------------------------------*/
    fun deleteDeviceToken(context: Context) {
        Thread {
            try {
                FirebaseMessaging.getInstance().deleteToken()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()
    }

    fun registerDeviceToken(context: Context, function: (String) -> Unit) {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener {
                function(it)
            }
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    return@OnCompleteListener
                }
            })
    }

    fun connectServiceDB(sharedPrefsController: SharedPrefsController, function: () -> Unit) {
        val path = VERSION_PATH

        FirebaseDatabase.getInstance().getReference(path)
            .addValueEventListener(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {}

                override fun onDataChange(p0: DataSnapshot) {
                    if (((p0.value as? Long) ?: 0L) > BuildConfig.VERSION_CODE) {

                        sharedPrefsController.setNewVersion((p0.value as? Long) ?: 0L)
                        function()

                    } else if (((p0.value as? Long) ?: 0L) == BuildConfig.VERSION_CODE.toLong()) {
                        sharedPrefsController.remove(SharedPrefsController.FORCE_UPDATE_VERSION)
                    }
                }
            })
    }

    fun logOutAll(context: Context) {
        val path = LOG_OUT_PATH
        FirebaseDatabase.getInstance().getReference(path)
            .addValueEventListener(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {

                }
                override fun onDataChange(p0: DataSnapshot) {
                    if  (((p0.value as? Long) ?: 0L) == LOG_OUT_ALL) {
                        deleteDeviceToken(context)
                        AuthUI.getInstance().signOut(context)
                    }
                }
            })
    }
}
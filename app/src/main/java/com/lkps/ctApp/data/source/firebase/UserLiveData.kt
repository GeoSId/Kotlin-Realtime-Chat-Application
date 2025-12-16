package com.lkps.ctApp.data.source.firebase

import androidx.lifecycle.LiveData
import com.lkps.ctApp.data.model.User
import com.lkps.ctApp.utils.states.AuthenticationState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.messaging.FirebaseMessaging
import com.lkps.ctApp.utils.Constant.REF_FCM_TOKEN
import com.lkps.ctApp.utils.Constant.REF_IS_ONLINE
import com.lkps.ctApp.utils.Constant.REF_LAST_SEEN_TIME_STAMP
import com.lkps.ctApp.utils.Constant.REF_USERS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserLiveData : LiveData<Pair<User?, AuthenticationState>>() {

    private var uiScope: CoroutineScope? = null

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val dbRefUsers by lazy { firebaseFirestore.collection(REF_USERS) }

    var mUser: User? = null

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        firebaseAuth.currentUser?.uid?.let { userId ->
            uiScope?.launch {
                val user = getUser(userId) ?: User().apply { this.userId = userId }
                value = Pair(user, AuthenticationState.Authenticated(userId))
                updateUser(userId, true)
            }
        } ?: run {
            value = Pair(null, AuthenticationState.Unauthenticated)
        }
    }


    private suspend fun getUser(userId: String): User? {
        val user: User?
        try {
            val documentSnapshot = dbRefUsers.document(userId).get().await()
            user = documentSnapshot.toObject(User::class.java)
        } catch (e: FirebaseFirestoreException) {
            return null
        }
        return user
    }

    public suspend fun getUsers(): MutableList<User?>? {
        val users : MutableList<User?> = mutableListOf()
        try {
            dbRefUsers.get()
                .addOnSuccessListener { documents ->
                    uiScope?.launch {
                        for (document in documents) {
                            users.add(getUser(document.id))
                        }
                    }
                }

        } catch (e: FirebaseFirestoreException) {
            return null
        }
        return users
    }

    private suspend fun clearUser(documentUserId: String): Boolean? {
        val user: User?
        try {
            dbRefUsers.document(documentUserId).delete()
        } catch (e: FirebaseFirestoreException) {
            return false
        }
        return true
    }

    private suspend fun updateUser(userId: String, isOnline: Boolean) {
        val map = mutableMapOf<String, Any>()
        updateFcmToken(map, isOnline)
        map[REF_IS_ONLINE] = isOnline
        map[REF_LAST_SEEN_TIME_STAMP] = FieldValue.serverTimestamp()
        dbRefUsers.document(userId).update(map)
    }

    private suspend fun updateFcmToken(map: MutableMap<String, Any>, isOnline: Boolean) {
        FirebaseMessaging.getInstance().token
                .addOnSuccessListener {
                    val refreshedToken = it ?: return@addOnSuccessListener
                    val fcmTokenList = mUser?.fcmTokenList ?: arrayListOf()
                    if (isOnline) fcmTokenList.add(refreshedToken) else fcmTokenList.remove(refreshedToken)
                    fcmTokenList.distinct()
                    map[REF_FCM_TOKEN] = fcmTokenList
                }.await()
    }

    fun setNewUser(user: User) {
        user.userId?.let { userId ->
            value = Pair(user, AuthenticationState.Authenticated(userId))
            uiScope?.launch {
                updateUser(userId, true)
            }
        }
    }

    override fun onActive() {
        uiScope = CoroutineScope(Job() + Dispatchers.Main)
        firebaseAuth.addAuthStateListener(authStateListener)
    }

    override fun onInactive() {
        firebaseAuth.removeAuthStateListener(authStateListener)
        uiScope?.launch {
            mUser?.userId?.let { updateUser(it, false) }
            uiScope?.cancel()
        }
    }
}
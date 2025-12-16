package com.lkps.ctApp.data

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.lkps.ctApp.MainCoroutineRule
import com.lkps.ctApp.data.repository.RepositoryImpl
import com.lkps.ctApp.data.source.firebase.FirebaseDaoImpl
import com.lkps.ctApp.util.any
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.verify

@ExperimentalCoroutinesApi
class ShoppingListRepositoryImplTest {

    private val firebaseDao = Mockito.mock(FirebaseDaoImpl::class.java)
    private val repo = RepositoryImpl(firebaseDao)

    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    @Rule
    @JvmField
    @ExperimentalCoroutinesApi
    val mainCoroutineRule = MainCoroutineRule()

    @Test
    fun getChatRoomListLiveData() {
        repo.getChatRoomListLiveData()
        verify(firebaseDao).chatRoomListLiveData
    }

    @Test
    fun getChatRoomLiveData() {
        repo.getChatRoomLiveData()
        verify(firebaseDao).chatRoomLiveData
    }

    @Test
    fun getUserLiveData() {
        repo.getUserLiveData()
        verify(firebaseDao).userLiveData
    }

    @Test
    fun isUsernameAvailable() = runBlocking {
        val username = "TestName"
        repo.isUsernameAvailable(username) {}
        verify(firebaseDao).isUsernameAvailable(any(), any())
    }

    @Test
    fun searchForUser() = runBlocking {
        val username = "TestName"
        repo.searchForUser(username) { p1, p2 -> }
        verify(firebaseDao).searchForUser(any(), any())
    }

    @Test
    fun addUsername() {
        val username = "TestName"
        repo.addUsername(username) {}
        verify(firebaseDao).addUsername(any(), any())
    }

    @Test
    fun fetchConfigMsgLength() {
        repo.fetchConfigMsgLength {}
        verify(firebaseDao).fetchConfigMsgLength(any())
    }
}
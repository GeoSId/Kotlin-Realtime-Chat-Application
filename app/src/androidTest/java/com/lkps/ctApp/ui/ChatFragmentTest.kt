package com.lkps.ctApp.ui

import android.content.Context
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lkps.ctApp.util.mock
import com.lkps.ctApp.utils.FileHelper
import com.lkps.ctApp.utils.states.NetworkState
import com.lkps.ctApp.view.FirebaseViewModel
import com.lkps.ctApp.view.chatRoom.ChatFragment
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.`when`

@RunWith(AndroidJUnit4::class)
class ChatFragmentTest {

    companion object {
        val viewModel: FirebaseViewModel = mock()
        val fileHelperMock: FileHelper = mock()
    }

    private val pushImgStatus = MutableLiveData<NetworkState>()
    private val pushAudioStatus = MutableLiveData<NetworkState>()
    private lateinit var scenario: FragmentScenario<ChatFragmentTest>
    private lateinit var appContext: Context

    @Before
    fun init() {
        Mockito.reset(viewModel)
        appContext = InstrumentationRegistry.getInstrumentation().context
        `when`(viewModel.pushImgStatus).thenReturn(pushImgStatus)
        `when`(viewModel.pushAudioStatus).thenReturn(pushAudioStatus)
        scenario = launchFragmentInContainer<ChatFragmentTest>()
    }

    @Test
    fun onPhotoPickerClick() {
    }

    @Test
    fun onSendButtonTouch() {
    }

    @Test
    fun onSendButtonClick() {
    }

    @Test
    fun pushMsg() {
    }

    @Test
    fun pushImg() {
    }

    @Test
    fun pushAudio() {
    }

    class ChatFragmentTest : ChatFragment() {

        override fun injectMembers() {
            this.fileHelper = fileHelperMock
            this.viewModelFactory = ViewModelFactory(viewModel)
        }

        @Suppress("UNCHECKED_CAST")
        class ViewModelFactory<T>(private val mock: T) : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>) = mock as T
        }
    }
}
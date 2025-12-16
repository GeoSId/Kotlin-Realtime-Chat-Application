package com.lkps.ctApp.view.chatRoom

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.work.*
import com.lkps.ct.R
import com.lkps.ct.databinding.FragmentChatBinding
import com.lkps.ctApp.data.model.Message
import com.lkps.ctApp.data.source.firebase.FirebaseDaoImpl
import com.lkps.ctApp.testing.CustomDaggerFragment
import com.lkps.ctApp.utils.FileHelper
import com.lkps.ctApp.utils.IntentManager.getIntentType
import com.lkps.ctApp.utils.Utility
import com.lkps.ctApp.utils.bindingFakeAudioProgress
import com.lkps.ctApp.utils.extension.afterTextChanged
import com.lkps.ctApp.utils.states.FragmentState
import com.lkps.ctApp.utils.states.NetworkState
import com.lkps.ctApp.view.FirebaseViewModel
import com.lkps.ctApp.view.adapters.MsgAdapter
import javax.inject.Inject

open class ChatFragment : CustomDaggerFragment() {

    companion object {
        val TAG = ChatFragment::class.java.simpleName
        const val LOADING = "loading"
    }

    lateinit var audioHelper: AudioHelper

    private val fakeMsgAudio = Message(
            audioUrl = LOADING,
            audioFile = LOADING,
            isOwner = true
    )

    private val fakeMsg = Message(
            fileUrl = LOADING,
            isOwner = true
    )

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var fileHelper: FileHelper

    private val firebaseVm: FirebaseViewModel by activityViewModels { viewModelFactory }
    private lateinit var binding: FragmentChatBinding
    private lateinit var adapter: MsgAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //TODO WORKER
//        val workManager: WorkManager = WorkManager.getInstance(requireContext())
//        val outputDeleteChat: LiveData<List<WorkInfo>> = workManager.getWorkInfosByTagLiveData(WorkerKeys.WORK_M_NAME)
        //TODO WORKER
        //OBSERVER COULD WORK IN OneTimeWorker with return outPut but we have PeriodicRequest which handle widget and Notications
//        outputDeleteChat.observe(this, deleteChatObservre() )
    }

    private fun  deleteChatObservre(): Observer<List<WorkInfo>> {
        return Observer {  listofworkInfo ->
            Log.e("WORKERRR",  " START")

            if (listofworkInfo.isNullOrEmpty()){
                return@Observer
            }
            val workInfo = listofworkInfo[0]
            workInfo.state
            if(workInfo.state.isFinished){
                //showWorkFinished()
            }
           Log.e("WORKERRR",  " "+workInfo.state)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = FragmentChatBinding.inflate(LayoutInflater.from(context))
        binding.lifecycleOwner = this
        binding.firebaseVm = firebaseVm

        audioHelper = AudioHelper(fileHelper, activity as AppCompatActivity)

        adapter = MsgAdapter(getMsgAdapterListener())
        binding.recyclerView.layoutAnimation = null
        binding.recyclerView.itemAnimator = null
        binding.recyclerView.adapter = adapter

        observePushFileState()
        observePushAudioState()

        binding.messageEditText.afterTextChanged { text ->
            binding.sendButton.isActivated = text.isNotBlank()
        }

        binding.photoPickerButton.setOnClickListener { onPhotoPickerClick() }

        binding.sendButton.setOnTouchListener(onSendButtonTouch())

        binding.sendButton.isActivated = false


        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    Log.d(TAG, "Fragment back pressed invoked")
                    // Do custom work here
                    // if you want onBackPressed() to be called as normal afterwards
                    if (isEnabled) {
                        isEnabled = false //disable the BackPressed button
                        requireActivity().onBackPressed()
                    }
                }
            }
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        /*
         when intent filter works from outside the app we send the file and after we null tha flag
         */
        if(firebaseVm.shareUriFile != null && firebaseVm.shareUriFile.toString().isNotEmpty()){
            firebaseVm.pushFile(firebaseVm.shareUriFile!!, getIntentType(firebaseVm.intent!!))
            firebaseVm.shareUriFile = null
        }
    }

    private fun onPhotoPickerClick() {
        val intentGallery = Intent(Intent.ACTION_PICK)
        intentGallery.type = "image/*"
        intentGallery.putExtra(Intent.EXTRA_LOCAL_ONLY, true)

        val chooserIntent = Intent.createChooser(intentGallery, "Select picture")
        fileHelper.createPhotoMediaFile()?.let { photoFile ->
            val cameraIntent = Utility.getCameraIntent(requireContext(), photoFile)
            FileHelper.currentPhotoPath = photoFile.absolutePath
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))
        }
        requireActivity().startActivityForResult(chooserIntent, FirebaseDaoImpl.RC_PHOTO_PICKER)
    }

    private fun onSendButtonTouch() = object : View.OnTouchListener {
        override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
            when (view.isActivated) {
                true -> {
                    onSendButtonClick(motionEvent, view)
                }
                false -> if (onMicButtonClick(motionEvent)) return true
            }

            when (motionEvent.action) {
                MotionEvent.ACTION_UP -> {
                    view.isPressed = false
                }
                MotionEvent.ACTION_DOWN -> {
                    view.isPressed = true
                }
            }
            return true
        }
    }

    private fun onSendButtonClick(motionEvent: MotionEvent, view: View) {
        when (motionEvent.action) {
            MotionEvent.ACTION_UP -> {
                if (view.isActivated) pushMsg()
                binding.messageEditText.setText("")
            }
        }
    }

    private fun showWave(isShow: Boolean) {
//        binding.spinKit.isVisible = isShow
        binding.messageEditText.isInvisible = isShow
    }

    private fun pushMsg() {
        firebaseVm.pushMsg(binding.messageEditText.text.toString())
    }

    private fun onMicButtonClick(motionEvent: MotionEvent): Boolean {
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> {
                audioHelper.startRecording()
                showWave(true)
            }
            MotionEvent.ACTION_UP -> {
                showWave(false)
                audioHelper.stopRecording()
                val recordFileName = audioHelper.recordFileName ?: return false
                val recorderDuration = audioHelper.recorderDuration ?: 0
                if (recorderDuration > 1000) pushAudio(recordFileName, recorderDuration)
            }
        }
        return false
    }

    private fun pushAudio(recordFileName: String, recorderDuration: Long) {
        fakeMsgAudio.apply {
            audioFile = recordFileName
            audioDuration = recorderDuration
        }
        firebaseVm.pushAudio(recordFileName, recorderDuration)
    }

    private fun observePushFileState() {
        firebaseVm.pushFileStatus.observe(viewLifecycleOwner) {
            when (it) {
                NetworkState.LOADING -> {
                    binding.progressBar.isVisible = true
                    val newList = mutableListOf<Message>()
                    newList.addAll(adapter.currentList)
                    newList.add(fakeMsg)
                    adapter.submitList(newList) { scrollToPosition() }
                    adapter.notifyDataSetChanged()
                }
                NetworkState.LOADED -> {
                    binding.progressBar.isVisible = false
                }
                NetworkState.FAILED -> {
                    Toast.makeText(requireContext(), "File Error", Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
    }

    private fun observePushAudioState() {
        firebaseVm.pushAudioStatus.observe(viewLifecycleOwner, Observer {
            when (it) {
                NetworkState.LOADING -> {
                    binding.progressBar.isVisible = true
                    val newList = mutableListOf<Message>()
                    newList.addAll(adapter.currentList)
                    newList.add(fakeMsgAudio)
                    adapter.submitList(newList) { scrollToPosition() }
                    adapter.notifyDataSetChanged()
                }
                NetworkState.LOADED ->{
                    binding.progressBar.isVisible = false
                }
                NetworkState.FAILED -> {
                    Toast.makeText(requireContext(),"Audio Error",Toast.LENGTH_LONG).show()
                }
                else -> {
                }
            }
        })
    }

    private fun scrollToPosition() {
        binding.recyclerView.layoutManager?.scrollToPosition(adapter.itemCount - 1)
    }

    private fun getMsgAdapterListener(): MsgAdapter.MsgAdapterListener {
        return object : MsgAdapter.MsgAdapterListener {
            override fun showPic(view: View, message: Message) {
                val reviewDialog = PhotoDialog(message.fileUrl!!)
                reviewDialog.show(parentFragmentManager, PhotoDialog::class.simpleName)
            }

            override fun showPdf(view: View, message: Message) {
                openPdf(message.fileUrl!!)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar, item: Message) {
                val parentView = seekBar.parent as ConstraintLayout
                val playButton = parentView.findViewById<View>(R.id.playButton)
                if (playButton.isActivated) audioHelper.stopTimer()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar, item: Message) {
                val parentView = seekBar.parent as ConstraintLayout
                val playButton = parentView.findViewById<View>(R.id.playButton) ?: return
                if (playButton.isActivated) {
                    audioHelper.stopTimer()
                    audioHelper.stopPlaying()
                    audioHelper.startPlaying()
                } else {
                    val audioTimeView = parentView.findViewById<TextView>(R.id.audioTimeTextView)
                    val audioDuration = item.audioDuration ?: return
                    audioHelper.setAudioTime(seekBar, audioTimeView, audioDuration)
                }
            }

            override fun onAudioClick(view: View, message: Message) {
                if (message.audioDownloaded.not()) bindingFakeAudioProgress(view, message)
                else audioHelper.setupAudioHelper(view, message)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        audioHelper.onStop()
    }

    private fun openPdf(source: String){
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(Uri.parse(source), "application/pdf")
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        val newIntent = Intent.createChooser(intent, "Open File")
        try {
            startActivity(newIntent)
        } catch (e: ActivityNotFoundException) {
            // Instruct the user to install a PDF reader here, or something
            Toast.makeText(context, "No application available to view PDF", Toast.LENGTH_LONG).show()
        }
    }
}
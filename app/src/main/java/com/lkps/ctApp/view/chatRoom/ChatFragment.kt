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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
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
import com.lkps.ctApp.testing.CustomDaggerFragment
import com.lkps.ctApp.utils.FileHelper
import com.lkps.ctApp.utils.IntentManager.getIntentType
import com.lkps.ctApp.utils.PermissionManager
import com.lkps.ctApp.utils.Utility
import com.lkps.ctApp.utils.bindingFakeAudioProgress
import com.lkps.ctApp.utils.extension.afterTextChanged
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

    // Activity Result launchers for permissions
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var requestMultiplePermissionsLauncher: ActivityResultLauncher<Array<String>>

    // Permission request state tracking
    private var pendingPermissionType: PermissionType? = null

    private enum class PermissionType {
        CAMERA,
        MICROPHONE,
        STORAGE
    }

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
            Log.e(TAG,  " START")

            if (listofworkInfo.isNullOrEmpty()){
                return@Observer
            }
            val workInfo = listofworkInfo[0]
            workInfo.state
            if(workInfo.state.isFinished){
                //showWorkFinished()
            }
           Log.e(TAG,  " "+workInfo.state)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Register Activity Result launchers
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            handlePermissionResult(isGranted)
        }

        requestMultiplePermissionsLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.values.all { it }
            handleMultiplePermissionsResult(allGranted)
        }

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

        // Observe message list changes
        firebaseVm.msgList.observe(viewLifecycleOwner) { messages ->
            android.util.Log.d(TAG, "Message list updated: ${messages?.size} messages")
            // Update adapter manually to ensure it reflects changes
            adapter.submitList(messages) {
                if (messages?.isNotEmpty() == true) {
                    scrollToPosition()
                }
            }
        }

        /*
         when intent filter works from outside the app we send the file and after we null tha flag
         */
        if(firebaseVm.shareUriFile != null && firebaseVm.shareUriFile.toString().isNotEmpty()){
            firebaseVm.pushFile(firebaseVm.shareUriFile!!, getIntentType(firebaseVm.intent!!))
            firebaseVm.shareUriFile = null
        }
    }

    private fun onPhotoPickerClick() {
        // Show dialog to choose between camera and gallery
        val options = arrayOf("Take Photo", "Choose from Gallery")
        AlertDialog.Builder(requireContext())
            .setTitle("Select Photo")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera() // Take Photo
                    1 -> openGallery() // Choose from Gallery
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openCamera() {
        // Check camera permission first
        pendingPermissionType = PermissionType.CAMERA
        val cameraGranted = PermissionManager.checkAndRequestCameraPermission(
            fragment = this,
            permissionLauncher = requestPermissionLauncher,
            onGranted = {
                launchCameraDirectly()
            },
            onDenied = {
                showPermissionDeniedMessage("Camera permission is required to take photos")
            }
        )

        // If camera permission was already granted, launch camera immediately
        if (cameraGranted) {
            launchCameraDirectly()
        }
    }

    private fun openGallery() {
        // Check storage permission first
        pendingPermissionType = PermissionType.STORAGE
        val storageGranted = PermissionManager.checkAndRequestStoragePermission(
            fragment = this,
            multiplePermissionsLauncher = requestMultiplePermissionsLauncher,
            onGranted = {
                launchGalleryPicker()
            },
            onDenied = {
                showPermissionDeniedMessage("Storage permission is required to access photos from gallery")
            }
        )

        // If storage permission was already granted, launch gallery immediately
        if (storageGranted) {
            launchGalleryPicker()
        }
    }

    private fun launchCameraDirectly() {
        fileHelper.createPhotoMediaFile()?.let { photoFile ->
            android.util.Log.d(TAG, "Created photo file: ${photoFile.absolutePath}")
            val cameraIntent = Utility.getCameraIntent(requireContext(), photoFile)
            cameraIntent?.let { intent ->
                FileHelper.currentPhotoPath = photoFile.absolutePath

                // Grant URI permissions to camera app for the FileProvider URI
                val imageUri = intent.getParcelableExtra<android.net.Uri>(android.provider.MediaStore.EXTRA_OUTPUT)
                android.util.Log.d(TAG, "Camera intent output URI: $imageUri")
                if (imageUri != null) {
                    val cameraPackage = intent.resolveActivity(requireContext().packageManager)?.packageName
                    android.util.Log.d(TAG, "Camera package: $cameraPackage")
                    if (cameraPackage != null) {
                        requireContext().grantUriPermission(
                            cameraPackage,
                            imageUri,
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        android.util.Log.d(TAG, "Granted URI permissions to camera app")
                    }
                }

                (requireActivity() as? com.lkps.ctApp.view.MainActivity)?.launchPhotoPicker(intent)
            } ?: run {
                showPermissionDeniedMessage("Camera is not available on this device")
            }
        } ?: run {
            showPermissionDeniedMessage("Unable to create photo file")
        }
    }

    private fun launchGalleryPicker() {
        val galleryIntent = Intent(Intent.ACTION_PICK)
        galleryIntent.type = "image/*"
        galleryIntent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)

        (requireActivity() as? com.lkps.ctApp.view.MainActivity)?.launchPhotoPicker(galleryIntent)
    }


    private fun showPermissionDeniedMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun handlePermissionResult(isGranted: Boolean) {
        when (pendingPermissionType) {
            PermissionType.CAMERA -> {
                if (isGranted) {
                    // Camera permission granted, launch camera
                    launchCameraDirectly()
                } else {
                    showPermissionDeniedMessage("Camera permission is required to take photos")
                }
            }
            PermissionType.MICROPHONE -> {
                if (isGranted) {
                    // Microphone permission granted, start recording
                    audioHelper.startRecording()
                    showWave(true)
                } else {
                    showPermissionDeniedMessage("Microphone permission is required to record audio")
                }
            }
            else -> {
                // Unknown permission type
            }
        }
        pendingPermissionType = null
    }

    private fun handleMultiplePermissionsResult(allGranted: Boolean) {
        when (pendingPermissionType) {
            PermissionType.STORAGE -> {
                if (allGranted) {
                    // Storage permission granted, launch gallery picker
                    launchGalleryPicker()
                } else {
                    showPermissionDeniedMessage("Storage permission is required to access photos from gallery")
                }
            }
            else -> {
                // Unknown permission type
            }
        }
        pendingPermissionType = null
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
                // Check microphone permission before starting recording
                pendingPermissionType = PermissionType.MICROPHONE
                val micGranted = PermissionManager.checkAndRequestMicrophonePermission(
                    fragment = this,
                    permissionLauncher = requestPermissionLauncher,
                    onGranted = {
                        audioHelper.startRecording()
                        showWave(true)
                    },
                    onDenied = {
                        showPermissionDeniedMessage("Microphone permission is required to record audio")
                    }
                )

                // If permission was already granted, start recording immediately
                if (micGranted) {
                    audioHelper.startRecording()
                    showWave(true)
                }
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
                    android.util.Log.d(TAG, "File upload started")
                }
                NetworkState.LOADED -> {
                    binding.progressBar.isVisible = false
                    android.util.Log.d(TAG, "File upload completed - message should appear via snapshot listener")
                }
                NetworkState.FAILED -> {
                    binding.progressBar.isVisible = false
                    Toast.makeText(requireContext(), "File upload failed", Toast.LENGTH_LONG).show()
                    android.util.Log.e(TAG, "File upload failed")
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
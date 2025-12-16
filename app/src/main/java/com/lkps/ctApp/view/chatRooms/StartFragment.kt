package com.lkps.ctApp.view.chatRooms

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.*
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.lkps.ct.BuildConfig
import com.lkps.ct.R
import com.lkps.ct.databinding.FragmentStartBinding
import com.lkps.ctApp.controllers.shared_preferences.SharedPrefsController
import com.lkps.ctApp.testing.CustomDaggerFragment
import com.lkps.ctApp.utils.extension.clear
import com.lkps.ctApp.utils.general.hideKeyboard
import com.lkps.ctApp.utils.states.FragmentState
import com.lkps.ctApp.view.FirebaseViewModel
import com.lkps.ctApp.view.adapters.ChatListAdapter
import javax.inject.Inject

open class StartFragment : CustomDaggerFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val firebaseVm: FirebaseViewModel by activityViewModels { viewModelFactory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentStartBinding.inflate(LayoutInflater.from(context))
        binding.lifecycleOwner = this
        binding.firebaseVm = firebaseVm
        binding.recyclerView.adapter = ChatListAdapter(getOnRoomClickListener())
        firebaseVm.setMsgList(mutableListOf())
        context.hideKeyboard()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        firebaseVm.validationApp(requireContext()) {
            showForceUpdateDialog(requireContext())
        }
    }

    override fun onStop() {
        super.onStop()
        firebaseVm._chatRoomList.clear()
        firebaseVm.setMsgList(mutableListOf())
    }

    private fun getOnRoomClickListener(): ChatListAdapter.OnChatClickListener {
        return ChatListAdapter.OnChatClickListener { user ->
            firebaseVm.setReceiver(user.user?.value)
            firebaseVm.setFragmentState(FragmentState.CHAT)
        }
    }

    private fun showForceUpdateDialog(context: Context?) {
        val sharedPrefsController = SharedPrefsController(context)
        val showDialog: Boolean = (sharedPrefsController.getNewVersion()
            ?: 0L) > BuildConfig.VERSION_CODE
        if (showDialog) {
            showMessageDialog(
                context,
                context?.resources?.getString(R.string.update_message)?: "",
                R.string.ok
            ) { dialog, which ->
                dialog.dismiss()
                when {
                    true -> requireActivity().finish()
                }
            }
        } else {
            sharedPrefsController.remove(SharedPrefsController.FORCE_UPDATE_VERSION)
        }
    }

    private fun showMessageDialog(
        context: Context?,
        message: String,
        @StringRes positive: Int?,
        positiveListener: DialogInterface.OnClickListener?
    ) {
        val builder = context?.let { AlertDialog.Builder(it, R.style.AlertDialogStyle) }
        builder?.setCancelable(false)
        builder?.setTitle(R.string.app_name)
        builder?.setMessage(message)
        positive?.let { builder?.setPositiveButton(it, positiveListener) }
        builder?.show()
    }
}
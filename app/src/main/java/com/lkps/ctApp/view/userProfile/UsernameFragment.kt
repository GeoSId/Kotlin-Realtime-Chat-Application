package com.lkps.ctApp.view.userProfile

import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.lkps.ctApp.testing.CustomDaggerFragment
import com.lkps.ctApp.utils.states.Status
import com.lkps.ctApp.view.FirebaseViewModel
import com.firebase.ui.auth.AuthUI
import com.lkps.ct.R
import com.lkps.ct.databinding.FragmentUsernameBinding
import com.lkps.ctApp.utils.extension.afterTextChangedLowerCase
import javax.inject.Inject

open class UsernameFragment : CustomDaggerFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val firebaseVm: FirebaseViewModel by activityViewModels { viewModelFactory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentUsernameBinding.inflate(LayoutInflater.from(context))
        binding.usernameButton.setOnClickListener {
            firebaseVm.addUsername(binding.usernameEditText.text.toString())
        }
        observeUsernameEditText(binding)
        observeUsernameStatus(binding)
//        firebaseVm.setFragmentState(FragmentState.USERNAME, false)
        val isLogin = firebaseVm.user.value?.first?.username != null
        if(!isLogin){
            binding.root.onBackPress()
        }else{
            requireActivity().finish()
        }
        return binding.root
    }

    private fun observeUsernameEditText(binding: FragmentUsernameBinding) {
        binding.usernameEditText.afterTextChangedLowerCase { text ->
            if (text.isNotEmpty() && text.length > 3) {
                firebaseVm.isUsernameAvailable(text)
            } else {
                binding.progressBar.visibility = View.GONE
                binding.usernameButton.visibility = View.VISIBLE
                binding.usernameButton.isEnabled = false
                binding.usernameEditText.isSelected = false
                binding.usernameErrorTextView.text = getString(R.string.usernameHint)
                binding.usernameErrorTextView.setTextColor(getColor(R.color.colorText))
            }
        }
    }

    private fun observeUsernameStatus(binding: FragmentUsernameBinding) {
        firebaseVm.usernameStatus.observe(viewLifecycleOwner, Observer {
            when (it.status) {
                Status.RUNNING -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.usernameButton.visibility = View.GONE
                }
                Status.SUCCESS -> {
                    binding.usernameErrorTextView.text = getString(R.string.usernameHint)
                    binding.usernameErrorTextView.setTextColor(getColor(R.color.colorText))
                    binding.progressBar.visibility = View.GONE
                    binding.usernameButton.visibility = View.VISIBLE
                    binding.usernameButton.isEnabled = true
                    binding.usernameEditText.isSelected = false
                }
                Status.FAILED -> {
                    binding.progressBar.visibility = View.GONE
                    binding.usernameButton.visibility = View.VISIBLE
                    binding.usernameButton.isEnabled = false
                    binding.usernameEditText.isSelected = true
                    binding.usernameErrorTextView.text = getString(R.string.usernameError)
                    binding.usernameErrorTextView.setTextColor(getColor(R.color.colorError))
                }
            }
        })
    }

    private fun getColor(color: Int): Int {
        return ContextCompat.getColor(requireActivity(), color)
    }

    private fun View.onBackPress() {
        this.isFocusableInTouchMode = true
        this.requestFocus()
        this.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                AuthUI.getInstance().signOut(requireContext())
                true
            } else {
                false
            }
        }
    }
}
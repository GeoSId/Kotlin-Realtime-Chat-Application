package com.lkps.ctApp.view

import android.os.Bundle
import android.view.*
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.lkps.ct.R
import com.lkps.ct.databinding.FragmentManagerBinding
import com.lkps.ctApp.data.model.User
import com.lkps.ctApp.testing.CustomDaggerFragment
import com.lkps.ctApp.view.adapters.UsersAdapter
import javax.inject.Inject

open class ManagerFragment : CustomDaggerFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val firebaseVm: FirebaseViewModel by activityViewModels { viewModelFactory }
    private lateinit var binding: FragmentManagerBinding
    private lateinit var adapter: UsersAdapter
    private var selectedUser: User ?= null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentManagerBinding.inflate(LayoutInflater.from(context))
        adapter = UsersAdapter(object : UsersAdapter.OnUsersClickListener{

            override fun onClick(view: View, user: User) {
                selectedUser = user
                binding.deleteUsersButton.text = resources.getString(R.string.delete_user_)+"  "+ user.username
            }
        })
        binding.managerRecyclerView.adapter = adapter
        binding.lifecycleOwner = this
        binding.firebaseVm = firebaseVm
        firebaseVm.getUsers()
        binding.managerRecyclerView.itemAnimator = null

        binding.deleteUsersButton.setOnClickListener {
//            selectedUser?.userId?.let { it1 -> firebaseVm.deleteUsers(it1) }
        }

        return binding.root
    }
}
package com.lkps.ctApp.view.searchUser

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.lkps.ct.R
import com.lkps.ct.databinding.FragmentSearchBinding
import com.lkps.ctApp.testing.CustomDaggerFragment
import com.lkps.ctApp.utils.states.FragmentState
import com.lkps.ctApp.utils.states.NetworkState
import com.lkps.ctApp.view.FirebaseViewModel
import com.lkps.ctApp.view.adapters.SearchAdapter
import javax.inject.Inject

open class SearchFragment : CustomDaggerFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val firebaseVm: FirebaseViewModel by activityViewModels { viewModelFactory }
    lateinit var binding: FragmentSearchBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSearchBinding.inflate(LayoutInflater.from(context))
        binding.lifecycleOwner = this
        binding.firebaseVm = firebaseVm
        binding.recyclerView.itemAnimator = null
        firebaseVm.onSearchTextChange("")
        binding.recyclerView.adapter = SearchAdapter(SearchAdapter.OnSearchClickListener { user ->
            firebaseVm.setReceiver(user)
            firebaseVm.setFragmentState(FragmentState.CHAT)
            onSearchViewClose()
        })

        firebaseVm.userSearchStatus.observe(viewLifecycleOwner) {
            binding.searchView.visibility = View.VISIBLE
            when (it) {
                NetworkState.LOADING -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                    binding.info.visibility = View.GONE
                }
                NetworkState.LOADED -> {
                    binding.progressBar.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                    binding.info.visibility = View.GONE
                }
                NetworkState.FAILED -> {
                    binding.progressBar.visibility = View.GONE
                    binding.recyclerView.visibility = View.GONE
                    binding.info.visibility = View.VISIBLE
                }
                else -> {
                }
            }
        }

        setOnQueryTextFocusChangeListener()
        return binding.root
    }

    private fun onSearchViewClose() {
        binding.root.visibility = View.GONE
    }

    private fun setOnQueryTextFocusChangeListener() {
        binding.searchViewBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
//                binding.root.visibility = View.VISIBLE
//                binding.searchView.visibility = View.GONE
                return false
            }
            override fun onQueryTextChange(newText: String): Boolean {
                firebaseVm.onSearchTextChange(newText)
                return false
            }
        })
    }
}
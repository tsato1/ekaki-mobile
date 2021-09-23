package com.tsato.mobile.ekaki.ui.setup.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.tsato.mobile.ekaki.R
import com.tsato.mobile.ekaki.databinding.FragmentUsernameBinding
import com.tsato.mobile.ekaki.ui.setup.UserNameViewModel
import com.tsato.mobile.ekaki.util.Constants.MAX_USERNAME_LENGTH
import com.tsato.mobile.ekaki.util.Constants.MIN_USERNAME_LENGTH
import com.tsato.mobile.ekaki.util.hideKeyboard
import com.tsato.mobile.ekaki.util.navigateSafely
import com.tsato.mobile.ekaki.util.snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class UserNameFragment : Fragment(R.layout.fragment_username) {

    private var _binding: FragmentUsernameBinding? = null
    private val binding: FragmentUsernameBinding
        get() = _binding!!

    private val viewModel: UserNameViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentUsernameBinding.bind(view)

        listenToEvents()

        binding.btnNext.setOnClickListener {
            viewModel.validateUserNameAndNavigateToSelectRoom(
                binding.etUsername.text.toString()
            )
            requireActivity().hideKeyboard(binding.root)
        }
    }

    private fun listenToEvents() {
        lifecycleScope.launchWhenStarted { // starts when this fragment started its lifecycle
            viewModel.setupEvent.collect { event ->
                // equivalent of observe in livedata, it's sharedflow not stateflow

                when (event) {
                    is UserNameViewModel.SetupEvent.NavigateToSelectRoomEvent -> {
//                        findNavController().navigate(
//                            UserNameFragmentDirections.actionUserNameFragmentToSelectRoomFragment(
//                                event.userName
//                            )
//                        )
                        findNavController().navigateSafely(
                            R.id.action_userNameFragment_to_selectRoomFragment,
                            args = Bundle().apply { putString("userName", event.userName) }
                        )
                    }
                    is UserNameViewModel.SetupEvent.InputEmptyError -> {
                        snackbar(R.string.error_field_empty)
                    }
                    is UserNameViewModel.SetupEvent.InputTooShortError -> {
                        snackbar(getString(R.string.error_username_too_short, MIN_USERNAME_LENGTH))
                    }
                    is UserNameViewModel.SetupEvent.InputTooLongError -> {
                        snackbar(getString(R.string.error_username_too_long, MAX_USERNAME_LENGTH))
                    }
                    else -> Unit
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
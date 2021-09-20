package com.tsato.mobile.ekaki.ui.setup.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.tsato.mobile.ekaki.R
import com.tsato.mobile.ekaki.databinding.FragmentSelectRoomBinding
import com.tsato.mobile.ekaki.databinding.FragmentUsernameBinding

class SelectRoomFragment : Fragment(R.layout.fragment_select_room) {

    private var _binding: FragmentSelectRoomBinding? = null
    private val bingin: FragmentSelectRoomBinding
        get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSelectRoomBinding.bind(view)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
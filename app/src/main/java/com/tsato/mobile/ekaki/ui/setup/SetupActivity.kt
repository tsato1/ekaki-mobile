package com.tsato.mobile.ekaki.ui.setup

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.tsato.mobile.ekaki.R
import com.tsato.mobile.ekaki.databinding.ActivitySetupBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
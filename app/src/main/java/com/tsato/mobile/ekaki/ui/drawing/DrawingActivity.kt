package com.tsato.mobile.ekaki.ui.drawing

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tsato.mobile.ekaki.databinding.ActivityDrawingBinding

class DrawingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDrawingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDrawingBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
package com.befool.others.tricks


import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.befool.others.tricks.databinding.ActivityGuideBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GuideActivity : AppCompatActivity() {
    private val binding by lazy { ActivityGuideBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.guide)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        this.supportActionBar?.hide()
        onBackPressedDispatcher.addCallback {
        }
        startCountdown()
    }

    private fun startCountdown() {
        lifecycleScope.launch {
            // 将2010ms分成100份，每份约20ms
            for (i in 0..100) {
                binding.progressBar.progress = i
                delay(20)
            }
            startActivity(Intent(this@GuideActivity, MainActivity::class.java))
            finish()
        }
    }
}

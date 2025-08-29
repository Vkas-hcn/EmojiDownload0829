package com.befool.others.tricks


import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.befool.others.tricks.databinding.ActivityPrivacyBinding
import kotlin.apply


class PrivacyActivity : AppCompatActivity() {

    private val binding by lazy { ActivityPrivacyBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.privacy)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        this.supportActionBar?.hide()
        binding.apply {
            imgBack.setOnClickListener {
                finish()
            }
            atvShare.setOnClickListener {
                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "text/plain"
                intent.putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=${this@PrivacyActivity.packageName}")
                try {
                    startActivity(Intent.createChooser(intent, "Share via"))
                } catch (ex: Exception) {
                    // Handle error
                }
            }
            atvPlo.setOnClickListener {
                //TODO
                val intent = Intent(Intent .ACTION_VIEW)
                intent.data = "https://www.google.com/".toUri()
                startActivity(intent)
            }
        }

    }

}
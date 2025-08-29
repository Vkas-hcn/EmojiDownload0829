package com.befool.others.tricks

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.befool.others.tricks.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        this.supportActionBar?.hide()
        click()
    }

    private fun click() {
        onBackPressedDispatcher.addCallback {
            finish()
        }

        binding.apply {
            textView.setOnClickListener {
                startActivity(Intent(this@MainActivity, PrivacyActivity::class.java))
            }

            findViewById<LinearLayout>(R.id.layout_love)?.setOnClickListener {
                val intent = Intent(this@MainActivity, EmojiListActivity::class.java)
                intent.putExtra("emoji_type", "love")
                intent.putExtra("title", "Lover Emoji")
                startActivity(intent)
            }

            findViewById<LinearLayout>(R.id.layout_qi)?.setOnClickListener {
                val intent = Intent(this@MainActivity, EmojiListActivity::class.java)
                intent.putExtra("emoji_type", "qi")
                intent.putExtra("title", "Penguin people Emoji")
                startActivity(intent)
            }

            findViewById<LinearLayout>(R.id.layout_emoji)?.setOnClickListener {
                val intent = Intent(this@MainActivity, EmojiListActivity::class.java)
                intent.putExtra("emoji_type", "emoji")
                intent.putExtra("title", "Funny Emoji")
                startActivity(intent)
            }

            findViewById<LinearLayout>(R.id.layout_cute)?.setOnClickListener {
                val intent = Intent(this@MainActivity, EmojiListActivity::class.java)
                intent.putExtra("emoji_type", "cute")
                intent.putExtra("title", "Cute Emoji")
                startActivity(intent)
            }

            findViewById<LinearLayout>(R.id.layout_cat)?.setOnClickListener {
                val intent = Intent(this@MainActivity, EmojiListActivity::class.java)
                intent.putExtra("emoji_type", "cat")
                intent.putExtra("title", "Cat Emoji")
                startActivity(intent)
            }

            findViewById<LinearLayout>(R.id.layout_line)?.setOnClickListener {
                val intent = Intent(this@MainActivity, EmojiListActivity::class.java)
                intent.putExtra("emoji_type", "line")
                intent.putExtra("title", "Line Emoji")
                startActivity(intent)
            }

            findViewById<LinearLayout>(R.id.layout_bullet)?.setOnClickListener {
                val intent = Intent(this@MainActivity, EmojiListActivity::class.java)
                intent.putExtra("emoji_type", "bullet")
                intent.putExtra("title", "Bullet Emoji")
                startActivity(intent)
            }
        }
    }
}
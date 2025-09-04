package com.befool.others.tricks

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
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
        setupRecyclerView()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        val emojiSets = listOf(
            EmojiSet("emoji", "Funny Emoji", ImageDataCon.iconEmoji),
            EmojiSet("box", "Boxing Emoji", ImageDataCon.iconBox),
            EmojiSet("dog", "Dog Emoji", ImageDataCon.iconDog),
            EmojiSet("cute", "Cute Emoji", ImageDataCon.iconCute),
            EmojiSet("cat", "Cat Emoji", ImageDataCon.iconCat),
            EmojiSet("face", "Face Emoji", ImageDataCon.iconFace),
            EmojiSet("paw", "Cat's Paw Emoji", ImageDataCon.iconPat)
        )

        val adapter = MainAdapter(emojiSets) { emojiType, title ->
            val intent = Intent(this, EmojiListActivity::class.java)
            intent.putExtra("emoji_type", emojiType)
            intent.putExtra("title", title)
            startActivity(intent)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun setupClickListeners() {
        onBackPressedDispatcher.addCallback {
            finish()
        }

        binding.textView.setOnClickListener {
            startActivity(Intent(this, NetGoActivity::class.java))
        }
    }
}

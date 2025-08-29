package com.befool.others.tricks

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.befool.others.tricks.databinding.ActivityEmojiDetailBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class EmojiDetailActivity : AppCompatActivity() {
    private val binding by lazy { ActivityEmojiDetailBinding.inflate(layoutInflater) }

    private var resourceType: String = ""
    private var drawableId: Int = 0
    private var gifPath: String = ""

    private var position: Int = 0
    private var emojiType: String = ""
    private var titleText: String = ""

    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.detail)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        this.supportActionBar?.hide()

        initPermissionLauncher()

        getIntentData()

        setupUI()

        setupClickListeners()
    }

    private fun initPermissionLauncher() {
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                downloadSingleImage()
            } else {
                Toast.makeText(this, "Permission denied. Cannot download image.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getIntentData() {
        resourceType = intent.getStringExtra("resource_type") ?: "drawable"
        drawableId = intent.getIntExtra("drawable_id", 0)
        gifPath = intent.getStringExtra("gif_path") ?: ""
        position = intent.getIntExtra("position", 0)
        emojiType = intent.getStringExtra("emoji_type") ?: "love"
        titleText = intent.getStringExtra("title") ?: "Emoji Detail"
    }

    private fun setupUI() {
        binding.tvTitle.text = titleText

        when (resourceType) {
            "drawable" -> {
                Glide.with(MyAppGlideModule.instance)
                    .load(drawableId)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(binding.ivEmojiDetail)
            }
            "gif" -> {
                Glide.with(MyAppGlideModule.instance)
                    .asGif()
                    .load("file:///android_asset/$gifPath")
                    .diskCacheStrategy(DiskCacheStrategy.DATA)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(binding.ivEmojiDetail)
            }
        }
    }

    private fun setupClickListeners() {
        binding.imgBack.setOnClickListener {
            finish()
        }

        binding.btnShare.setOnClickListener {
            shareImage()
        }

        binding.btnDownload.setOnClickListener {
            checkPermissionAndDownload()
        }

        onBackPressedDispatcher.addCallback {
            finish()
        }
    }

    private fun shareImage() {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    when (resourceType) {
                        "drawable" -> shareDrawableImage()
                        "gif" -> shareGifImage()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@EmojiDetailActivity,
                        "Share failed: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private suspend fun shareDrawableImage() {
        val drawable = ContextCompat.getDrawable(this@EmojiDetailActivity, drawableId)
        drawable?.let {
            val bitmap = createBitmap(it.intrinsicWidth, it.intrinsicHeight)
            val canvas = Canvas(bitmap)
            it.setBounds(0, 0, canvas.width, canvas.height)
            it.draw(canvas)

            val file = File(cacheDir, "shared_emoji_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            withContext(Dispatchers.Main) {
                val uri = FileProvider.getUriForFile(
                    this@EmojiDetailActivity,
                    "${packageName}.fileprovider",
                    file
                )

                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, "Check out this emoji sticker!")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                startActivity(Intent.createChooser(shareIntent, "Share Emoji"))
            }
        }
    }

    private suspend fun shareGifImage() {
        val inputStream: InputStream = assets.open(gifPath)
        val file = File(cacheDir, "shared_emoji_${System.currentTimeMillis()}.gif")

        FileOutputStream(file).use { out ->
            inputStream.copyTo(out)
        }
        inputStream.close()

        withContext(Dispatchers.Main) {
            val uri = FileProvider.getUriForFile(
                this@EmojiDetailActivity,
                "${packageName}.fileprovider",
                file
            )

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "image/gif"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Check out this animated emoji sticker!")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Share GIF"))
        }
    }

    private fun checkPermissionAndDownload() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // Android 10及以上不需要存储权限
                downloadSingleImage()
            }
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                downloadSingleImage()
            }
            else -> {
                permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun downloadSingleImage() {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val success = when (resourceType) {
                        "drawable" -> saveDrawableToStorage()
                        "gif" -> saveGifToStorage()
                        else -> false
                    }

                    withContext(Dispatchers.Main) {
                        if (success) {
                            val fileType = if (resourceType == "gif") "GIF" else "Image"
                            Toast.makeText(this@EmojiDetailActivity, "$fileType saved to album", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@EmojiDetailActivity, "Saving failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EmojiDetailActivity, "Saving failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveDrawableToStorage(): Boolean {
        return try {
            val bitmap = ContextCompat.getDrawable(this, drawableId)?.let { drawable ->
                val bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap
            }

            bitmap?.let { bmp ->
                val filename = "emoji_pic_${System.currentTimeMillis()}.png"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    }

                    val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    uri?.let {
                        contentResolver.openOutputStream(it)?.use { outputStream ->
                            bmp.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                        }
                        true
                    } ?: false
                } else {
                    val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    val file = File(picturesDir, filename)
                    val fileOutputStream = FileOutputStream(file)
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
                    fileOutputStream.close()
                    sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))
                    true
                }
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun saveGifToStorage(): Boolean {
        return try {
            val inputStream: InputStream = assets.open(gifPath)
            val filename = "emoji_gif_${System.currentTimeMillis()}.gif"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/gif")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }

                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    contentResolver.openOutputStream(it)?.use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    inputStream.close()
                    true
                } ?: false
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val file = File(picturesDir, filename)
                val fileOutputStream = FileOutputStream(file)
                inputStream.copyTo(fileOutputStream)
                fileOutputStream.close()
                inputStream.close()

                sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
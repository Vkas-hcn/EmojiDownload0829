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
import androidx.core.graphics.createBitmap
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.befool.others.tricks.databinding.ActivityEmojiListBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class EmojiListActivity : AppCompatActivity() {
    private val binding by lazy { ActivityEmojiListBinding.inflate(layoutInflater) }
    private lateinit var emojiAdapter: EmojiAdapter
    private var emojiList: List<ImageResource> = emptyList()
    private var emojiType: String = ""
    private var titleText: String = ""

    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.emoji)) { v, insets ->
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
                downloadAllImages()
            } else {
                Toast.makeText(this, "Permission denied. Cannot download images.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getIntentData() {
        emojiType = intent.getStringExtra("emoji_type") ?: "love"
        titleText = intent.getStringExtra("title") ?: "Emoji Stickers"

        emojiList = when (emojiType) {
            "love" -> ImageDataCon.iconLover
            "qi" -> ImageDataCon.iconQi
            "emoji" -> ImageDataCon.iconEmoji
            "cute" -> ImageDataCon.iconCute
            "cat" -> ImageDataCon.iconCat
            "line" -> ImageDataCon.iconLine
            "bullet" -> ImageDataCon.iconBullet
            else -> ImageDataCon.iconLover
        }
    }

    private fun setupUI() {
        binding.tvTitle.text = titleText

        emojiList.firstOrNull()?.let { firstImage ->
            when (firstImage) {
                is ImageResource.DrawableRes -> {
                    Glide.with(MyAppGlideModule.instance)
                        .load(firstImage.drawableId)
                        .into(binding.imgIcon)
                }
                is ImageResource.GifAsset -> {
                    Glide.with(MyAppGlideModule.instance)
                        .asGif()
                        .load("file:///android_asset/${firstImage.gifPath}")
                        .diskCacheStrategy(DiskCacheStrategy.DATA)
                        .into(binding.imgIcon)
                }
            }
        }

        binding.tvName1.text = titleText
        binding.tvNum1.text = "${emojiList.size} Stickers"

        val gridLayoutManager = GridLayoutManager(this, 3)
        binding.rvEmoji.layoutManager = gridLayoutManager

        emojiAdapter = EmojiAdapter(emojiList) { imageResource, position ->
            val intent = Intent(this, EmojiDetailActivity::class.java)
            when (imageResource) {
                is ImageResource.DrawableRes -> {
                    intent.putExtra("resource_type", "drawable")
                    intent.putExtra("drawable_id", imageResource.drawableId)
                }
                is ImageResource.GifAsset -> {
                    intent.putExtra("resource_type", "gif")
                    intent.putExtra("gif_path", imageResource.gifPath)
                }
            }
            intent.putExtra("position", position)
            intent.putExtra("emoji_type", emojiType)
            intent.putExtra("title", titleText)
            startActivity(intent)
        }

        binding.rvEmoji.adapter = emojiAdapter
    }

    private fun setupClickListeners() {
        binding.imgBack.setOnClickListener {
            finish()
        }

        binding.btnDownloadAll.setOnClickListener {
            checkPermissionAndDownload()
        }

        onBackPressedDispatcher.addCallback {
            finish()
        }
    }

    private fun checkPermissionAndDownload() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                downloadAllImages()
            }
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                downloadAllImages()
            }
            else -> {
                permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun downloadAllImages() {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    var successCount = 0

                    emojiList.forEachIndexed { index, imageResource ->
                        if (saveImageToStorage(imageResource, "${emojiType}_${index + 1}")) {
                            successCount++
                        }
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@EmojiListActivity,
                            "Downloaded $successCount/${emojiList.size} images successfully",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@EmojiListActivity,
                        "Download failed: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun saveImageToStorage(imageResource: ImageResource, fileName: String): Boolean {
        return try {
            when (imageResource) {
                is ImageResource.DrawableRes -> {
                    saveDrawableToStorage(imageResource.drawableId, fileName)
                }
                is ImageResource.GifAsset -> {
                    saveGifToStorage(imageResource.gifPath, fileName)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun saveDrawableToStorage(drawableId: Int, fileName: String): Boolean {
        return try {
            val bitmap = ContextCompat.getDrawable(this, drawableId)?.let { drawable ->
                val bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap
            }

            bitmap?.let { bmp ->
                val filename = "${fileName}.png"
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

    private fun saveGifToStorage(gifPath: String, fileName: String): Boolean {
        return try {
            val inputStream: InputStream = assets.open(gifPath)
            val filename = "${fileName}.gif"

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
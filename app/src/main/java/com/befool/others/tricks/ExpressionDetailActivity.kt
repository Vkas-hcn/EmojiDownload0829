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
import kotlin.properties.Delegates

// 策略模式：分享策略
sealed interface ShareStrategy {
    suspend fun execute(activity: EmojiDetailActivity): Result<Intent>

    data class DrawableShareStrategy(
        private val drawableId: Int,
        private val packageName: String
    ) : ShareStrategy {

        override suspend fun execute(activity: EmojiDetailActivity): Result<Intent> = runCatching {
            withContext(Dispatchers.IO) {
                val drawable = ContextCompat.getDrawable(activity, drawableId)
                    ?: throw IllegalStateException("Drawable not found")

                val bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
                Canvas(bitmap).apply {
                    drawable.setBounds(0, 0, width, height)
                    drawable.draw(this)
                }

                val file = File(activity.cacheDir, "shared_emoji_${System.currentTimeMillis()}.png")
                FileOutputStream(file).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }

                val uri = FileProvider.getUriForFile(activity, "$packageName.fileprovider", file)

                Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, "Check out this emoji sticker!")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
        }
    }

    data class GifShareStrategy(
        private val gifPath: String,
        private val packageName: String
    ) : ShareStrategy {

        override suspend fun execute(activity: EmojiDetailActivity): Result<Intent> = runCatching {
            withContext(Dispatchers.IO) {
                val inputStream = activity.assets.open(gifPath)
                val file = File(activity.cacheDir, "shared_emoji_${System.currentTimeMillis()}.gif")

                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                inputStream.close()

                val uri = FileProvider.getUriForFile(activity, "$packageName.fileprovider", file)

                Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "image/gif"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, "Check out this animated emoji sticker!")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
        }
    }
}

// 策略模式：保存策略
sealed interface SaveStrategy {
    suspend fun execute(activity: EmojiDetailActivity): Boolean

    data class DrawableSaveStrategy(private val drawableId: Int) : SaveStrategy {
        override suspend fun execute(activity: EmojiDetailActivity): Boolean =
            withContext(Dispatchers.IO) {
                runCatching {
                    val bitmap = activity.createBitmapFromDrawable(drawableId) ?: return@runCatching false
                    val filename = "emoji_pic_${System.currentTimeMillis()}.png"
                    activity.saveToStorage(
                        filename = filename,
                        mimeType = "image/png",
                        saveAction = { outputStream ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                        }
                    )
                }.getOrElse { false }
            }
    }

    data class GifSaveStrategy(private val gifPath: String) : SaveStrategy {
        override suspend fun execute(activity: EmojiDetailActivity): Boolean =
            withContext(Dispatchers.IO) {
                runCatching {
                    val filename = "emoji_gif_${System.currentTimeMillis()}.gif"
                    activity.saveToStorage(
                        filename = filename,
                        mimeType = "image/gif",
                        saveAction = { outputStream ->
                            activity.assets.open(gifPath).use { inputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                    )
                }.getOrElse { false }
            }
    }
}

// 建造者模式：UI状态构建器
data class UIState private constructor(
    val title: String,
    val resourceType: String,
    val drawableId: Int,
    val gifPath: String,
    val position: Int,
    val emojiType: String
) {
    class Builder {
        private var title: String = ""
        private var resourceType: String = ""
        private var drawableId: Int = 0
        private var gifPath: String = ""
        private var position: Int = 0
        private var emojiType: String = ""

        fun title(title: String) = apply { this.title = title }
        fun resourceType(type: String) = apply { this.resourceType = type }
        fun drawableId(id: Int) = apply { this.drawableId = id }
        fun gifPath(path: String) = apply { this.gifPath = path }
        fun position(pos: Int) = apply { this.position = pos }
        fun emojiType(type: String) = apply { this.emojiType = type }

        fun build() = UIState(title, resourceType, drawableId, gifPath, position, emojiType)
    }

    companion object {
        fun builder() = Builder()

        @JvmStatic
        fun fromIntent(intent: Intent): UIState = builder()
            .title(intent.getStringExtra("title") ?: "Emoji Detail")
            .resourceType(intent.getStringExtra("resource_type") ?: "drawable")
            .drawableId(intent.getIntExtra("drawable_id", 0))
            .gifPath(intent.getStringExtra("gif_path") ?: "")
            .position(intent.getIntExtra("position", 0))
            .emojiType(intent.getStringExtra("emoji_type") ?: "love")
            .build()
    }
}

class EmojiDetailActivity : AppCompatActivity() {

    private val binding by lazy { ActivityEmojiDetailBinding.inflate(layoutInflater) }

    // 观察者模式：状态变化监听
    private var uiState: UIState by Delegates.observable(
        UIState.builder().build()
    ) { _, _, newState ->
        renderUI(newState)
    }

    // 函数式编程：权限结果处理
    private val permissionResultProcessor: (Boolean) -> Unit = { isGranted ->
        when (isGranted) {
            true -> performDownload()
            false -> showMessage("Permission denied. Cannot download image.")
        }
    }

    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeActivity()
    }

    // 链式初始化
    private fun initializeActivity() {
        setupLayout()
            .also { initializePermissionLauncher() }
            .also { extractDataFromIntent() }
            .also { configureClickHandlers() }
    }

    private fun setupLayout(): EmojiDetailActivity {
        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.detail)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        supportActionBar?.hide()
        return this
    }

    private fun initializePermissionLauncher() {
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
            permissionResultProcessor
        )
    }

    private fun extractDataFromIntent() {
        uiState = UIState.fromIntent(intent)
    }

    // 函数式风格：UI渲染
    private fun renderUI(state: UIState) {
        binding.tvTitle.text = state.title

        val imageLoader = createImageLoader(state)
        imageLoader.invoke()
    }

    // 高阶函数：图片加载器工厂
    private fun createImageLoader(state: UIState): () -> Unit = when (state.resourceType) {
        "drawable" -> { ->
            loadDrawableImage(state.drawableId)
        }
        "gif" -> { ->
            loadGifImage(state.gifPath)
        }
        else -> { ->
            loadDrawableImage(R.drawable.ic_launcher_foreground)
        }
    }

    private fun loadDrawableImage(drawableId: Int) {
        Glide.with(MyAppGlideModule.instance)
            .load(drawableId)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .placeholder(R.drawable.ic_launcher_foreground)
            .error(R.drawable.ic_launcher_foreground)
            .into(binding.ivEmojiDetail)
    }

    private fun loadGifImage(gifPath: String) {
        Glide.with(MyAppGlideModule.instance)
            .asGif()
            .load("file:///android_asset/$gifPath")
            .diskCacheStrategy(DiskCacheStrategy.DATA)
            .placeholder(R.drawable.ic_launcher_foreground)
            .error(R.drawable.ic_launcher_foreground)
            .into(binding.ivEmojiDetail)
    }

    private fun configureClickHandlers() {
        val clickHandlers = mapOf<Int, () -> Unit>(
            R.id.img_back to ::finish,
            R.id.btn_share to ::initiateShare,
            R.id.btn_download to ::checkPermissionAndDownload
        )

        clickHandlers.forEach { (viewId, action) ->
            findViewById<android.view.View>(viewId)?.setOnClickListener { action() }
        }

    }

    // 函数式风格：分享逻辑
    private fun initiateShare() {
        lifecycleScope.launch {
            val shareStrategy = createShareStrategy()

            shareStrategy.execute(this@EmojiDetailActivity)
                .onSuccess { shareIntent ->
                    withContext(Dispatchers.Main) {
                        startActivity(Intent.createChooser(shareIntent, getShareTitle()))
                    }
                }
                .onFailure { exception ->
                    withContext(Dispatchers.Main) {
                        showMessage("Share failed: ${exception.message}")
                    }
                }
        }
    }

    // 策略工厂方法
    private fun createShareStrategy(): ShareStrategy = when (uiState.resourceType) {
        "drawable" -> ShareStrategy.DrawableShareStrategy(uiState.drawableId, packageName)
        "gif" -> ShareStrategy.GifShareStrategy(uiState.gifPath, packageName)
        else -> throw IllegalStateException("Unknown resource type: ${uiState.resourceType}")
    }

    private fun getShareTitle(): String = when (uiState.resourceType) {
        "gif" -> "Share GIF"
        else -> "Share Emoji"
    }

    // 权限检查和下载
    private fun checkPermissionAndDownload() {
        val hasPermission = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> true
            else -> ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

        if (hasPermission) {
            performDownload()
        } else {
            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    private fun performDownload() {
        lifecycleScope.launch {
            val saveStrategy = createSaveStrategy()
            val success = saveStrategy.execute(this@EmojiDetailActivity)

            withContext(Dispatchers.Main) {
                val message = if (success) {
                    val fileType = if (uiState.resourceType == "gif") "GIF" else "Image"
                    "$fileType saved to album"
                } else {
                    "Saving failed"
                }
                showMessage(message)
            }
        }
    }

    // 策略工厂方法
    private fun createSaveStrategy(): SaveStrategy = when (uiState.resourceType) {
        "drawable" -> SaveStrategy.DrawableSaveStrategy(uiState.drawableId)
        "gif" -> SaveStrategy.GifSaveStrategy(uiState.gifPath)
        else -> throw IllegalStateException("Unknown resource type: ${uiState.resourceType}")
    }

    // 高阶函数：统一保存逻辑
    internal inline fun saveToStorage(
        filename: String,
        mimeType: String,
        crossinline saveAction: (java.io.OutputStream) -> Unit
    ): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveToModernStorage(filename, mimeType, saveAction)
        } else {
            saveToLegacyStorage(filename, saveAction)
        }
    }

    private inline fun saveToModernStorage(
        filename: String,
        mimeType: String,
        crossinline saveAction: (java.io.OutputStream) -> Unit
    ): Boolean {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        return contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?.let { uri ->
                contentResolver.openOutputStream(uri)?.use(saveAction)
                true
            } ?: false
    }

    private inline fun saveToLegacyStorage(
        filename: String,
        crossinline saveAction: (java.io.OutputStream) -> Unit
    ): Boolean {
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val file = File(picturesDir, filename)

        return runCatching {
            FileOutputStream(file).use(saveAction)
            sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))
            true
        }.getOrElse { false }
    }

    // 工具方法
    internal fun createBitmapFromDrawable(drawableId: Int): Bitmap? {
        return ContextCompat.getDrawable(this, drawableId)?.let { drawable ->
            val bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
            Canvas(bitmap).apply {
                drawable.setBounds(0, 0, width, height)
                drawable.draw(this)
            }
            bitmap
        }
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
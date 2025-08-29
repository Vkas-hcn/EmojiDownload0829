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
import kotlin.properties.Delegates

// 观察者模式：下载状态监听器
interface DownloadObserver {
    fun onDownloadStart(total: Int)
    fun onDownloadProgress(current: Int, total: Int)
    fun onDownloadComplete(success: Int, total: Int)
    fun onDownloadError(error: String)
}

// 策略模式：权限处理策略
sealed class PermissionStrategy {
    abstract fun checkPermission(activity: EmojiListActivity): Boolean
    abstract fun requestPermission(launcher: ActivityResultLauncher<String>)

    object ModernAndroidStrategy : PermissionStrategy() {
        override fun checkPermission(activity: EmojiListActivity): Boolean =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

        override fun requestPermission(launcher: ActivityResultLauncher<String>) {
            // Android 10+ 不需要请求存储权限
        }
    }

    data class LegacyAndroidStrategy(private val permission: String = Manifest.permission.WRITE_EXTERNAL_STORAGE) : PermissionStrategy() {
        override fun checkPermission(activity: EmojiListActivity): Boolean =
            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED

        override fun requestPermission(launcher: ActivityResultLauncher<String>) {
            launcher.launch(permission)
        }
    }
}

// 建造者模式：Intent构建器
class EmojiDetailIntentBuilder private constructor(private val activity: EmojiListActivity) {
    private var resourceType: String = ""
    private var drawableId: Int = 0
    private var gifPath: String = ""
    private var position: Int = 0
    private var emojiType: String = ""
    private var titleText: String = ""

    companion object {
        @JvmStatic
        fun from(activity: EmojiListActivity): EmojiDetailIntentBuilder =
            EmojiDetailIntentBuilder(activity)
    }

    fun withResource(resource: ImageResource, position: Int): EmojiDetailIntentBuilder = apply {
        this.position = position
        when (resource) {
            is ImageResource.DrawableRes -> {
                resourceType = "drawable"
                drawableId = resource.drawableId
            }
            is ImageResource.GifAsset -> {
                resourceType = "gif"
                gifPath = resource.gifPath
            }
        }
    }

    fun withMetadata(emojiType: String, titleText: String): EmojiDetailIntentBuilder = apply {
        this.emojiType = emojiType
        this.titleText = titleText
    }

    fun build(): Intent = Intent(activity, EmojiDetailActivity::class.java).apply {
        putExtra("resource_type", resourceType)
        putExtra("drawable_id", drawableId)
        putExtra("gif_path", gifPath)
        putExtra("position", position)
        putExtra("emoji_type", emojiType)
        putExtra("title", titleText)
    }
}

class EmojiListActivity : AppCompatActivity() {
    private val binding by lazy { ActivityEmojiListBinding.inflate(layoutInflater) }

    // 使用委托属性观察状态变化
    private var emojiList: List<ImageResource> by Delegates.observable(emptyList()) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            updateUI()
        }
    }

    // 单独声明属性
    private var emojiType: String = ""
    private var titleText: String = ""

    // 策略模式：权限处理
    private val permissionStrategy: PermissionStrategy by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            PermissionStrategy.ModernAndroidStrategy
        } else {
            PermissionStrategy.LegacyAndroidStrategy()
        }
    }

    // 函数式编程：高阶函数处理权限结果
    private val permissionResultHandler: (Boolean) -> Unit = { isGranted ->
        when {
            isGranted -> executeDownloadAllImages()
            else -> showToast("Permission denied. Cannot download images.")
        }
    }

    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var emojiAdapter: EmojiAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupView()
        initializeComponents()
        configureUIElements()
        setupEventHandlers()
    }

    // 函数式重构：链式初始化
    private fun setupView() {
        enableEdgeToEdge()
        setContentView(binding.root)
        findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.emoji)?.let { view ->
            ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }
        supportActionBar?.hide()
    }

    private fun initializeComponents() {
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
            permissionResultHandler
        )
        extractIntentData()
    }

    // 函数式风格：使用高阶函数处理Intent数据
    private fun extractIntentData() {
        val intentExtractor: (String, String) -> String = { key, defaultValue ->
            intent.getStringExtra(key) ?: defaultValue
        }

        emojiType = intentExtractor("emoji_type", "love")
        titleText = intentExtractor("title", "Emoji Stickers")

        // 使用策略模式获取资源列表
        emojiList = ImageDataCon[emojiType]
    }

    private fun configureUIElements() {
        binding.run {
            tvTitle.text = titleText
            tvName1.text = titleText
            tvNum1.text = "${emojiList.size} Stickers"

            // 使用函数式方式配置第一个图片
            emojiList.firstOrNull()?.let(::loadFirstImage)

            rvEmoji.layoutManager = GridLayoutManager(this@EmojiListActivity, 3)
            emojiAdapter = createEmojiAdapter()
            rvEmoji.adapter = emojiAdapter
        }
    }

    // 高阶函数：创建适配器
    private fun createEmojiAdapter(): EmojiAdapter {
        val itemClickHandler: (ImageResource, Int) -> Unit = { imageResource, position ->
            EmojiDetailIntentBuilder
                .from(this)
                .withResource(imageResource, position)
                .withMetadata(emojiType, titleText)
                .build()
                .let(::startActivity)
        }

        return EmojiAdapter(emojiList, itemClickHandler)
    }

    // 策略模式：图片加载
    private fun loadFirstImage(imageResource: ImageResource) = with(binding.imgIcon) {
        val glideBuilder = Glide.with(MyAppGlideModule.instance)

        when (imageResource) {
            is ImageResource.DrawableRes -> {
                glideBuilder.load(imageResource.drawableId).into(this)
            }
            is ImageResource.GifAsset -> {
                glideBuilder.asGif()
                    .load("file:///android_asset/${imageResource.gifPath}")
                    .diskCacheStrategy(DiskCacheStrategy.DATA)
                    .into(this)
            }
        }
    }

    private fun setupEventHandlers() {
        binding.imgBack.setOnClickListener { finish() }
        binding.btnDownloadAll.setOnClickListener { initiateDownload() }

        onBackPressedDispatcher.addCallback(this) { finish() }
    }

    private fun updateUI() {
        binding.tvNum1.text = "${emojiList.size} Stickers"
        ::emojiAdapter.isInitialized.takeIf { it }?.let {
            emojiAdapter.notifyDataSetChanged()
        }
    }

    // 函数式风格：权限检查和下载
    private fun initiateDownload() {
        val hasPermission = permissionStrategy.checkPermission(this)
        if (hasPermission) {
            executeDownloadAllImages()
        } else {
            permissionStrategy.requestPermission(permissionLauncher)
        }
    }

    private fun executeDownloadAllImages() {
        val downloadObserver = createDownloadObserver()

        lifecycleScope.launch {
            try {
                downloadObserver.onDownloadStart(emojiList.size)

                val downloadResults = withContext(Dispatchers.IO) {
                    emojiList
                        .asSequence()
                        .mapIndexed { index, imageResource ->
                            val filename = "${emojiType}_${index + 1}"
                            val success = performSingleDownload(imageResource, filename)
                            downloadObserver.onDownloadProgress(index + 1, emojiList.size)
                            success
                        }
                        .count { it }
                }

                downloadObserver.onDownloadComplete(downloadResults, emojiList.size)

            } catch (e: Exception) {
                downloadObserver.onDownloadError(e.message ?: "Unknown error")
            }
        }
    }

    // 高阶函数：创建下载观察者
    private fun createDownloadObserver(): DownloadObserver = object : DownloadObserver {
        override fun onDownloadStart(total: Int) {
            // 可以添加进度条显示
        }

        override fun onDownloadProgress(current: Int, total: Int) {
            // 更新进度
        }

        override fun onDownloadComplete(success: Int, total: Int) {
            showToast("Downloaded $success/$total images successfully")
        }

        override fun onDownloadError(error: String) {
            showToast("Download failed: $error")
        }
    }

    // 策略模式：文件保存处理
    private fun performSingleDownload(imageResource: ImageResource, fileName: String): Boolean {
        return try {
            when (imageResource) {
                is ImageResource.DrawableRes -> saveDrawableToStorage(imageResource.drawableId, fileName)
                is ImageResource.GifAsset -> saveGifToStorage(imageResource.gifPath, fileName)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // 函数式风格：文件保存逻辑
    private fun saveDrawableToStorage(drawableId: Int, fileName: String): Boolean {
        return runCatching {
            val bitmap = createBitmapFromDrawable(drawableId) ?: return false
            val filename = "$fileName.png"

            saveToMediaStore(
                bitmap = bitmap,
                filename = filename,
                mimeType = "image/png",
                compressAction = { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
            )
        }.getOrElse { false }
    }

    private fun saveGifToStorage(gifPath: String, fileName: String): Boolean {
        return runCatching {
            val inputStream = assets.open(gifPath)
            val filename = "$fileName.gif"

            saveToMediaStore(
                inputStream = inputStream,
                filename = filename,
                mimeType = "image/gif",
                copyAction = { outputStream ->
                    inputStream.copyTo(outputStream)
                    inputStream.close()
                }
            )
        }.getOrElse { false }
    }

    // 高阶函数：统一的媒体存储保存逻辑
    private inline fun saveToMediaStore(
        bitmap: Bitmap? = null,
        inputStream: InputStream? = null,
        filename: String,
        mimeType: String,
        noinline compressAction: ((java.io.OutputStream) -> Unit)? = null,
        noinline copyAction: ((java.io.OutputStream) -> Unit)? = null
    ): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveToModernMediaStore(filename, mimeType, compressAction, copyAction)
        } else {
            saveToLegacyStorage(filename, mimeType, bitmap, inputStream, compressAction, copyAction)
        }
    }

    private inline fun saveToModernMediaStore(
        filename: String,
        mimeType: String,
        noinline compressAction: ((java.io.OutputStream) -> Unit)?,
        noinline copyAction: ((java.io.OutputStream) -> Unit)?
    ): Boolean {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        return contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?.let { uri ->
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    compressAction?.invoke(outputStream) ?: copyAction?.invoke(outputStream)
                }
                true
            } ?: false
    }

    private inline fun saveToLegacyStorage(
        filename: String,
        mimeType: String,
        bitmap: Bitmap?,
        inputStream: InputStream?,
        noinline compressAction: ((java.io.OutputStream) -> Unit)?,
        noinline copyAction: ((java.io.OutputStream) -> Unit)?
    ): Boolean {
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val file = File(picturesDir, filename)

        return FileOutputStream(file).use { fileOutput ->
            when {
                bitmap != null && compressAction != null -> compressAction(fileOutput)
                inputStream != null && copyAction != null -> copyAction(fileOutput)
                else -> return false
            }

            sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))
            true
        }
    }

    // 辅助函数：从Drawable创建Bitmap
    private fun createBitmapFromDrawable(drawableId: Int): Bitmap? {
        return ContextCompat.getDrawable(this, drawableId)?.let { drawable ->
            val bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
    }

    // 扩展函数：简化Toast显示
    private fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, message, duration).show()
    }
}
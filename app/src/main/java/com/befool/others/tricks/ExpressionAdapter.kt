package com.befool.others.tricks

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import kotlin.properties.Delegates

// 策略模式：图片加载策略
sealed interface ImageLoadStrategy {
    fun load(imageView: ImageView, resource: ImageResource)

    object DrawableLoadStrategy : ImageLoadStrategy {
        override fun load(imageView: ImageView, resource: ImageResource) {
            require(resource is ImageResource.DrawableRes) { "Resource must be DrawableRes" }

            Glide.with(MyAppGlideModule.instance)
                .load(resource.drawableId)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .transition(DrawableTransitionOptions.withCrossFade())
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .into(imageView)
        }
    }

    object GifLoadStrategy : ImageLoadStrategy {
        override fun load(imageView: ImageView, resource: ImageResource) {
            require(resource is ImageResource.GifAsset) { "Resource must be GifAsset" }

            Glide.with(MyAppGlideModule.instance)
                .asGif()
                .load("file:///android_asset/${resource.gifPath}")
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .into(imageView)
        }
    }
}

// 函数式编程：ViewHolder工厂
object ViewHolderFactory {

    // 高阶函数：创建ViewHolder
    fun create(
        parent: ViewGroup,
        layoutId: Int = R.layout.item_emoji,
        clickHandler: (ImageResource, Int) -> Unit
    ): EmojiAdapter.EmojiViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return EmojiAdapter.EmojiViewHolder(view, clickHandler)
    }
}

// 策略模式：加载策略工厂
object LoadStrategyFactory {

    private val strategies = mapOf<String, ImageLoadStrategy>(
        "drawable" to ImageLoadStrategy.DrawableLoadStrategy,
        "gif" to ImageLoadStrategy.GifLoadStrategy
    )

    fun getStrategy(resource: ImageResource): ImageLoadStrategy = when (resource) {
        is ImageResource.DrawableRes -> strategies["drawable"]
        is ImageResource.GifAsset -> strategies["gif"]
    } ?: ImageLoadStrategy.DrawableLoadStrategy

    // 操作符重载：允许通过索引访问策略
    operator fun get(resource: ImageResource): ImageLoadStrategy = getStrategy(resource)
}

class EmojiAdapter(
    initialList: List<ImageResource>,
    private val itemClickListener: (ImageResource, Int) -> Unit
) : RecyclerView.Adapter<EmojiAdapter.EmojiViewHolder>() {

    // 观察者模式：数据变化监听
    private var emojiList: List<ImageResource> by Delegates.observable(initialList) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            notifyDataSetChanged()
        }
    }

    // 操作符重载：允许通过索引访问元素
    operator fun get(position: Int): ImageResource? =
        emojiList.getOrNull(position)

    // 操作符重载：允许设置元素
    operator fun set(position: Int, resource: ImageResource) {
        if (position in emojiList.indices) {
            emojiList = emojiList.toMutableList().apply {
                set(position, resource)
            }
        }
    }

    // 操作符重载：加法运算用于添加元素
    operator fun plus(resource: ImageResource): EmojiAdapter {
        emojiList = emojiList + resource
        return this
    }

    // 操作符重载：减法运算用于移除元素
    operator fun minus(resource: ImageResource): EmojiAdapter {
        emojiList = emojiList - resource
        return this
    }

    // 操作符重载：包含检查
    operator fun contains(resource: ImageResource): Boolean = resource in emojiList

    inner class EmojiViewHolder(
        itemView: View,
        private val clickHandler: (ImageResource, Int) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        val emojiImage: ImageView = itemView.findViewById(R.id.iv_emoji)

        // 函数式编程：使用高阶函数处理点击事件
        private val clickProcessor: () -> Unit = {
            adapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { position ->
                emojiList.getOrNull(position)?.let { resource ->
                    clickHandler(resource, position)
                }
            }
        }

        init {
            itemView.setOnClickListener { clickProcessor() }
        }

        // 函数式风格：绑定数据
        fun bind(imageResource: ImageResource) {
            val loadStrategy = LoadStrategyFactory[imageResource]
            loadStrategy.load(emojiImage, imageResource)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmojiViewHolder =
        ViewHolderFactory.create(parent, R.layout.item_emoji, itemClickListener)

    override fun onBindViewHolder(holder: EmojiViewHolder, position: Int) {
        emojiList.getOrNull(position)?.let(holder::bind)
    }

    override fun getItemCount(): Int = emojiList.size

    // 函数式编程：使用序列获取第一个资源
    fun getFirstImageResource(): ImageResource? =
        emojiList.asSequence().firstOrNull()

    // 高阶函数：提供过滤功能
    fun filtered(predicate: (ImageResource) -> Boolean): Sequence<ImageResource> =
        emojiList.asSequence().filter(predicate)

    // 高阶函数：提供映射功能
    fun <R> mapped(transform: (ImageResource) -> R): Sequence<R> =
        emojiList.asSequence().map(transform)

    // 函数式编程：分组功能
    fun groupedByType(): Map<String, List<ImageResource>> =
        emojiList.groupBy { resource ->
            when (resource) {
                is ImageResource.DrawableRes -> "drawable"
                is ImageResource.GifAsset -> "gif"
            }
        }

    // 高阶函数：查找功能
    fun findFirst(predicate: (ImageResource) -> Boolean): ImageResource? =
        emojiList.asSequence().find(predicate)

    // 函数式编程：统计功能
    fun countByType(): Map<String, Int> =
        emojiList.asSequence()
            .groupingBy { resource ->
                when (resource) {
                    is ImageResource.DrawableRes -> "drawable"
                    is ImageResource.GifAsset -> "gif"
                }
            }
            .eachCount()

    // 高阶函数：批量操作
    fun processEach(action: (ImageResource, Int) -> Unit) {
        emojiList.forEachIndexed(action)
    }

    // 函数式编程：聚合操作
    fun <R> foldResources(initial: R, operation: (R, ImageResource) -> R): R =
        emojiList.fold(initial, operation)

    // 高阶函数：更新数据
    fun updateData(
        newList: List<ImageResource>,
        diffCallback: ((List<ImageResource>, List<ImageResource>) -> Unit)? = null
    ) {
        val oldList = emojiList
        emojiList = newList
        diffCallback?.invoke(oldList, newList)
    }

    // 函数式编程：转换为其他集合类型
    fun asSequence(): Sequence<ImageResource> = emojiList.asSequence()

    // 操作符重载：迭代器支持
    operator fun iterator(): Iterator<ImageResource> = emojiList.iterator()
}

// 扩展函数：为适配器添加额外功能
fun EmojiAdapter.isEmpty(): Boolean = itemCount == 0

fun EmojiAdapter.isNotEmpty(): Boolean = !isEmpty()

// 高阶函数：创建适配器的便捷方法
fun createEmojiAdapter(
    resources: List<ImageResource>,
    clickHandler: (ImageResource, Int) -> Unit
): EmojiAdapter = EmojiAdapter(resources, clickHandler)

// 操作符重载：允许使用 + 操作符合并适配器数据
operator fun List<ImageResource>.plus(adapter: EmojiAdapter): List<ImageResource> =
    this + adapter.asSequence().toList()

// 函数式编程：为资源列表添加便捷操作
fun List<ImageResource>.toEmojiAdapter(
    clickHandler: (ImageResource, Int) -> Unit
): EmojiAdapter = createEmojiAdapter(this, clickHandler)
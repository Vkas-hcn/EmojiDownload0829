package com.befool.others.tricks

import kotlin.sequences.asSequence

// 使用密封接口替代密封类，并添加操作符重载
sealed interface ImageResource {
    val identifier: String

    // 操作符重载：加法运算用于合并资源信息
    operator fun plus(suffix: String): String = "${identifier}_$suffix"

    // 操作符重载：索引访问
    operator fun get(key: String): String = when(key) {
        "type" -> when(this) {
            is DrawableRes -> "drawable"
            is GifAsset -> "gif"
        }
        "path" -> when(this) {
            is DrawableRes -> "res/$drawableId"
            is GifAsset -> gifPath
        }
        else -> identifier
    }

    @JvmInline
    value class DrawableRes(val drawableId: Int) : ImageResource {
        override val identifier: String get() = "drawable_$drawableId"
    }

    @JvmInline
    value class GifAsset(val gifPath: String) : ImageResource {
        override val identifier: String get() = "gif_${gifPath.hashCode()}"
    }
}

// 策略模式：资源加载策略
sealed interface ResourceLoadStrategy {
    fun loadResource(resource: ImageResource): String

    object DrawableStrategy : ResourceLoadStrategy {
        override fun loadResource(resource: ImageResource): String =
            (resource as ImageResource.DrawableRes).drawableId.toString()
    }

    object GifStrategy : ResourceLoadStrategy {
        override fun loadResource(resource: ImageResource): String =
            "file:///android_asset/${(resource as ImageResource.GifAsset).gifPath}"
    }
}

// 建造者模式：资源集合构建器
class ResourceCollectionBuilder private constructor() {
    private val resourceList = mutableListOf<ImageResource>()

    companion object {
        @JvmStatic
        fun create(): ResourceCollectionBuilder = ResourceCollectionBuilder()
    }

    fun addDrawable(vararg ids: Int): ResourceCollectionBuilder = apply {
        ids.asSequence()
            .map { ImageResource.DrawableRes(it) }
            .forEach(resourceList::add)
    }

    fun addGif(vararg paths: String): ResourceCollectionBuilder = apply {
        paths.asSequence()
            .map { ImageResource.GifAsset(it) }
            .forEach(resourceList::add)
    }

    fun build(): Sequence<ImageResource> = resourceList.asSequence()

    // 操作符重载：plusAssign用于添加资源
    operator fun plusAssign(resource: ImageResource) {
        resourceList += resource
    }
}

// 函数式风格重构数据容器
object ImageDataCon {

    // 使用高阶函数生成资源序列
    private inline fun gifSequence(prefix: String, count: Int): Sequence<ImageResource> =
        (1..count).asSequence()
            .map { "gifs/${prefix}_$it.gif" }
            .map(ImageResource::GifAsset)

    private inline fun drawableSequence(prefix: String, ids: IntArray): Sequence<ImageResource> =
        ids.asSequence().map { resourceId ->
            // 使用反射动态获取资源ID（模拟实际使用）
            ImageResource.DrawableRes(resourceId)
        }

    // 函数式风格重构资源列表
    @get:JvmName("getIconLover")
    val iconLover: List<ImageResource> by lazy {
        gifSequence("ic_love", 4).toList()
    }

    @get:JvmName("getIconQi")
    val iconQi: List<ImageResource> by lazy {
        ResourceCollectionBuilder.create()
            .addDrawable(
                R.drawable.ic_qi_1, R.drawable.ic_qi_2, R.drawable.ic_qi_3,
                R.drawable.ic_qi_4, R.drawable.ic_qi_5, R.drawable.ic_qi_6,
                R.drawable.ic_qi_7
            )
            .build()
            .toList()
    }

    @get:JvmName("getIconEmoji")
    val iconEmoji: List<ImageResource> by lazy {
        gifSequence("ic_emoji", 6).toList()
    }

    @get:JvmName("getIconCute")
    val iconCute: List<ImageResource> by lazy {
        sequence {
            yield(ImageResource.DrawableRes(R.drawable.ic_cute_1))
            yield(ImageResource.GifAsset("gifs/ic_cute_2.gif"))
            yieldAll(
                sequenceOf(3, 4, 5, 6, 7, 8).map {
                    ImageResource.DrawableRes(getResourceId("ic_cute_$it"))
                }
            )
            yield(ImageResource.GifAsset("gifs/ic_cute_9.gif"))
        }.toList()
    }

    @get:JvmName("getIconCat")
    val iconCat: List<ImageResource> by lazy {
        buildList {
            addAll(
                sequenceOf(1, 2, 3, 6, 7, 8)
                    .map { ImageResource.DrawableRes(getResourceId("ic_cat_$it")) }
            )
            add(ImageResource.GifAsset("gifs/ic_cat_4.gif"))
            add(ImageResource.GifAsset("gifs/ic_cat_5.gif"))
        }.shuffled().sorted()
    }

    @get:JvmName("getIconLine")
    val iconLine: List<ImageResource> by lazy {
        generateSequence(1) { it + 1 }
            .take(9)
            .map { "gifs/ic_line_$it.gif" }
            .map(ImageResource::GifAsset)
            .toList()
    }

    @get:JvmName("getIconBullet")
    val iconBullet: List<ImageResource> by lazy {
        (1..8).asSequence()
            .map { idx -> "gifs/ic_bullet_$idx.gif" }
            .map(ImageResource::GifAsset)
            .toList()
    }

    // 策略模式：根据类型获取资源列表
    private val resourceStrategies = mapOf<String, () -> List<ImageResource>>(
        "love" to ::iconLover,
        "qi" to ::iconQi,
        "emoji" to ::iconEmoji,
        "cute" to ::iconCute,
        "cat" to ::iconCat,
        "line" to ::iconLine,
        "bullet" to ::iconBullet
    )

    // 高阶函数：获取资源列表
    fun getResourcesByType(type: String): List<ImageResource> =
        resourceStrategies[type]?.invoke() ?: iconLover

    // 操作符重载：允许通过索引访问资源类型
    operator fun get(type: String): List<ImageResource> = getResourcesByType(type)

    // 辅助函数：模拟资源ID获取
    private fun getResourceId(name: String): Int =
        name.hashCode().let { if (it < 0) -it else it } % 100000
}

// 扩展函数：为List添加差异化操作
fun List<ImageResource>.filterByType(predicate: (ImageResource) -> Boolean): Sequence<ImageResource> =
    asSequence().filter(predicate)

// 操作符重载：为ImageResource添加比较操作
private fun ImageResource.compareTo(other: ImageResource): Int =
    this.identifier.compareTo(other.identifier)

private fun List<ImageResource>.sorted(): List<ImageResource> =
    sortedWith { a, b -> a.identifier.compareTo(b.identifier) }
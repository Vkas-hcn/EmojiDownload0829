package com.befool.others.tricks

sealed interface ImageResource {
    val identifier: String

    operator fun plus(suffix: String): String = "${identifier}_$suffix"

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

object ImageDataCon {

    private inline fun gifSequence(prefix: String, count: Int): Sequence<ImageResource> =
        (1..count).asSequence()
            .map { "gifs/${prefix}_$it.gif" }
            .map(ImageResource::GifAsset)

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
        buildList {
            add(ImageResource.DrawableRes(R.drawable.ic_cute_1))
            add(ImageResource.GifAsset("gifs/ic_cute_2.gif"))
            add(ImageResource.DrawableRes(R.drawable.ic_cute_3))
            add(ImageResource.DrawableRes(R.drawable.ic_cute_4))
            add(ImageResource.DrawableRes(R.drawable.ic_cute_5))
            add(ImageResource.DrawableRes(R.drawable.ic_cute_6))
            add(ImageResource.DrawableRes(R.drawable.ic_cute_7))
            add(ImageResource.DrawableRes(R.drawable.ic_cute_8))


            add(ImageResource.GifAsset("gifs/ic_cute_9.gif"))
        }
    }

    @get:JvmName("getIconCat")
    val iconCat: List<ImageResource> by lazy {
        buildList {
            add(ImageResource.DrawableRes(R.drawable.ic_cat_1))
            add(ImageResource.DrawableRes(R.drawable.ic_cat_2))
            add(ImageResource.DrawableRes(R.drawable.ic_cat_3))
            add(ImageResource.GifAsset("gifs/ic_cat_4.gif"))
            add(ImageResource.GifAsset("gifs/ic_cat_5.gif"))
            add(ImageResource.DrawableRes(R.drawable.ic_cat_6))
            add(ImageResource.DrawableRes(R.drawable.ic_cat_7))
            add(ImageResource.DrawableRes(R.drawable.ic_cat_8))
        }
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
}

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

    private inline fun jpgSequence(prefix: String, count: Int): Sequence<ImageResource> =
        (1..count).asSequence()
            .map { "gifs/${prefix}_$it.jpg" }
            .map(ImageResource::GifAsset)
    @get:JvmName("getIconEmoji")
    val iconEmoji: List<ImageResource> by lazy {
        gifSequence("emoji", 11).toList()
    }
    @get:JvmName("getIconBox")
    val iconBox: List<ImageResource> by lazy {
        gifSequence("box", 8).toList()
    }

    @get:JvmName("getIconDog")
    val iconDog: List<ImageResource> by lazy {
        gifSequence("dog", 6).toList()
    }

    @get:JvmName("getIconFace")
    val iconFace: List<ImageResource> by lazy {
        gifSequence("face", 9).toList()

    }
    @get:JvmName("getIconCute")
    val iconCute: List<ImageResource> by lazy {
        gifSequence("cute", 9).toList()
    }

    @get:JvmName("getIconCat")
    val iconCat: List<ImageResource> by lazy {
        ResourceCollectionBuilder.create()
            .addDrawable(
                R.drawable.cat_1,
                R.drawable.cat_2,
                R.drawable.cat_3,
                R.drawable.cat_4,
                R.drawable.cat_5,
                R.drawable.cat_6,
                R.drawable.cat_7,
                R.drawable.cat_8,
                R.drawable.cat_9,
            )
            .build()
            .toList()
    }
    @get:JvmName("getIconPat")
    val iconPat: List<ImageResource> by lazy {
        gifSequence("paw", 11).toList()

    }


    // 策略模式：根据类型获取资源列表
    private val resourceStrategies = mapOf<String, () -> List<ImageResource>>(
        "emoji" to ::iconEmoji,
        "iconBox" to ::iconBox,
        "dog" to ::iconDog,
        "cute" to ::iconCute,
        "cat" to ::iconCat,
        "face" to ::iconFace,
        "paw" to ::iconPat
    )

    // 高阶函数：获取资源列表
    fun getResourcesByType(type: String): List<ImageResource> =
        resourceStrategies[type]?.invoke() ?: iconBox

    // 操作符重载：允许通过索引访问资源类型
    operator fun get(type: String): List<ImageResource> = getResourcesByType(type)
}

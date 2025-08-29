package com.befool.others.tricks


sealed class ImageResource {

    data class DrawableRes(val drawableId: Int) : ImageResource()


    data class GifAsset(val gifPath: String) : ImageResource()
}

object ImageDataCon {
    val iconLover = listOf(
        ImageResource.GifAsset("gifs/ic_love_1.gif"),
        ImageResource.GifAsset("gifs/ic_love_2.gif"),
        ImageResource.GifAsset("gifs/ic_love_3.gif"),
        ImageResource.GifAsset("gifs/ic_love_4.gif"),
    )

    val iconQi = listOf(
        ImageResource.DrawableRes(R.drawable.ic_qi_1),
        ImageResource.DrawableRes(R.drawable.ic_qi_2),
        ImageResource.DrawableRes(R.drawable.ic_qi_3),
        ImageResource.DrawableRes(R.drawable.ic_qi_4),
        ImageResource.DrawableRes(R.drawable.ic_qi_5),
        ImageResource.DrawableRes(R.drawable.ic_qi_6),
        ImageResource.DrawableRes(R.drawable.ic_qi_7),
    )

    val iconEmoji = listOf(
        ImageResource.GifAsset("gifs/ic_emoji_1.gif"),
        ImageResource.GifAsset("gifs/ic_emoji_2.gif"),
        ImageResource.GifAsset("gifs/ic_emoji_3.gif"),
        ImageResource.GifAsset("gifs/ic_emoji_4.gif"),
        ImageResource.GifAsset("gifs/ic_emoji_5.gif"),
        ImageResource.GifAsset("gifs/ic_emoji_6.gif"),
    )

    val iconCute = listOf(
        ImageResource.DrawableRes(R.drawable.ic_cute_1),
        ImageResource.GifAsset("gifs/ic_cute_2.gif"),
        ImageResource.DrawableRes(R.drawable.ic_cute_3),
        ImageResource.DrawableRes(R.drawable.ic_cute_4),
        ImageResource.DrawableRes(R.drawable.ic_cute_5),
        ImageResource.DrawableRes(R.drawable.ic_cute_6),
        ImageResource.DrawableRes(R.drawable.ic_cute_7),
        ImageResource.DrawableRes(R.drawable.ic_cute_8),
        ImageResource.GifAsset("gifs/ic_cute_9.gif"),
    )

    val iconCat = listOf(
        ImageResource.DrawableRes(R.drawable.ic_cat_1),
        ImageResource.DrawableRes(R.drawable.ic_cat_2),
        ImageResource.DrawableRes(R.drawable.ic_cat_3),
        ImageResource.GifAsset("gifs/ic_cat_4.gif"),
        ImageResource.GifAsset("gifs/ic_cat_5.gif"),
        ImageResource.DrawableRes(R.drawable.ic_cat_6),
        ImageResource.DrawableRes(R.drawable.ic_cat_7),
        ImageResource.DrawableRes(R.drawable.ic_cat_8),
    )

    val iconLine = listOf(
        ImageResource.GifAsset("gifs/ic_line_1.gif"),
        ImageResource.GifAsset("gifs/ic_line_2.gif"),
        ImageResource.GifAsset("gifs/ic_line_3.gif"),
        ImageResource.GifAsset("gifs/ic_line_4.gif"),
        ImageResource.GifAsset("gifs/ic_line_5.gif"),
        ImageResource.GifAsset("gifs/ic_line_6.gif"),
        ImageResource.GifAsset("gifs/ic_line_7.gif"),
        ImageResource.GifAsset("gifs/ic_line_8.gif"),
        ImageResource.GifAsset("gifs/ic_line_9.gif"),
    )

    val iconBullet = listOf(
        ImageResource.GifAsset("gifs/ic_bullet_1.gif"),
        ImageResource.GifAsset("gifs/ic_bullet_2.gif"),
        ImageResource.GifAsset("gifs/ic_bullet_3.gif"),
        ImageResource.GifAsset("gifs/ic_bullet_4.gif"),
        ImageResource.GifAsset("gifs/ic_bullet_5.gif"),
        ImageResource.GifAsset("gifs/ic_bullet_6.gif"),
        ImageResource.GifAsset("gifs/ic_bullet_7.gif"),
        ImageResource.GifAsset("gifs/ic_bullet_8.gif"),
    )
}
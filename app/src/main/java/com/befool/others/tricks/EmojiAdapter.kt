package com.befool.others.tricks

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

class EmojiAdapter(
    private val emojiList: List<ImageResource>,
    private val onItemClick: (ImageResource, Int) -> Unit // ImageResource, position
) : RecyclerView.Adapter<EmojiAdapter.EmojiViewHolder>() {

    inner class EmojiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val emojiImage: ImageView = itemView.findViewById(R.id.iv_emoji)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(emojiList[position], position)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmojiViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_emoji, parent, false)
        return EmojiViewHolder(view)
    }

    override fun onBindViewHolder(holder: EmojiViewHolder, position: Int) {
        val imageResource = emojiList[position]

        when (imageResource) {
            is ImageResource.DrawableRes -> {
                Glide.with(MyAppGlideModule.instance)
                    .load(imageResource.drawableId)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(holder.emojiImage)
            }

            is ImageResource.GifAsset -> {
                Glide.with(MyAppGlideModule.instance)
                    .asGif()
                    .load("file:///android_asset/${imageResource.gifPath}")
                    .diskCacheStrategy(DiskCacheStrategy.DATA)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(holder.emojiImage)
            }
        }
    }

    override fun getItemCount(): Int = emojiList.size


    fun getFirstImageResource(): ImageResource? {
        return emojiList.firstOrNull()
    }
}
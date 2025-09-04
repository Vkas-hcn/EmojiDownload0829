package com.befool.others.tricks


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

class MainAdapter(
    private val emojiSets: List<EmojiSet>,
    private val onItemClick: (String, String) -> Unit // emoji_type, title
) : RecyclerView.Adapter<MainAdapter.MainViewHolder>() {

    inner class MainViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val emojiImage: ImageView = itemView.findViewById(R.id.emoji_image)
        val titleText: TextView = itemView.findViewById(R.id.tv_name)
        val countText: TextView = itemView.findViewById(R.id.tv_num)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val emojiSet = emojiSets[position]
                    onItemClick(emojiSet.type, emojiSet.title)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_main, parent, false)
        return MainViewHolder(view)
    }

    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        val emojiSet = emojiSets[position]

        val firstImage = emojiSet.imageList.firstOrNull()
        if (firstImage != null) {
            when (firstImage) {
                is ImageResource.DrawableRes -> {
                    Glide.with(MyAppGlideModule.instance)
                        .load(firstImage.drawableId)
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(holder.emojiImage)
                }
                is ImageResource.GifAsset -> {
                    Glide.with(MyAppGlideModule.instance)
                        .asGif()
                        .load("file:///android_asset/${firstImage.gifPath}")
                        .diskCacheStrategy(DiskCacheStrategy.DATA)
                        .into(holder.emojiImage)
                }
            }
        }

        holder.titleText.text = emojiSet.title
        holder.countText.text = "${emojiSet.imageList.size} Stickers"
    }

    override fun getItemCount(): Int = emojiSets.size
}

data class EmojiSet(
    val type: String,
    val title: String,
    val imageList: List<ImageResource>
)

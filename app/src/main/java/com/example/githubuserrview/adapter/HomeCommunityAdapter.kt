package com.example.githubuserrview.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.githubuserrview.R
import com.example.githubuserrview.response.DetailUserResponse

class HomeCommunityAdapter(
    private val users: List<DetailUserResponse>,
    private val onItemClicked: (DetailUserResponse) -> Unit
) : RecyclerView.Adapter<HomeCommunityAdapter.HomeCommunityViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeCommunityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_item, parent, false)
        return HomeCommunityViewHolder(view)
    }

    override fun onBindViewHolder(holder: HomeCommunityViewHolder, position: Int) {
        holder.bind(users[position], onItemClicked)
    }

    override fun getItemCount(): Int = users.size

    class HomeCommunityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val avatarView: ImageView = itemView.findViewById(R.id.iv_photo)
        private val userIdView: TextView = itemView.findViewById(R.id.tv_userId)
        private val nameView: TextView = itemView.findViewById(R.id.tv_name)
        private val metaView: TextView = itemView.findViewById(R.id.tv_meta)
        private val statusView: TextView = itemView.findViewById(R.id.tv_status)
        private val badgeView: TextView = itemView.findViewById(R.id.tv_badge)

        fun bind(item: DetailUserResponse, onItemClicked: (DetailUserResponse) -> Unit) {
            val context = itemView.context

            Glide.with(itemView)
                .load(item.avatarUrl)
                .centerCrop()
                .into(avatarView)
            nameView.text = item.name ?: item.login
            userIdView.text = context.getString(R.string.detail_username_format, item.login)
            metaView.text = context.getString(
                R.string.home_card_meta,
                item.company ?: context.getString(R.string.detail_unknown_value),
                item.location ?: context.getString(R.string.detail_unknown_value)
            )
            statusView.text = context.getString(
                R.string.home_card_status,
                item.followers,
                item.publicRepos
            )
            badgeView.text = when {
                item.followers >= 10_000 -> context.getString(R.string.home_card_badge_popular)
                item.publicRepos >= 75 -> context.getString(R.string.detail_signal_prolific)
                else -> context.getString(R.string.home_card_badge_builder)
            }

            itemView.setOnClickListener { onItemClicked(item) }
        }
    }
}

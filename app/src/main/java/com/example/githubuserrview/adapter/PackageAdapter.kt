package com.example.githubuserrview.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.githubuserrview.R
import com.example.githubuserrview.api.Package

class PackageAdapter(
    private val packages: List<Package>,
    private val onItemClicked: (Package) -> Unit
) : RecyclerView.Adapter<PackageAdapter.PackageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PackageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_item, parent, false)
        return PackageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PackageViewHolder, position: Int) {
        holder.bind(packages[position], onItemClicked)
    }

    override fun getItemCount(): Int = packages.size

    class PackageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val avatarView: ImageView = itemView.findViewById(R.id.iv_photo)
        private val userIdView: TextView = itemView.findViewById(R.id.tv_userId)
        private val nameView: TextView = itemView.findViewById(R.id.tv_name)
        private val metaView: TextView = itemView.findViewById(R.id.tv_meta)
        private val statusView: TextView = itemView.findViewById(R.id.tv_status)
        private val badgeView: TextView = itemView.findViewById(R.id.tv_badge)

        fun bind(item: Package, onItemClicked: (Package) -> Unit) {
            val context = itemView.context

            avatarView.setImageResource(item.photo)
            nameView.text = item.surename
            userIdView.text = context.getString(R.string.detail_username_format, item.username)
            metaView.text = context.getString(
                R.string.home_card_meta,
                item.company,
                item.location
            )
            statusView.text = context.getString(
                R.string.home_card_status,
                item.followers,
                item.repository
            )
            badgeView.text = if (item.followers >= 10_000) {
                context.getString(R.string.home_card_badge_popular)
            } else {
                context.getString(R.string.home_card_badge_builder)
            }

            itemView.setOnClickListener { onItemClicked(item) }
        }
    }
}

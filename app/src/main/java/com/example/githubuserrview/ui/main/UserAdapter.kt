package com.example.githubuserrview.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.githubuserrview.R
import com.example.githubuserrview.data.model.User
import com.example.githubuserrview.databinding.SearchItemBinding

class UserAdapter : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    private var onItemClickCallback: OnItemClickCallback? = null
    fun setOnItemClickCallback(onItemClickCallback: OnItemClickCallback) {
        this.onItemClickCallback = onItemClickCallback
    }

    private val list = ArrayList<User>()

    fun setList (users: ArrayList<User>) {
        list.clear()
        list.addAll(users)
        notifyDataSetChanged()
    }

    inner class UserViewHolder(private val binding: SearchItemBinding): RecyclerView.ViewHolder(binding.root){
        fun bind(user: User){
            binding.root.setOnClickListener{
                onItemClickCallback?.onItemClicked(user)
            }
            binding.apply {
                val context = itemView.context
                Glide.with(itemView)
                    .load(user.avatar_url)
                    .centerCrop()
                    .into(ivPhotoRetro)
                tvNameRetro.text = user.login
                tvBadgeRetro.text = context.getString(R.string.search_card_badge_live)
                tvUserIdRetro.text = context.getString(R.string.detail_username_format, user.login)
                tvWebsiteRetro.text = context.getString(R.string.search_card_meta_id, user.id)
                tvStatusRetro.text = context.getString(R.string.search_card_status_hint)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = SearchItemBinding.inflate(LayoutInflater.from(parent.context), parent,false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(list[position])
    }
    interface OnItemClickCallback {
        fun onItemClicked(data: User)
    }

    override fun getItemCount(): Int = list.size
}

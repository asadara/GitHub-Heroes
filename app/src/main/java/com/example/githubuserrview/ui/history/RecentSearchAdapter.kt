package com.example.githubuserrview.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.githubuserrview.R

class RecentSearchAdapter(
    private val onItemClicked: (String) -> Unit
) : RecyclerView.Adapter<RecentSearchAdapter.RecentSearchViewHolder>() {

    private val items = mutableListOf<String>()

    fun submitList(searches: List<String>) {
        items.clear()
        items.addAll(searches)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentSearchViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_search, parent, false)
        return RecentSearchViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecentSearchViewHolder, position: Int) {
        holder.bind(items[position], onItemClicked)
    }

    override fun getItemCount(): Int = items.size

    class RecentSearchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val queryView: TextView = itemView.findViewById(R.id.tv_recent_query)

        fun bind(query: String, onItemClicked: (String) -> Unit) {
            queryView.text = query
            itemView.setOnClickListener { onItemClicked(query) }
        }
    }
}

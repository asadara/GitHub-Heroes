package com.example.githubuserrview.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.githubuserrview.R
import com.example.githubuserrview.data.model.GithubRepo
import com.example.githubuserrview.databinding.ItemProfileRepoBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ProfileRepoAdapter(
    private val onRepoClick: (GithubRepo) -> Unit
) : RecyclerView.Adapter<ProfileRepoAdapter.ProfileRepoViewHolder>() {

    private val items = mutableListOf<GithubRepo>()

    fun submitList(repositories: List<GithubRepo>) {
        items.clear()
        items.addAll(repositories)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileRepoViewHolder {
        val binding = ItemProfileRepoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProfileRepoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProfileRepoViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ProfileRepoViewHolder(
        private val binding: ItemProfileRepoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(repository: GithubRepo) {
            val context = binding.root.context

            binding.root.setOnClickListener { onRepoClick(repository) }
            binding.tvRepoName.text = repository.name
            binding.tvRepoMeta.text = repository.fullName
            binding.tvRepoDescription.text = repository.description
                ?.takeIf { it.isNotBlank() }
                ?: context.getString(R.string.profile_repo_description_fallback)
            binding.tvRepoSignals.text = context.getString(
                R.string.profile_repo_signals,
                repository.language ?: context.getString(R.string.detail_unknown_value),
                repository.stargazersCount,
                repository.forksCount,
                repository.watchersCount,
                repository.openIssuesCount
            )
            binding.tvRepoLicense.text = context.getString(
                R.string.profile_repo_license,
                repository.license?.name ?: context.getString(R.string.detail_unknown_value)
            )
            binding.tvRepoUpdated.text = context.getString(
                R.string.profile_repo_updated,
                formatUpdatedAt(
                    repository.updatedAt,
                    context.getString(R.string.detail_joined_unknown)
                )
            )
        }
    }

    private fun formatUpdatedAt(rawDate: String?, fallback: String): String {
        if (rawDate.isNullOrBlank()) {
            return fallback
        }

        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            parser.parse(rawDate)?.let(formatter::format) ?: fallback
        } catch (_: Exception) {
            fallback
        }
    }
}

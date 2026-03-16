package com.example.githubuserrview.adapter

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.githubuserrview.fragments.FollowerFragment
import com.example.githubuserrview.fragments.FollowingFragment
import com.example.githubuserrview.fragments.StarredRepositoriesFragment

class SectionsPagerAdapter(activity: AppCompatActivity, private val userId: String) :
    FragmentStateAdapter(activity) {

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> FollowerFragment.userName(userId)
            1 -> FollowingFragment.userName(userId)
            2 -> StarredRepositoriesFragment.userName(userId)
            else -> throw IllegalArgumentException("Unknown position: $position")
        }
    }

    override fun getItemCount(): Int {
        return 3
    }
}

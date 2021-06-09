package com.example.musify.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.example.musify.ui.fragments.OfflineSongFragment
import com.example.musify.ui.fragments.OnlineSongFragment

class ViewPagerAdapter(fm: FragmentManager, behavior: Int) : FragmentStatePagerAdapter(fm, behavior) {

    companion object{
        const val ONLINE_PAGE = 0
        const val OFFLINE_PAGE = 1
        const val MAX_PAGES = 2
    }
    private val fragmentList = listOf<Fragment>(OnlineSongFragment(),OfflineSongFragment())
    override fun getCount(): Int {
        return MAX_PAGES
    }

    override fun getItem(position: Int): Fragment {
        return fragmentList[position]
    }
}
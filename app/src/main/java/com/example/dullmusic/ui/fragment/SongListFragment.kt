package com.example.dullmusic.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.base.base.BaseFragment
import com.example.dullmusic.R
import com.example.dullmusic.databinding.FragmentSongListBinding

class SongListFragment : BaseFragment() {
    val binding by lazy{
        FragmentSongListBinding.inflate(layoutInflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {



        return binding.root
    }
}
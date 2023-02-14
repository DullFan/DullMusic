package com.example.dullmusic.ui.fragment

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import com.example.base.base.BaseFragment
import com.example.dullmusic.databinding.FragmentSongListBinding
import com.example.dullmusic.ui.activity.main.MainActivity

class TheAlbumFragment : BaseFragment() {
    val binding by lazy{
        FragmentSongListBinding.inflate(layoutInflater)
    }
    private val mMainActivity by lazy {
        (activity as MainActivity)
    }
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mMainActivity.mainViewModel.isOtherPages.value = true
        binding.rootLayout.setOnClickListener {  }




        return binding.root
    }
}
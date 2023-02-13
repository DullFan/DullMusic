package com.example.dullmusic.ui.fragment

import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import com.example.base.base.BaseFragment
import com.example.dullmusic.R
import com.example.dullmusic.databinding.FragmentArtistBinding
import com.example.dullmusic.ui.activity.main.MainActivity

class ArtistFragment : BaseFragment() {
    val binding by lazy {
        FragmentArtistBinding.inflate(layoutInflater)
    }
    private val mMainActivity by lazy {
        (activity as MainActivity)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mMainActivity.mainViewModel.isOtherPages.value = true
        binding.rootLayout.setOnClickListener {  }
        return binding.root
    }

}
package com.example.dullmusic.ui.fragment

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.base.base.BaseFragment
import com.example.base.base.BaseRvAdapter
import com.example.base.utils.*
import com.example.dullmusic.R
import com.example.dullmusic.bean.AllGsonSongBean
import com.example.dullmusic.bean.GsonSongBean
import com.example.dullmusic.databinding.DialogAddSongListLayoutBinding
import com.example.dullmusic.databinding.FragmentMediaLibraryBinding
import com.example.dullmusic.databinding.ItemSongListLayoutBinding
import com.example.dullmusic.tool.ALL_SONG_PLAY_LIST_STRING
import com.example.dullmusic.ui.activity.main.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayListFragment : BaseFragment() {
    val binding by lazy {
        FragmentMediaLibraryBinding.inflate(layoutInflater)
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

        binding.mediaListAdd.setOnClickListener(myOnMultiClickListener {
            mMainActivity.showDialogAddSongList {
                initRv()
            }
        })
        mMainActivity.mainViewModel.playListSongNumberString.observe(this){
            binding.mediaText.text = it
        }
        initRv()

        return binding.root
    }

    companion object {
        lateinit var playListBaseAdapter: BaseRvAdapter<GsonSongBean>
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun initRv() {
        val allSongPlayListString = mMainActivity.mainViewModel.getAllSongPlayListString()
        if (allSongPlayListString.isNotEmpty()) {
            val allGsonSongBean = gson.fromJson(allSongPlayListString, AllGsonSongBean::class.java)
            val linearLayoutManager = LinearLayoutManager(requireContext())
            mMainActivity.mainViewModel.playListSongNumberString.value = "${allGsonSongBean.allGsonSongBeanList.size} 个歌单"

            binding.mediaListRv.layoutManager = linearLayoutManager
            playListBaseAdapter = BaseRvAdapter(
                allGsonSongBean.allGsonSongBeanList.reversed(),
                R.layout.item_song_list_layout
            ) { itemData, view, position ->
                val itemSongListLayoutBinding = ItemSongListLayoutBinding.bind(view)
                CoroutineScope(Dispatchers.IO).launch {
                    val bitmap = if (itemData.musicList.size == 0) {
                        mMainActivity.mainViewModel.defaultAvatar
                    } else {
                        getAlbumPicture(itemData.musicList[0].data)
                            ?: mMainActivity.mainViewModel.defaultAvatar
                    }
                    withContext(Dispatchers.Main) {
                        itemSongListLayoutBinding.musicPhotos.setImageBitmap(bitmap)
                    }
                }
                itemSongListLayoutBinding.root.setOnClickListener(myOnMultiClickListener {
                    mMainActivity.mainViewModel.selectMusicSongBeanList = itemData
                    mMainActivity.mainViewModel.selectMusicSongBeanListPosition = position
                    mMainActivity.startSongListDetailsFragment()
                })
                itemSongListLayoutBinding.musicTitle.text = itemData.name
                itemSongListLayoutBinding.musicAuthor.text = "${itemData.musicList.size} 首歌曲"
                itemSongListLayoutBinding.itemMusicEdit.setOnClickListener(myOnMultiClickListener {
                    mMainActivity.showDialogAddSongList(true, itemData.name, position) {
                        itemSongListLayoutBinding.musicTitle.text = it
                    }
                })
            }
            binding.mediaListRv.adapter = playListBaseAdapter
        } else {
            mMainActivity.mainViewModel.playListSongNumberString.value = "0 个歌单"
        }
    }

}
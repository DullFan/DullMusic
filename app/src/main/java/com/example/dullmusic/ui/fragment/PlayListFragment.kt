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
import androidx.annotation.RequiresApi
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.base.base.BaseFragment
import com.example.base.base.BaseRvAdapter
import com.example.base.utils.getAlbumPicture
import com.example.base.utils.gson
import com.example.base.utils.myOnMultiClickListener
import com.example.base.utils.showLog
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
        initRv()

        binding.mediaListAdd.setOnClickListener(myOnMultiClickListener {
            mMainActivity.showDialogAddSongList{
                initRv()
            }
        })


        return binding.root
    }

    lateinit var baseAdapter: BaseRvAdapter<GsonSongBean>
    @RequiresApi(Build.VERSION_CODES.O)
    private fun initRv() {
        val allSongPlayListString = mMainActivity.mainViewModel.getAllSongPlayListString()
        if (allSongPlayListString.isNotEmpty()) {
            val allGsonSongBean = gson.fromJson(allSongPlayListString, AllGsonSongBean::class.java)
            binding.mediaText.text = "${allGsonSongBean.allGsonSongBeanList.size} 个歌单"
            val linearLayoutManager = LinearLayoutManager(requireContext())
            binding.mediaListRv.layoutManager = linearLayoutManager
            if(!::baseAdapter.isInitialized){
                baseAdapter = BaseRvAdapter(
                    allGsonSongBean.allGsonSongBeanList.reversed(),
                    R.layout.item_song_list_layout
                ) { itemData, view, position ->
                    val itemSongListLayoutBinding = ItemSongListLayoutBinding.bind(view)
                    CoroutineScope(Dispatchers.IO).launch {
                        val bitmap = if (itemData.musicList.size == 0) {
                            mMainActivity.defaultAvatar
                        } else {
                            getAlbumPicture(itemData.musicList[0].data) ?: mMainActivity.defaultAvatar
                        }
                        withContext(Dispatchers.Main) {
                            itemSongListLayoutBinding.musicPhotos.setImageBitmap(bitmap)
                        }
                    }
                    itemSongListLayoutBinding.musicTitle.text = itemData.name
                    itemSongListLayoutBinding.musicAuthor.text = "${itemData.musicList.size} 首歌曲"
                    itemSongListLayoutBinding.itemMusicEdit.setOnClickListener(myOnMultiClickListener {
                        mMainActivity.showDialogAddSongList(true,itemData.name,position) {
                            itemSongListLayoutBinding.musicTitle.text = it
                        }
                    })
                }
                binding.mediaListRv.adapter = baseAdapter
            }else{
                baseAdapter.dataList = allGsonSongBean.allGsonSongBeanList
            }
        } else {
            binding.mediaText.text = "0 个歌单"
        }
    }

}
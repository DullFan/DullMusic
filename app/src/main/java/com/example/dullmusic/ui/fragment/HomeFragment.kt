package com.example.dullmusic.ui.fragment

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.toBitmap
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.base.base.BaseFragment
import com.example.base.base.BaseRvAdapter
import com.example.base.utils.*
import com.example.dullmusic.R
import com.example.dullmusic.R.color
import com.example.dullmusic.bean.GsonSongBean
import com.example.dullmusic.bean.IconTextBean
import com.example.dullmusic.bean.SelectSongBean
import com.example.dullmusic.databinding.FragmentHomeBinding
import com.example.dullmusic.databinding.ItemSongLayoutBinding
import com.example.dullmusic.bean.Song
import com.example.dullmusic.databinding.DialogItemMenuLayoutBinding
import com.example.dullmusic.databinding.DialogPermissionsLayoutBinding
import com.example.dullmusic.databinding.ItemHomeEntryLayoutBinding
import com.example.dullmusic.tool.*
import com.example.dullmusic.ui.activity.main.MainActivity
import com.google.android.exoplayer2.MediaItem
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

@RequiresApi(Build.VERSION_CODES.O)
class HomeFragment : BaseFragment() {
    private val binding by lazy {
        FragmentHomeBinding.inflate(layoutInflater)
    }

    private val mMainActivity by lazy {
        (activity as MainActivity)
    }

    private val mainViewModel by lazy {
        mMainActivity.mainViewModel
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        initEntryRv()
        initMediaRv()
        initClick()
        initRefresh()
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initRefresh() {
        binding.musicRefresh.setOnClickListener(myOnMultiClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                mainViewModel.isOnClickRefresh = true
                mainViewModel.requestMusicSong(true)
                withContext(Dispatchers.Main) {
                    showToast(requireContext(), "刷新成功")
                }
            }
        })

    }

    /**
     * 初始化点击事件
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun initClick() {
        binding.contentPlay.setOnClickListener(myOnMultiClickListener {
            if (mainViewModel.musicSongList.value!!.size > 0) {
                val song = mainViewModel.musicSongList.value!![0]
                mainViewModel.sharedPreferencesEditCommitData {
                    putString(SELECT_SONG_PATH, song.data)
                }
                songBaseRvAdapter.index = 0
                mainViewModel.musicPlaySongList.value =
                    mainViewModel.musicSongList.value
            }
        })
    }

    /**
     * 设置入口
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun initEntryRv() {
        val mutableListOf = mutableListOf<IconTextBean>()
        mutableListOf += IconTextBean(R.drawable.icon_play_list, "歌单")
        mutableListOf += IconTextBean(R.drawable.icon_music_library, "专辑")
        mutableListOf += IconTextBean(R.drawable.people_black_24dp, "艺术家")
        binding.contentEntryRv.adapter = BaseRvAdapter(
            mutableListOf, R.layout.item_home_entry_layout
        ) { itemData, view, position ->
            val itemHomeEntryLayoutBinding = ItemHomeEntryLayoutBinding.bind(view)
            itemHomeEntryLayoutBinding.itemHomeEntryIcon.setImageResource(itemData.icon)
            itemHomeEntryLayoutBinding.itemHomeEntryText.text = itemData.text
            itemHomeEntryLayoutBinding.itemHomeEntryCard.setOnClickListener(myOnMultiClickListener {
                when (position) {
                    0 -> mMainActivity.startPLayListFragment()
                    1 -> mMainActivity.startTheAlbumFragment()
                    2 -> mMainActivity.startArtistFragment()
                }
            })
        }
    }


    lateinit var songBaseRvAdapter: BaseRvAdapter<Song>

    /**
     * 初始化媒体库
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun initMediaRv() {
        mMainActivity.setMusicRv(binding.contentRv,binding.mediaText){
            songBaseRvAdapter = it
        }
    }
}
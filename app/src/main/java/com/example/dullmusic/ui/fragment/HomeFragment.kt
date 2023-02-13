package com.example.dullmusic.ui.fragment

import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.base.base.BaseFragment
import com.example.base.base.BaseRvAdapter
import com.example.base.utils.*
import com.example.dullmusic.R
import com.example.dullmusic.R.color
import com.example.dullmusic.bean.IconTextBean
import com.example.dullmusic.bean.SelectSongBean
import com.example.dullmusic.databinding.FragmentHomeBinding
import com.example.dullmusic.databinding.ItemSongLayoutBinding
import com.example.dullmusic.bean.Song
import com.example.dullmusic.databinding.ItemHomeEntryLayoutBinding
import com.example.dullmusic.ui.activity.main.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class HomeFragment : BaseFragment() {
    private val binding by lazy {
        FragmentHomeBinding.inflate(layoutInflater)
    }

    private val mMainActivity by lazy {
        (activity as MainActivity)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        initEntryRv()
        initMediaRv()
        return binding.root
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
            mutableListOf,
            R.layout.item_home_entry_layout
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
        mMainActivity.mainViewModel.isOtherPages.observe(this){
            if(!it){
                binding.contentRv.visibility = View.VISIBLE
            }else{
                binding.contentRv.visibility = View.GONE
            }
        }
    }

    /**
     * 初始化媒体库
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun initMediaRv() {
        // 取消过渡动画
        (binding.contentRv.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        binding.contentRv.itemAnimator = null
        mMainActivity.mainViewModel.musicSongList.observe(this) { songList ->
            binding.mediaText.text = "${songList.size} 首歌曲"
            val songBaseRvAdapter =
                BaseRvAdapter(songList, R.layout.item_song_layout) { itemData, view, position ->
                    val itemSongLayoutBinding = ItemSongLayoutBinding.bind(view)
                    itemSongLayoutBinding.musicTitle.text = itemData.name
                    itemSongLayoutBinding.musicAuthor.text = itemData.artist
                    //设置Bitmap
                    setImageBitmap(itemData, itemSongLayoutBinding, position)
                    if (index == position) {
                        val typedValue = TypedValue()
                        requireContext().theme.resolveAttribute(
                            androidx.appcompat.R.attr.colorAccent, typedValue, true
                        )
                        itemSongLayoutBinding.musicCard.setCardBackgroundColor(typedValue.data)
                    } else {
                        itemSongLayoutBinding.musicCard.setCardBackgroundColor(
                            resources.getColor(
                                color.background_color
                            )
                        )
                    }

                    itemSongLayoutBinding.root.setOnClickListener(myOnMultiClickListener {
                        if (index != position) {
                            mMainActivity.isClickOnTheNextSong = true
                            index = position
                            // 开启动画效果
                            if (!isNotFirstEntry) isNotFirstEntry = true
                        }
                    })
                }
            val selectSongPosition = mMainActivity.sharedPreferences.getInt("selectSongPosition", 0)
            songBaseRvAdapter.index = selectSongPosition
            mMainActivity.audioBinder.seekIndexNotPlayer(selectSongPosition)
            binding.contentRv.adapter = songBaseRvAdapter
            mMainActivity.setEndOfSongListener {
                mMainActivity.isTheNextSongClick = true
                songBaseRvAdapter.index = it
            }

            mMainActivity.setSeekToNextOnClickListener { selectPosition, musicSongListMaxSize ->
                if (musicSongListMaxSize == selectPosition + 1) {
                    songBaseRvAdapter.index = 0
                } else {
                    songBaseRvAdapter.index = selectPosition + 1
                }
            }

            mMainActivity.setSeekToPreviousOnClickListener { selectPosition, musicSongListMaxSize ->
                if (selectPosition - 1 < 0) {
                    songBaseRvAdapter.index = musicSongListMaxSize - 1
                } else {
                    songBaseRvAdapter.index = selectPosition - 1
                }
            }
        }
    }

    /**
     * 设置Bitmap
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun BaseRvAdapter<Song>.setImageBitmap(
        itemData: Song,
        itemSongLayoutBinding: ItemSongLayoutBinding,
        position: Int
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            flow {
                val bitmap = getAlbumPicture(itemData.data)
                emit(bitmap)
            }.collect {
                MainScope().launch {
                    itemSongLayoutBinding.musicPhotos.setImageBitmap(it)
                    if (index == position) {
                        mMainActivity.mainViewModel.selectBitmap.value = it
                    }
                }
            }
        }

        if (index == position) {
            mMainActivity.sharedPreferencesEdit.putInt("selectSongPosition", position)
            mMainActivity.sharedPreferencesEdit.commit()
            selectedEvents(position, itemData)
        }
    }

    /**
     * 选中事件
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun BaseRvAdapter<Song>.selectedEvents(
        position: Int, itemData: Song
    ) {
        if (index == position) {
            mMainActivity.mainViewModel.setSelectSong(
                SelectSongBean(
                    itemData, position
                )
            )
        }
    }
}
package com.example.dullmusic.ui.fragment

import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
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
import com.example.dullmusic.databinding.ItemHomeEntryLayoutBinding
import com.example.dullmusic.ui.activity.main.MainActivity
import com.google.android.exoplayer2.MediaItem
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

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
        initClick()
        return binding.root
    }

    /**
     * 初始化点击事件
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun initClick() {
        binding.contentPlay.setOnClickListener(myOnMultiClickListener {
            if (mMainActivity.mainViewModel.musicSongList.value!!.size > 0) {
                val song = mMainActivity.mainViewModel.musicSongList.value!![0]
                mMainActivity.sharedPreferencesEdit.putString("selectSongPath", song.data)
                mMainActivity.sharedPreferencesEdit.commit()
                songBaseRvAdapter.index = 0
                mMainActivity.mainViewModel.musicPlaySongList.value =
                    mMainActivity.mainViewModel.musicSongList.value
            }
        })
    }

    /**
     * 设置入口
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun initEntryRv() {
        val mutableListOf = mutableListOf<IconTextBean>()
        mutableListOf += IconTextBean(R.drawable.icon_play_list, "播放列表")
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
        mMainActivity.mainViewModel.isOtherPages.observe(this) {
            if (!it) {
                binding.contentRv.visibility = View.VISIBLE
            } else {
                binding.contentRv.visibility = View.GONE
            }
        }
    }


    lateinit var songBaseRvAdapter: BaseRvAdapter<Song>

    /**
     * 初始化媒体库
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun initMediaRv() {
        // 取消过渡动画
        (binding.contentRv.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        binding.contentRv.itemAnimator = null
        mMainActivity.mainViewModel.musicSongList.observe(this) { songList ->
            showLog("媒体库的数据发生了变化")
            binding.mediaText.text = "${songList.size} 首歌曲"
            songBaseRvAdapter =
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
                            // 判断当前选中的歌曲是否在播放列表中，存在则跳过，不存在则添加
                            if ((mMainActivity.mainViewModel.musicPlaySongList.value?.contains(
                                    itemData
                                ) == false)
                            ) {
                                val currentPosition =
                                    mMainActivity.audioBinder.getCurrentMediaItemIndex()
                                if (currentPosition + 1 == mMainActivity.mainViewModel.musicPlaySongList.value!!.size) {
                                    mMainActivity.audioBinder.addMediaItem(
                                        MediaItem.fromUri(
                                            itemData.data
                                        )
                                    )
                                    mMainActivity.mainViewModel.musicPlaySongList.value!!.add(
                                        itemData
                                    )
                                } else {
                                    mMainActivity.audioBinder.addMediaItem(
                                        MediaItem.fromUri(
                                            itemData.data
                                        ), currentPosition + 1
                                    )
                                    mMainActivity.mainViewModel.musicPlaySongList.value!!.add(
                                        currentPosition + 1, itemData
                                    )
                                }
                                mMainActivity.sharedPreferencesEdit.putString(
                                    "SongPlayListString",
                                    gson.toJson(GsonSongBean(mMainActivity.mainViewModel.musicPlaySongList.value!!))
                                )
                                mMainActivity.sharedPreferencesEdit.commit()
                            }
                            mMainActivity.isClickOnTheNextSong = true
                            index = position
                            // 开启动画效果
                            if (!isNotFirstEntry) isNotFirstEntry = true
                        }
                    })
                }

            val selectSongPath = mMainActivity.sharedPreferences.getString("selectSongPath", "")
            songBaseRvAdapter.index = mMainActivity.selectIndex(selectSongPath)
            mMainActivity.audioBinder.seekIndexNotPlayer(
                mMainActivity.selectIndexMusicPlay(
                    selectSongPath
                )
            )
            val linearLayoutManager = LinearLayoutManager(requireContext())
            binding.contentRv.layoutManager = linearLayoutManager
            binding.contentRv.adapter = songBaseRvAdapter
            mMainActivity.setEndOfSongListener {
                mMainActivity.isTheNextSongClick = true
                songBaseRvAdapter.index = it
            }

            mMainActivity.setPlayListDialogF(object : MainActivity.PlayListDialog {
                override fun onPlayListener(path: String) {
                    songBaseRvAdapter.index = mMainActivity.selectIndex(path)
                }
            })

            mMainActivity.setSeekToNextOnClickListener { selectPosition ->
                songBaseRvAdapter.index = selectPosition
            }

            mMainActivity.setSeekToPreviousOnClickListener { selectPosition ->
                songBaseRvAdapter.index = selectPosition
            }
        }
    }

    /**
     * 设置Bitmap
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun BaseRvAdapter<Song>.setImageBitmap(
        itemData: Song, itemSongLayoutBinding: ItemSongLayoutBinding, position: Int
    ) {
        // 这里逻辑有点问题
        CoroutineScope(Dispatchers.IO).launch {
            flow {
                val bitmap = getAlbumPicture(itemData.data)
                emit(bitmap)
            }.collect {
                withContext(Dispatchers.Main) {
                    itemSongLayoutBinding.musicPhotos.setImageBitmap(it)
                    if (index == position) {
                        mMainActivity.mainViewModel.selectBitmap.value = it
                    }
                }
            }
        }

        if (index == position) {
            mMainActivity.sharedPreferencesEdit.putString("selectSongPath", itemData.data)
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
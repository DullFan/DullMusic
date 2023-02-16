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

    val defaultAvatar by lazy {
        resources.getDrawable(R.drawable.default_avatar).toBitmap()
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
        mainViewModel.isOtherPages.observe(this) {
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
        mainViewModel.musicSongList.observe(this) { songList ->
            binding.mediaText.text = "${songList.size} 首歌曲"
            if (!::songBaseRvAdapter.isInitialized) {
                songBaseRvAdapter =
                    BaseRvAdapter(songList, R.layout.item_song_layout) { itemData, view, position ->
                        val itemSongLayoutBinding = ItemSongLayoutBinding.bind(view)
                        itemSongLayoutBinding.musicTitle.text = itemData.name
                        itemSongLayoutBinding.musicAuthor.text = itemData.artist
                        //设置Bitmap
                        setImageBitmap(itemData, itemSongLayoutBinding, position)
                        //设置选中的背景颜色
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

                        //Item点击事件
                        itemSongLayoutBinding.root.setOnClickListener(myOnMultiClickListener {
                            if (fileExists(itemData.data)) {
                                if (index != position) {
                                    // 判断当前选中的歌曲是否在播放列表中，存在则跳过，不存在则添加
                                    if ((mainViewModel.musicPlaySongList.value?.contains(
                                            itemData
                                        ) == false)
                                    ) {
                                        val currentPosition =
                                            mainViewModel.audioBinder.getCurrentMediaItemIndex()
                                        if (currentPosition + 1 == mainViewModel.musicPlaySongList.value!!.size) {
                                            mainViewModel.audioBinder.addMediaItem(
                                                MediaItem.fromUri(
                                                    itemData.data
                                                )
                                            )
                                            mainViewModel.musicPlaySongList.value!!.add(
                                                itemData
                                            )
                                        } else {
                                            mainViewModel.audioBinder.addMediaItem(
                                                MediaItem.fromUri(
                                                    itemData.data
                                                ), currentPosition + 1
                                            )
                                            mainViewModel.musicPlaySongList.value!!.add(
                                                currentPosition + 1, itemData
                                            )
                                        }
                                        mainViewModel.sharedPreferencesEditCommitData {
                                            putString(
                                                SONG_PLAY_LIST_STRING,
                                                gson.toJson(GsonSongBean(mainViewModel.musicPlaySongList.value!!))
                                            )
                                        }
                                    }
                                    isClickOnTheNextSong = true
                                    index = position
                                    // 开启动画效果
                                    if (!isNotFirstEntry) isNotFirstEntry = true
                                }
                            } else {
                                showToast(requireContext(), "找不到此文件,可能文件已经被迁移或被删除")
                            }
                        })

                        itemSongLayoutBinding.itemMusicMenu.setOnClickListener(
                            myOnMultiClickListener {
                                val dialog = AlertDialog.Builder(requireContext()).create()
                                val dialogView = DialogItemMenuLayoutBinding.inflate(layoutInflater)
                                dialog.setView(dialogView.root)
                                dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                                dialog.window!!.decorView.setBackgroundColor(Color.TRANSPARENT)
                                dialog.show()

                                dialogView.musicTitle.text = itemData.name
                                dialogView.musicAuthor.text = itemData.artist
                                dialogView.musicPhotos.setImageBitmap(mainViewModel.selectBitmap.value)
                                dialogView.musicNextSong.setOnClickListener(myOnMultiClickListener {
                                    val currentPosition =
                                        mainViewModel.audioBinder.getCurrentMediaItemIndex()
                                    if ((mainViewModel.musicPlaySongList.value?.contains(
                                            itemData
                                        ) == false)
                                    ) {
                                        // 不存在
                                        if (currentPosition + 1 == mainViewModel.musicPlaySongList.value!!.size) {
                                            mainViewModel.audioBinder.addMediaItem(
                                                MediaItem.fromUri(
                                                    itemData.data
                                                )
                                            )
                                            mainViewModel.musicPlaySongList.value!!.add(
                                                itemData
                                            )
                                        } else {
                                            mainViewModel.audioBinder.addMediaItem(
                                                MediaItem.fromUri(
                                                    itemData.data
                                                ), currentPosition + 1
                                            )
                                            mainViewModel.musicPlaySongList.value!!.add(
                                                currentPosition + 1, itemData
                                            )
                                        }
                                    } else {
                                        //存在
                                        if (currentPosition + 1 == mainViewModel.musicPlaySongList.value!!.size) {
                                            mainViewModel.audioBinder.moveMediaItem(
                                                mainViewModel.selectIndexMusicPlay(itemData.data),
                                                mainViewModel.musicPlaySongList.value!!.size
                                            )
                                            mainViewModel.musicPlaySongList.value!!.remove(itemData)
                                            mainViewModel.musicPlaySongList.value!!.add(itemData)
                                        } else {
                                            mainViewModel.audioBinder.moveMediaItem(
                                                mainViewModel.selectIndexMusicPlay(itemData.data),
                                                currentPosition
                                            )
                                            mainViewModel.musicPlaySongList.value!!.remove(itemData)
                                            mainViewModel.musicPlaySongList.value!!.add(
                                                currentPosition,
                                                itemData
                                            )
                                        }
                                    }
                                    mainViewModel.sharedPreferencesEditCommitData {
                                        putString(
                                            SONG_PLAY_LIST_STRING,
                                            gson.toJson(GsonSongBean(mainViewModel.musicPlaySongList.value!!))
                                        )
                                    }
                                    dialog.dismiss()
                                })
                            })
                    }

                val selectSongPath = mainViewModel.getSelectSongPath()
                songBaseRvAdapter.index = mainViewModel.selectIndex(selectSongPath)
                mainViewModel.audioBinder.seekIndexNotPlayer(
                    mainViewModel.selectIndexMusicPlay(
                        selectSongPath
                    )
                )
                val linearLayoutManager = LinearLayoutManager(requireContext())
                binding.contentRv.layoutManager = linearLayoutManager
                binding.contentRv.adapter = songBaseRvAdapter
                mMainActivity.setEndOfSongListener {
                    isTheNextSongClick = true
                    songBaseRvAdapter.index = it
                }
                mMainActivity.setPlayListDialogF { path ->
                    songBaseRvAdapter.index = mainViewModel.selectIndex(path)
                }

                mMainActivity.setSeekToNextOnClickListener { selectPosition ->
                    songBaseRvAdapter.index = selectPosition
                }

                mMainActivity.setSeekToPreviousOnClickListener { selectPosition ->
                    songBaseRvAdapter.index = selectPosition
                }
            } else {
                songBaseRvAdapter.dataList = songList
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
        CoroutineScope(Dispatchers.IO).launch {
            flow {
                val bitmap = getAlbumPicture(itemData.data)
                emit(bitmap)
            }.catch {
                emit(defaultAvatar)
            }
                .collect {
                    withContext(Dispatchers.Main) {
                        itemSongLayoutBinding.musicPhotos.setImageBitmap(it)
                        if (index == position) {
                            mainViewModel.setSelectBitmap(it ?: defaultAvatar)
                        }
                    }
                }
        }

        if (index == position) {
            mainViewModel.sharedPreferencesEditCommitData {
                putString(SELECT_SONG_PATH, itemData.data)
            }
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
            mainViewModel.setSelectSong(
                SelectSongBean(
                    itemData, position
                )
            )
        }
    }
}
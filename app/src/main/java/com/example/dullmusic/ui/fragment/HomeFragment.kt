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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.base.base.BaseFragment
import com.example.base.base.BaseRvAdapter
import com.example.base.utils.*
import com.example.dullmusic.R
import com.example.dullmusic.R.color
import com.example.dullmusic.bean.*
import com.example.dullmusic.databinding.*
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

    companion object {
        lateinit var songBaseRvAdapter: BaseRvAdapter<Song>
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
                    putString(
                        SONG_PLAY_LIST_STRING,
                        gson.toJson(GsonSongBean(mainViewModel.musicSongList.value!!))
                    )
                }
                songBaseRvAdapter.index = 0
                mainViewModel.musicPlaySongList.value = mainViewModel.musicSongList.value
                if (!mMainActivity.audioBinder.mediaIsPlaying()) {
                    mMainActivity.playMusic()
                }
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


    /**
     * 初始化媒体库
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun initMediaRv() {
        // 取消过渡动画
        (binding.contentRv.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        binding.contentRv.itemAnimator = null
        mainViewModel.musicSongList.observe(viewLifecycleOwner) { songList ->
            binding.mediaText.text = "${songList.size} 首歌曲"
            songBaseRvAdapter =
                BaseRvAdapter(songList, R.layout.item_song_layout) { itemData, view, position ->
                    val itemSongLayoutBinding = ItemSongLayoutBinding.bind(view)
                    itemSongLayoutBinding.musicTitle.text = itemData.name
                    itemSongLayoutBinding.musicAuthor.text = itemData.artist
                    // 设置Bitmap
                    setImageBitmap(itemData, itemSongLayoutBinding, position)
                    // 设置选中的背景颜色
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
                                        mMainActivity.audioBinder.getCurrentMediaItemIndex()
                                    if (currentPosition + 1 == mainViewModel.musicPlaySongList.value!!.size) {
                                        mMainActivity.audioBinder.addMediaItem(
                                            MediaItem.fromUri(
                                                itemData.data
                                            )
                                        )
                                        mainViewModel.musicPlaySongList.value!!.add(
                                            itemData
                                        )
                                    } else {
                                        mMainActivity.audioBinder.addMediaItem(
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
                }

            val selectSongPath = mainViewModel.getSelectSongPath()
            songBaseRvAdapter.index = mainViewModel.selectIndex(selectSongPath)
            mMainActivity.audioBinder.seekIndexNotPlayer(
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
        }
    }

    /**
     * 设置Bitmap
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun BaseRvAdapter<Song>.setImageBitmap(
        oneitemData: Song, itemSongLayoutBinding: ItemSongLayoutBinding, position: Int
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            flow {
                val bitmap = if (mainViewModel.musicSongListBitmap.containsKey(oneitemData.data)) {
                    mainViewModel.musicSongListBitmap[oneitemData.data]
                } else {
                    getAlbumPicture(oneitemData.data)
                }
                emit(bitmap)
            }.catch {
                emit(mainViewModel.defaultAvatar)
            }.collect {
                withContext(Dispatchers.Main) {
                    try {
                        mainViewModel.musicSongListBitmap[oneitemData.data] = it!!
                    } catch (e: Exception) {
                        mainViewModel.musicSongListBitmap[oneitemData.data] = mainViewModel.defaultAvatar
                    }
                    itemSongLayoutBinding.musicPhotos.setImageBitmap(mainViewModel.musicSongListBitmap[oneitemData.data])
                    if (index == position) {
                        mainViewModel.setSelectBitmap(mainViewModel.musicSongListBitmap[oneitemData.data]!!)
                    }
                    itemSongLayoutBinding.itemMusicMenu.setOnClickListener(myOnMultiClickListener {
                        val dialog = AlertDialog.Builder(requireContext()).create()
                        val dialogView = DialogItemMenuLayoutBinding.inflate(layoutInflater)
                        dialog.setView(dialogView.root)
                        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                        dialog.window!!.decorView.setBackgroundColor(Color.TRANSPARENT)
                        dialog.show()

                        dialogView.musicTitle.text = oneitemData.name
                        dialogView.musicAuthor.text = oneitemData.artist
                        dialogView.musicPhotos.setImageBitmap(mainViewModel.musicSongListBitmap[oneitemData.data])
                        dialogView.musicAddPlaySong.setOnClickListener(myOnMultiClickListener {
                            val addSongListDialog = AlertDialog.Builder(requireContext()).create()
                            val dialogAddSongToListLayoutBinding =
                                DialogAddSongToListLayoutBinding.inflate(layoutInflater)
                            addSongListDialog.window!!.setBackgroundDrawable(
                                ColorDrawable(Color.TRANSPARENT)
                            )
                            addSongListDialog.window!!.decorView.setBackgroundColor(
                                Color.TRANSPARENT
                            )
                            val allGsonSongBean = gson.fromJson(
                                mainViewModel.getAllSongPlayListString(),
                                AllGsonSongBean::class.java
                            )
                            val baseAdapter = BaseRvAdapter(
                                allGsonSongBean?.allGsonSongBeanList?.reversed() ?: mutableListOf(),
                                R.layout.item_song_to_list_layout
                            ) { itemData, view, position ->
                                val itemSongListLayoutBinding =
                                    ItemSongToListLayoutBinding.bind(view)
                                CoroutineScope(Dispatchers.IO).launch {
                                    val bitmap = if (itemData.musicList.size == 0) {
                                        mainViewModel.defaultAvatar
                                    } else {
                                        getAlbumPicture(itemData.musicList[0].data)
                                            ?: mainViewModel.defaultAvatar
                                    }
                                    withContext(Dispatchers.Main) {
                                        itemSongListLayoutBinding.musicPhotos.setImageBitmap(
                                            bitmap
                                        )
                                    }
                                }
                                itemSongListLayoutBinding.musicTitle.text = itemData.name
                                itemSongListLayoutBinding.musicAuthor.text =
                                    "${itemData.musicList.size} 首歌曲"
                                itemSongListLayoutBinding.root.setOnClickListener(
                                    myOnMultiClickListener {
                                        val fromJson = gson.fromJson(
                                            mainViewModel.getAllSongPlayListString(),
                                            AllGsonSongBean::class.java
                                        )
                                        val reversed = fromJson.allGsonSongBeanList.reversed()
                                        val musicList =
                                            reversed[position].musicList
                                        if (!musicList.contains(oneitemData)) {
                                            reversed[position].musicList.add(
                                                oneitemData
                                            )
                                            mainViewModel.sharedPreferencesEditCommitData {
                                                putString(
                                                    ALL_SONG_PLAY_LIST_STRING, gson.toJson(fromJson)
                                                )
                                            }
                                        }
                                        showToast(requireContext(), "已添加到歌单")
                                        addSongListDialog.dismiss()
                                        dialog.dismiss()
                                    })
                            }


                            dialogAddSongToListLayoutBinding.rv.adapter = baseAdapter

                            dialogAddSongToListLayoutBinding.dialogButton.setOnClickListener(
                                myOnMultiClickListener {
                                    mMainActivity.showDialogAddSongList {
                                        baseAdapter.dataList = gson.fromJson(
                                            mainViewModel.getAllSongPlayListString(),
                                            AllGsonSongBean::class.java
                                        ).allGsonSongBeanList
                                    }
                                })
                            addSongListDialog.setView(dialogAddSongToListLayoutBinding.root)
                            addSongListDialog.show()
                        })

                        dialogView.musicNextSong.setOnClickListener(myOnMultiClickListener {
                            val currentPosition =
                                mMainActivity.audioBinder.getCurrentMediaItemIndex()
                            if ((mainViewModel.musicPlaySongList.value?.contains(
                                    oneitemData
                                ) == false)
                            ) {
                                // 不存在
                                if (currentPosition + 1 == mainViewModel.musicPlaySongList.value!!.size) {
                                    mMainActivity.audioBinder.addMediaItem(
                                        MediaItem.fromUri(
                                            oneitemData.data
                                        )
                                    )
                                    mainViewModel.musicPlaySongList.value!!.add(
                                        oneitemData
                                    )
                                } else {
                                    mMainActivity.audioBinder.addMediaItem(
                                        MediaItem.fromUri(
                                            oneitemData.data
                                        ), currentPosition + 1
                                    )
                                    mainViewModel.musicPlaySongList.value!!.add(
                                        currentPosition + 1, oneitemData
                                    )
                                }
                            } else {
                                //存在
                                if (currentPosition + 1 == mainViewModel.musicPlaySongList.value!!.size) {
                                    mMainActivity.audioBinder.moveMediaItem(
                                        mainViewModel.selectIndexMusicPlay(oneitemData.data),
                                        mainViewModel.musicPlaySongList.value!!.size
                                    )
                                    mainViewModel.musicPlaySongList.value!!.remove(
                                        oneitemData
                                    )
                                    mainViewModel.musicPlaySongList.value!!.add(oneitemData)
                                } else {
                                    mMainActivity.audioBinder.moveMediaItem(
                                        mainViewModel.selectIndexMusicPlay(oneitemData.data),
                                        currentPosition
                                    )
                                    mainViewModel.musicPlaySongList.value!!.remove(
                                        oneitemData
                                    )
                                    mainViewModel.musicPlaySongList.value!!.add(
                                        currentPosition, oneitemData
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
            }
        }
        if (index == position) {
            mainViewModel.sharedPreferencesEditCommitData {
                putString(SELECT_SONG_PATH, oneitemData.data)
            }
            selectedEvents(position, oneitemData)
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
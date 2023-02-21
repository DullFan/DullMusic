package com.example.dullmusic.ui.fragment

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import com.example.base.base.BaseFragment
import com.example.base.base.BaseRvAdapter
import com.example.base.utils.*
import com.example.dullmusic.R
import com.example.dullmusic.bean.AllGsonSongBean
import com.example.dullmusic.bean.GsonSongBean
import com.example.dullmusic.bean.SelectSongBean
import com.example.dullmusic.bean.Song
import com.example.dullmusic.databinding.*
import com.example.dullmusic.tool.ALL_SONG_PLAY_LIST_STRING
import com.example.dullmusic.tool.SELECT_SONG_PATH
import com.example.dullmusic.tool.SONG_PLAY_LIST_STRING
import com.example.dullmusic.tool.isClickOnTheNextSong
import com.example.dullmusic.ui.activity.main.MainActivity
import com.google.android.exoplayer2.MediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 通用列表媒体详情页
 */
@RequiresApi(Build.VERSION_CODES.O)
class MediaListDetailsFragment : BaseFragment() {
    val binding by lazy {
        FragmentMediaListDetailsBinding.inflate(layoutInflater)
    }

    private val mMainActivity by lazy {
        (activity as MainActivity)
    }

    private val mainViewModel by lazy {
        mMainActivity.mainViewModel
    }

    companion object {
        var mediaListDetailsIsShow = false
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaListDetailsIsShow = false
    }

    val selectMusicSongBeanList by lazy {
        mMainActivity.mainViewModel.selectMusicSongBeanList
    }

    lateinit var songBaseRvAdapter: BaseRvAdapter<Song>

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mediaListDetailsIsShow = true
        CoroutineScope(Dispatchers.IO).launch {
            flow {
                val bitmap = getAlbumPicture(selectMusicSongBeanList.musicList[0].data)
                emit(bitmap)
            }.catch {
                emit(mainViewModel.defaultAvatar)
            }.collect {
                withContext(Dispatchers.Main) {
                    binding.image.setImageBitmap(it)
                }
            }
        }
        binding.mediaTitle.text = selectMusicSongBeanList.musicList[0].album
        binding.mediaText.text = "${selectMusicSongBeanList.musicList.size} 首歌曲"
        songBaseRvAdapter = BaseRvAdapter(selectMusicSongBeanList.musicList, R.layout.item_song_layout) { itemData, view, position ->
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
                        R.color.background_color
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
                        HomeFragment.songBaseRvAdapter.index =
                            mainViewModel.selectIndex(itemData.data)
                        // 开启动画效果
                        if (!isNotFirstEntry) isNotFirstEntry = true
                    }
                } else {
                    showToast(requireContext(), "找不到此文件,可能文件已经被迁移或被删除")
                }
            })
        }

        binding.mediaListRv.adapter = songBaseRvAdapter
        songBaseRvAdapter.index = 9999
        mainViewModel.selectMusicSongBeanList.musicList.forEachIndexed { index, song ->
            if(mainViewModel.getSelectSongPath() == song.data){
                songBaseRvAdapter.index = index
            }
        }

        binding.mediaListPlay.setOnClickListener(myOnMultiClickListener {
            if (mainViewModel.selectMusicSongBeanList.musicList.size > 0) {
                val song = mainViewModel.selectMusicSongBeanList.musicList[0]
                mainViewModel.sharedPreferencesEditCommitData {
                    putString(SELECT_SONG_PATH, song.data)
                    putString(SONG_PLAY_LIST_STRING, gson.toJson(mainViewModel.selectMusicSongBeanList))
                }
                mainViewModel.musicPlaySongList.value =
                    mainViewModel.selectMusicSongBeanList.musicList
                songBaseRvAdapter.index = 0
                HomeFragment.songBaseRvAdapter.index = mainViewModel.selectIndex(song.data)
                mMainActivity.playMusic()
            }
        })
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun BaseRvAdapter<Song>.setImageBitmap(
        oneitemData: Song, itemSongLayoutBinding: ItemSongLayoutBinding, position: Int
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            flow {
                val bitmap = getAlbumPicture(oneitemData.data)
                emit(bitmap)
            }.catch {
                emit(mainViewModel.defaultAvatar)
            }.collect {
                withContext(Dispatchers.Main) {
                    itemSongLayoutBinding.musicPhotos.setImageBitmap(it)
                    if (index == position) {
                        mainViewModel.setSelectBitmap(it ?: mainViewModel.defaultAvatar)
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
                        dialogView.musicPhotos.setImageBitmap(it)

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
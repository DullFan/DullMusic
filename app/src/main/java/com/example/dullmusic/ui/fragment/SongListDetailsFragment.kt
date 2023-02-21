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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.base.base.BaseFragment
import com.example.base.base.BaseRvAdapter
import com.example.base.utils.*
import com.example.dullmusic.MyItemTouchCallback
import com.example.dullmusic.R
import com.example.dullmusic.bean.AllGsonSongBean
import com.example.dullmusic.bean.GsonSongBean
import com.example.dullmusic.bean.SelectSongBean
import com.example.dullmusic.bean.Song
import com.example.dullmusic.databinding.*
import com.example.dullmusic.tool.*
import com.example.dullmusic.ui.activity.main.MainActivity
import com.google.android.exoplayer2.MediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

var firstTimeDoesNotStart = false
var firstTimeDoesNotStartBitmap = false

/**
 * 歌单详情页
 */
@RequiresApi(Build.VERSION_CODES.O)
class SongListDetailsFragment : BaseFragment() {

    val binding by lazy {
        FragmentSongListDetailsBinding.inflate(layoutInflater)
    }

    private val mMainActivity by lazy {
        (activity as MainActivity)
    }

    private val mainViewModel by lazy {
        mMainActivity.mainViewModel
    }

    lateinit var songBaseRvAdapter: BaseRvAdapter<Song>

    companion object {
        var songListDetailsIsShow = false
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mMainActivity.mainViewModel.isOtherPages.value = true
        songListDetailsIsShow = true
        firstTimeDoesNotStart = false
        firstTimeDoesNotStartBitmap = false

        binding.mediaTitle.text = mainViewModel.selectMusicSongBeanList.name

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

        binding.mediaListDel.setOnClickListener(myOnMultiClickListener {
            val delDialog = AlertDialog.Builder(requireContext()).create()
            val dialogDelSongListLayoutBinding =
                DialogDelSongListLayoutBinding.inflate(layoutInflater)
            delDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            delDialog.window!!.decorView.setBackgroundColor(Color.TRANSPARENT)
            delDialog.setView(dialogDelSongListLayoutBinding.root)
            dialogDelSongListLayoutBinding.dialogCloseButton.setOnClickListener {
                delDialog.dismiss()
            }
            dialogDelSongListLayoutBinding.dialogDoneButton.setOnClickListener {
                val allGsonSongBean = gson.fromJson(
                    mainViewModel.getAllSongPlayListString(),
                    AllGsonSongBean::class.java
                )
                allGsonSongBean.allGsonSongBeanList.remove(mainViewModel.selectMusicSongBeanList)
                PlayListFragment.playListBaseAdapter.dataList =
                    allGsonSongBean.allGsonSongBeanList.reversed()
                mMainActivity.mainViewModel.playListSongNumberString.value =
                    "${allGsonSongBean.allGsonSongBeanList.size} 个歌单"
                mainViewModel.sharedPreferencesEditCommitData {
                    putString(ALL_SONG_PLAY_LIST_STRING, gson.toJson(allGsonSongBean))
                }
                showToast(requireContext(), "删除成功")
                delDialog.dismiss()
                mMainActivity.supportFragmentManager.popBackStack()
            }
            delDialog.show()
        })


        initMediaRv()
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        songListDetailsIsShow = false
    }

    /**
     * 初始化媒体库
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun initMediaRv() {
        (binding.mediaListRv.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        binding.mediaListRv.itemAnimator = null
        if (mainViewModel.selectMusicSongBeanList.musicList.size > 0) {
            binding.mediaText.text = "${mainViewModel.selectMusicSongBeanList.musicList.size} 首歌曲"
            songBaseRvAdapter =
                BaseRvAdapter(
                    mainViewModel.selectMusicSongBeanList.musicList,
                    R.layout.item_song_layout
                ) { itemData, view, position ->
                    val itemSongLayoutBinding = ItemSongLayoutBinding.bind(view)
                    itemSongLayoutBinding.musicTitle.text = itemData.name
                    itemSongLayoutBinding.musicAuthor.text = itemData.artist
                    itemSongLayoutBinding.itemMusicMenu.visibility = View.GONE

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
                    //设置Bitmap
                    setImageBitmap(itemData, itemSongLayoutBinding, position)
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

            val linearLayoutManager = LinearLayoutManager(requireContext())
            binding.mediaListRv.layoutManager = linearLayoutManager
            songBaseRvAdapter.index = 9999
            mainViewModel.selectMusicSongBeanList.musicList.forEachIndexed { index, song ->
                if(mainViewModel.getSelectSongPath() == song.data){
                    songBaseRvAdapter.index = index
                }
            }

            binding.mediaListRv.adapter = songBaseRvAdapter

            val itemTouchHelper = ItemTouchHelper(MyItemTouchCallback {
                mainViewModel.selectMusicSongBeanList.musicList.removeAt(it)
                songBaseRvAdapter.notifyItemRemoved(it)
                val fromJson = gson.fromJson(
                    mainViewModel.getAllSongPlayListString(),
                    AllGsonSongBean::class.java
                )
                val reversed = fromJson.allGsonSongBeanList.reversed()
                reversed[mainViewModel.selectMusicSongBeanListPosition].musicList = mainViewModel.selectMusicSongBeanList.musicList
                mainViewModel.sharedPreferencesEditCommitData {
                    putString(ALL_SONG_PLAY_LIST_STRING,gson.toJson(fromJson))
                }
                PlayListFragment.playListBaseAdapter.dataList = fromJson.allGsonSongBeanList.reversed()
                binding.mediaText.text = "${mainViewModel.selectMusicSongBeanList.musicList.size} 首歌曲"
            })
            itemTouchHelper.attachToRecyclerView(binding.mediaListRv)
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
                val bitmap = getAlbumPicture(oneitemData.data)
                emit(bitmap)
            }.catch {
                emit(mainViewModel.defaultAvatar)
            }.collect {
                withContext(Dispatchers.Main) {
                    itemSongLayoutBinding.musicPhotos.setImageBitmap(it)
                    if (firstTimeDoesNotStartBitmap) {
                        if (index == position) {
                            mainViewModel.setSelectBitmap(it ?: mainViewModel.defaultAvatar)
                        }
                    }
                    if (position == mainViewModel.selectMusicSongBeanList.musicList.size - 1) {
                        firstTimeDoesNotStartBitmap = true
                        firstTimeDoesNotStart = true
                    }
                }
            }
        }

        if (index == position && firstTimeDoesNotStart) {
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
package com.example.dullmusic.ui.fragment

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.annotation.RequiresApi
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.base.base.BaseFragment
import com.example.base.base.BaseRvAdapter
import com.example.base.utils.*
import com.example.dullmusic.R
import com.example.dullmusic.R.color
import com.example.dullmusic.bean.SelectSongBean
import com.example.dullmusic.databinding.FragmentHomeBinding
import com.example.dullmusic.databinding.ItemSongLayoutBinding
import com.example.dullmusic.bean.Song
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
        binding.contentMediaCard.setOnClickListener(myOnMultiClickListener {
            mMainActivity.startMediaLibraryFragment()
        })
        binding.contentPlayListCard.setOnClickListener(myOnMultiClickListener {
            mMainActivity.startSongListFragment()
        })
        //取消过渡动画
        (binding.contentRv.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        binding.contentRv.itemAnimator = null
        mMainActivity.mainViewModel.musicSongList.observe(this) { songList ->
            val songBaseRvAdapter =
                BaseRvAdapter(songList, R.layout.item_song_layout) { itemData, view, position ->
                    val itemSongLayoutBinding = ItemSongLayoutBinding.bind(view)
                    itemSongLayoutBinding.musicTitle.text = itemData.name
                    itemSongLayoutBinding.musicAuthor.text = itemData.artist
                    //设置Bitmap
                    setImageBitmap(itemData, itemSongLayoutBinding, position, songList)
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
        return binding.root
    }

    /**
     * 设置Bitmap
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun BaseRvAdapter<Song>.setImageBitmap(
        itemData: Song,
        itemSongLayoutBinding: ItemSongLayoutBinding,
        position: Int,
        songList: MutableList<Song>
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
            mMainActivity.sharedPreferencesEdit.putInt("selectSongPosition",position)
            mMainActivity.sharedPreferencesEdit.commit()
            selectedEvents(position, songList, itemData)
        }
    }

    /**
     * 选中事件
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun BaseRvAdapter<Song>.selectedEvents(
        position: Int, songList: MutableList<Song>, itemData: Song
    ) {
        if (index == position) {
            binding.contentText.text = "正在播放 (${(position + 1)} / ${songList.size})"
            mMainActivity.mainViewModel.setSelectSong(
                SelectSongBean(
                    itemData, position
                )
            )
        }
    }
}
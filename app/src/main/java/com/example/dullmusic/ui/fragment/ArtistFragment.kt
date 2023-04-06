package com.example.dullmusic.ui.fragment

import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import com.example.base.base.BaseFragment
import com.example.base.base.BaseRvAdapter
import com.example.base.utils.getAlbumPicture
import com.example.base.utils.myOnMultiClickListener
import com.example.dullmusic.R
import com.example.dullmusic.bean.GsonSongBean
import com.example.dullmusic.databinding.FragmentArtistBinding
import com.example.dullmusic.databinding.ItemArtistLayoutBinding
import com.example.dullmusic.ui.activity.main.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@RequiresApi(Build.VERSION_CODES.O)
class ArtistFragment : BaseFragment() {
    val binding by lazy {
        FragmentArtistBinding.inflate(layoutInflater)
    }
    private val mMainActivity by lazy {
        (activity as MainActivity)
    }

    private val mMainViewModel by lazy {
        mMainActivity.mainViewModel
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mMainActivity.mainViewModel.isOtherPages.value = true

        val hashMap = HashMap<String, GsonSongBean>()
        mMainViewModel.musicSongList.value!!.forEachIndexed { index, oneSong ->
            if (hashMap.containsKey(oneSong.artist)) {
                hashMap[oneSong.artist]!!.musicList.add(oneSong)
            } else {
                hashMap[oneSong.artist] = GsonSongBean(mutableListOf(oneSong))
            }
        }
        mMainViewModel.artistMediaList = ArrayList(hashMap.values)
        binding.mediaTitle.text = "${hashMap.size} 个艺术家"
        val baseAdapter= BaseRvAdapter(
            mMainViewModel.artistMediaList,
            R.layout.item_artist_layout
        ) { itemData, view, position ->
            val itemArtistLayoutBinding = ItemArtistLayoutBinding.bind(view)
            CoroutineScope(Dispatchers.IO).launch {
                flow {
                    emit(mMainViewModel.musicSongListBitmap[itemData.musicList[0].data])
                }.catch {
                    emit(mMainViewModel.defaultAvatar)
                }.collect {
                    withContext(Dispatchers.Main) {
                        itemArtistLayoutBinding.musicPhotos.setImageBitmap(it)
                    }
                }
            }
            itemArtistLayoutBinding.musicTitle.text = itemData.musicList[0].artist
            itemArtistLayoutBinding.musicAuthor.text = "${itemData.musicList.size} 首"
            itemArtistLayoutBinding.root.setOnClickListener(myOnMultiClickListener {
                mMainActivity.mainViewModel.selectMusicSongBeanList = itemData
                mMainActivity.mainViewModel.selectMusicSongBeanListPosition = position
                mMainActivity.startMediaListDetailsFragment()
            })
        }
        binding.mediaRv.adapter = baseAdapter
        binding.search.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(p0: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                var mutableListOf = mutableListOf<GsonSongBean>()
                if(p0.toString().isEmpty()){
                    mutableListOf = mMainViewModel.artistMediaList
                }else{
                    mMainViewModel.artistMediaList.forEach {
                        if(it.musicList[0].artist.contains(p0.toString())){
                            mutableListOf.add(it)
                        }
                    }
                }
                baseAdapter.dataList = mutableListOf
                binding.mediaTitle.text = "${mutableListOf.size} 个艺术家"
                return false
            }
        })
        return binding.root
    }
}
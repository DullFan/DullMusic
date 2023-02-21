package com.example.dullmusic.ui.fragment

import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.annotation.RequiresApi
import com.example.base.base.BaseFragment
import com.example.base.base.BaseRvAdapter
import com.example.base.utils.getAlbumPicture
import com.example.base.utils.myOnMultiClickListener
import com.example.base.utils.showLog
import com.example.dullmusic.R
import com.example.dullmusic.bean.GsonSongBean
import com.example.dullmusic.bean.Song
import com.example.dullmusic.databinding.FragmentSongListBinding
import com.example.dullmusic.databinding.ItemTheAlbumListLayoutBinding
import com.example.dullmusic.ui.activity.main.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@RequiresApi(Build.VERSION_CODES.O)
class TheAlbumFragment : BaseFragment() {
    val binding by lazy {
        FragmentSongListBinding.inflate(layoutInflater)
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
            if (hashMap.containsKey(oneSong.album)) {
                hashMap[oneSong.album]!!.musicList.add(oneSong)
            } else {
                hashMap[oneSong.album] = GsonSongBean(mutableListOf(oneSong))
            }
        }
        mMainViewModel.theAlbumMediaList = ArrayList(hashMap.values)
        binding.mediaTitle.text = "${hashMap.size} 个专辑"
        val baseAdapter = BaseRvAdapter(
            mMainViewModel.theAlbumMediaList,
            R.layout.item_the_album_list_layout
        ) { itemData, view, position ->
            val itemTheAlbumListLayoutBinding  =ItemTheAlbumListLayoutBinding.bind(view)
            CoroutineScope(Dispatchers.IO).launch {
                flow {
                    val bitmap = getAlbumPicture(itemData.musicList[0].data)
                    emit(bitmap)
                }.catch {
                    emit(mMainViewModel.defaultAvatar)
                }.collect {
                    withContext(Dispatchers.Main){
                        itemTheAlbumListLayoutBinding.image.setImageBitmap(it)
                    }
                }
            }
            itemTheAlbumListLayoutBinding.title.text = itemData.musicList[0].album
            itemTheAlbumListLayoutBinding.number.text = "${itemData.musicList.size} 首"
            itemTheAlbumListLayoutBinding.root.setOnClickListener(myOnMultiClickListener {
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
                    mutableListOf = mMainViewModel.theAlbumMediaList
                }else{
                    mMainViewModel.theAlbumMediaList.forEach {
                        if(it.musicList[0].album.contains(p0.toString())){
                            mutableListOf.add(it)
                        }
                    }
                }
                baseAdapter.dataList = mutableListOf
                binding.mediaTitle.text = "${mutableListOf.size} 个专辑"
                return false
            }
        })
        return binding.root
    }
}
package com.example.dullmusic.ui.fragment

import android.graphics.Color
import android.os.*
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import androidx.recyclerview.widget.RecyclerView.VIEW_LOG_TAG
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.base.base.BaseFragment
import com.example.base.base.BaseRvAdapter
import com.example.base.utils.*
import com.example.dullmusic.R
import com.example.dullmusic.databinding.FragmentLrcBinding
import com.example.dullmusic.databinding.ItemLrcRvLayoutBinding
import com.example.dullmusic.lrc.LrcBean
import com.example.dullmusic.ui.activity.main.MainActivity


class LrcFragment : BaseFragment() {
    private val binding by lazy {
        FragmentLrcBinding.inflate(layoutInflater)
    }
    private val mMainActivity by lazy {
        (activity as MainActivity)
    }
    var currentIndex = 0

    lateinit var baseRvAdapter: BaseRvAdapter<LrcBean>
    lateinit var linearLayoutManager:LinearLayoutManager

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        linearLayoutManager = LinearLayoutManager(requireContext())

        mMainActivity.setMainLrcHandlerF(object :MainActivity.MainLrcHandler{
            override fun onMainHandlerListener() {
                val currentPosition = mMainActivity.audioBinder.getCurrentPosition()
                if (::baseRvAdapter.isInitialized) {
                    mMainActivity.lrcBeanList.forEachIndexed { index, lrcBean ->
                        if (currentPosition >= lrcBean.start && index != baseRvAdapter.index) {
                            currentIndex = index
                            baseRvAdapter.index = index
                            if(binding.rv.scrollState == SCROLL_STATE_IDLE){
                                linearLayoutManager.scrollToPositionWithOffset(index,100.px.toInt())
                            }
                        }
                    }
                }
            }
        })
        mMainActivity.setMainLrcEndOfSongF(object :MainActivity.MainLrcEndOfSong{
            override fun onEndOfSongPlayListener() {
                baseRvAdapter.dataList = mMainActivity.lrcBeanList
                if(mMainActivity.lrcBeanList.size == 0){
                    binding.rv.visibility = View.GONE
                    binding.noData.visibility = View.VISIBLE
                }else{
                    binding.rv.visibility = View.VISIBLE
                    binding.noData.visibility = View.GONE
                }
            }
        })

        binding.rv.layoutManager = linearLayoutManager
        (binding.rv.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        baseRvAdapter = BaseRvAdapter(
            mMainActivity.lrcBeanList,
            R.layout.item_lrc_rv_layout
        ) { itemData, view, position ->
            val itemLrcRvLayoutBinding = ItemLrcRvLayoutBinding.bind(view)
            itemLrcRvLayoutBinding.itemText01.text = itemData.lrc
            if (itemData.translateLrc == "") {
                itemLrcRvLayoutBinding.itemText02.visibility = View.GONE
            } else {
                itemLrcRvLayoutBinding.itemText02.visibility = View.VISIBLE
                itemLrcRvLayoutBinding.itemText02.text = itemData.translateLrc
            }

            when (position) {
                0 -> {
                    itemLrcRvLayoutBinding.itemLayout.setPadding(20.px.toInt(), 80.px.toInt(), 20.px.toInt(), 0)
                }
                mMainActivity.lrcBeanList.size - 1 -> {
                    itemLrcRvLayoutBinding.itemLayout.setPadding(20.px.toInt(), 0, 20.px.toInt(), 80.px.toInt())
                }
                else -> {
                    itemLrcRvLayoutBinding.itemLayout.setPadding(20.px.toInt(), 5.px.toInt(), 2.px.toInt(), 20.px.toInt())
                }
            }

            itemLrcRvLayoutBinding.itemLayout.setOnClickListener(myOnMultiClickListener {
                mMainActivity.audioBinder.seekTo(itemData.start)
                if(!mMainActivity.audioBinder.mediaIsPlaying()){
                    mMainActivity.playMusic()
                }
                index = position
            })

            if (index == position) {
                itemLrcRvLayoutBinding.itemText01.setTextColor(resources.getColor(R.color.black))
                itemLrcRvLayoutBinding.itemText02.setTextColor(resources.getColor(R.color.black))
            } else {
                itemLrcRvLayoutBinding.itemText01.setTextColor(resources.getColor(R.color.text_grey))
                itemLrcRvLayoutBinding.itemText02.setTextColor(resources.getColor(R.color.text_grey))
            }
        }
        baseRvAdapter.index = currentIndex
        binding.rv.adapter = baseRvAdapter


        return binding.root
    }
}
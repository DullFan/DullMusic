package com.example.dullmusic.ui.activity

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.base.base.BaseActivity
import com.example.base.base.BaseRvAdapter
import com.example.base.utils.myOnMultiClickListener
import com.example.dullmusic.R
import com.example.dullmusic.databinding.ActivitySettingBinding
import com.example.dullmusic.databinding.ItemSettingLayoutBinding

class SettingActivity : BaseActivity() {
    private val binding by lazy {
        ActivitySettingBinding.inflate(layoutInflater)
    }
    private var playMode = 0
    private var playSpeed = 0
    private var playTimer = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.musicSettingUp.setOnClickListener(myOnMultiClickListener {
            onBackPressed()
        })

        val playModeStringList = mutableListOf("列表", "循环", "单曲")
        binding.musicSettingPlayModeRv.layoutManager = LinearLayoutManager(this).apply {
            orientation = LinearLayoutManager.HORIZONTAL
        }
        binding.musicSettingPlayModeRv.adapter = BaseRvAdapter(
            playModeStringList,
            R.layout.item_setting_layout
        ) { itemData, view, position ->
            val itemSettingLayoutBinding = ItemSettingLayoutBinding.bind(view)
            if (playMode == position) {
                itemSettingLayoutBinding.itemSettingLayoutCard.setCardBackgroundColor(
                    resources.getColor(
                        R.color.white
                    )
                )
            } else {
                itemSettingLayoutBinding.itemSettingLayoutCard.setCardBackgroundColor(
                    resources.getColor(
                        R.color.background_color
                    )
                )
            }
            itemSettingLayoutBinding.itemSettingLayoutText.text = itemData

            itemSettingLayoutBinding.root.setOnClickListener(myOnMultiClickListener {
                playMode = position
                notifyDataSetChanged()
            })
        }

        val playSpeedStringList = mutableListOf(
            "0.25",
            "0.5",
            "0.75",
            "0.9",
            "1.0",
            "1.1",
            "1.25",
            "1.5",
            "1.75",
            "2.0",
            "3.0",
            "4.0",
            "5.0"
        )
        binding.musicSettingSpeedRv.layoutManager = LinearLayoutManager(this).apply {
            orientation = LinearLayoutManager.HORIZONTAL
        }
        binding.musicSettingSpeedRv.adapter = BaseRvAdapter(
            playSpeedStringList,
            R.layout.item_setting_layout
        ) { itemData, view, position ->
            val itemSettingLayoutBinding = ItemSettingLayoutBinding.bind(view)
            if (playSpeed == position) {
                itemSettingLayoutBinding.itemSettingLayoutCard.setCardBackgroundColor(
                    resources.getColor(
                        R.color.white
                    )
                )
            } else {
                itemSettingLayoutBinding.itemSettingLayoutCard.setCardBackgroundColor(
                    resources.getColor(
                        R.color.background_color
                    )
                )
            }
            itemSettingLayoutBinding.itemSettingLayoutText.text = itemData

            itemSettingLayoutBinding.root.setOnClickListener(myOnMultiClickListener {
                playSpeed = position
                notifyDataSetChanged()
            })
        }

        val playTimerStringList = mutableListOf(
            "OFF",
            "15min",
            "30min",
            "45min",
            "1h",
            "1h30min",
            "2h",
            "2h30min",
            "3h",
        )
        binding.musicSettingTimerRv.layoutManager = LinearLayoutManager(this).apply {
            orientation = LinearLayoutManager.HORIZONTAL
        }
        binding.musicSettingTimerRv.adapter = BaseRvAdapter(
            playTimerStringList,
            R.layout.item_setting_layout
        ) { itemData, view, position ->
            val itemSettingLayoutBinding = ItemSettingLayoutBinding.bind(view)
            if (playTimer == position) {
                itemSettingLayoutBinding.itemSettingLayoutCard.setCardBackgroundColor(
                    resources.getColor(
                        R.color.white
                    )
                )
            } else {
                itemSettingLayoutBinding.itemSettingLayoutCard.setCardBackgroundColor(
                    resources.getColor(
                        R.color.background_color
                    )
                )
            }
            itemSettingLayoutBinding.itemSettingLayoutText.text = itemData

            itemSettingLayoutBinding.root.setOnClickListener(myOnMultiClickListener {
                playTimer = position
                notifyDataSetChanged()
            })
        }



    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(
            R.anim.slide_pop_in_top,
            R.anim.slide_pop_out_bottom
        )
    }
}
package com.example.base.base

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/**
 * 通用RecyclerView适配器
 */
class BaseRvAdapterPosition<T>(
    _dataList: List<T>,
    var layoutId: Int,
    private val onBind: BaseRvAdapterPosition<T>.(itemData: T, view: View, position: Int,holderPosition: Int) -> Unit
) : RecyclerView.Adapter<BaseRvViewHolder>() {
    var index = 0
        set(value) {
            notifyItemChanged(field)
            field = value
            notifyItemChanged(index)
        }
    var dataList = _dataList
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseRvViewHolder {
        return BaseRvViewHolder(
            LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        )
    }

    override fun onBindViewHolder(holder: BaseRvViewHolder, position: Int) {
        onBind.invoke(this, dataList[position], holder.itemView, position,holder.bindingAdapterPosition)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

}


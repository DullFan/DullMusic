package com.example.base.base

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/**
 * 通用RecyclerView适配器
 */
class BaseRvAdapter<T>(
    _dataList: List<T>,
    var layoutId: Int,
    private val onBind: BaseRvAdapter<T>.(itemData: T, view: View, position: Int) -> Unit
) : RecyclerView.Adapter<BaseRvViewHolder>() {
    var index = 0
        set(value) {
            notifyItemChanged(field)
            field = value
            notifyItemChanged(field)
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
        onBind.invoke(this, dataList[position], holder.itemView, position)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

}

class BaseRvViewHolder(view: View) : RecyclerView.ViewHolder(view)
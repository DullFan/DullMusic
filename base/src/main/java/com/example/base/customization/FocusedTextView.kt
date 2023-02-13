package com.example.base.customization

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView

class FocusedTextView(context: Context, attrs: AttributeSet?) :
    androidx.appcompat.widget.AppCompatTextView(context, attrs) {
    var isFocusedTextView = false
        set(value) {
            field = value
            invalidate()
        }

    override fun isFocused(): Boolean {
        return isFocusedTextView
    }
}
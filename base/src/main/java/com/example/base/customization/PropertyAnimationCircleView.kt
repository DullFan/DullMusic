package com.example.base.customization

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import com.example.base.utils.px


class PropertyAnimationCircleView(context: Context, attributeSet: AttributeSet) :
    View(context, attributeSet) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    var radius = 50.px
        set(value) {
            field = value
            invalidate()
        }

    init {
        paint.color = Color.parseColor("#00796B")
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val path = Path()
        val radii = floatArrayOf(
            10.px, 10.px, // top left corner
            15.px, 15.px, // top right corner
            20.px, 20.px, // bottom right corner
            25.px, 25.px // bottom left corner
        )
        path.addRoundRect(0.px, 100.px, 100.px, 200.px, radii, Path.Direction.CW)
        canvas.clipPath(path)
        canvas.drawRoundRect(0.px, 100.px, 100.px, 200.px,0f,0f,paint)
    }
}
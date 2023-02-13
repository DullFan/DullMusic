package com.example.dullmusic.lrc

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.example.base.utils.px

/**
 * 歌词,有点问题,滚动的时候看很变扭
 */
class LrcView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    /**
     * 歌词画笔
     */
    val lrcPaint by lazy {
        Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
            color = lrcTextColor
            textSize = lrcTextSize
            textAlign = Paint.Align.CENTER
        }
    }

    /**
     * 当前歌词画笔
     */
    val lrcHighLinePaint by lazy {
        Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
            color = highLineTextColor
            textSize = lrcTextSize
            textAlign = Paint.Align.CENTER
        }
    }

    /**
     * 歌词颜色
     */
    var lrcTextColor: Int = Color.GRAY

    /**
     * 当前歌词颜色
     */
    var highLineTextColor = Color.BLACK

    /**
     * 行间距
     */
    var lineSpacing = 15.px

    /**
     * 字体大小
     */
    var lrcTextSize = 20.px

    /**
     * 当前歌词位置
     */
    var currentPosition = 0

    /**
     * 上一句歌词位置
     */
    var lastPosition = 0

    /**
     * 当前歌曲播放的时间
     */
    var currentSongPosition = 0
        set(value) {
            field = value
            invalidate()
        }


    var lrcList: MutableList<LrcBean> = mutableListOf()
        set(value) {
            field = value
            lastPosition = 0
            currentPosition = 0
            invalidate()
        }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        getCurrentPosition()
        drawLrc(canvas)
        scrollLrc()
    }

    /**
     * 歌词滑动 ----> 有点问题
     */
    private fun scrollLrc() {
        val startTime = lrcList[currentPosition].start
        val fontMetrics = lrcHighLinePaint.fontMetrics
        val textHeight = (fontMetrics.bottom - fontMetrics.top) + lineSpacing
        val y = if (currentSongPosition - startTime > 500) {
            currentPosition * textHeight
        } else {
            lastPosition * textHeight + (currentPosition - lastPosition) * textHeight * ((currentSongPosition - startTime) / 500f)
        }
        scrollTo(0, y.toInt())
        if (scaleY == currentPosition * textHeight) {
            lastPosition = currentPosition
        }

    }

    /**
     * 画歌词
     */
    private fun drawLrc(canvas: Canvas) {
        lrcList.forEachIndexed { index, lrcBean ->
            val fontMetrics = lrcHighLinePaint.fontMetrics
            canvas.drawText(
                lrcBean.lrc,
                measuredWidth / 2f,
                measuredHeight / 2f + index * (fontMetrics.bottom - fontMetrics.top) + lineSpacing,
                if (currentPosition == index) lrcHighLinePaint else lrcPaint
            )
        }
    }

    /**
     * 得到当前歌词位置
     */
    private fun getCurrentPosition() {
        if (currentSongPosition < lrcList[0].start || currentSongPosition > 10 * 60 * 1000) {
            currentSongPosition = 0
            return
        } else if (currentSongPosition > lrcList[lrcList.size - 1].start) {
            currentPosition = lrcList.size - 1
            return
        }
        lrcList.forEachIndexed { index, lrcBean ->
            if (currentSongPosition >= lrcList[index].start && currentSongPosition <= lrcList[index].end) {
                currentPosition = index
            }
        }
    }
}
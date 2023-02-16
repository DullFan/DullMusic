package com.example.base.utils

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Toast
import com.google.gson.Gson
import java.io.File
import java.util.regex.Pattern

const val MIN_CLICK_DELAY_TIME = 250
var lastClickTime = 0L
val gson by lazy {
    Gson()
}

/**
 * 工具类
 */

/**
 * 防止重复点击
 */
fun myOnMultiClickListener(click: () -> Unit): View.OnClickListener {
    return View.OnClickListener {
        val curClickTime = System.currentTimeMillis()
        if (curClickTime - lastClickTime >= MIN_CLICK_DELAY_TIME) {
            lastClickTime = System.currentTimeMillis()
            click.invoke()
        }
    }
}

/**
 * 打印Log
 */
fun showLog(content: Any, tag: String = "TestFan") {
    Log.e(tag, "$content")
}


private lateinit var toast: Toast

/**
 * 打印Toast
 */
fun showToast(context: Context, content: Any) {
    toast = if (::toast.isInitialized) {
        toast.cancel()
        Toast.makeText(context, "$content", Toast.LENGTH_LONG)
    } else {
        Toast.makeText(context, "$content", Toast.LENGTH_LONG)
    }
    toast.show()
}

/**
 * dp -> px
 */
val Float.px
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this,
        Resources.getSystem().displayMetrics
    )

val Int.px
    get() = this.toFloat().px

// dp -> px
fun Float.dp(context: Context): Float {
    val scale = context.resources.displayMetrics.density
    return this / scale + 0.5f
}

fun Int.dp(context: Context): Int {
    return this.toFloat().dp(context).toInt()
}

/**
 * 获取专辑封面
 *
 * @param path    歌曲路径
 * @return
 */
suspend fun getAlbumPicture(path: String): Bitmap? {
    // 歌曲检索
    val mmr = MediaMetadataRetriever()
    //设置数据源
    mmr.setDataSource(path)
    //获取图片数据
    val data: ByteArray? = mmr.embeddedPicture
    if (data != null) {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        options.inPreferredConfig = Bitmap.Config.RGB_565
        BitmapFactory.decodeByteArray(data, 0, data.size, options)
        val originalWidth = options.outWidth
        val originalHeight = options.outHeight
        options.inSampleSize = getSimpleSize(originalWidth, originalHeight, 30, 30)
        /*
          inJustDecodeBounds设置为false, 真正的去加载Bitmap
         */
        options.inJustDecodeBounds = false;
        val albumPicture = BitmapFactory.decodeByteArray(data, 0, data.size, options)
        return albumPicture
    } else {
        return null
    }
}

/**
 * 获取压缩比例，如果原图宽比高长，则按照宽来压缩，反之则按照高来压缩
 *
 * @return 压缩比例，原图和压缩后图的比例
 */
private fun getSimpleSize(
    originalWidth: Int,
    originalHeight: Int,
    pixWidth: Int,
    pixHeight: Int
): Int {
    var simpleSize = 1
    if (originalWidth > originalHeight && originalWidth > pixWidth) {
        simpleSize = originalWidth / pixWidth
    } else if (originalHeight > originalWidth && originalHeight > pixHeight) {
        simpleSize = originalHeight / pixHeight
    }
    if (simpleSize <= 0) {
        simpleSize = 1
    }
    return simpleSize
}

/**
 * 将duration转换成00:00格式
 */
fun convertComponentSeconds(duration: Int): String {
    val second = duration / 1000
    val point = second / 60
    val remainingSecond = second - 60 * point
    return "${if (point >= 10) point else "0$point"}:${if (remainingSecond >= 10) "$remainingSecond" else "0${remainingSecond}"}"
}

/**
 * 判断该路径的文件是否存在
 * */
fun fileExists(targetFileAbsPath: String): Boolean {
    try {
        val f = File(targetFileAbsPath)
        if (!f.exists()) return false
    } catch (e: Exception) {
        return false
    }
    return true
}
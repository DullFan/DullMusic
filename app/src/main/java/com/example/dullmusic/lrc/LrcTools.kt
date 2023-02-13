package com.example.dullmusic.lrc

import android.text.TextUtils
import java.io.*
import java.nio.charset.StandardCharsets

fun parseLrcFile(MusicPath: String): String {
    if (TextUtils.isEmpty(MusicPath) || !MusicPath.contains(".lrc")) return ""
    try {
        val lrcStr = StringBuilder()
        val fis = FileInputStream(File(MusicPath))
        val bis = BufferedInputStream(fis)
        //首先确定文件的文本编码类型，"utf-8"或者"gbk"不然乱码
        val reader = BufferedReader(InputStreamReader(bis, StandardCharsets.UTF_8))
        var line = reader.readLine()
        while (line != null) {
            lrcStr.append(line).append("\n")
            line = reader.readLine()
        }
        fis.close()
        bis.close()
        return lrcStr.toString()
    } catch (e: IOException) {
        return ""
    }
}

fun parseStr2List(lrcStr: String): MutableList<LrcBean> {
    val list: MutableList<LrcBean> = mutableListOf()
    // 值为1表示没有翻译、值为2表示有翻译
    var theNumberOfOccurrences = 0
    try {
        //1.更替转义字符
        val lrcText = lrcStr.replace("&#58;".toRegex(), ":").replace("&#10;".toRegex(), "\n")
            .replace("&#46;".toRegex(), ".").replace("&#32;".toRegex(), " ")
            .replace("&#45;".toRegex(), "-").replace("&#13;".toRegex(), "\r")
            .replace("&#39;".toRegex(), "'").replace("&nbsp;".toRegex(), " ") //空格替换
            .replace("&apos;".toRegex(), "'") //分号替换
            .replace("&&".toRegex(), "/") //空格替换
            .replace("\\|".toRegex(), "/")

        //2.更替转义字符后，将此字符串转换成字符数组，区分每个字符的边界是"\n"换行符
        val split = lrcText.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        //3.根据定界正则表达式【换行符】为条件，转换成字符数组后，对每行字符串进行信息提炼（开始时间，每行歌词）
        for (i in split.indices) {
            val lrcInfo = split[i]
            if (" " == lrcInfo || TextUtils.isEmpty(lrcInfo)) continue
            if (lrcInfo.contains("[ar:") || lrcInfo.contains("[offset:") || lrcInfo.contains(
                    "[al:"
                ) || lrcInfo.contains("[by:")
            ) {
                continue
            }
            if (lrcInfo.contains("[ti:")) {
                theNumberOfOccurrences++
                continue
            }
            val lrc = lrcInfo.substring(lrcInfo.indexOf("]") + 1)
            //如果该行文字为空或者1个空格符
            if (TextUtils.isEmpty(lrc) || " " == lrc || "//" == lrc) continue

            //解析当前行歌词信息
            val min = lrcInfo.substring(lrcInfo.indexOf("[") + 1, lrcInfo.indexOf("[") + 3)
            val seconds = lrcInfo.substring(lrcInfo.indexOf(":") + 1, lrcInfo.indexOf(":") + 3)
            val mills = lrcInfo.substring(lrcInfo.indexOf(".") + 1, lrcInfo.indexOf(".") + 3)
            val startTime = min.toLong() * 60 * 1000 + //解析分钟
                    seconds.toLong() * 1000 + //解析秒钟
                    mills.toLong() * 10 //解析毫秒
            //判断是否为翻译
            if (list.size > 0
                && !lrc.contains("by:")
                && !lrc.contains("：")
                && !lrc.contains("词：")
                && !lrc.contains(
                    "曲："
                ) && theNumberOfOccurrences == 2
            ) {
                list.forEachIndexed { index, lrcBean ->
                    // 可能翻译有1ms误差
                    if (startTime == lrcBean.start ||
                        startTime in (lrcBean.start - 20)..(lrcBean.start + 20)) {
                        list[index].translateLrc = lrc
                    }
                }
                continue
            }
            //处理完成，装载歌词
            val lrcBean = LrcBean(lrc, start = startTime)
            list.add(lrcBean)
            //设置当前句歌词结束时间为下一句歌词开始时间
            if (list.size > 1) list[list.size - 2].end = startTime
            //如果是最后一句歌词，则设置结束时间为无限长，超过歌曲播放时长也可
            if (i == split.size - 1) list[list.size - 1].end = (startTime + 100000)
        }
        return list
    } catch (e: Exception) {
        return ArrayList()
    }
}
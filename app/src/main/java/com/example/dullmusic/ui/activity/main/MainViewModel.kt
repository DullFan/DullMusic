package com.example.dullmusic.ui.activity.main

import android.app.Application
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.example.base.utils.gson
import com.example.dullmusic.bean.GsonSongBean
import com.example.dullmusic.bean.SelectSongBean
import com.example.dullmusic.bean.Song
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class MainViewModel(private val application: Application) : AndroidViewModel(application) {
    var musicSongList = MutableLiveData<MutableList<Song>>()
    var selectSongBean = MutableLiveData<SelectSongBean>()
    var selectBitmap = MutableLiveData<Bitmap>()

    /**
     * 请求媒体库数据
     */
    suspend fun requestMusicSong(
        musicData: String,
        isRefresh: Boolean = false,
        action: (dataList: MutableList<Song>) -> Unit = {}
    ) {
        //判断数据库是否存在数据，如果不存在则查询本地媒体库
        if (musicData.isEmpty() || isRefresh) {
            val cursor = application.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,
                null,
                null,
                MediaStore.Audio.AudioColumns.IS_MUSIC,
            )
            if (cursor != null) {
                val _musicSongList = mutableListOf<Song>()
                while (cursor.moveToNext()) {
                    //歌曲时长
                    val duration =
                        cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
                    //不保存60秒以下的文件
                    if (duration / 1000 > 60) {
                        //歌曲名称
                        var name =
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME))
                        // 注释部分是切割标题，分离出歌曲名和歌手 （本地媒体库读取的歌曲信息不规范）
                        if (name.contains("-")) {
                            val str: List<String> = name.split("-")
                            name = str[1].substring(1, str[1].length)
                            if (name.contains(".")) {
                                val split = name.split(".")
                                name = split[0]
                                if (name.contains("[")) {
                                    name = name.split("[")[0]
                                }
                            }
                        }
                        //歌手
                        val artist =
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
                        //专辑名称
                        val album =
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM))
                        //歌曲路径
                        val data =
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
                        //歌曲大小
                        val size =
                            cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE))
                        _musicSongList += Song(0, name, artist, album, data, duration, size)
                    }
                }
                MainScope().launch {
                    _musicSongList.reverse()
                    musicSongList.value = _musicSongList
                    action.invoke(_musicSongList)
                }
                cursor.close()
            }
        } else {
            MainScope().launch {
                val fromJson = gson.fromJson(musicData, GsonSongBean::class.java)
                fromJson.musicList.reverse()
                musicSongList.value = fromJson.musicList
            }
        }
    }

    fun setSelectSong(
        selectSongData: SelectSongBean,
    ) {
        selectSongBean.value = selectSongData
    }

    fun selectBitmap(
        bitmap: Bitmap,
    ) {
        selectBitmap.value = bitmap
    }
}
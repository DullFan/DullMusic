package com.example.dullmusic.ui.activity.main

import android.app.Application
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.example.base.utils.gson
import com.example.base.utils.showLog
import com.example.dullmusic.R
import com.example.dullmusic.bean.GsonSongBean
import com.example.dullmusic.bean.SelectSongBean
import com.example.dullmusic.bean.Song
import com.example.dullmusic.lrc.LrcBean
import com.example.dullmusic.tool.ALL_SONG_PLAY_LIST_STRING
import com.example.dullmusic.tool.MUSIC_LIST_STRING
import com.example.dullmusic.tool.SELECT_SONG_PATH
import com.example.dullmusic.tool.SONG_PLAY_LIST_STRING
import com.example.media.ExoPlayerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainViewModel(private val application: Application) : AndroidViewModel(application) {
    /**
     * 媒体库
     */
    var musicSongList = MutableLiveData<MutableList<Song>>()

    /**
     * 播放列表
     */
    var musicPlaySongList = MutableLiveData<MutableList<Song>>()

    /**
     * 播放列表
     */
    var musicSongListBitmap = HashMap<String,Bitmap>()

    /**
     * 选中的列表
     */
    var selectMusicSongBeanList:GsonSongBean = GsonSongBean(mutableListOf(),"")

    /**
     * 选中的列表Position
     */
    var selectMusicSongBeanListPosition = 0

    val defaultAvatar by lazy {
        application.resources.getDrawable(R.drawable.default_avatar).toBitmap()
    }

    /**
     * 当前选中的歌曲
     */
    var selectSongBean = MutableLiveData<SelectSongBean>()

    /**
     * 当前选中的Bitmap
     */
    var selectBitmap = MutableLiveData<Bitmap>()

    /**
     * 是否开启其他Fragment
     */
    var isOtherPages = MutableLiveData(false)

    /**
     *  控制点击刷新后动画、跳转指定歌词不触发
     */
    var isOnClickRefresh = false

    /**
     * 当前选中的歌词
     */
    var currentLrcIndex = 0

    /**
     * 存放歌词集合
     */
    var lrcBeanList: MutableList<LrcBean> = mutableListOf()

    /**
     * 歌单详情和歌单列表中的显示文字
     */
    var playListSongNumberString = MutableLiveData<String>()

    /**
     * 专辑列表数据
     */
    var theAlbumMediaList = mutableListOf<GsonSongBean>()

    /**
     * 艺术家列表
     */
    var artistMediaList = mutableListOf<GsonSongBean>()

    private val sharedPreferences: SharedPreferences by lazy {
        application.getSharedPreferences("data", AppCompatActivity.MODE_PRIVATE)
    }
    val sharedPreferencesEdit: SharedPreferences.Editor by lazy {
        sharedPreferences.edit()
    }

    fun getMusicListString() = sharedPreferences.getString(MUSIC_LIST_STRING, "")!!
    fun getSelectSongPath() = sharedPreferences.getString(SELECT_SONG_PATH, "")!!
    fun getSongPlayListString() = sharedPreferences.getString(SONG_PLAY_LIST_STRING, "")!!
    fun getAllSongPlayListString() = sharedPreferences.getString(ALL_SONG_PLAY_LIST_STRING, "")!!

    fun sharedPreferencesEditCommitData(_action: SharedPreferences.Editor.() -> Unit) {
        _action.invoke(sharedPreferencesEdit)
        sharedPreferencesEdit.commit()
    }

    /**
     * 请求媒体库数据
     */
    suspend fun requestMusicSong(
        isRefresh: Boolean = false
    ) {
        val musicData = getMusicListString()
        val musicPlayData = getSongPlayListString()
        //判断数据库是否存在数据，如果不存在则查询本地
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
                withContext(Dispatchers.Main) {
                    _musicSongList.reverse()
                    if (!isRefresh) {
                        musicPlaySongList.value = _musicSongList
                    }
                    musicSongList.value = _musicSongList
                    if (!isRefresh) {
                        sharedPreferencesEditCommitData {
                            putString(
                                SONG_PLAY_LIST_STRING,
                                gson.toJson(GsonSongBean(_musicSongList))
                            )
                        }
                    }

                    sharedPreferencesEditCommitData {
                        putString(MUSIC_LIST_STRING, gson.toJson(GsonSongBean(_musicSongList)))
                    }
                }
                cursor.close()
            }
        } else {
            withContext(Dispatchers.Main) {
                if (musicPlayData != "") {
                    val songMutableList =
                        gson.fromJson(musicPlayData, GsonSongBean::class.java).musicList
                    musicPlaySongList.value = songMutableList
                }
                val fromJson = gson.fromJson(musicData, GsonSongBean::class.java)
                musicSongList.value = fromJson.musicList
            }
        }
    }

    fun setSelectSong(
        selectSongData: SelectSongBean,
    ) {
        selectSongBean.value = selectSongData
    }

    fun setSelectBitmap(
        bitmap: Bitmap,
    ) {
        selectBitmap.value = bitmap
    }

    /**
     * 通过路径判断当前播放列表下标位置
     */
    fun selectIndexMusicPlay(selectSongPath: String?): Int {
        val index = if (selectSongPath == "") {
            0
        } else {
            var i = 0
            musicPlaySongList.value?.forEachIndexed { index, song ->
                if (selectSongPath == song.data) {
                    i = index
                }
            }
            i
        }
        return index
    }

    /**
     * 通过路径判断下标
     */
    fun selectIndex(selectSongPath: String?): Int {
        val index = if (selectSongPath == "") {
            0
        } else {
            var i = 0
            musicSongList.value?.forEachIndexed { index, song ->
                if (selectSongPath == song.data) {
                    i = index
                }
            }
            i
        }
        return index
    }
}
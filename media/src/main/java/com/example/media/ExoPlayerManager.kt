package com.example.media

import android.app.Application
import com.example.base.utils.showLog
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.MEDIA_ITEM_TRANSITION_REASON_AUTO

class ExoPlayerManager constructor(application: Application) {
    private val exoPlayer: ExoPlayer
    lateinit var endOfSongFan: EndOfSongFan

    init {
        exoPlayer = ExoPlayer.Builder(application.baseContext).build()
        // 设置循环播放
        exoPlayer.repeatMode = Player.REPEAT_MODE_ALL
        exoPlayer.addListener(object : Player.Listener {
            override fun onSeekForwardIncrementChanged(seekForwardIncrementMs: Long) {
                super.onSeekForwardIncrementChanged(seekForwardIncrementMs)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                if (MEDIA_ITEM_TRANSITION_REASON_AUTO == reason) {
                    if (::endOfSongFan.isInitialized) {
                        endOfSongFan.onEndOfSongPlayListener(exoPlayer.currentMediaItemIndex)
                    }
                }
            }
        })

    }

    /**
     * 歌曲播放完成回调
     */
    interface EndOfSongFan {
        fun onEndOfSongPlayListener(currentPosition: Int)
    }

    fun setEndOfSong(_endOfSong: EndOfSongFan) {
        endOfSongFan = _endOfSong
    }

    /**
     * 添加单个音乐到指定位置
     */
    fun addMediaItem(mediaItem: MediaItem, index: Int) {
        exoPlayer.addMediaItem(index, mediaItem)
    }

    /**
     * 添加单个音乐到播放列表末尾
     */
    fun addMediaItem(mediaItem: MediaItem) {
        exoPlayer.addMediaItem(mediaItem)
    }

    /**
     * 添加到歌单到播放列表末尾
     */
    fun addMediaItems(mediaItems: MutableList<MediaItem>) {
        exoPlayer.addMediaItems(mediaItems)
    }

    /**
     * 切换播放列表
     */
    fun setMediaItems(mediaItems: MutableList<MediaItem>) {
        exoPlayer.setMediaItems(mediaItems)
    }

    /**
     * 销毁
     */
    fun onDestroy() {
        stopMediaPlayer()
        exoPlayer.release()
    }

    /**
     * 移除歌曲
     */
    fun removeMediaItem(index: Int) {
        exoPlayer.removeMediaItem(index)
    }

    /**
     * 停止播放
     */
    fun stopMediaPlayer() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        }
    }

    /**
     * 暂停
     */
    fun stopMediaPlayerNoJudgment() {
        exoPlayer.pause()
    }

    /**
     * 跳转到指定位置
     */
    fun seekIndex(index: Int) {
        exoPlayer.seekTo(index, 0)
        playerMedia()
    }

    /**
     * 跳转到指定位置,不播放歌曲
     */
    fun seekIndexNotPlayer(index: Int) {
        exoPlayer.seekTo(index, 0)
    }

    /**
     * 跳转进度
     */
    fun seekTo(pos: Long) {
        exoPlayer.seekTo(pos)
    }

    /***
     * 播放音乐
     */
    fun playerMedia() {
        if (!exoPlayer.isPlaying) {
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    /**
     * 下一首
     */
    fun seekToNext() {
        exoPlayer.seekToNext()
    }

    /**
     * 上一首
     */
    fun seekToPrevious() {
        exoPlayer.seekToPrevious()
    }

    /**
     * 媒体是否在播放
     */
    fun mediaIsPlaying(): Boolean {
        return exoPlayer.isPlaying
    }

    /**
     * 获取歌曲播放时长
     */
    fun getDuration(): Long {
        return exoPlayer.duration
    }

    /**
     * 获取当前歌曲播放进度
     */
    fun getCurrentPosition(): Long {
        return exoPlayer.currentPosition
    }

    /**
     * 获取当前播放歌曲Index
     */
    fun getCurrentMediaItemIndex(): Int {
        return exoPlayer.currentMediaItemIndex
    }

    /**
     * 获取当前播放列表总数量
     */
    fun getMediaItemCount(): Int {
        return exoPlayer.mediaItemCount
    }

    /**
     * 更换播放顺序
     */
    fun moveMediaItem(currentIndex: Int, newIndex: Int) {
        exoPlayer.moveMediaItem(currentIndex, newIndex)
    }


}
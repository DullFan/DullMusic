package com.example.media

import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import androidx.media.MediaBrowserServiceCompat
import com.example.base.utils.showLog
import com.google.android.exoplayer2.MediaItem

class ExoPlayerService : MediaBrowserServiceCompat() {
    lateinit var exoPlayerManager: ExoPlayerManager

    val myBinder = AudioBinder()

    lateinit var seekToCallBack: SeekToCallBack

    inner class AudioBinder : Binder() {
        fun setMyBroadcastReceiverListenerF(_myBroadcastReceiverListener:MyBroadcastReceiverListener){
            myBroadcastReceiver.setMyBroadcastReceiverListenerF(_myBroadcastReceiverListener)
        }
        fun setSeekToCallBack(_seekToCallBack: SeekToCallBack){
            seekToCallBack = _seekToCallBack
        }
        @RequiresApi(Build.VERSION_CODES.O)
        fun buildNotification(
            isPlaying: Boolean,
            currentLocation: Long,
            title: String,
            artist: String,
            album: String,
            duration: Long,
            bitmap: Bitmap
        ) {
            getSystemService<NotificationManager>()?.notify(
                1, MediaNotification.buildNotification(
                    this@ExoPlayerService.applicationContext,
                    mediaSession,
                    isPlaying,
                    currentLocation,
                    1f,
                    title,
                    artist,
                    album,
                    duration,
                    bitmap
                )
            )
        }

        fun addMediaItem(mediaItem: MediaItem) {
            exoPlayerManager.addMediaItem(mediaItem)
        }

        fun playerMedia() {
            exoPlayerManager.playerMedia()
        }

        fun setMediaItems(mediaItems: MutableList<MediaItem>) {
            exoPlayerManager.setMediaItems(mediaItems)
        }

        fun addMediaItem(mediaItem: MediaItem, index: Int) {
            exoPlayerManager.addMediaItem(mediaItem, index)
        }

        fun seekIndex(index: Int) {
            exoPlayerManager.seekIndex(index)
        }

        fun seekIndexNotPlayer(index: Int) {
            exoPlayerManager.seekIndexNotPlayer(index)
        }

        fun onDestroy() {
            exoPlayerManager.onDestroy()
        }

        fun stopMediaPlayer() {
            exoPlayerManager.stopMediaPlayer()
        }

        fun stopMediaPlayerNoJudgment() {
            exoPlayerManager.stopMediaPlayerNoJudgment()
        }

        fun seekTo(pos: Long) {
            exoPlayerManager.seekTo(pos)
        }

        fun seekToNext() {
            exoPlayerManager.seekToNext()
        }

        fun seekToPrevious() {
            exoPlayerManager.seekToPrevious()
        }

        fun mediaIsPlaying(): Boolean {
            return exoPlayerManager.mediaIsPlaying()
        }

        fun removeMediaItem(index: Int) {
            exoPlayerManager.removeMediaItem(index)
        }

        fun setEndOfSong(_endOfSong: ExoPlayerManager.EndOfSongFan) {
            exoPlayerManager.setEndOfSong(_endOfSong)
        }

        fun getDuration(): Long {
            return exoPlayerManager.getDuration()
        }

        fun getCurrentPosition(): Long {
            return exoPlayerManager.getCurrentPosition()
        }

        fun getCurrentMediaItemIndex(): Int {
            return exoPlayerManager.getCurrentMediaItemIndex()
        }

        fun moveMediaItem(currentIndex: Int, newIndex: Int) {
            exoPlayerManager.moveMediaItem(currentIndex, newIndex)
        }

        fun getMediaItemCount(): Int {
            return exoPlayerManager.getMediaItemCount()
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return myBinder
    }

    lateinit var mediaSession: MediaSessionCompat
    lateinit var buildNotification: Notification
    val myBroadcastReceiver by lazy {
        MyBroadcastReceiver()
    }
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        exoPlayerManager = ExoPlayerManager(application)
        val intentFilter = IntentFilter()
        intentFilter.addAction(FAN_CUSTOM_ACTION_NEXT)
        intentFilter.addAction(FAN_CUSTOM_ACTION_PAUSE)
        intentFilter.addAction(FAN_CUSTOM_ACTION_PLAY)
        intentFilter.addAction(FAN_CUSTOM_ACTION_PREVIOUS)
        registerReceiver(myBroadcastReceiver, intentFilter)
        MediaNotification.createChannel(this.applicationContext)
        mediaSession = MediaSessionCompat(this, "ExoPlayerService")
        mediaSession.setCallback(object :MediaSessionCompat.Callback() {
            override fun onSeekTo(pos: Long) {
                super.onSeekTo(pos)
                if(::seekToCallBack.isInitialized){
                    seekToCallBack.seekTo(pos)
                }
            }
        })
        // 设置会话的令牌，以便客户端活动可以与其通信。
        sessionToken = mediaSession.sessionToken
        buildNotification =
            MediaNotification.buildNotification(this, mediaSession)
        getSystemService<NotificationManager>()?.notify(1, buildNotification)
        //设置为前景
        startForeground(1, buildNotification)
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        return BrowserRoot("Fan", null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {

    }
}
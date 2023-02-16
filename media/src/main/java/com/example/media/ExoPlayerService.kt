package com.example.media

import android.content.Intent
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaBrowserCompat
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.MediaItem

class ExoPlayerService : MediaBrowserServiceCompat() {
    companion object {
        lateinit var exoPlayerManager: ExoPlayerManager
    }

    val myBinder = AudioBinder()

    inner class AudioBinder : Binder() {
        fun addMediaItem(mediaItem: MediaItem) {
            exoPlayerManager.addMediaItem(mediaItem)
        }
        fun playerMedia() {
            exoPlayerManager.playerMedia()
        }
        fun setMediaItems(mediaItems: MutableList<MediaItem>){
            exoPlayerManager.setMediaItems(mediaItems)
        }
        fun addMediaItem(mediaItem: MediaItem,index: Int) {
            exoPlayerManager.addMediaItem(mediaItem,index)
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
        fun seekToNext(){
            exoPlayerManager.seekToNext()
        }
        fun seekToPrevious(){
            exoPlayerManager.seekToPrevious()
        }
        fun mediaIsPlaying(): Boolean {
            return exoPlayerManager.mediaIsPlaying()
        }
        fun removeMediaItem(index:Int){
            exoPlayerManager.removeMediaItem(index)
        }
        fun setEndOfSong(_endOfSong: ExoPlayerManager.EndOfSongFan){
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

    override fun onCreate() {
        super.onCreate()
        exoPlayerManager = ExoPlayerManager(application)
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
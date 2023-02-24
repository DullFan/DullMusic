package com.example.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.toBitmap
import androidx.media.session.MediaButtonReceiver
import com.example.base.utils.showLog
import java.time.Duration

object MediaNotification {
    private const val channelId = "DullMusic"
    lateinit var previousAction : NotificationCompat.Action
    lateinit var playAction : NotificationCompat.Action
    lateinit var pauseAction : NotificationCompat.Action
    lateinit var nextAction : NotificationCompat.Action

    @RequiresApi(Build.VERSION_CODES.O)
    fun createChannel(context: Context) {
        val notificationManager = context.getSystemService<NotificationManager>()
        val mChannel = NotificationChannel(channelId, "Fan", NotificationManager.IMPORTANCE_LOW)
        mChannel.description = "Media playback controls"
        mChannel.setShowBadge(false)
        mChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        notificationManager?.createNotificationChannel(mChannel)

        previousAction = NotificationCompat.Action(
            R.drawable.icon_skip_previous,
            "上一首",
            getPendingIntent(context, FAN_CUSTOM_ACTION_PREVIOUS)
        )

        playAction = NotificationCompat.Action(
            R.drawable.icon_play,
            "播放",
            getPendingIntent(context, FAN_CUSTOM_ACTION_PLAY)
        )


        pauseAction = NotificationCompat.Action(
            R.drawable.icon_pause,
            "暂停",
            getPendingIntent(context, FAN_CUSTOM_ACTION_PAUSE)
        )

        nextAction = NotificationCompat.Action(
            R.drawable.icon_skip_next,
            "下一首",
            getPendingIntent(context, FAN_CUSTOM_ACTION_NEXT)
        )
    }

    fun buildNotification(
        context: Context,
        mediaSession: MediaSessionCompat?,
        isPlaying: Boolean = false,
        position: Long = 0,
        playbackSpeed: Float = 1f,
        title: String = "",
        artist: String = "",
        album: String = "",
        duration: Long = 0,
        bitmap: Bitmap = context.resources.getDrawable(R.drawable.default_avatar).toBitmap(),
    ): Notification {
        mediaSession?.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
                .build()
        )
        mediaSession?.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(
                    if (isPlaying) PlaybackStateCompat.STATE_PLAYING
                    else PlaybackStateCompat.STATE_PAUSED,
                    position,
                    playbackSpeed
                )
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                            PlaybackStateCompat.ACTION_PAUSE or
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                            PlaybackStateCompat.ACTION_SEEK_TO
                )
                .build()
        )
        mediaSession
        val build = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.default_avatar)
            .setContentTitle(title)
            .setContentText(artist)
            .setOngoing(true)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .addAction(previousAction)
            .addAction(if (isPlaying) playAction else pauseAction)
            .addAction(nextAction)
            .build()
        return build
    }

    fun cancelNotification(context: Context) {
        context.getSystemService<NotificationManager>()?.cancel(1)
    }

    private fun getPendingIntent(context: Context, action: String): PendingIntent? {
        return PendingIntent.getBroadcast(
            context.applicationContext,
            0, Intent(action), PendingIntent.FLAG_IMMUTABLE
        )
    }
}
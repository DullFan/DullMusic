package com.example.dullmusic.bean

import android.graphics.Bitmap

data class SelectSongBean constructor(
    var song: Song,
    var selectPosition: Int
)
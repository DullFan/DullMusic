package com.example.dullmusic.bean


/**
 * 媒体库
 */
data class Song constructor(
     var id: Int,
     var name: String,
     var artist: String,
     var album: String,
     var data: String,
     var duration: Int,
     var size: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Song

        if (id != other.id) return false
        if (name != other.name) return false
        if (artist != other.artist) return false
        if (album != other.album) return false
        if (data != other.data) return false
        if (duration != other.duration) return false
        if (size != other.size) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + name.hashCode()
        result = 31 * result + artist.hashCode()
        result = 31 * result + album.hashCode()
        result = 31 * result + data.hashCode()
        result = 31 * result + duration
        result = 31 * result + size.hashCode()
        return result
    }
}
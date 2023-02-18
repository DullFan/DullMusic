package com.example.dullmusic.tool

import com.example.dullmusic.ui.fragment.HomeFragment

/**
 * 存放全局变量
 */

/**
 * 是否展开媒体
 */
var motionLayoutIsExpand = false

/**
 * 是否手动点击切换音乐，防止二次调用audioBinder.seekIndex(it.selectPosition)
 */
var isClickOnTheNextSong = true

/**
 *  判断展开播放器点击的是下一首还是上一首，分别开启不同的动画效果
 */
var isTheNextSongClick = true

/**
 * 媒体库列表 key
 */
const val MUSIC_LIST_STRING = "musicListString"

/**
 * 选中的歌曲路径 key
 */
const val SELECT_SONG_PATH = "selectSongPath"

/**
 * 播放列表歌单 key
 */
const val SONG_PLAY_LIST_STRING = "SongPlayListString"

/**
 * 所有歌单 key
 */
const val ALL_SONG_PLAY_LIST_STRING = "AllSongPlayListString"

/**
 * Fragment 名字
 */
const val HomeFragmentName = "HomeFragmentName"
const val SongListDetailsFragmentName = "SongListDetailsFragmentName"


package com.example.dullmusic.ui.activity.main

import android.Manifest
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Animatable
import android.graphics.drawable.ColorDrawable
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.*
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.base.base.BaseActivity
import com.example.base.base.BaseRvAdapter
import com.example.base.base.BaseRvAdapterPosition
import com.example.base.utils.*
import com.example.dullmusic.R
import com.example.dullmusic.bean.AllGsonSongBean
import com.example.dullmusic.bean.GsonSongBean
import com.example.dullmusic.bean.SelectSongBean
import com.example.dullmusic.bean.Song
import com.example.dullmusic.databinding.*
import com.example.dullmusic.lrc.parseLrcFile
import com.example.dullmusic.lrc.parseStr2List
import com.example.dullmusic.tool.*
import com.example.dullmusic.ui.activity.SettingActivity
import com.example.dullmusic.ui.fragment.*
import com.example.media.ExoPlayerManager
import com.example.media.ExoPlayerService
import com.google.android.exoplayer2.MediaItem
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

const val MIN_OUT_LAYOUT_TIME = 1000
var lastOutLayoutTime = 0L

@Suppress("IMPLICIT_CAST_TO_ANY")
@RequiresApi(Build.VERSION_CODES.O)
open class MainActivity : BaseActivity() {
    lateinit var binding: ActivityMainBinding

    val mainViewModel by lazy {
        ViewModelProvider(
            this, ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[MainViewModel::class.java]
    }

    /**
     * 权限申请对话框
     */
    lateinit var permissionsDialog: AlertDialog

    /**
     * 权限申请
     */
    val registerForActivityResult =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (it[Manifest.permission.READ_EXTERNAL_STORAGE]!! && it[Manifest.permission.WRITE_EXTERNAL_STORAGE]!!) {
                requestData()
            }
        }

    private val permissions = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    /**
     * 歌曲进度监听Handler
     */
    private val progressHandler: Handler = object : Handler(Looper.myLooper()!!) {
        override fun handleMessage(msg: Message) {
            if (binding.musicPlayPause.tag == "play") {
                val currentPosition = mainViewModel.audioBinder.getCurrentPosition().toInt()
                binding.musicSeekbar.progress =
                    mainViewModel.audioBinder.getCurrentPosition().toInt()
                binding.musicSeekbarStartTime.text = convertComponentSeconds(currentPosition)
                lrcSet(currentPosition)
                if (::mainLrcHandler.isInitialized && binding.fragment.visibility == View.VISIBLE) {
                    mainLrcHandler.onMainHandlerListener()
                }
            }
            sendEmptyMessageDelayed(1, 500)
        }
    }

    lateinit var mainLrcEndOfSong: MainLrcEndOfSong
    lateinit var mainLrcHandler: MainLrcHandler
    lateinit var playListDialog: PlayListDialog

    fun setMainLrcHandlerF(_action: MainLrcHandler) {
        mainLrcHandler = _action
    }

    fun setMainLrcEndOfSongF(_action: MainLrcEndOfSong) {
        mainLrcEndOfSong = _action
    }

    fun setPlayListDialogF(_action: PlayListDialog) {
        playListDialog = _action
    }

    /**
     * 切换歌曲时通知歌词切换数据
     */
    fun interface MainLrcEndOfSong {
        fun onEndOfSongPlayListener()
    }

    /**
     * 歌词详情Handler监听
     */
    fun interface MainLrcHandler {
        fun onMainHandlerListener()
    }

    /**
     * 和播放列表进行绑定
     */
    fun interface PlayListDialog {
        fun onPlayListener(path: String)
    }

    private fun lrcSet(currentPosition: Int) {
        if (mainViewModel.lrcBeanList.size != 0 && binding.musicLyrics.text != "暂无歌词") {
            mainViewModel.lrcBeanList.forEach {
                //因为要启动动画所以加400毫秒
                val i = currentPosition + 400
                if (i >= it.start && i <= it.end && binding.musicLyrics.text != it.lrc) {
                    startTextLrcAnimator(binding.musicLyrics) {
                        binding.musicLyrics.text = it.lrc
                    }
                }
            }
        }
    }

    val audioManager by lazy {
        getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }


    /**
     * 焦点请求
     */
    val focusRequest by lazy {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
            setAudioAttributes(AudioAttributes.Builder().run {
                setUsage(AudioAttributes.USAGE_GAME)
                setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                build()
            })
            setAcceptsDelayedFocusGain(true)
            setOnAudioFocusChangeListener { focusChange ->
                mainViewModel.audioBinder.stopMediaPlayerNoJudgment()
                binding.musicPlayPause.tag = "pause"
                binding.musicPlayPause.setImageResource(R.drawable.icon_pause)
            }
            build()
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestPermission()
    }

    /**
     * 请求数据
     */
    private fun requestData() {
        CoroutineScope(Dispatchers.IO).launch {
            mainViewModel.requestMusicSong()
            withContext(Dispatchers.Main) {
                if (::permissionsDialog.isInitialized) {
                    permissionsDialog.dismiss()
                }
                bindExoPlayerService()
            }
        }
    }

    val bottomSheetDialog by lazy {
        BottomSheetDialog(this, R.style.BottomSheetDialogStyle)
    }

    lateinit var dialogBottomSheetRvAdapter: BaseRvAdapterPosition<Song>

    /**
     * 播放列表
     */
    private fun clickBottomSheetDialog() {
        binding.musicList.setOnClickListener(myOnMultiClickListener {
            showToast(this, "1")
            if (mainViewModel.musicPlaySongList.value?.size != 0) {
                val dialogPlayListLayoutBinding =
                    DialogPlayListLayoutBinding.inflate(layoutInflater)
                bottomSheetDialog.setContentView(dialogPlayListLayoutBinding.root)
                bottomSheetDialog.behavior.isHideable = false
                dialogPlayListLayoutBinding.close.setOnClickListener(myOnMultiClickListener {
                    bottomSheetDialog.hide()
                })
                val linearLayoutManager = LinearLayoutManager(this)
                dialogPlayListLayoutBinding.dialogPlayListRv.layoutManager = linearLayoutManager
                val songMutableList = mainViewModel.musicPlaySongList.value
                dialogBottomSheetRvAdapter = BaseRvAdapterPosition(
                    songMutableList!!.toList(), R.layout.item_play_list_layout
                ) { itemData, view, position, holderPosition ->
                    val itemPlayListLayoutBinding = ItemPlayListLayoutBinding.bind(view)
                    itemPlayListLayoutBinding.title.text = itemData.name
                    itemPlayListLayoutBinding.name.text = itemData.artist

                    if (index == position) {
                        mainViewModel.sharedPreferencesEditCommitData {
                            putString(SELECT_SONG_PATH, itemData.data)
                        }
                        itemPlayListLayoutBinding.itemClose.visibility = View.GONE
                        itemPlayListLayoutBinding.title.setTextColor(resources.getColor(R.color.purple_200))
                        itemPlayListLayoutBinding.name.setTextColor(resources.getColor(R.color.purple_200))
                    } else {
                        itemPlayListLayoutBinding.itemClose.visibility = View.VISIBLE
                        itemPlayListLayoutBinding.title.setTextColor(resources.getColor(R.color.black))
                        itemPlayListLayoutBinding.name.setTextColor(resources.getColor(R.color.text_grey))
                    }

                    itemPlayListLayoutBinding.root.setOnClickListener(myOnMultiClickListener {
                        if (fileExists(itemData.data)) {
                            if (index != position) {
                                isClickOnTheNextSong = true
                                if (!isNotFirstEntry) isNotFirstEntry = true
                                index = position
                                playListDialog.onPlayListener(itemData.data)
                            }
                        } else {
                            showToast(this@MainActivity, "找不到此文件,可能文件已经被迁移或被删除")
                        }
                    })
                    itemPlayListLayoutBinding.itemClose.setOnClickListener(myOnMultiClickListener {
                        val selectIndexMusicPlay = mainViewModel.selectIndexMusicPlay(itemData.data)
                        val value = mainViewModel.musicPlaySongList.value
                        value!!.removeAt(selectIndexMusicPlay)
                        mainViewModel.sharedPreferencesEditCommitData {
                            putString(
                                SONG_PLAY_LIST_STRING, gson.toJson(GsonSongBean(value))
                            )
                        }
                        mainViewModel.audioBinder.removeMediaItem(selectIndexMusicPlay)
                        if (position < index) {
                            index -= 1
                        }
                        dataList = value
                    })
                }
                dialogPlayListLayoutBinding.dialogPlayListRv.adapter = dialogBottomSheetRvAdapter
                val selectSongPath = mainViewModel.getSelectSongPath()
                val selectIndex = mainViewModel.selectIndexMusicPlay(selectSongPath)
                linearLayoutManager.scrollToPositionWithOffset(selectIndex, 100.px.toInt())
                dialogBottomSheetRvAdapter.index = selectIndex
                bottomSheetDialog.show()
            }
        })
    }


    /**
     * TODO 横竖屏切换的时候使用的方法
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)


    }

    /**
     * 基础配置(播放、上一首、下一首等)
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun basicConfiguration() {
        mainViewModel.musicPlaySongList.observe(this) {
            val mediaItems = mutableListOf<MediaItem>()
            it.forEach {
                mediaItems += MediaItem.fromUri(it.data)
            }
            mainViewModel.audioBinder.setMediaItems(mediaItems)
        }

        // 启动Handler
        progressHandler.sendEmptyMessageDelayed(1, 500)
        /**
         * 选中歌触发
         */
        mainViewModel.selectSongBean.observe(this) {
            if (!isNotFirstEntry) {
                setTitleAuthorText(it)
            }
            //判断启动什么动画效果
            if (motionLayoutIsExpand) {
                if (isTheNextSongClick) {
                    startTextExpandNextAnimator(binding.musicTitleExpand) {
                        setTitleAuthorText(it)
                    }
                    startTextExpandNextAnimator(binding.musicAuthorExpand)
                    startTextExpandNextAnimator(binding.musicMenu)
                } else {
                    startTextPreviousExpandAnimator(binding.musicTitleExpand) {
                        setTitleAuthorText(it)
                    }
                    startTextPreviousExpandAnimator(binding.musicAuthorExpand)
                    startTextPreviousExpandAnimator(binding.musicMenu)
                }
            } else {
                startTextAnimator(binding.musicTitle) {
                    setTitleAuthorText(it)
                }
                startTextAnimator(binding.musicAuthor)
            }

            setLrc(it)
            //将时长进行初始化
            binding.musicSeekbarStartTime.text = "00:00"
            binding.musicSeekbarEndTime.text = convertComponentSeconds(it.song.duration)
            binding.musicSeekbar.max = it.song.duration
            binding.musicSeekbar.progress = 0

            if (fileExists(it.song.data)) {
                if (!mainViewModel.isOnClickRefresh) {
                    // 争夺焦点
                    audioManager.requestAudioFocus(focusRequest)
                    //进行播放歌曲
                    if (isNotFirstEntry && isClickOnTheNextSong) {
                        mainViewModel.audioBinder.seekIndex(
                            mainViewModel.selectIndexMusicPlay(
                                it.song.data
                            )
                        )
                        isTheStateSuspended()
                    }
                }
            } else {
                showToast(this, "找不到此文件,可能文件已经被迁移或被删除")
            }
            mainViewModel.isOnClickRefresh = false
        }

        mainViewModel.selectBitmap.observe(this) {
            binding.musicPhotos.setImageBitmap(it)
        }

        binding.materialCardPlayView.setOnClickListener(myOnMultiClickListener {
            audioManager.requestAudioFocus(focusRequest)
            mediaPlayerPause()
        })

        binding.musicPlayPause.setOnClickListener(myOnMultiClickListener {
            audioManager.requestAudioFocus(focusRequest)
            mediaPlayerPause()
        })

        binding.musicLyrics.setOnClickListener(myOnMultiClickListener {
            binding.motionLayout.visibility = View.INVISIBLE
            binding.fragment.visibility = View.VISIBLE
            startAlphaEnterAnimator(binding.fragment)
            startAlphaOutAnimator(binding.motionLayout)
            startLrcFragment()
        })

        binding.musicSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                binding.musicSeekbarStartTime.text = convertComponentSeconds(p1)
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {

            }

            override fun onStopTrackingTouch(p0: SeekBar) {
                mainViewModel.audioBinder.seekTo(p0.progress.toLong())
                binding.musicSeekbarStartTime.text =
                    convertComponentSeconds(mainViewModel.audioBinder.getCurrentPosition().toInt())
                isTheStateSuspended()
            }
        })
    }

    /**
     * 设置歌词
     */
    private fun setLrc(it: SelectSongBean) {
        CoroutineScope(Dispatchers.IO).launch {
            mainViewModel.lrcBeanList.clear()
            val replace = it.song.data.replace(".mp3", ".lrc")
            if (fileExists(replace)) {
                val parseLrcFile = parseLrcFile(replace)
                mainViewModel.lrcBeanList = parseStr2List(parseLrcFile)
                withContext(Dispatchers.Main) {
                    if (mainViewModel.lrcBeanList.size != 0) {
                        binding.musicLyrics.text = mainViewModel.lrcBeanList[0].lrc
                        withContext(Dispatchers.Main) {
                            if (::mainLrcEndOfSong.isInitialized && binding.fragment.visibility == View.VISIBLE) {
                                mainLrcEndOfSong.onEndOfSongPlayListener()
                            }
                        }
                    } else {
                        binding.musicLyrics.text = "暂无歌词"
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    binding.musicLyrics.text = "暂无歌词"
                }
            }
        }
    }

    /**
     * 在Fragment中启动音乐
     */
    fun playMusic() {
        binding.musicPlayPause.tag = "play"
        binding.musicPlayPause.setImageResource(R.drawable.icon_play)
        mainViewModel.audioBinder.playerMedia()
        progressHandler.sendEmptyMessageDelayed(1, 500)
    }

    private fun setTitleAuthorText(it: SelectSongBean) {
        binding.musicTitle.text = it.song.name
        binding.musicTitleExpand.text = it.song.name
        binding.musicAuthor.text = it.song.artist
        binding.musicAuthorExpand.text = it.song.artist
    }

    fun setSeekToNextOnClickListener(action: (selectPosition: Int) -> Unit) {
        binding.musicSkipNext.setOnClickListener(myOnMultiClickListener {
            if (!isNotFirstEntry) isNotFirstEntry = true
            isTheNextSongClick = true
            val selectSongPath = mainViewModel.getSelectSongPath()
            val selectIndex = mainViewModel.selectIndexMusicPlay(selectSongPath)
            val songMutableList = mainViewModel.musicPlaySongList.value
            val data = if (selectIndex + 1 == songMutableList!!.size) {
                songMutableList[0].data
            } else {
                songMutableList[selectIndex + 1].data
            }
            action.invoke(mainViewModel.selectIndex(data))
            mainViewModel.audioBinder.seekToNext()
        })
    }

    fun setSeekToPreviousOnClickListener(action: (selectPosition: Int) -> Unit) {
        binding.musicSkipPrevious.setOnClickListener(myOnMultiClickListener {
            if (!isNotFirstEntry) isNotFirstEntry = true
            isTheNextSongClick = false
            val selectSongPath = mainViewModel.getSelectSongPath()
            val selectIndex = mainViewModel.selectIndexMusicPlay(selectSongPath)
            val songMutableList = mainViewModel.musicPlaySongList.value
            val data = if (selectIndex - 1 < 0) {
                songMutableList!![songMutableList.size - 1].data
            } else {
                songMutableList!![selectIndex - 1].data
            }
            action.invoke(mainViewModel.selectIndex(data))
            mainViewModel.audioBinder.seekToPrevious()
        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun isTheStateSuspended() {
        if (!mainViewModel.audioBinder.mediaIsPlaying()) {
            if (binding.musicPlayPause.tag == "pause") {
                mediaPlayerPause()
            } else {
                mainViewModel.audioBinder.playerMedia()
            }
        }
    }

    fun setEndOfSongListener(callback: (currentPosition: Int) -> Unit) {
        mainViewModel.audioBinder.setEndOfSong(object : ExoPlayerManager.EndOfSongFan {
            override fun onEndOfSongPlayListener(currentPosition: Int) {
                isClickOnTheNextSong = false
                isTheNextSongClick = true
                val songMutableList = mainViewModel.musicPlaySongList.value
                callback.invoke(mainViewModel.selectIndex(songMutableList?.get(currentPosition)?.data))
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun mediaPlayerPause() {
        if (!isNotFirstEntry) isNotFirstEntry = true
        if (mainViewModel.audioBinder.mediaIsPlaying()) {
            binding.musicPlayPause.tag = "pause"
            binding.musicPlayPause.setImageResource(R.drawable.icon_play_anim)
            mainViewModel.audioBinder.stopMediaPlayer()
        } else {
            binding.musicPlayPause.tag = "play"
            binding.musicPlayPause.setImageResource(R.drawable.icon_pause_anim)
            mainViewModel.audioBinder.playerMedia()
        }
        ((binding.musicPlayPause.drawable) as Animatable).start()
    }

    /**
     * 绑定Service
     */
    private fun bindExoPlayerService() {
        val connection = object : ServiceConnection {
            override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
                if (p1 != null) {
                    mainViewModel.audioBinder = p1 as ExoPlayerService.AudioBinder
                    startHomeFragment()
                    startSetting()
                    motionLayoutListener()
                    clickBottomSheetDialog()
                    basicConfiguration()
                }
            }

            override fun onServiceDisconnected(p0: ComponentName?) {

            }
        }
        bindService(Intent(this, ExoPlayerService::class.java), connection, BIND_AUTO_CREATE)
    }

    /**
     * 获取数据
     */
    private fun requestPermission() {
        if (ContextCompat.checkSelfPermission(
                this, permissions[0]
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsDialog = AlertDialog.Builder(this).create()
            val dialogView = DialogPermissionsLayoutBinding.inflate(layoutInflater)
            permissionsDialog.setCancelable(false)
            permissionsDialog.setView(dialogView.root)
            permissionsDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            permissionsDialog.window!!.decorView.setBackgroundColor(Color.TRANSPARENT)
            permissionsDialog.show()
            dialogView.dialogButton.setOnClickListener(myOnMultiClickListener {
                registerForActivityResult.launch(permissions)
            })
        } else {
            requestData()
        }
    }

    /**
     * 跳转Home
     */
    private fun startHomeFragment() {
        replaceFragment(binding.contentFragment.id, HomeFragment())
    }

    /**
     * 跳转歌词详情
     */
    private fun startLrcFragment() {
        replaceFragment(binding.fragment.id, LrcFragment())
    }

    /**
     * 跳转媒体库
     */
    fun startPLayListFragment() {
        addFragment(binding.contentFragment.id, PlayListFragment())
    }

    /**
     * 跳转媒体库
     */
    fun startArtistFragment() {
        addFragment(binding.contentFragment.id, ArtistFragment())
    }

    /**
     * 跳转歌单
     */
    fun startTheAlbumFragment() {
        addFragment(binding.contentFragment.id, TheAlbumFragment())
    }

    /**
     * 点击进入播放器设置页面
     */
    private fun startSetting() {
        binding.musicMenu.setOnClickListener(myOnMultiClickListener {
            if (motionLayoutIsExpand) {
                startA(SettingActivity::class.java)
                overridePendingTransition(
                    R.anim.slide_in_bottom, R.anim.slide_out_top
                )
            }
        })
    }

    /**
     * MotionLayout监听器
     */
    private fun motionLayoutListener() {
        binding.motionLayout.setTransitionListener(object : MotionLayout.TransitionListener {
            override fun onTransitionStarted(p0: MotionLayout?, p1: Int, p2: Int) {

            }

            override fun onTransitionChange(p0: MotionLayout?, p1: Int, p2: Int, p3: Float) {

            }

            override fun onTransitionCompleted(p0: MotionLayout?, p1: Int) {
                motionLayoutIsExpand = !binding.materialCardPlayView.isVisible
            }

            override fun onTransitionTrigger(p0: MotionLayout?, p1: Int, p2: Boolean, p3: Float) {

            }
        })
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (binding.motionLayout.visibility == View.INVISIBLE) {
            binding.motionLayout.visibility = View.VISIBLE
            binding.fragment.visibility = View.GONE
            startAlphaEnterAnimator(binding.motionLayout)
            startAlphaOutAnimator(binding.fragment)
        } else if (motionLayoutIsExpand) {
            motionLayoutIsExpand = false
            binding.motionLayout.transitionToStart()
        } else if (mainViewModel.isOtherPages.value == true) {
            supportFragmentManager.popBackStack()
            mainViewModel.isOtherPages.value = false
        } else {
            val currentTimeMillis = System.currentTimeMillis()
            if (currentTimeMillis - lastOutLayoutTime >= MIN_OUT_LAYOUT_TIME) {
                lastOutLayoutTime = System.currentTimeMillis()
                showToast(this, "再按一次返回桌面")
            } else {
                super.onBackPressed()
            }
        }
    }

    /**
     * 添加歌单对话框
     */
    fun showDialogAddSongList(
        isEdit: Boolean = false,
        name: String = "",
        index: Int = 0,
        _action: (content:String) -> Unit
    ) {
        val dialog = AlertDialog.Builder(this).create()
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window!!.decorView.setBackgroundColor(Color.TRANSPARENT)
        val dialogAddSongListLayoutBinding =
            DialogAddSongListLayoutBinding.inflate(layoutInflater)
        dialog.setView(dialogAddSongListLayoutBinding.root)
        dialogAddSongListLayoutBinding.editContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun afterTextChanged(p0: Editable?) {
                dialogAddSongListLayoutBinding.dialogDoneButton.isEnabled =
                    dialogAddSongListLayoutBinding.editContent.text.isNotEmpty()
            }
        })
        if (isEdit) {
            dialogAddSongListLayoutBinding.editContent.setText(name)
            dialogAddSongListLayoutBinding.title.text = "编辑"
        }

        dialogAddSongListLayoutBinding.dialogCloseButton.setOnClickListener(
            myOnMultiClickListener {
                dialog.dismiss()
            })

        dialogAddSongListLayoutBinding.dialogDoneButton.setOnClickListener(
            myOnMultiClickListener {
                if (isEdit) {
                    val fromJson = gson.fromJson(
                        mainViewModel.getAllSongPlayListString(),
                        AllGsonSongBean::class.java
                    )
                    fromJson.allGsonSongBeanList[index].name =
                        dialogAddSongListLayoutBinding.editContent.text.toString()
                    mainViewModel.sharedPreferencesEditCommitData {
                        putString(
                            ALL_SONG_PLAY_LIST_STRING,
                            gson.toJson(fromJson)
                        )
                    }
                } else {
                    val gsonSongBean = GsonSongBean(
                        mutableListOf(),
                        dialogAddSongListLayoutBinding.editContent.text.toString()
                    )
                    val allSongPlayListString = mainViewModel.getAllSongPlayListString()
                    if (allSongPlayListString.isEmpty()) {
                        mainViewModel.sharedPreferencesEditCommitData {
                            putString(
                                ALL_SONG_PLAY_LIST_STRING,
                                gson.toJson(AllGsonSongBean(mutableListOf(gsonSongBean)))
                            )
                        }

                    } else {
                        val fromJson = gson.fromJson(
                            mainViewModel.getAllSongPlayListString(),
                            AllGsonSongBean::class.java
                        )
                        fromJson.allGsonSongBeanList.add(gsonSongBean)
                        mainViewModel.sharedPreferencesEditCommitData {
                            putString(
                                ALL_SONG_PLAY_LIST_STRING,
                                gson.toJson(fromJson)
                            )
                        }
                    }
                }
                _action.invoke(dialogAddSongListLayoutBinding.editContent.text.toString())
                dialog.dismiss()
            })
        dialog.show()
    }

    /**
     * 列表Adapter
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun setMusicRv(
        contentRv: RecyclerView,
        mediaText: TextView,
        _action: (BaseRvAdapter<Song>) -> Unit
    ) {
        // 取消过渡动画
        (contentRv.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        contentRv.itemAnimator = null
        mainViewModel.musicSongList.observe(this) { songList ->
            mediaText.text = "${songList.size} 首歌曲"
            val songBaseRvAdapter =
                BaseRvAdapter(songList, R.layout.item_song_layout) { itemData, view, position ->
                    val itemSongLayoutBinding = ItemSongLayoutBinding.bind(view)
                    itemSongLayoutBinding.musicTitle.text = itemData.name
                    itemSongLayoutBinding.musicAuthor.text = itemData.artist
                    //设置Bitmap
                    setImageBitmap(itemData, itemSongLayoutBinding, position)
                    //设置选中的背景颜色
                    if (index == position) {
                        val typedValue = TypedValue()
                        theme.resolveAttribute(
                            androidx.appcompat.R.attr.colorAccent, typedValue, true
                        )
                        itemSongLayoutBinding.musicCard.setCardBackgroundColor(typedValue.data)
                    } else {
                        itemSongLayoutBinding.musicCard.setCardBackgroundColor(getColor(R.color.background_color))
                    }

                    //Item点击事件
                    itemSongLayoutBinding.root.setOnClickListener(myOnMultiClickListener {
                        if (fileExists(itemData.data)) {
                            if (index != position) {
                                // 判断当前选中的歌曲是否在播放列表中，存在则跳过，不存在则添加
                                if ((mainViewModel.musicPlaySongList.value?.contains(
                                        itemData
                                    ) == false)
                                ) {
                                    val currentPosition =
                                        mainViewModel.audioBinder.getCurrentMediaItemIndex()
                                    if (currentPosition + 1 == mainViewModel.musicPlaySongList.value!!.size) {
                                        mainViewModel.audioBinder.addMediaItem(
                                            MediaItem.fromUri(
                                                itemData.data
                                            )
                                        )
                                        mainViewModel.musicPlaySongList.value!!.add(
                                            itemData
                                        )
                                    } else {
                                        mainViewModel.audioBinder.addMediaItem(
                                            MediaItem.fromUri(
                                                itemData.data
                                            ), currentPosition + 1
                                        )
                                        mainViewModel.musicPlaySongList.value!!.add(
                                            currentPosition + 1, itemData
                                        )
                                    }
                                    mainViewModel.sharedPreferencesEditCommitData {
                                        putString(
                                            SONG_PLAY_LIST_STRING,
                                            gson.toJson(GsonSongBean(mainViewModel.musicPlaySongList.value!!))
                                        )
                                    }
                                }
                                isClickOnTheNextSong = true
                                index = position
                                // 开启动画效果
                                if (!isNotFirstEntry) isNotFirstEntry = true
                            }
                        } else {
                            showToast(this@MainActivity, "找不到此文件,可能文件已经被迁移或被删除")
                        }
                    })
                }
            _action.invoke(songBaseRvAdapter)
            val selectSongPath = mainViewModel.getSelectSongPath()
            songBaseRvAdapter.index = mainViewModel.selectIndex(selectSongPath)
            mainViewModel.audioBinder.seekIndexNotPlayer(
                mainViewModel.selectIndexMusicPlay(
                    selectSongPath
                )
            )
            val linearLayoutManager = LinearLayoutManager(this)
            contentRv.layoutManager = linearLayoutManager
            contentRv.adapter = songBaseRvAdapter
            setEndOfSongListener {
                isTheNextSongClick = true
                songBaseRvAdapter.index = it
            }
            setPlayListDialogF { path ->
                songBaseRvAdapter.index = mainViewModel.selectIndex(path)
            }

            setSeekToNextOnClickListener { selectPosition ->
                songBaseRvAdapter.index = selectPosition
            }

            setSeekToPreviousOnClickListener { selectPosition ->
                songBaseRvAdapter.index = selectPosition
            }
        }
    }

    val defaultAvatar by lazy {
        resources.getDrawable(R.drawable.default_avatar).toBitmap()
    }

    /**
     * 设置Bitmap
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun BaseRvAdapter<Song>.setImageBitmap(
        oneitemData: Song, itemSongLayoutBinding: ItemSongLayoutBinding, position: Int
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            flow {
                val bitmap = getAlbumPicture(oneitemData.data)
                emit(bitmap)
            }.catch {
                emit(defaultAvatar)
            }
                .collect {
                    withContext(Dispatchers.Main) {
                        itemSongLayoutBinding.musicPhotos.setImageBitmap(it)
                        if (index == position) {
                            mainViewModel.setSelectBitmap(it ?: defaultAvatar)
                        }
                        itemSongLayoutBinding.itemMusicMenu.setOnClickListener(
                            myOnMultiClickListener {
                                val dialog = AlertDialog.Builder(this@MainActivity).create()
                                val dialogView = DialogItemMenuLayoutBinding.inflate(layoutInflater)
                                dialog.setView(dialogView.root)
                                dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                                dialog.window!!.decorView.setBackgroundColor(Color.TRANSPARENT)
                                dialog.show()

                                dialogView.musicTitle.text = oneitemData.name
                                dialogView.musicAuthor.text = oneitemData.artist
                                dialogView.musicPhotos.setImageBitmap(it)

                                dialogView.musicAddPlaySong.setOnClickListener(
                                    myOnMultiClickListener {
                                        val addSongListDialog =
                                            AlertDialog.Builder(this@MainActivity).create()
                                        val dialogAddSongToListLayoutBinding =
                                            DialogAddSongToListLayoutBinding.inflate(layoutInflater)
                                        addSongListDialog.window!!.setBackgroundDrawable(
                                            ColorDrawable(Color.TRANSPARENT)
                                        )
                                        addSongListDialog.window!!.decorView.setBackgroundColor(
                                            Color.TRANSPARENT
                                        )
                                        val allGsonSongBean = gson.fromJson(
                                            mainViewModel.getAllSongPlayListString(),
                                            AllGsonSongBean::class.java
                                        )

                                        if (allGsonSongBean != null) {
                                            val baseAdapter = BaseRvAdapter(
                                                allGsonSongBean.allGsonSongBeanList.reversed(),
                                                R.layout.item_song_to_list_layout
                                            ) { itemData, view, position ->
                                                val itemSongListLayoutBinding =
                                                    ItemSongToListLayoutBinding.bind(view)
                                                CoroutineScope(Dispatchers.IO).launch {
                                                    val bitmap = if (itemData.musicList.size == 0) {
                                                        defaultAvatar
                                                    } else {
                                                        getAlbumPicture(itemData.musicList[0].data)
                                                            ?: defaultAvatar
                                                    }
                                                    withContext(Dispatchers.Main) {
                                                        itemSongListLayoutBinding.musicPhotos.setImageBitmap(
                                                            bitmap
                                                        )
                                                    }
                                                }
                                                itemSongListLayoutBinding.musicTitle.text =
                                                    itemData.name
                                                itemSongListLayoutBinding.musicAuthor.text =
                                                    "${itemData.musicList.size} 首歌曲"
                                                itemSongListLayoutBinding.root.setOnClickListener(
                                                    myOnMultiClickListener {
                                                        val fromJson = gson.fromJson(
                                                            mainViewModel.getAllSongPlayListString(),
                                                            AllGsonSongBean::class.java
                                                        )

                                                        val musicList =
                                                            fromJson.allGsonSongBeanList[position].musicList
                                                        if(!musicList.contains(oneitemData)){
                                                            fromJson.allGsonSongBeanList[position].musicList.add(
                                                                oneitemData
                                                            )
                                                            mainViewModel.sharedPreferencesEditCommitData {
                                                                putString(
                                                                    ALL_SONG_PLAY_LIST_STRING,
                                                                    gson.toJson(fromJson)
                                                                )
                                                            }
                                                        }
                                                        showToast(this@MainActivity, "已添加到歌单")
                                                        addSongListDialog.dismiss()
                                                        dialog.dismiss()
                                                    })
                                            }

                                            dialogAddSongToListLayoutBinding.rv.adapter =
                                                baseAdapter

                                        }
                                        dialogAddSongToListLayoutBinding.dialogButton.setOnClickListener(
                                            myOnMultiClickListener {
                                                showDialogAddSongList {
                                                    if (allGsonSongBean != null) {
                                                        (dialogAddSongToListLayoutBinding.rv.adapter as BaseRvAdapter<GsonSongBean>).dataList =
                                                            gson.fromJson(
                                                                mainViewModel.getAllSongPlayListString(),
                                                                AllGsonSongBean::class.java
                                                            ).allGsonSongBeanList
                                                    }
                                                }
                                            })
                                        addSongListDialog.setView(dialogAddSongToListLayoutBinding.root)
                                        addSongListDialog.show()
                                    })

                                dialogView.musicNextSong.setOnClickListener(myOnMultiClickListener {
                                    val currentPosition =
                                        mainViewModel.audioBinder.getCurrentMediaItemIndex()
                                    if ((mainViewModel.musicPlaySongList.value?.contains(
                                            oneitemData
                                        ) == false)
                                    ) {
                                        // 不存在
                                        if (currentPosition + 1 == mainViewModel.musicPlaySongList.value!!.size) {
                                            mainViewModel.audioBinder.addMediaItem(
                                                MediaItem.fromUri(
                                                    oneitemData.data
                                                )
                                            )
                                            mainViewModel.musicPlaySongList.value!!.add(
                                                oneitemData
                                            )
                                        } else {
                                            mainViewModel.audioBinder.addMediaItem(
                                                MediaItem.fromUri(
                                                    oneitemData.data
                                                ), currentPosition + 1
                                            )
                                            mainViewModel.musicPlaySongList.value!!.add(
                                                currentPosition + 1, oneitemData
                                            )
                                        }
                                    } else {
                                        //存在
                                        if (currentPosition + 1 == mainViewModel.musicPlaySongList.value!!.size) {
                                            mainViewModel.audioBinder.moveMediaItem(
                                                mainViewModel.selectIndexMusicPlay(oneitemData.data),
                                                mainViewModel.musicPlaySongList.value!!.size
                                            )
                                            mainViewModel.musicPlaySongList.value!!.remove(
                                                oneitemData
                                            )
                                            mainViewModel.musicPlaySongList.value!!.add(oneitemData)
                                        } else {
                                            mainViewModel.audioBinder.moveMediaItem(
                                                mainViewModel.selectIndexMusicPlay(oneitemData.data),
                                                currentPosition
                                            )
                                            mainViewModel.musicPlaySongList.value!!.remove(
                                                oneitemData
                                            )
                                            mainViewModel.musicPlaySongList.value!!.add(
                                                currentPosition,
                                                oneitemData
                                            )
                                        }
                                    }
                                    mainViewModel.sharedPreferencesEditCommitData {
                                        putString(
                                            SONG_PLAY_LIST_STRING,
                                            gson.toJson(GsonSongBean(mainViewModel.musicPlaySongList.value!!))
                                        )
                                    }
                                    dialog.dismiss()
                                })
                            })
                    }
                }
        }

        if (index == position) {
            mainViewModel.sharedPreferencesEditCommitData {
                putString(SELECT_SONG_PATH, oneitemData.data)
            }
            selectedEvents(position, oneitemData)
        }
    }

    /**
     * 选中事件
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun BaseRvAdapter<Song>.selectedEvents(
        position: Int, itemData: Song
    ) {
        if (index == position) {
            mainViewModel.setSelectSong(
                SelectSongBean(
                    itemData, position
                )
            )
        }
    }
}
package com.example.dullmusic.ui.activity.main

import android.Manifest
import android.content.*
import android.graphics.Color
import android.graphics.drawable.Animatable
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.*
import android.view.View
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.example.base.base.BaseActivity
import com.example.base.utils.*
import com.example.dullmusic.R
import com.example.dullmusic.bean.GsonSongBean
import com.example.dullmusic.bean.SelectSongBean
import com.example.dullmusic.databinding.ActivityMainBinding
import com.example.dullmusic.lrc.LrcBean
import com.example.dullmusic.lrc.parseLrcFile
import com.example.dullmusic.lrc.parseStr2List
import com.example.dullmusic.ui.activity.SettingActivity
import com.example.dullmusic.ui.fragment.HomeFragment
import com.example.dullmusic.ui.fragment.LrcFragment
import com.example.dullmusic.ui.fragment.MediaLibraryFragment
import com.example.dullmusic.ui.fragment.SongListFragment
import com.example.media.ExoPlayerManager
import com.example.media.ExoPlayerService
import com.google.android.exoplayer2.MediaItem
import kotlinx.coroutines.*


const val MIN_OUT_LAYOUT_TIME = 1000
var lastOutLayoutTime = 0L

@RequiresApi(Build.VERSION_CODES.O)
open class MainActivity : BaseActivity() {
    lateinit var binding: ActivityMainBinding

    val mainViewModel by lazy {
        ViewModelProvider(
            this, ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[MainViewModel::class.java]
    }

    /**
     * 是否展开媒体
     */
    private var motionLayoutIsExpand = false

    /**
     * 是否开启其他Fragment
     */
    private var isOtherPages = false

    /**
     * 是否手动点击切换音乐，防止二次调用audioBinder.seekIndex(it.selectPosition)
     */
    var isClickOnTheNextSong = true

    /**
     *  判断展开播放器点击的是下一首还是上一首，分别开启不同的动画效果
     */
    var isTheNextSongClick = true

    val sharedPreferences: SharedPreferences by lazy {
        getSharedPreferences("data", MODE_PRIVATE)
    }
    val sharedPreferencesEdit: SharedPreferences.Editor by lazy {
        sharedPreferences.edit()
    }


    /**
     * 权限申请
     */
    val registerForActivityResult =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (it[Manifest.permission.READ_EXTERNAL_STORAGE]!! && it[Manifest.permission.WRITE_EXTERNAL_STORAGE]!!) {
                CoroutineScope(Dispatchers.IO).launch {
                    mainViewModel.requestMusicSong(
                        sharedPreferences.getString(
                            "musicListString", ""
                        ) ?: ""
                    ) { musicList ->
                        sharedPreferencesEdit.putString(
                            "musicListString", gson.toJson(GsonSongBean(musicList))
                        )
                        sharedPreferencesEdit.commit()
                    }
                }
            } else {
                showToast(this, "请同意权限,要不然无法运行程序")
            }
        }

    private val permissions = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    lateinit var audioBinder: ExoPlayerService.AudioBinder

    /**
     * 歌曲进度监听Handler
     */
    private val progressHandler: Handler = object : Handler(Looper.myLooper()!!) {
        override fun handleMessage(msg: Message) {
            if (::audioBinder.isInitialized && binding.musicPlayPause.tag == "play") {
                val currentPosition = audioBinder.getCurrentPosition().toInt()
                binding.musicSeekbar.progress = audioBinder.getCurrentPosition().toInt()
                binding.musicSeekbarStartTime.text = convertComponentSeconds(currentPosition)
                lrcSet(currentPosition)
                if(::mainLrcHandler.isInitialized && binding.fragment.visibility == View.VISIBLE){
                    mainLrcHandler.onMainHandlerListener()
                }
            }
            sendEmptyMessageDelayed(1, 500)
        }
    }

    /**
     * 切换歌曲时通知歌词切换数据
     */
    interface MainLrcEndOfSong {
        fun onEndOfSongPlayListener()
    }

    /**
     * 歌词详情Handler监听
     */
    interface MainLrcHandler {
        fun onMainHandlerListener()
    }
    lateinit var mainLrcEndOfSong: MainLrcEndOfSong
    lateinit var mainLrcHandler: MainLrcHandler
    fun setMainLrcHandlerF(_action: MainLrcHandler) {
        mainLrcHandler = _action
    }
    fun setMainLrcEndOfSongF(_action: MainLrcEndOfSong) {
        mainLrcEndOfSong = _action
    }

    private fun lrcSet(currentPosition: Int) {
        if (lrcBeanList.size != 0) {
            lrcBeanList.forEach {
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

    var lrcBeanList: MutableList<LrcBean> = mutableListOf()

    val focusRequest by lazy {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
            setAudioAttributes(AudioAttributes.Builder().run {
                setUsage(AudioAttributes.USAGE_GAME)
                setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                build()
            })
            setAcceptsDelayedFocusGain(true)
            setOnAudioFocusChangeListener { focusChange ->
                audioBinder.stopMediaPlayerNoJudgment()
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
        window.statusBarColor = Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)
        startHomeFragment()
        bindExoPlayerService()
        requestData()
        basicConfiguration()
        startSetting()
        motionLayoutListener()
    }

    /**
     * 基础配置(播放、上一首、下一首等)
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun basicConfiguration() {
        // 启动Handler
        progressHandler.sendEmptyMessageDelayed(1, 500)
        mainViewModel.musicSongList.observe(this) {
            val mediaItems = mutableListOf<MediaItem>()
            it.forEach {
                mediaItems += MediaItem.fromUri(it.data)
            }
            audioBinder.setMediaItems(mediaItems)
        }

        /**
         * 选中歌词触发
         */
        mainViewModel.selectSongBean.observe(this) {
            if (!isNotFirstEntry) setTitleAuthorText(it)
            // 争夺焦点
            audioManager.requestAudioFocus(focusRequest)
            binding.musicPhotos.setImageBitmap(mainViewModel.selectBitmap.value)
            //处理歌词
            CoroutineScope(Dispatchers.IO).launch {
                lrcBeanList.clear()
                val replace = it.song.data.replace(".mp3", ".lrc")
                if (fileExists(replace)) {
                    val parseLrcFile = parseLrcFile(replace)
                    lrcBeanList = parseStr2List(parseLrcFile)
                    if (lrcBeanList.size != 0) {
                        MainScope().launch {
                            binding.musicLyrics.text = lrcBeanList[0].lrc
                        }
                    } else {
                        binding.musicLyrics.text = "暂无歌词"
                    }
                } else {
                    binding.musicLyrics.text = "暂无歌词"
                }

                MainScope().launch {
                    if(::mainLrcEndOfSong.isInitialized && binding.fragment.visibility == View.VISIBLE){
                        mainLrcEndOfSong.onEndOfSongPlayListener()
                    }
                }
            }


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
            binding.musicSeekbarStartTime.text = "00:00"
            binding.musicSeekbarEndTime.text = convertComponentSeconds(it.song.duration)
            binding.musicSeekbar.max = it.song.duration
            binding.musicSeekbar.progress = 0

            if (::audioBinder.isInitialized && isClickOnTheNextSong) {
                if (isNotFirstEntry) {
                    audioBinder.seekIndex(it.selectPosition)
                    isTheStateSuspended()
                }
            }
        }
        mainViewModel.selectBitmap.observe(this) {
            binding.musicPhotos.setImageBitmap(it)
        }


        binding.materialCardPlayView.setOnClickListener {
            audioManager.requestAudioFocus(focusRequest)
            mediaPlayerPause()
        }

        binding.musicPlayPause.setOnClickListener(myOnMultiClickListener {
            audioManager.requestAudioFocus(focusRequest)
            mediaPlayerPause()
        })

        binding.musicLyrics.setOnClickListener(myOnMultiClickListener {
            startLrcFragment()
            binding.motionLayout.visibility = View.INVISIBLE
            binding.fragment.visibility = View.VISIBLE
            startAlphaEnterAnimator(binding.fragment)
            startAlphaOutAnimator(binding.motionLayout)
        })

        binding.musicSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                binding.musicSeekbarStartTime.text = convertComponentSeconds(p1)
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {

            }

            override fun onStopTrackingTouch(p0: SeekBar) {
                audioBinder.seekTo(p0.progress.toLong())
                binding.musicSeekbarStartTime.text =
                    convertComponentSeconds(audioBinder.getCurrentPosition().toInt())
                isTheStateSuspended()
            }
        })
    }

    /**
     * 在Fragment中启动音乐
     */
    fun playMusic(){
        binding.musicPlayPause.tag = "play"
        binding.musicPlayPause.setImageResource(R.drawable.icon_pause_anim)
        audioBinder.playerMedia()
        progressHandler.sendEmptyMessageDelayed(1, 500)
    }

    private fun setTitleAuthorText(it: SelectSongBean) {
        binding.musicTitle.text = it.song.name
        binding.musicTitleExpand.text = it.song.name
        binding.musicAuthor.text = it.song.artist
        binding.musicAuthorExpand.text = it.song.artist
    }

    fun setSeekToNextOnClickListener(action: (selectPosition: Int, musicSongListMaxSize: Int) -> Unit) {
        binding.musicSkipNext.setOnClickListener {
            if (!isNotFirstEntry) isNotFirstEntry = true
            isTheNextSongClick = true
            action.invoke(
                mainViewModel.selectSongBean.value?.selectPosition ?: 0,
                mainViewModel.musicSongList.value?.size ?: 0
            )
            audioBinder.seekToNext()
        }
    }

    fun setSeekToPreviousOnClickListener(action: (selectPosition: Int, musicSongListMaxSize: Int) -> Unit) {
        binding.musicSkipPrevious.setOnClickListener {
            if (!isNotFirstEntry) isNotFirstEntry = true
            isTheNextSongClick = false
            action.invoke(
                mainViewModel.selectSongBean.value?.selectPosition ?: 0,
                mainViewModel.musicSongList.value?.size ?: 0
            )
            audioBinder.seekToPrevious()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun isTheStateSuspended() {
        if (!audioBinder.mediaIsPlaying()) {
            if (binding.musicPlayPause.tag == "pause") {
                mediaPlayerPause()
            } else {
                audioBinder.playerMedia()
            }
        }
    }

    fun setEndOfSongListener(callback: (currentPosition: Int) -> Unit) {
        audioBinder.setEndOfSong(object : ExoPlayerManager.EndOfSongFan {
            override fun onEndOfSongPlayListener(currentPosition: Int) {
                isClickOnTheNextSong = false
                callback.invoke(currentPosition)
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun mediaPlayerPause() {
        if (!isNotFirstEntry) isNotFirstEntry = true
        if (audioBinder.mediaIsPlaying()) {
            binding.musicPlayPause.tag = "pause"
            binding.musicPlayPause.setImageResource(R.drawable.icon_play_anim)
            audioBinder.stopMediaPlayer()
        } else {
            binding.musicPlayPause.tag = "play"
            binding.musicPlayPause.setImageResource(R.drawable.icon_pause_anim)
            audioBinder.playerMedia()
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
                    audioBinder = p1 as ExoPlayerService.AudioBinder
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
    private fun requestData() {
        registerForActivityResult.launch(permissions)
    }

    /**
     * 跳转Home
     */
    private fun startHomeFragment() {
        replaceFragment(binding.contentFragment.id, HomeFragment())
        isOtherPages = true
    }

    /**
     * 跳转歌词详情
     */
    private fun startLrcFragment() {
        replaceFragment(binding.fragment.id, LrcFragment())
        isOtherPages = true
    }

    /**
     * 跳转媒体库
     */
    public fun startMediaLibraryFragment() {
        replaceFragment(binding.contentFragment.id, MediaLibraryFragment())
        isOtherPages = true
    }

    /**
     * 跳转歌单
     */
    public fun startSongListFragment() {
        replaceFragment(binding.contentFragment.id, SongListFragment())
        isOtherPages = true
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
        binding.contentLayout.setOnClickListener { }
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
        } else if (isOtherPages) {
            startHomeFragment()
            isOtherPages = false
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
}
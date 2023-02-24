package com.example.media

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.base.utils.showLog

//通知Action
const val FAN_CUSTOM_ACTION_PLAY = "play_fan"
const val FAN_CUSTOM_ACTION_PAUSE = "pause_fan"
const val FAN_CUSTOM_ACTION_PREVIOUS = "previous_fan"
const val FAN_CUSTOM_ACTION_NEXT = "next_fan"
class MyBroadcastReceiver : BroadcastReceiver() {
    lateinit var myBroadcastReceiverListener:MyBroadcastReceiverListener
    fun setMyBroadcastReceiverListenerF(_myBroadcastReceiverListener:MyBroadcastReceiverListener){
        myBroadcastReceiverListener = _myBroadcastReceiverListener
    }
    override fun onReceive(context: Context, intent: Intent) {
        when(intent.action){
            FAN_CUSTOM_ACTION_PLAY ->{
                myBroadcastReceiverListener.onPlay()
            }
            FAN_CUSTOM_ACTION_PAUSE ->{
                myBroadcastReceiverListener.onPause()
            }
            FAN_CUSTOM_ACTION_PREVIOUS ->{
                myBroadcastReceiverListener.onPrevious()
            }
            FAN_CUSTOM_ACTION_NEXT ->{
                myBroadcastReceiverListener.onNext()
            }
        }
    }
}

interface MyBroadcastReceiverListener {
    fun onPlay()
    fun onPause()
    fun onNext()
    fun onPrevious()
}

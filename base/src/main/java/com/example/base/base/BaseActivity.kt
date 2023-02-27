
package com.example.base.base

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import com.example.base.R
import com.google.android.material.snackbar.Snackbar
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes


/**
 * 通用Activity
 */
open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        window.statusBarColor = Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    fun<T> startA(clazz: Class<T>){
        val intent = Intent(this, clazz)
        startActivity(intent)
    }

    /**
     * 打印SnackBar
     */
    fun showSnackBar(content:Any){
        Snackbar.make(window.decorView, "$content",Snackbar.LENGTH_LONG).show()
    }

    fun replaceFragment(viewId:Int,fragment:Fragment,anim01:Int,anim02:Int,anim03:Int,anim04:Int){
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                anim01,
                anim02,
                anim03,
                anim04
            )
            .replace(viewId, fragment)
            .commit()
    }

    fun addFragment(viewId:Int,fragment:Fragment,anim01:Int,anim02:Int,anim03:Int,anim04:Int){
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                anim01,
                anim02,
                anim03,
                anim04
            )
            .add(viewId, fragment)
            .addToBackStack("")
            .commit()
    }


}
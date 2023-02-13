
package com.example.base.base

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
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
        AppCenter.start(
            application,
            "7f4ffe49-58c2-4546-8467-8371f9c4e261",
            Analytics::class.java,
            Crashes::class.java
        )
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

    fun replaceFragment(viewId:Int,fragment:Fragment){
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                androidx.navigation.ui.R.anim.nav_default_enter_anim,
                androidx.navigation.ui.R.anim.nav_default_exit_anim,
                androidx.navigation.ui.R.anim.nav_default_pop_enter_anim,
                androidx.navigation.ui.R.anim.nav_default_pop_exit_anim
            )
            .replace(viewId, fragment)
            .commit()
    }

    fun addFragment(viewId:Int,fragment:Fragment){
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                androidx.navigation.ui.R.anim.nav_default_enter_anim,
                androidx.navigation.ui.R.anim.nav_default_exit_anim,
                androidx.navigation.ui.R.anim.nav_default_pop_enter_anim,
                androidx.navigation.ui.R.anim.nav_default_pop_exit_anim
            )
            .add(viewId, fragment)
            .addToBackStack("")
            .commit()
    }


}
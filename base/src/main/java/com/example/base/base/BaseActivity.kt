
package com.example.base.base

import android.content.Intent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar

/**
 * 通用Activity
 */
open class BaseActivity : AppCompatActivity() {

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
            .addToBackStack("")
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

package com.example.base.base

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar

/**
 * 通用Fragment
 */
open class BaseFragment : Fragment() {

    fun<T> startA(clazz: Class<T>){
        val intent = Intent(context, clazz)
        startActivity(intent)
    }

    /**
     * 打印SnackBar
     */
    fun showSnackBar(content:Any){
        view?.let { Snackbar.make(it, "$content",Snackbar.LENGTH_LONG).show() }
    }


}
package com.example.base.utils

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.LinearInterpolator


/**
 * 动画工具类
 */


/**
 * 首次进入程序,默认不开始动画效果、媒体播放
 */
var isNotFirstEntry = false

/**
 * 播放器收起时切换歌曲动画
 */
fun startTextAnimator(view: View, action: () -> Unit = {}) {
    if (isNotFirstEntry) {
        val animator = ObjectAnimator.ofFloat(
            view,
            "translationX",
            0f,
            -10f
        ).setDuration(150)
        val animator2 =
            ObjectAnimator.ofFloat(view, "alpha", 0f).setDuration(150)
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(animator, animator2)
        animatorSet.interpolator = LinearInterpolator()
        animatorSet.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(p0: Animator) {

            }

            override fun onAnimationEnd(p0: Animator) {
                val animator =
                    ObjectAnimator.ofFloat(view, "translationX", 100f, 0f).setDuration(
                        200
                    )
                val animator2 =
                    ObjectAnimator.ofFloat(view, "alpha", 0.5f, 1f).setDuration(200)
                AnimatorSet().apply {
                    interpolator = LinearInterpolator()
                    playTogether(animator, animator2)
                    start()
                }
                action.invoke()
            }

            override fun onAnimationCancel(p0: Animator) {

            }

            override fun onAnimationRepeat(p0: Animator) {

            }
        })
        animatorSet.start()
    }
}

/**
 * 播放器展开下一首事件动画
 */
fun startTextExpandNextAnimator(view: View, action: () -> Unit = {}) {
    if (isNotFirstEntry) {
        val animator = ObjectAnimator.ofFloat(
            view,
            "translationX",
            0f,
            -1000f
        ).setDuration(200)

        animator.interpolator = LinearInterpolator()
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(p0: Animator) {

            }

            override fun onAnimationEnd(p0: Animator) {
                action.invoke()
                val animator2 =
                    ObjectAnimator.ofFloat(view, "translationX", 1000f, 0f).setDuration(
                        200
                    )
                animator2.interpolator = LinearInterpolator()
                animator2.start()
            }

            override fun onAnimationCancel(p0: Animator) {

            }

            override fun onAnimationRepeat(p0: Animator) {

            }
        })
        animator.start()
    }
}

/**
 * 播放器展开上一首事件动画
 */
fun startTextPreviousExpandAnimator(view: View, action: () -> Unit = {}) {
    if (isNotFirstEntry) {
        val animator = ObjectAnimator.ofFloat(
            view,
            "translationX",
            0f,
            1000f
        ).setDuration(200)

        animator.interpolator = LinearInterpolator()
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(p0: Animator) {

            }

            override fun onAnimationEnd(p0: Animator) {
                action.invoke()
                val animator2 =
                    ObjectAnimator.ofFloat(view, "translationX", -1000f, 0f).setDuration(
                        200
                    )
                animator2.interpolator = LinearInterpolator()
                animator2.start()
            }

            override fun onAnimationCancel(p0: Animator) {

            }

            override fun onAnimationRepeat(p0: Animator) {

            }
        })
        animator.start()
    }
}

/**
 * 歌词切换动画
 */
fun startTextLrcAnimator(view: View, action: () -> Unit = {}) {
    try {
        if (isNotFirstEntry) {
            val animator = ObjectAnimator.ofFloat(
                view,
                "translationY",
                0f,
                -50f
            ).setDuration(200)
            val animator2 = ObjectAnimator.ofFloat(
                view,
                "alpha",
                1f,
                0f
            ).setDuration(200)

            val animator3 = ObjectAnimator.ofFloat(
                view,
                "ScaleX",
                1f,
                0.8f
            ).setDuration(200)

            val animatorSet = AnimatorSet()
            animatorSet.playTogether(animator, animator2, animator3)
            animatorSet.interpolator = LinearInterpolator()
            animatorSet.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(p0: Animator) {

                }

                override fun onAnimationEnd(p0: Animator) {
                    action.invoke()
                    val animator1 =
                        ObjectAnimator.ofFloat(view, "translationY", 50f, 0f).setDuration(
                            200
                        )
                    val animator2 = ObjectAnimator.ofFloat(
                        view,
                        "alpha",
                        0f,
                        1f
                    ).setDuration(200)
                    val animator3 = ObjectAnimator.ofFloat(
                        view,
                        "ScaleX",
                        0.8f,
                        1f
                    ).setDuration(200)

                    val animatorSet = AnimatorSet()
                    animatorSet.playTogether(animator1, animator2, animator3)
                    animatorSet.interpolator = LinearInterpolator()
                    animatorSet.start()
                }

                override fun onAnimationCancel(p0: Animator) {

                }

                override fun onAnimationRepeat(p0: Animator) {

                }
            })
            animatorSet.start()
        }
    } catch (e: Exception) {
    }
}

/**
 * 透明度进入
 */
fun startAlphaEnterAnimator(view: View, duration:Long = 300) {
    val animator = ObjectAnimator.ofFloat(
        view,
        "alpha",
        0.5f,
        1f
    ).setDuration(duration)
    animator.interpolator = LinearInterpolator()
    animator.start()
}

/**
 * 透明度退出
 */
fun startAlphaOutAnimator(view: View, duration:Long = 300) {
    val animator = ObjectAnimator.ofFloat(
        view,
        "alpha",
        1f,
        0.5f
    ).setDuration(duration)
    animator.interpolator = LinearInterpolator()
    animator.start()
}

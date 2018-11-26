package edu.fgcu.terrorturret.utils

import android.animation.AnimatorInflater
import android.view.View
import edu.fgcu.terrorturret.R

/**
 * Rapidly buzzes/shakes the view horizontally a little for half a second.
 *
 * Useful when paired with a vibration to indicate an invalid input on a form field.
 */
fun View.shake() {
    val animator = AnimatorInflater.loadAnimator(context, R.animator.shake)
    animator.setTarget(this)
    animator.start()
}

package edu.fgcu.terrorturret.utils

import android.animation.AnimatorInflater
import android.content.Context
import android.util.DisplayMetrics
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

/**
 * This class contains util functions that should go under the namespace of a class, but aren't
 * really good extension functions.
 */
object ViewUtils {

    /**
     * Be sure to pass the appContext here, not the activity context, to avoid memory leaks.
     */
    fun dpToPx(appContext: Context, dp: Int): Int {
        val displayMetrics = appContext.resources.displayMetrics
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT))
    }

}

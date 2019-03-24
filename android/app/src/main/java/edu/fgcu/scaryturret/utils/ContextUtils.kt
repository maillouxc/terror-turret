package edu.fgcu.scaryturret.utils

import android.content.Context
import android.widget.Toast

/**
 * This file contains useful extension methods to the Android Context class.
 *
 * They are mainly just syntactic sugar to allow for cleaner code.
 */

fun Context.toast(text: String) {
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}

fun Context.toast(stringResId: Int) {
    toast(getString(stringResId))
}

fun Context.longToast(text: String) {
    Toast.makeText(this, text, Toast.LENGTH_LONG).show()
}

fun Context.longToast(stringResId: Int) {
    longToast(getString(stringResId))
}

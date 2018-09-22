package edu.fgcu.terrorturret.utils

/**
 * Linearly maps this number from one range to another and returns the value in the new range.
 *
 * For instance, to convert 0.6 from the range [0, 1] to the range [0, 10], you would call this
 * function as map(0, 1, 0, 10), which would return a value of 6.
 *
 * Since this is a linear mapping, the formula applied is essentially y = mx + b, where m is the
 * ratio between the two ranges, y is the result, x is the input, and b is the offset of the
 * destination range.
 */
fun Double.map(fromMin: Double, fromMax: Double, toMin: Double, toMax: Double): Double {
    val fromRange = fromMax - fromMin
    val toRange = toMax - toMin
    val ratio = toRange / fromRange
    val fromMinusOffset = this - fromMin
    return (ratio * fromMinusOffset) + toMin
}

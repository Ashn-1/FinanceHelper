package de.ash.financehelper.util

import android.view.View
import java.time.LocalDate

/*
FLOAT
 */
fun Float.format(digits: Int) = "%.${digits}f".format(this)

/*
LOCALDATE
 */
fun LocalDate.toInteger() = dayOfMonth or (monthValue shl 8) or (year shl 12)

/*
VIEW
 */
fun View.enableView(enable: Boolean)
{
    isClickable = enable
    isEnabled = enable
}


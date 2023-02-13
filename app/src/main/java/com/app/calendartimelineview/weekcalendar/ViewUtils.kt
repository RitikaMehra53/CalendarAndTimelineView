package com.app.calendartimelineview.weekcalendar

import android.widget.TextView
import android.util.TypedValue

object ViewUtils {
    fun setTextType(textStyle: Int, vararg textViews: TextView) {
        for (tv in textViews) {
            tv.setTypeface(tv.typeface, textStyle)
        }
    }

    fun setTextSize(textSize: Int, vararg textViews: TextView) {
        for (tv in textViews) {
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize.toFloat())
        }
    }
}
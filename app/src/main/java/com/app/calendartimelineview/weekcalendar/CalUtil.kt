package com.app.calendartimelineview.weekcalendar

import android.content.Context
import com.app.calendartimelineview.utils.MyApplication.Companion.instance
import net.danlew.android.joda.JodaTimeAndroid
import org.joda.time.LocalDateTime
import java.util.*

class CalUtil {
    var startDate: LocalDateTime? = null
    var selectedDate: LocalDateTime? = null

    /**
     * Initial calculation of the week
     */
    fun calculate(mContext: Context?) {

        JodaTimeAndroid.init(mContext)

        //Initializing Start with current month
        val currentDateTime = LocalDateTime()
        setStartDate(currentDateTime.year, currentDateTime.monthOfYear, currentDateTime.dayOfMonth)
        val weekGap = mDateGap(
            currentDateTime.dayOfWeek().asText.substring(0, 3).lowercase(
                Locale.getDefault()
            )
        )
        if (weekGap != 0) {
            //if the current date is not the first day of the week the rest of days is added

            //Calendar set to the current date
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -weekGap)

            //now the date is weekGap days back
            val ldt = LocalDateTime.fromCalendarFields(calendar)
            setStartDate(ldt.year, ldt.monthOfYear, ldt.dayOfMonth)
        }
    }

    /*Set The Start day (week)from calender*/
    private fun setStartDate(year: Int, month: Int, day: Int) {
        startDate = LocalDateTime(year, month, day, 0, 0, 0)
        selectedDate = startDate
        instance!!.setDate(selectedDate)
    }

    companion object {
        /**
         * Get the day difference in the selected day and the first day in the week
         */
        fun mDateGap(dayName: String): Int {
            return when (dayName) {
                "mon" -> {
                    1
                }
                "tue" -> {
                    2
                }
                "wed" -> {
                    3
                }
                "thu" -> {
                    4
                }
                "fri" -> {
                    5
                }
                "sat" -> {
                    6
                }
                else -> {
                    0
                }
            }
        }

        fun isSameDay(day1: LocalDateTime, day2: LocalDateTime): Boolean {
            return day1 != null && day2 != null && day1.year == day2.year && day1.monthOfYear == day2.monthOfYear && day1.dayOfMonth == day2.dayOfMonth
        }

        @JvmStatic
        fun isDayInList(day: LocalDateTime, days: List<LocalDateTime>): Boolean {
            if (days == null || day == null) {
                return false
            }
            for (isDay in days) {
                if (isSameDay(isDay, day)) {
                    return true
                }
            }
            return false
        }
    }
}
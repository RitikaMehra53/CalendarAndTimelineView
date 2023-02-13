package com.app.calendartimelineview.weekcalendar

import org.joda.time.LocalDateTime

interface CalenderListener {
    /**
     * listener notify if select date picker
     */
    fun onSelectPicker()

    /**
     * Notify when date selected
     *
     * @param mSelectedDate
     */
    fun onSelectDate(mSelectedDate: LocalDateTime?)
}
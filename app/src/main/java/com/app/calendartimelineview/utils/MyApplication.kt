package com.app.calendartimelineview.utils

import android.app.Application
import org.joda.time.LocalDateTime

class MyApplication : Application() {

    var setDate: LocalDateTime? = null
    var selectedDate: LocalDateTime? = null
    override fun onCreate() {
        super.onCreate()
        instance = this

    }

    companion object {
        @get:Synchronized
        var instance: MyApplication? = null

    }

    fun setDate(setDate: LocalDateTime?) {
        this.setDate = setDate
    }

    fun setSelected(selectedDate: LocalDateTime?) {
        this.selectedDate = selectedDate
    }

    /*getting the current week*/
    fun getDate(): LocalDateTime? {
        return setDate
    }

    /*getting the selected week*/
    fun getSelected(): LocalDateTime? {
        return selectedDate
    }
}

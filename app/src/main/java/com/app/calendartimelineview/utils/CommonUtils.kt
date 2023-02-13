package com.app.calendartimelineview.utils

import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class CommonUtils {
    companion object {
        val instance: CommonUtils = CommonUtils()
    }

    fun convertDateInSpecificFormat(
        readFormat: String,
        writeFormat: String,
        input: String
    ): String {
        val originalFormat: DateFormat = SimpleDateFormat(readFormat, Locale.ENGLISH)
        val targetFormat: DateFormat = SimpleDateFormat(writeFormat)
        val date: Date = originalFormat.parse(input)
        return targetFormat.format(date) // 20120821
    }

    fun convertToMilliSecond(input: String): Long {
        val sdf = SimpleDateFormat(CommonCodes.DATE_FROM_SERVER_FOR_APPOINTMENT)

        try {
            val mDate = sdf.parse(input)
            val timeInMilliseconds = mDate.time
            println("Date in milli :: $timeInMilliseconds")
            return timeInMilliseconds
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        return 0L
    }
}
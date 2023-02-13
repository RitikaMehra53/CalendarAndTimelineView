package com.app.calendartimelineview.timelinemodule

import java.text.SimpleDateFormat
import java.util.*

data class EventI(
    val name: String,
    val startIndex: Float,
    val endIndex: Float,
    val description: String,
    var drName: String = "",
    var procedureName: String = "",
    var confirmedStatus: String = "",
) {

    constructor(event: Event, start: Int): this(
        event.name,
        getHourIndex(event.startTime, start),
        getHourIndex(event.endTime, start),
        "${convertUnixToString(event.startTime)} - ${convertUnixToString(event.endTime)}",
        event.drName,
        event.procedureName,
        event.confirmedStatus,
    )


    companion object {
        fun convertUnixToString(time: Long): String = SimpleDateFormat("hh:mm a", Locale.getDefault())
            .format(time).removePrefix("0")


        fun getHourIndex(time: Long, start: Int): Float {
            val formatter = SimpleDateFormat("HH mm", Locale.getDefault())

            //val hm = formatter.format(Date(time * 1000)).split(" ")
            val hm = formatter.format(time).split(" ")
            var h = hm[0].toFloat()
            val m = hm[1].toInt()
            h = h + m/60f - start

            if (h < 0) h += TimelineView.TOTAL
            return h
        }

        fun convertUnixToStringZone(time: Long, timeZone: TimeZone): String {
            val s = SimpleDateFormat("hh:mm a", Locale.US)
            /*val currentTimeZone: TimeZone = TimeZone.getDefault()
            s.timeZone = currentTimeZone
            val format: String = s.format(time)

            val parsed = s.parse(format)
            s.timeZone = timeZone*/
            return s.format(time).removePrefix("0")
        }

        fun getHourIndexZone(time: Long, start: Int): Float {
            val formatter = SimpleDateFormat("HH mm")

            //val hm = formatter.format(Date(time * 1000)).split(" ")
            val hm = formatter.format(time).split(" ")
            var h = hm[0].toFloat()
            val m = hm[1].toInt()
            h = h + m/60f - start

            if (h < 0) h += TimelineView.TOTAL
            return h
        }

    }

}

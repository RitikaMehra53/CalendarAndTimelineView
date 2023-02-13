package com.app.calendartimelineview.timelinemodule

data class Event(
    var name: String = "",
    var startTime: Long = 0,
    var endTime: Long = 0,
    var drName: String = "",
    var procedureName: String = "",
    var confirmedStatus: String = "",
)

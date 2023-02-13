package com.app.calendartimelineview.utils

import java.util.*

class FragmentObserver : Observable() {
    override fun notifyObservers() {
        setChanged() // Set the changed flag to true, otherwise observers won't be notified.
        super.notifyObservers()
    }
}
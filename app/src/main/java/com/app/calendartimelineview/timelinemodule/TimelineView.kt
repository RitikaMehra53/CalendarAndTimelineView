package com.app.calendartimelineview.timelinemodule

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.util.AttributeSet
import android.util.TypedValue
import android.view.*
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.GestureDetectorCompat
import com.app.calendartimelineview.R
import com.app.calendartimelineview.databinding.ScheduleEventItemBinding
import com.app.calendartimelineview.databinding.TimelineViewBinding
import com.app.calendartimelineview.utils.CommonUtils
import kotlin.math.ceil
import kotlin.math.floor


class TimelineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {
    private val timelineViews = ArrayList<TimelineViewBinding>()
    private var timelineItemViews = ArrayList<View>()
    var currentTimeView = ArrayList<View>()
    private var timeLineItems = ArrayList<EventI>()
    var currentTime = ArrayList<EventI>()
    private val eventViewMargin = dpToPx(18)
    private val timelineItemRect = ArrayList<Rect>()
    private fun dpToPx(dp: Int) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        resources.displayMetrics
    )

    var timelineEvents: List<Event> = ArrayList()
        set(value) {

            timeLineItems.clear()
            timelineItemRect.clear()
            value.forEach {
                timelineItemRect.add(Rect())
                timeLineItems.add(EventI(it, startTime))
            }
            field = value
            addTimelineItemViews()
            requestLayout()
        }

    var startTime = DEFAULT_START_LABEL
        set(value) {
            if (value !in 0 until TOTAL) {
                throw IllegalArgumentException("Start time has to be fom 0 to 23")
            }
            field = value
            initLabels()
            invalidate()
        }

    private var mDetector: GestureDetectorCompat

    @ColorInt
    var eventBg: Int = Color.BLUE

    @ColorInt
    var eventNameColor: Int = DEFAULT_TEXT_COLOR

    @ColorInt
    var eventTimeColor: Int = DEFAULT_TEXT_COLOR

    init {
        orientation = VERTICAL
        val typedArray = context.theme.obtainStyledAttributes(attrs, R.styleable.TimelineView,
            0, 0)

        startTime = 0
        setBackgroundColor(typedArray.getColor(R.styleable.TimelineView_backgroundColor, DEFAULT_BG_COLOR))
        eventNameColor = typedArray.getColor(R.styleable.TimelineView_eventNameColor, DEFAULT_TEXT_COLOR)
        eventTimeColor = typedArray.getColor(R.styleable.TimelineView_eventTimeColor, DEFAULT_TEXT_COLOR)
        eventBg = typedArray.getColor(R.styleable.TimelineView_eventBackground, getColor(R.color.app_color))


        typedArray.recycle()

        initLabels()


        mDetector = GestureDetectorCompat(context, object: GestureDetector.SimpleOnGestureListener(){

            override fun onDown(event: MotionEvent): Boolean {
                return true
            }

            override fun onContextClick(e: MotionEvent?): Boolean {
                return super.onContextClick(e)
            }

            override fun onLongPress(e: MotionEvent?) {
                if (e != null) {
                    val array = ArrayList<Int>()
                    for(i in 0 until timelineItemRect.size){
                        if(timelineItemRect[i].contains(e.x.toInt(), e.y.toInt()))
                        {
                            array.add(i)
                        }
                    }
                    //val i = timelineItemRect.indexOfFirst { it.contains(e.x.toInt(), e.y.toInt()) }
                    if (array.size > 0 ) {
                        val i = array[array.size -1]
                        if(timelineItemViews.size != i)
                            timelineItemViews[i].callOnClick()
                    }
                }
            }

            override fun onShowPress(e: MotionEvent?) {

            }
        })

    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        timelineItemViews.forEachIndexed {index, view ->
            val divWidth = timelineViews[0].divider.width
            if(timeLineItems.size == index){
                return
            }
            //val left = left
            val left = (right - eventViewMargin - divWidth + eventViewMargin).toInt()
            val top = getPosFromIndex(timeLineItems[index].startIndex)
            val bottom = getPosFromIndex(timeLineItems[index].endIndex)
            val right = (right - eventViewMargin).toInt()
            //val right = right

            val widthSpec = MeasureSpec.makeMeasureSpec (right.minus(left), MeasureSpec.EXACTLY)
            val heightSpec = MeasureSpec.makeMeasureSpec (bottom-top, MeasureSpec.EXACTLY)
            view.measure(widthSpec, heightSpec)
            view.layout(0, dpToPx(1).toInt(), right-left, bottom-top)
        }

        currentTimeView.forEachIndexed {index, view ->

            if(currentTime.size == index){
                return
            }
            //val left = (right - divWidth).toInt()
            val top = getPosFromIndex(currentTime[index].startIndex)

            val widthSpec = MeasureSpec.makeMeasureSpec (right-left, MeasureSpec.EXACTLY)
            val heightSpec = MeasureSpec.makeMeasureSpec (bottom-top, MeasureSpec.EXACTLY)
            view.measure(widthSpec, heightSpec)
            view.layout(0, 0, right-left, bottom-top)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (mDetector.onTouchEvent(event)) {
            true
        } else {
            super.onTouchEvent(event)
        }
    }

    override fun dispatchDraw(canvas: Canvas?) {
        super.dispatchDraw(canvas)
        var left: Int
        val divWidth = timelineViews[0].divider.width
        timelineItemViews.forEachIndexed {index, view ->
            if(timeLineItems.size == index){
                return
            }
            left =  (right - eventViewMargin - divWidth + eventViewMargin).toInt()
            //left =  getLeft()

            val top = getPosFromIndex(timeLineItems[index].startIndex)
            val bottom = getPosFromIndex(timeLineItems[index].endIndex)
            val right = (right - eventViewMargin).toInt()
            //val right = right
            timelineItemRect[index].set(left, top, right, bottom)
            canvas?.save()
            canvas?.translate(left.toFloat(), top.toFloat())
            view.draw(canvas)
            canvas?.restore()

        }


        currentTimeView.forEachIndexed {index, view ->
            if(currentTime.size == index){
                return
            }
            left =  getLeft()
            val top = getPosFromIndex(currentTime[index].startIndex)

            if(timelineItemRect.size == 0){
                timelineItemRect.add(Rect())
            }
            timelineItemRect[index].set(left, top, right, bottom)
            canvas?.save()
            canvas?.translate(left.toFloat(), top.toFloat())
            view.draw(canvas)
            canvas?.restore()

        }
    }
    
    @SuppressLint("SetTextI18n")
    private fun initLabels() {
        /*timelineViews.clear()
        removeAllViewsInLayout()

        for (i in 0 until TOTAL) {
            val binding = TimelineViewBinding.inflate(LayoutInflater.from(context), this, false)
            binding.timelineLabel.text = getTime(i, startTime)
            if(binding.timelineLabel.text.equals("12 PM"))
                binding.timelineLabel.text = "Noon"
            timelineViews.add(binding)
            addView(binding.root)
        }*/
    }

    fun updateTimeView(){
        var currentTimes = ""
        if(currentTime.size != 0){
            currentTimes = currentTime[0].name
            currentTimes = CommonUtils.instance.convertDateInSpecificFormat("hh:mm a", "HH:mm", currentTimes)
        }


        timelineViews.clear()
        removeAllViewsInLayout()

        for (i in 0 until TOTAL) {
            val binding = TimelineViewBinding.inflate(LayoutInflater.from(context), this, false)
            binding.timelineLabel.text = getTime(i, startTime)
            if(binding.timelineLabel.text.equals("12 PM"))
                binding.timelineLabel.text = "Noon"

            var time = if(binding.timelineLabel.text.equals("Noon"))
                CommonUtils.instance.convertDateInSpecificFormat("hh a", "HH", "12 PM")
            else
                CommonUtils.instance.convertDateInSpecificFormat("hh a", "HH", binding.timelineLabel.text.toString())
            if(!currentTimes.equals("") && !time.equals("")){
                var splitCurrentTime = currentTimes.split(":")
                if(splitCurrentTime.size > 0){
                    var minutes = splitCurrentTime[1].toInt()
                    var hour = splitCurrentTime[0].toInt()
                    if(hour == time.toInt() && minutes < 10){
                        binding.timelineLabel.visibility = View.INVISIBLE
                    }
                    else if (minutes > 50 && hour + 1 == time.toInt())
                    {
                        binding.timelineLabel.visibility = View.INVISIBLE
                    }
                    else{
                        binding.timelineLabel.visibility = View.VISIBLE
                    }
                }
            }
            timelineViews.add(binding)
            addView(binding.root)
        }
    }

    private fun getHourIndex(time: String, start: Int): Float {
        //val hm = formatter.format(Date(time * 1000)).split(" ")
        val hm = time.split(" ")
        var h = hm[0].toFloat()
        val m = hm[1].toInt()
        h = h + m / 60f - start

        if (h < 0) h += TimelineView.TOTAL
        return h
    }

    private fun addTimelineItemViews() {
        timelineItemViews.clear()

        for (i in 0 until timeLineItems.size) {
            val item = timeLineItems[i]
            val binding  = ScheduleEventItemBinding.inflate(LayoutInflater.from(context))
            /*binding.event = item
            binding.cardView.setCardBackgroundColor(eventBg)
            binding.cardView1.setCardBackgroundColor(eventBg)

            binding.cardView1.visibility = View.GONE

            binding.constrained = true
            binding.eventName.setTextColor(eventNameColor)
            binding.eventTime.setTextColor(eventTimeColor)

            binding.eventName1.setTextColor(eventNameColor)
            binding.eventTime1.setTextColor(eventTimeColor)*/

          /*  val top = getPosFromIndex(timeLineItems[i].startIndex)
            val bottom = getPosFromIndex(timeLineItems[i].endIndex)
            binding.cardView.getLayoutParams().height = bottom - top*/

            binding.tvTime.text = item.description
            binding.tvTime2.text = item.description
            binding.tvProcedureName.text = item.procedureName
            binding.tvDrName.text = item.drName
            binding.tvNames.text = item.name
            binding.tvNames2.text = item.name
            val diff = item.endIndex - item.startIndex


            when (item.confirmedStatus) {
                "1" -> {
                    binding.cardView.setBackgroundResource(R.drawable.appointment_bg_confirm)
                }
                "0" -> {
                    binding.cardView.setBackgroundResource(R.drawable.appointment_bg_not_confirm)
                }
                else -> {
                    binding.cardView.setBackgroundResource(R.drawable.appointment_bg_blocked)
                }
            }
            if(diff < 1){
                binding.tvDrName.visibility = View.GONE
                binding.tvProcedureName.visibility = View.GONE

                binding.tvTime.visibility = View.GONE
                binding.tvNames.visibility = View.GONE

                binding.tvTime2.visibility = View.VISIBLE
                binding.tvNames2.visibility = View.VISIBLE
            }
            else if (diff == 1f){
                binding.tvDrName.visibility = View.VISIBLE
                binding.tvProcedureName.visibility = View.VISIBLE

                binding.tvTime.visibility = View.VISIBLE
                binding.tvNames.visibility = View.VISIBLE

                binding.tvTime2.visibility = View.GONE
                binding.tvNames2.visibility = View.GONE

                val constraintLayout = binding.cardView
                val constraintSet = ConstraintSet()
                constraintSet.clone(constraintLayout)
                constraintSet.connect(
                    R.id.tv_procedure_name,
                    ConstraintSet.BOTTOM,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.BOTTOM,
                    dpToPx(6).toInt()
                )
                constraintSet.connect(
                    R.id.tv_dr_name,
                    ConstraintSet.BOTTOM,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.BOTTOM,
                    dpToPx(4).toInt()
                )
                constraintSet.applyTo(constraintLayout)
            }
            else{
                binding.tvDrName.visibility = View.VISIBLE
                binding.tvProcedureName.visibility = View.VISIBLE

                binding.tvTime.visibility = View.VISIBLE
                binding.tvNames.visibility = View.VISIBLE

                binding.tvTime2.visibility = View.GONE
                binding.tvNames2.visibility = View.GONE

                val constraintLayout = binding.cardView
                val constraintSet = ConstraintSet()
                constraintSet.clone(constraintLayout)
                constraintSet.connect(
                    R.id.tv_dr_name,
                    ConstraintSet.BOTTOM,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.BOTTOM,
                    dpToPx(6).toInt()
                )
                constraintSet.connect(
                    R.id.tv_procedure_name,
                    ConstraintSet.BOTTOM,

                    ConstraintSet.PARENT_ID,
                    ConstraintSet.BOTTOM,
                    dpToPx(6).toInt()
                )
                constraintSet.applyTo(constraintLayout)
            }

            timelineItemViews.add(binding.root)

            binding.executePendingBindings()
        }
    }

    private fun getTime(raw: Int, start: Int): String {
        var state = "AM"
        var time = (raw + start) % TOTAL

        if (time >= 12) state = "PM"
        if (time == 0) time = 12
        if (time > 12) time -= 12

        return "${"%2d".format(time)} $state"
    }

    fun getPosFromIndex(index: Float): Int {
        val l = floor(index).toInt()
        val h = ceil(index).toInt()

        if(l == timelineViews.size)
            return 0
        val lVal = timelineViews[l].let { it.root.top + it.divider.top }

        if(h == timelineViews.size)
            return 0
        val hVal = timelineViews[h].let { it.root.top + it.divider.top }

        return (lVal + (index - l) * (hVal - lVal)).toInt()
    }

    private fun getColor(colorAttr: Int): Int {
        val typedValue = TypedValue()
        val a: TypedArray = context.obtainStyledAttributes(typedValue.data, intArrayOf(colorAttr))
        val color = a.getColor(0, 0)
        a.recycle()
        return color
    }

    companion object {
        const val TOTAL = 24
        const val DEFAULT_START_LABEL = 7
        const val DEFAULT_BG_COLOR = Color.BLACK
        const val DEFAULT_TEXT_COLOR = Color.WHITE
    }
}
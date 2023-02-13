package com.app.calendartimelineview.views

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.app.calendartimelineview.R
import com.app.calendartimelineview.databinding.CurrentTimeViewBinding
import com.app.calendartimelineview.databinding.FragmentMainBinding
import com.app.calendartimelineview.timelinemodule.Event
import com.app.calendartimelineview.timelinemodule.EventI
import com.app.calendartimelineview.timelinemodule.TimelineView
import com.app.calendartimelineview.utils.*
import com.app.calendartimelineview.weekcalendar.CalUtil
import com.app.calendartimelineview.weekcalendar.CalenderListener
import com.app.calendartimelineview.weekcalendar.WeekCalendarOptions
import com.app.calendartimelineview.weekcalendar.WeekFragment
import org.joda.time.LocalDateTime
import org.joda.time.Weeks
import java.util.*

class MainFragment : Fragment() {
    companion object {
        lateinit var binding: FragmentMainBinding
        var eventDays = ArrayList<LocalDateTime>()
        var selectedPosition = 0
        var mPosition = 0
        private var sWeekCalendarInstance: MainFragment? = null
        var ARGUMENT_SELECTED_DATE_BACKGROUND = "bg:select:date"
        var ARGUMENT_SELECTED_DATE_HIGHLIGHT_COLOR = "selected:date:highlight:color"
        var ARGUMENT_CURRENT_DATE_TEXT_COLOR = "bg:current:bg"
        var ARGUMENT_PRIMARY_TEXT_COLOR = "primary:text:color"
        var ARGUMENT_DAY_TEXT_SIZE = "primary:day:size"
        var ARGUMENT_DAY_TEXT_STYLE = "day:text:style"
        var ARGUMENT_EVENT_DAYS = "event:days"
        var ARGUMENT_EVENT_COLOR = "event:color"
        var PACKAGE_NAME_VALUE: String? = null

        @Synchronized
        fun getsWeekCalendarInstance(): MainFragment? {
            return sWeekCalendarInstance
        }

        fun updateTopHeader(localDateTime: LocalDateTime?) {
            val headerDate = CommonUtils.instance.convertDateInSpecificFormat(
                "dd-MM-yyyy",
                "MMMM yyyy",
                localDateTime!!.dayOfMonth.toString()
                        + "-"
                        + localDateTime.monthOfYear.toString()
                        + "-"
                        + localDateTime.year.toString()
            )

            binding.tvMonthYear.text = headerDate
        }

        fun setPreSelectedDate(activity: MainFragment, calendar: Calendar?) {
            activity.mPreSelectedDate = calendar
        }

    }


    private var packageNameValue: String? = null
    private var mWeekCount = 53
    private var mMiddlePoint: Int = mWeekCount / 2


    private var mStartDate: LocalDateTime? = null
    private var mSelectedDate: LocalDateTime? = null

    private var mCalenderListener: CalenderListener? = null


    //initial values of calender property
    var mSelectorDateIndicatorValue = "bg_red"
    var mPrimaryTextSize = 0
    var mPrimaryTextStyle = -1
    var mEventColor: String = WeekCalendarOptions.EVENT_COLOR_WHITE
    var mEventDays = emptyArray<Long>()
    var selectedDate : LocalDateTime?= null

    var s_intentFilter = IntentFilter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sWeekCalendarInstance = this
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentMainBinding.inflate(layoutInflater, container, false)

        setWeekCalendarView()
        setTimelineView()

        return binding.root
    }

    private fun setTimelineView() {
        s_intentFilter.addAction(Intent.ACTION_TIME_TICK)
        s_intentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED)
        s_intentFilter.addAction(Intent.ACTION_TIME_CHANGED)
        requireActivity().registerReceiver(mTimeChangedReceiver, s_intentFilter)
        if (!isAdded)
            return
        performOwnTask()
        setAppointments()
    }

    fun performOwnTask() {


        val calendar = Calendar.getInstance(TimeZone.getDefault())

        val a = calendar.get(Calendar.AM_PM)

        var hour = calendar.get(Calendar.HOUR_OF_DAY)
        if(hour != 12){
            hour = calendar.get(Calendar.HOUR)
        }
        val hourMin = "${String.format("%02d",hour)}:${String.format("%02d",calendar.get(Calendar.MINUTE))}"

        val time = if (a == Calendar.AM)
            "$hourMin AM"
        else
            "$hourMin PM"

        val hour24Time = "${calendar.get(Calendar.HOUR_OF_DAY)} ${calendar.get(Calendar.MINUTE)}"

        //val time = calendar.timeInMillis
        //var time = 1661949000000
        val bindings = CurrentTimeViewBinding.inflate(LayoutInflater.from(context))
        bindings.timelineLabelw.text = time
        binding.timelineView.currentTime.clear()
        binding.timelineView.currentTimeView.clear()
        binding.timelineView.currentTime.add(
            EventI(
                time,
                getHourIndex(hour24Time, 0),
                getHourIndex(hour24Time, 0),
                "",
                "",
                "",
                "")
        )


        binding.timelineView.currentTimeView.add(bindings.root)
        bindings.executePendingBindings()

        binding.timelineView.requestLayout()
        binding.timelineView.invalidate()

        binding.timelineView.updateTimeView()

        if (isFirst) {
            Handler(Looper.getMainLooper()).postDelayed({
                val top =
                    binding.timelineView.getPosFromIndex(binding.timelineView.currentTime[0].startIndex) - 100

                if (top > 0) {
                    binding.scrollView.smoothScrollTo(0, top)
                    isFirst = false
                }
            }, 300L)
        }

    }

    var isFirst = true

    private fun setAppointments() {

        binding.timelineView.timelineEvents = java.util.ArrayList<Event>().apply {
            add(Event("Oliver",CommonUtils.instance.convertToMilliSecond("2022-07-20T14:00:00") , CommonUtils.instance.convertToMilliSecond("2022-07-20T16:00:00"), "Dr William", "Cataract surgery", "1"))
            add(Event("Ethan",CommonUtils.instance.convertToMilliSecond("2022-07-20T17:00:00") , CommonUtils.instance.convertToMilliSecond("2022-07-20T18:00:00"), "Dr Daniel", "Dental restorations", "1"))
            add(Event("Oscar",CommonUtils.instance.convertToMilliSecond("2022-07-20T16:00:00") , CommonUtils.instance.convertToMilliSecond("2022-07-20T17:00:00"), "Dr James", "Circumcision", "2"))

            add(Event("Amelia",CommonUtils.instance.convertToMilliSecond("2022-07-20T10:00:00") , CommonUtils.instance.convertToMilliSecond("2022-07-20T11:30:00"), "Dr Thomas", "Arthroscopy", "0"))
            add(Event("Mia",CommonUtils.instance.convertToMilliSecond("2022-07-20T13:00:00") , CommonUtils.instance.convertToMilliSecond("2022-07-20T14:00:00"), "Dr Charles", "Laparoscopy", "0"))
            add(Event("Lucas",CommonUtils.instance.convertToMilliSecond("2022-07-20T06:30:00") , CommonUtils.instance.convertToMilliSecond("2022-07-20T07:00:00"), "Dr Joseph", "Dental restorations", "0"))

            add(Event("Chloe",CommonUtils.instance.convertToMilliSecond("2022-07-20T19:00:00") , CommonUtils.instance.convertToMilliSecond("2022-07-20T19:30:00"), "Dr Richard", "Circumcision", "2"))
            add(Event("Ava",CommonUtils.instance.convertToMilliSecond("2022-07-20T19:30:00") , CommonUtils.instance.convertToMilliSecond("2022-07-20T20:00:00"), "Dr David", "Cataract surgery", "1"))
            add(Event("Noah",CommonUtils.instance.convertToMilliSecond("2022-07-20T20:00:00") , CommonUtils.instance.convertToMilliSecond("2022-07-20T20:30:00"), "Dr Michael", "Dental restorations", "0"))
        }

        binding.timelineView.requestLayout()
        binding.timelineView.invalidate()
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

    private val mTimeChangedReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val action = intent.action
            if (action == Intent.ACTION_TIME_CHANGED || action == Intent.ACTION_TIMEZONE_CHANGED || action == Intent.ACTION_TIME_TICK) {
                performOwnTask()
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        requireActivity().unregisterReceiver(mTimeChangedReceiver)
    }



    private fun setWeekCalendarView() {
        val cal = Calendar.getInstance()
        val date = CommonUtils.instance.convertDateInSpecificFormat(
            "dd-MM-yyyy",
            CommonCodes.DATE_FORMAT_FOR_SELECTED_DATE,
            cal.get(Calendar.DAY_OF_MONTH).toString()
                    + "-"
                    + (cal.get(Calendar.MONTH) + 1).toString()
                    + "-"
                    + cal.get(Calendar.YEAR).toString()
        )

        val headerDate = CommonUtils.instance.convertDateInSpecificFormat(
            "dd-MM-yyyy",
            "MMMM yyyy",
            cal.get(Calendar.DAY_OF_MONTH).toString()
                    + "-"
                    + (cal.get(Calendar.MONTH) + 1).toString()
                    + "-"
                    + cal.get(Calendar.YEAR).toString()
        )

        binding.tvSelectedDate.text = date
        binding.tvMonthYear.text = headerDate


        packageNameValue = requireContext().packageName

        var isFirst = true

        val listener: CalenderListener = object : CalenderListener {
            override fun onSelectPicker() {

            }

            override fun onSelectDate(mSelectedDate: LocalDateTime?) {

                val date = CommonUtils.instance.convertDateInSpecificFormat(
                    "dd-MM-yyyy",
                    CommonCodes.DATE_FORMAT_FOR_SELECTED_DATE,
                    mSelectedDate!!.dayOfMonth.toString()
                            + "-"
                            + mSelectedDate.monthOfYear
                            + "-"
                            + mSelectedDate.year
                )


                if (isFirst) {
                    isFirst = false
                } else {
                    binding.tvSelectedDate.text = date

                    val headerDate = CommonUtils.instance.convertDateInSpecificFormat(
                        "dd-MM-yyyy",
                        "MMMM yyyy",
                        mSelectedDate.dayOfMonth.toString()
                                + "-"
                                + mSelectedDate.monthOfYear
                                + "-"
                                + mSelectedDate.year
                    )

                    binding.tvMonthYear.text = headerDate

                    selectedDate = mSelectedDate
                }
            }

        }

        /**
         * For quick selection of a date.Any picker or custom date picker can de used
         */
        binding.tvSelectedDate.setOnClickListener {
            if (mCalenderListener != null) {
                mCalenderListener!!.onSelectPicker()
            }
        }

        //setting the listener
        setCalenderListener(listener)

        setPreSelectedDate(this, Calendar.getInstance())
    }


    @RequiresApi(Build.VERSION_CODES.O)
    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(@Nullable savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // update middle point of view pager
        mMiddlePoint = mWeekCount / 2
        /*Setting Calender Adapter*/setupViewPager()

        /*CalUtil is called*/
        val mCal = CalUtil()
        mCal.calculate(requireContext()) //date calculation called
        mSelectedDate = mCal.selectedDate //sets selected from CalUtil
        mStartDate = mCal.startDate //sets start date from CalUtil

        //Setting the selected date listener
        if (mCalenderListener != null) {
            mCalenderListener!!.onSelectDate(mStartDate)
        }
    }

    /**
     * Set set date of the selected week
     */

    private lateinit var weekFragment: WeekFragment

    private fun setDateWeek(calendar: Calendar?) {
        val ldt: LocalDateTime = LocalDateTime.fromCalendarFields(calendar)
        MyApplication.instance!!.setSelected(ldt)
        var nextPage: Int = Weeks.weeksBetween(mStartDate, ldt).weeks
        if (nextPage < 0 || (nextPage == 0 && ldt.isBefore(mStartDate))) {
            // make selected date to previous week on viewpager onSelected:
            // if result is negative or it is 0 weeks but earlier than week start date.
            --nextPage
        }
        if (nextPage >= -mMiddlePoint && nextPage < mMiddlePoint) {
            binding.viewPager.currentItem = nextPage + mMiddlePoint
            if (mCalenderListener != null) {
                mCalenderListener!!.onSelectDate(ldt)
            }
            weekFragment = binding.viewPager.adapter!!.instantiateItem(binding.viewPager, nextPage + mMiddlePoint) as WeekFragment
            weekFragment.ChangeSelector(ldt)
        }
    }

    var mPreSelectedDate: Calendar? = null

    override fun onResume() {
        super.onResume()
        if (mPreSelectedDate != null) {
            setDateWeek(mPreSelectedDate)
            mPreSelectedDate = null
        }
    }

    /**
     * Notify the selected date main page
     */
    fun getSelectedDate(mSelectedDate: LocalDateTime?) {
        if (mCalenderListener != null) {
            mCalenderListener!!.onSelectDate(mSelectedDate)
        }
    }

    /**
     * Set setCalenderListener when user click on a date
     */
    private fun setCalenderListener(calenderListener: CalenderListener) {
        this.mCalenderListener = calenderListener
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupViewPager() {
        val adapter = CalenderAdapter(childFragmentManager)
        binding.viewPager.adapter = adapter
        binding.viewPager.currentItem = mMiddlePoint
        binding.viewPager.offscreenPageLimit = 0
        /*Week change Listener*/binding.viewPager.setOnPageChangeListener(object :
            CustomViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(weekNumber: Int) {
                val addDays: Int = (weekNumber - mMiddlePoint) * 7
                mSelectedDate = mStartDate!!.plusDays(addDays) //add 7 days to the selected date
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
    }

    /**
     * Adaptor which shows weeks in the view
     */
    inner class CalenderAdapter(fm: FragmentManager?) : FragmentStatePagerAdapter(fm!!) {
        private val mObservers: Observable = FragmentObserver()
        override fun getCount(): Int {
            return mWeekCount
        }

        override fun getItem(pos: Int): WeekFragment {
             return WeekFragment.newInstance(
                 pos - mMiddlePoint,
                 mSelectorDateIndicatorValue,
                 resources.getColor(R.color.white),
                 resources.getColor(R.color.text_color),
                 mPrimaryTextSize,
                 mPrimaryTextStyle,
                 resources.getColor(R.color.white),
                 mEventDays,
                 mEventColor
             )
        }

    }
    override fun onDestroyView() {
        super.onDestroyView()
        selectedPosition = 0
    }

}
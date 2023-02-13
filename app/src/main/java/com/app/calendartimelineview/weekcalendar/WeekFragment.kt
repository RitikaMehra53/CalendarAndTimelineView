
package com.app.calendartimelineview.weekcalendar

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.app.calendartimelineview.views.MainFragment
import com.app.calendartimelineview.views.MainFragment.Companion.getsWeekCalendarInstance
import com.app.calendartimelineview.R
import com.app.calendartimelineview.utils.CommonCodes
import com.app.calendartimelineview.utils.CommonUtils
import com.app.calendartimelineview.utils.MyApplication.Companion.instance
import com.app.calendartimelineview.weekcalendar.CalUtil.Companion.isDayInList
import org.joda.time.LocalDateTime
import java.util.*
import kotlin.collections.ArrayList

class WeekFragment : Fragment() , Observer{
    private var mSelectedDate: LocalDateTime? = null
    private var mStartDate: LocalDateTime? = null
    private var mCurrentDate: LocalDateTime? = null
    private var mDirtySelector: LocalDateTime? = null
    private var mSundayTv: TextView? = null
    private var mMondayTv: TextView? = null
    private var mTuesdayTv: TextView? = null
    private var mWednesdayTv: TextView? = null
    private var mThursdayTv: TextView? = null
    private var mFridayTv: TextView? = null
    private var mSaturdayTv: TextView? = null

    private var mSundayWeek: TextView? = null
    private var mMondayWeek: TextView? = null
    private var mTuesdayWeek: TextView? = null
    private var mWednesdayWeek: TextView? = null
    private var mThursdayWeek: TextView? = null
    private var mFridayWeek: TextView? = null
    private var mSaturdayWeek: TextView? = null

    private var mSundayIv: ImageView? = null
    private var mMondayIv: ImageView? = null
    private var mTuesdayIv: ImageView? = null
    private var mWednesdayIv: ImageView? = null
    private var mThursdayIv: ImageView? = null
    private var mFridayIv: ImageView? = null
    private var mSaturdayIv: ImageView? = null

    lateinit var mTextViewArray: Array<TextView?>
    lateinit var mWeekViewArray: Array<TextView?>
    lateinit var mImageViewArray: Array<ImageView>
    lateinit var mImageViewDotArray: Array<ImageView>
    private var mDatePosition = 0
    private var mSelectorDateIndicatorValue = 0
    private var mCurrentDateIndicatorValue = 0
    private var mCurrentDateIndex = -1
    private var mPrimaryTextColor = 0
    private var mSelectorHighlightColor = -1
    private val mDateInWeekArray = ArrayList<LocalDateTime?>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.weekcell, container, false)
        mSundayTv = view.findViewById<View>(R.id.sun_txt) as TextView
        mMondayTv = view.findViewById<View>(R.id.mon_txt) as TextView
        mTuesdayTv = view.findViewById<View>(R.id.tue_txt) as TextView
        mWednesdayTv = view.findViewById<View>(R.id.wen_txt) as TextView
        mThursdayTv = view.findViewById<View>(R.id.thu_txt) as TextView
        mFridayTv = view.findViewById<View>(R.id.fri_txt) as TextView
        mSaturdayTv = view.findViewById<View>(R.id.sat_txt) as TextView
        val sundayEvent = view.findViewById<View>(R.id.img_sun_txt) as ImageView
        val mondayEvent = view.findViewById<View>(R.id.img_mon_txt) as ImageView
        val tuesdayEvent = view.findViewById<View>(R.id.img_tue_txt) as ImageView
        val wednesdayEvent = view.findViewById<View>(R.id.img_wen_txt) as ImageView
        val thursdayEvent = view.findViewById<View>(R.id.img_thu_txt) as ImageView
        val fridayEvent = view.findViewById<View>(R.id.img_fri_txt) as ImageView
        val saturdayEvent = view.findViewById<View>(R.id.img_sat_txt) as ImageView


        mSundayWeek = view.findViewById<View>(R.id.week_sunday) as TextView
        mMondayWeek = view.findViewById<View>(R.id.week_monday) as TextView
        mTuesdayWeek = view.findViewById<View>(R.id.week_tuesday) as TextView
        mWednesdayWeek = view.findViewById<View>(R.id.week_wednesday) as TextView
        mThursdayWeek = view.findViewById<View>(R.id.week_thursday) as TextView
        mFridayWeek = view.findViewById<View>(R.id.week_friday) as TextView
        mSaturdayWeek = view.findViewById<View>(R.id.week_saturday) as TextView


        mSundayIv = view.findViewById<View>(R.id.iv_sunday) as ImageView
        mMondayIv = view.findViewById<View>(R.id.iv_monday) as ImageView
        mTuesdayIv = view.findViewById<View>(R.id.iv_tuesday) as ImageView
        mWednesdayIv = view.findViewById<View>(R.id.iv_wednesday) as ImageView
        mThursdayIv = view.findViewById<View>(R.id.iv_thursday) as ImageView
        mFridayIv = view.findViewById<View>(R.id.iv_friday) as ImageView
        mSaturdayIv = view.findViewById<View>(R.id.iv_saturday) as ImageView

        mTextViewArray = arrayOf(
            mSundayTv, mMondayTv, mTuesdayTv, mWednesdayTv, mThursdayTv, mFridayTv, mSaturdayTv
        )


        mImageViewDotArray = arrayOf(
            mSundayIv!!, mMondayIv!!, mTuesdayIv!!, mWednesdayIv!!, mThursdayIv!!, mFridayIv!!, mSaturdayIv!!
        )

        mWeekViewArray = arrayOf(
            mSundayWeek,
            mMondayWeek,
            mTuesdayWeek,
            mWednesdayWeek,
            mThursdayWeek,
            mFridayWeek,
            mSaturdayWeek
        )
        mImageViewArray = arrayOf(
            sundayEvent,
            mondayEvent,
            tuesdayEvent,
            wednesdayEvent,
            thursdayEvent,
            fridayEvent,
            saturdayEvent
        )

        /*Setting the date info in the Application class*/mStartDate = instance!!.getDate()
        mCurrentDate = instance!!.getDate()
        /*Setting the Resources values and Customization values to the views*/
        val identifierName =
            requireArguments().getString(MainFragment.ARGUMENT_SELECTED_DATE_BACKGROUND)
        if (identifierName != null) {
            val resources = requireActivity().resources
            mSelectorDateIndicatorValue = resources.getIdentifier(
                identifierName, "drawable",
                MainFragment.PACKAGE_NAME_VALUE
            )
        }
        mCurrentDateIndicatorValue =
            requireArguments().getInt(MainFragment.ARGUMENT_CURRENT_DATE_TEXT_COLOR)
        mSelectorHighlightColor =
            requireArguments().getInt(MainFragment.ARGUMENT_SELECTED_DATE_HIGHLIGHT_COLOR)
        mDatePosition = requireArguments().getInt(POSITION_KEY)
        val addDays = mDatePosition * 7
        mStartDate = mStartDate!!.plusDays(addDays) //Adding the 7days to the previous week
        mSelectedDate = instance!!.getSelected()

        /*Fetching the data's for the week to display*/for (i in 0..6) {
            if (mSelectedDate != null) {
                if (mSelectedDate!!.dayOfMonth == mStartDate!!.dayOfMonth && mSelectedDate!!.monthOfYear == mStartDate!!.monthOfYear && mSelectedDate!!.year == mStartDate!!.year) {
                    /*Indicate  if the day is selected*/
                    MainFragment.mPosition = i
                    setSelectedDateBackground(mTextViewArray[i], mWeekViewArray[i]!!)
                    instance!!.setSelected(null) //null the selected date
                }
            }
            mDateInWeekArray.add(mStartDate) //Adding the days in the selected week to list
            mStartDate = mStartDate!!.plusDays(1) //Next day
        }
        val primaryTextStyle = requireArguments().getInt(MainFragment.ARGUMENT_DAY_TEXT_STYLE, -1)
        val primaryTextSize = requireArguments().getInt(MainFragment.ARGUMENT_DAY_TEXT_SIZE, 0)
        if (primaryTextSize > 0 || primaryTextStyle > -1) {
            for (tv in mTextViewArray) {
                if (primaryTextSize > 0) {
                    tv!!.setTextSize(TypedValue.COMPLEX_UNIT_SP, primaryTextSize.toFloat())
                }
                if (primaryTextStyle > -1) {
                    tv!!.setTypeface(tv.typeface, primaryTextStyle)
                }
            }
        }

        /*Setting color in the week views*/mPrimaryTextColor =
            requireArguments().getInt(MainFragment.ARGUMENT_PRIMARY_TEXT_COLOR)
        for (tv in mTextViewArray) {
            tv!!.setTextColor(mPrimaryTextColor)
        }
        /*Displaying the days in the week views*/
        var dayOfWeek = 0
        for (dayTv in mTextViewArray) {
            dayTv!!.text = mDateInWeekArray[dayOfWeek]!!.dayOfMonth.toString()
            /*if (!eventDays.isEmpty()) {
                if (isDayInList(mDateInWeekArray[dayOfWeek], eventDays)) {
                    mImageViewArray[dayOfWeek].setImageResource(eventColorDrawable)
                }
            }*/
            dayOfWeek++
        }

        MainFragment.eventDays.clear()

        /*if the selected week is the current week indicates the current day*/

        var date = MainFragment.binding.tvSelectedDate.text.toString()
        date = CommonUtils.instance.convertDateInSpecificFormat(
            CommonCodes.DATE_FORMAT_FOR_SELECTED_DATE,
            "dd-MM-yyyy",
            date
        )

        val splitDate = date.split("-")

        for (i in 0..6) {
            if (splitDate[0].toInt()
                == mDateInWeekArray[i]!!.dayOfMonth && splitDate[1].toInt() == mDateInWeekArray[i]!!.monthOfYear && splitDate[2].toInt() == mDateInWeekArray[i]!!.year
            ) {
                mCurrentDateIndex = i
                mTextViewArray[i]!!.setTextColor(resources.getColor(R.color.white))
                mTextViewArray[i]!!.setBackgroundResource(R.drawable.rounded_day_selected)
                val typeface = ResourcesCompat.getFont(requireContext(), R.font.eina_02_bold)
                mTextViewArray[i]!!.typeface = typeface
            }
        }
        /**
         * Click listener of all week days with the indicator change and passing listener info.
         */
        mSundayTv!!.setOnClickListener { view ->
            mSelectedDateInfo(0)
            setSelectedDateBackground(view as TextView, mWeekViewArray[0]!!)
        }
        mMondayTv!!.setOnClickListener { view ->
            mSelectedDateInfo(1)
            setSelectedDateBackground(view as TextView, mWeekViewArray[1]!!)
        }
        mTuesdayTv!!.setOnClickListener { view ->
            mSelectedDateInfo(2)
            setSelectedDateBackground(view as TextView, mWeekViewArray[2]!!)
        }
        mWednesdayTv!!.setOnClickListener { view ->
            mSelectedDateInfo(3)
            setSelectedDateBackground(view as TextView, mWeekViewArray[3]!!)
        }
        mThursdayTv!!.setOnClickListener { view ->
            mSelectedDateInfo(4)
            setSelectedDateBackground(view as TextView, mWeekViewArray[4]!!)
        }
        mFridayTv!!.setOnClickListener { view ->
            mSelectedDateInfo(5)
            setSelectedDateBackground(view as TextView, mWeekViewArray[5]!!)
        }
        mSaturdayTv!!.setOnClickListener { view ->
            mSelectedDateInfo(6)
            setSelectedDateBackground(view as TextView, mWeekViewArray[6]!!)
        }

        mSundayWeek!!.setOnClickListener { view ->
            mSelectedDateInfo(0)
            setSelectedDateBackground(mTextViewArray[0]!!, view as TextView)
        }
        mMondayWeek!!.setOnClickListener { view ->
            mSelectedDateInfo(1)
            setSelectedDateBackground(mTextViewArray[1]!!, view as TextView)
        }
        mTuesdayWeek!!.setOnClickListener { view ->
            mSelectedDateInfo(2)
            setSelectedDateBackground(mTextViewArray[2]!!, view as TextView)
        }
        mWednesdayWeek!!.setOnClickListener { view ->
            mSelectedDateInfo(3)
            setSelectedDateBackground(mTextViewArray[3]!!, view as TextView)
        }
        mThursdayWeek!!.setOnClickListener { view ->
            mSelectedDateInfo(4)
            setSelectedDateBackground(mTextViewArray[4]!!, view as TextView)
        }
        mFridayWeek!!.setOnClickListener { view ->
            mSelectedDateInfo(5)
            setSelectedDateBackground(mTextViewArray[5]!!, view as TextView)
        }
        mSaturdayWeek!!.setOnClickListener { view ->
            mSelectedDateInfo(6)
            setSelectedDateBackground(mTextViewArray[6]!!, view as TextView)
        }

        MainFragment.updateTopHeader(mDateInWeekArray[0])

        return view
    }

    fun updateDotUsingEvent(){
        for (i in 0 until mDateInWeekArray.size) {
            if (!MainFragment.eventDays.isEmpty()) {
                if (isDayInList(mDateInWeekArray[i]!!, MainFragment.eventDays)) {
                    mImageViewDotArray[i].visibility = View.VISIBLE
                }
                else{
                    mImageViewDotArray[i].visibility = View.INVISIBLE
                }
            }
        }
    }

   /* override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)


    }
*/
    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)

        /*if (isVisibleToUser) {
            if (mDateInWeekArray.size > 0) {
                // display first day of week if there is no selected date
                if (instance!!.getSelected() == null) {
                    getsWeekCalendarInstance()!!.getSelectedDate(mDateInWeekArray[0])
                    getsWeekCalendarInstance()!!.getSelectedDate(mDateInWeekArray[MainFragment.mPosition])
                }
            }
        }
        if (mSelectedDate != null) {
            setSelectedDateBackground(mTextViewArray!![0])
        }

        if (this::mTextViewArray.isInitialized)
            setSelectedDateBackground(mTextViewArray[MainFragment.mPosition], mWeekViewArray[MainFragment.mPosition]!!)*/
    }

    override fun onResume() {
        super.onResume()
        if (mDirtySelector != null) {
            ChangeSelector(mDirtySelector!!)
            mDirtySelector = null
        }
    }

    /**
     * Passing the selected date info
     */
    fun mSelectedDateInfo(position: Int) {
        MainFragment.mPosition = position
        getsWeekCalendarInstance()!!.getSelectedDate(mDateInWeekArray[position])
        mSelectedDate = mDateInWeekArray[position]
        instance!!.setSelected(mSelectedDate)
    }

    /**
     * Setting date when selected from picker
     */
    fun ChangeSelector(mSelectedDate: LocalDateTime) {
        if (!this::mTextViewArray.isInitialized) {
            return
        }
        if (mTextViewArray == null) {
            mDirtySelector = mSelectedDate
            return
        }
        var startDate = instance!!.getDate()
        val addDays = mDatePosition * 7
        startDate = startDate!!.plusDays(addDays)
        for (i in 0..6) {
            if (mSelectedDate.dayOfMonth == startDate!!.dayOfMonth && mSelectedDate.monthOfYear == startDate.monthOfYear && mSelectedDate.year == startDate.year) {
                setSelectedDateBackground(mTextViewArray[i], mWeekViewArray[i]!!)
            }
            startDate = startDate.plusDays(1)
        }
    }

    private fun setSelectedDateBackground(selectedDateTv: TextView?, selectedWeekTv: TextView) {
        for (tv in mTextViewArray) {
            tv!!.setBackgroundResource(R.drawable.ic_rounded_circle_day)
            tv.setTextColor(resources.getColor(R.color.text_color))

            val typeface = ResourcesCompat.getFont(requireContext(), R.font.eina_02_regular)
            tv.typeface = typeface
            selectedWeekTv.typeface = typeface
        }
        selectedDateTv!!.setBackgroundResource(R.drawable.rounded_day_selected)
        for (tv in mWeekViewArray) {
            val typeface = ResourcesCompat.getFont(requireContext(), R.font.eina_02_regular)
            tv!!.typeface = typeface
        }
        if (mCurrentDateIndex > -1) {
            mTextViewArray[mCurrentDateIndex]!!.setTextColor(resources.getColor(R.color.text_color))
        }
        selectedDateTv.setTextColor(resources.getColor(R.color.white))
        val typeface = ResourcesCompat.getFont(requireContext(), R.font.eina_02_bold)
        selectedDateTv.typeface = typeface

        val typeface1 = ResourcesCompat.getFont(requireContext(), R.font.eina_02_semi_bold)
        selectedWeekTv.typeface = typeface1
    }
    
    
    companion object {
        const val POSITION_KEY = "pos"

        /**
         * Set Values including customizable info
         */
        fun newInstance(
            position: Int,
            selectorDateIndicatorValue: String?,
            currentDateIndicatorValue: Int,
            primaryTextColor: Int,
            primaryTextSize: Int,
            primaryTextStyle: Int,
            selectorHighlightColor: Int,
            eventDays: Array<Long>,
            eventColor: String?
        ): WeekFragment {
            val f = WeekFragment()
            val b = Bundle()
            b.putInt(POSITION_KEY, position)
            b.putString(MainFragment.ARGUMENT_SELECTED_DATE_BACKGROUND, selectorDateIndicatorValue)
            b.putInt(MainFragment.ARGUMENT_SELECTED_DATE_HIGHLIGHT_COLOR, selectorHighlightColor)
            b.putInt(MainFragment.ARGUMENT_CURRENT_DATE_TEXT_COLOR, currentDateIndicatorValue)
            b.putInt(MainFragment.ARGUMENT_PRIMARY_TEXT_COLOR, primaryTextColor)
            b.putInt(MainFragment.ARGUMENT_DAY_TEXT_SIZE, primaryTextSize)
            b.putInt(MainFragment.ARGUMENT_DAY_TEXT_STYLE, primaryTextStyle)
            b.putSerializable(MainFragment.ARGUMENT_EVENT_DAYS, eventDays)
            b.putString(MainFragment.ARGUMENT_EVENT_COLOR, eventColor)
            f.arguments = b
            return f
        }
    }

    override fun update(o: Observable?, arg: Any?) {
        var views = view

        if(views == null)
            return
        mSundayIv = views.findViewById<View>(R.id.iv_sunday) as ImageView
        mMondayIv = views.findViewById<View>(R.id.iv_monday) as ImageView
        mTuesdayIv = views.findViewById<View>(R.id.iv_tuesday) as ImageView
        mWednesdayIv = views.findViewById<View>(R.id.iv_wednesday) as ImageView
        mThursdayIv = views.findViewById<View>(R.id.iv_thursday) as ImageView
        mFridayIv = views.findViewById<View>(R.id.iv_friday) as ImageView
        mSaturdayIv = views.findViewById<View>(R.id.iv_saturday) as ImageView

        mImageViewDotArray = arrayOf(
            mSundayIv!!, mMondayIv!!, mTuesdayIv!!, mWednesdayIv!!, mThursdayIv!!, mFridayIv!!, mSaturdayIv!!
        )

        updateDotUsingEvent()
    }
}
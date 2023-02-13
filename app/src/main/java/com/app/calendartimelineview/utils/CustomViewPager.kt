package com.app.calendartimelineview.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Parcel
import android.os.Parcelable
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.view.animation.Interpolator
import android.widget.Scroller
import androidx.core.os.ParcelableCompat
import androidx.core.os.ParcelableCompatCreatorCallbacks
import androidx.core.view.MotionEventCompat
import androidx.core.view.VelocityTrackerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.ViewConfigurationCompat
import androidx.core.widget.EdgeEffectCompat
import androidx.viewpager.widget.PagerAdapter
import java.util.*

class CustomViewPager : ViewGroup {
    class ItemInfo {
        var `object`: Any? = null
        var position = 0
        var scrolling = false
    }

    private val mItems = ArrayList<ItemInfo>()
    private var mAdapter: PagerAdapter? = null
    private var mCurItem // Index of currently displayed page.
            = 0
    private var mRestoredCurItem = -1
    private var mRestoredAdapterState: Parcelable? = null
    private var mRestoredClassLoader: ClassLoader? = null
    private var mScroller: Scroller? = null

    //private PagerAdapter.DataSetObserver mObserver;
    private var mPageMargin = 0
    private var mMarginDrawable: Drawable? = null
    private var mChildWidthMeasureSpec = 0
    private var mChildHeightMeasureSpec = 0
    private var mInLayout = false
    private var mScrollingCacheEnabled = false
    private var mPopulatePending = false
    private var mScrolling = false
    private var mOffscreenPageLimit = DEFAULT_OFFSCREEN_PAGES
    private var mIsBeingDragged = false
    private var mIsUnableToDrag = false
    private var mTouchSlop = 0
    private var mInitialMotionX = 0f

    /**
     * Position of the last motion event.
     */
    private var mLastMotionX = 0f
    private var mLastMotionY = 0f

    /**
     * ID of the active pointer. This is used to retain consistency during
     * drags/flings if multiple pointers are used.
     */
    private var mActivePointerId = INVALID_POINTER

    /**
     * Determines speed during touch scrolling
     */
    private var mVelocityTracker: VelocityTracker? = null
    private var mMinimumVelocity = 0
    private var mMaximumVelocity = 0
    private var mBaseLineFlingVelocity = 0f
    private var mFlingVelocityInfluence = 0f

    /**
     * Returns true if a fake drag is in progress.
     *
     * @return true if currently in a fake drag, false otherwise.
     * @see .beginFakeDrag
     * @see .fakeDragBy
     * @see .endFakeDrag
     */
    var isFakeDragging = false
        private set
    private var mFakeDragBeginTime: Long = 0
    private var mLeftEdge: EdgeEffectCompat? = null
    private var mRightEdge: EdgeEffectCompat? = null
    private var mFirstLayout = true
    private var mOnPageChangeListener: OnPageChangeListener? = null
    private var mScrollState = SCROLL_STATE_IDLE

    /**
     * Callback interface for responding to changing state of the selected page.
     */
    interface OnPageChangeListener {
        /**
         * This method will be invoked when the current page is scrolled, either as part
         * of a programmatically initiated smooth scroll or a user initiated touch scroll.
         *
         * @param position             Position index of the first page currently being displayed.
         * Page position+1 will be visible if positionOffset is nonzero.
         * @param positionOffset       Value from [0, 1) indicating the offset from the page at position.
         * @param positionOffsetPixels Value in pixels indicating the offset from position.
         */
        fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int)

        /**
         * This method will be invoked when a new page becomes selected. Animation is not
         * necessarily complete.
         *
         * @param position Position index of the new selected page.
         */
        fun onPageSelected(position: Int)

        /**
         * Called when the scroll state changes. Useful for discovering when the user
         * begins dragging, when the pager is automatically settling to the current page,
         * or when it is fully stopped/idle.
         *
         * @param state The new scroll state.
         * @see CustomViewPager.SCROLL_STATE_IDLE
         *
         * @see CustomViewPager.SCROLL_STATE_DRAGGING
         *
         * @see CustomViewPager.SCROLL_STATE_SETTLING
         */
        fun onPageScrollStateChanged(state: Int)
    }

    /**
     * Simple implementation of the [OnPageChangeListener] interface with stub
     * implementations of each method. Extend this if you do not intend to override
     * every method of [OnPageChangeListener].
     */
    class SimpleOnPageChangeListener : OnPageChangeListener {
        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
            // This space for rent
        }

        override fun onPageSelected(position: Int) {
            // This space for rent
        }

        override fun onPageScrollStateChanged(state: Int) {
            // This space for rent
        }
    }

    constructor(context: Context?) : super(context) {
        initViewPager()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        initViewPager()
    }

    fun initViewPager() {
        setWillNotDraw(false)
        descendantFocusability = FOCUS_AFTER_DESCENDANTS
        isFocusable = true
        val context = context
        mScroller = Scroller(context, sInterpolator)
        val configuration = ViewConfiguration.get(context)
        mTouchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(configuration)
        mMinimumVelocity = configuration.scaledMinimumFlingVelocity
        mMaximumVelocity = configuration.scaledMaximumFlingVelocity
        mLeftEdge = EdgeEffectCompat(context)
        mRightEdge = EdgeEffectCompat(context)
        val density = context.resources.displayMetrics.density
        mBaseLineFlingVelocity = 2500.0f * density
        mFlingVelocityInfluence = 0.4f
    }

    private fun setScrollState(newState: Int) {
        if (mScrollState == newState) {
            return
        }
        mScrollState = newState
        if (mOnPageChangeListener != null) {
            mOnPageChangeListener!!.onPageScrollStateChanged(newState)
        }
    }

    var adapter: PagerAdapter?
        get() = mAdapter
        set(adapter) {
            if (mAdapter != null) {
                mAdapter!!.startUpdate(this)
                for (i in mItems.indices) {
                    val ii = mItems[i]
                    mAdapter!!.destroyItem(this, ii.position, ii.`object`!!)
                }
                mAdapter!!.finishUpdate(this)
                mItems.clear()
                removeAllViews()
                mCurItem = 0
                scrollTo(0, 0)
            }
            mAdapter = adapter
            if (mAdapter != null) {
                mPopulatePending = false
                if (mRestoredCurItem >= 0) {
                    mAdapter!!.restoreState(mRestoredAdapterState, mRestoredClassLoader)
                    setCurrentItemInternal(mRestoredCurItem, false, true)
                    mRestoredCurItem = -1
                    mRestoredAdapterState = null
                    mRestoredClassLoader = null
                } else {
                    populate()
                }
            }
        }

    /**
     * Set the currently selected page.
     *
     * @param item         Item index to select
     * @param smoothScroll True to smoothly scroll to the new item, false to transition immediately
     */
    fun setCurrentItem(item: Int, smoothScroll: Boolean) {
        mPopulatePending = false
        setCurrentItemInternal(item, smoothScroll, false)
    }

    /**
     * Set the currently selected page. If the ViewPager has already been through its first
     * layout there will be a smooth animated transition between the current item and the
     * specified item.
     *
     * @param item Item index to select
     */
    var currentItem: Int
        get() = mCurItem
        set(item) {
            mPopulatePending = false
            setCurrentItemInternal(item, !mFirstLayout, false)
        }

    fun setCurrentItemInternal(item: Int, smoothScroll: Boolean, always: Boolean) {
        setCurrentItemInternal(item, smoothScroll, always, 0)
    }

    fun setCurrentItemInternal(item: Int, smoothScroll: Boolean, always: Boolean, velocity: Int) {
        var item = item
        if (mAdapter == null || mAdapter!!.count <= 0) {
            setScrollingCacheEnabled(false)
            return
        }
        if (!always && mCurItem == item && mItems.size != 0) {
            setScrollingCacheEnabled(false)
            return
        }
        if (item < 0) {
            item = 0
        } else if (item >= mAdapter!!.count) {
            item = mAdapter!!.count - 1
        }
        val pageLimit = mOffscreenPageLimit
        if (item > mCurItem + pageLimit || item < mCurItem - pageLimit) {
            // We are doing a jump by more than one page.  To avoid
            // glitches, we want to keep all current pages in the view
            // until the scroll ends.
            for (i in mItems.indices) {
                mItems[i].scrolling = true
            }
        }
        val dispatchSelected = mCurItem != item
        mCurItem = item
        populate()
        val destX = (width + mPageMargin) * item
        if (smoothScroll) {
            smoothScrollTo(destX, 0, velocity)
            if (dispatchSelected && mOnPageChangeListener != null) {
                mOnPageChangeListener!!.onPageSelected(item)
            }
        } else {
            if (dispatchSelected && mOnPageChangeListener != null) {
                mOnPageChangeListener!!.onPageSelected(item)
            }
            completeScroll()
            scrollTo(destX, 0)
        }
    }

    fun setOnPageChangeListener(listener: OnPageChangeListener?) {
        mOnPageChangeListener = listener
    }
    /**
     * Returns the number of pages that will be retained to either side of the
     * current page in the view hierarchy in an idle state. Defaults to 1.
     *
     * @return How many pages will be kept offscreen on either side
     * @see .setOffscreenPageLimit
     */
    /**
     * Set the number of pages that should be retained to either side of the
     * current page in the view hierarchy in an idle state. Pages beyond this
     * limit will be recreated from the adapter when needed.
     *
     *
     *
     * This is offered as an optimization. If you know in advance the number
     * of pages you will need to support or have lazy-loading mechanisms in place
     * on your pages, tweaking this setting can have benefits in perceived smoothness
     * of paging animations and interaction. If you have a small number of pages (3-4)
     * that you can keep active all at once, less time will be spent in layout for
     * newly created view subtrees as the user pages back and forth.
     *
     *
     *
     * You should keep this limit low, especially if your pages have complex layouts.
     * This setting defaults to 1.
     *
     * @param limit How many pages will be kept offscreen in an idle state.
     */
    var offscreenPageLimit: Int
        get() = mOffscreenPageLimit
        set(limit) {
            var limit = limit
            if (limit < DEFAULT_OFFSCREEN_PAGES) {
                Log.w(
                    TAG, "Requested offscreen page limit " + limit + " too small; defaulting to " +
                            DEFAULT_OFFSCREEN_PAGES
                )
                limit = DEFAULT_OFFSCREEN_PAGES
            }
            if (limit != mOffscreenPageLimit) {
                mOffscreenPageLimit = limit
                populate()
            }
        }
    /**
     * Return the margin between pages.
     *
     * @return The size of the margin in pixels
     */
    /**
     * Set the margin between pages.
     *
     * @param marginPixels Distance between adjacent pages in pixels
     * @see .getPageMargin
     * @see .setPageMarginDrawable
     * @see .setPageMarginDrawable
     */
    var pageMargin: Int
        get() = mPageMargin
        set(marginPixels) {
            val oldMargin = mPageMargin
            mPageMargin = marginPixels
            val width = width
            recomputeScrollPosition(width, width, marginPixels, oldMargin)
            requestLayout()
        }

    /**
     * Set a drawable that will be used to fill the margin between pages.
     *
     * @param d Drawable to display between pages
     */
    fun setPageMarginDrawable(d: Drawable?) {
        mMarginDrawable = d
        if (d != null) refreshDrawableState()
        setWillNotDraw(d == null)
        invalidate()
    }

    /**
     * Set a drawable that will be used to fill the margin between pages.
     *
     * @param resId Resource ID of a drawable to display between pages
     */
    fun setPageMarginDrawable(resId: Int) {
        setPageMarginDrawable(context.resources.getDrawable(resId))
    }

    override fun verifyDrawable(who: Drawable): Boolean {
        return super.verifyDrawable(who) || who === mMarginDrawable
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        val d = mMarginDrawable
        if (d != null && d.isStateful) {
            d.state = drawableState
        }
    }

    // We want the duration of the page snap animation to be influenced by the distance that
    // the screen has to travel, however, we don't want this duration to be effected in a
    // purely linear fashion. Instead, we use this method to moderate the effect that the distance
    // of travel has on the overall snap duration.
    fun distanceInfluenceForSnapDuration(f: Float): Float {
        var f = f
        f -= 0.5f // center the values about 0.
        f *= (0.3f * Math.PI / 2.0f).toFloat()
        return Math.sin(f.toDouble()).toFloat()
    }
    /**
     * Like [View.scrollBy], but scroll smoothly instead of immediately.
     *
     * @param x        the number of pixels to scroll by on the X axis
     * @param y        the number of pixels to scroll by on the Y axis
     * @param velocity the velocity associated with a fling, if applicable. (0 otherwise)
     */
    /**
     * Like [View.scrollBy], but scroll smoothly instead of immediately.
     *
     * @param x the number of pixels to scroll by on the X axis
     * @param y the number of pixels to scroll by on the Y axis
     */
    @JvmOverloads
    fun smoothScrollTo(x: Int, y: Int, velocity: Int = 0) {
        var velocity = velocity
        if (childCount == 0) {
            // Nothing to do.
            setScrollingCacheEnabled(false)
            return
        }
        val sx = scrollX
        val sy = scrollY
        val dx = x - sx
        val dy = y - sy
        if (dx == 0 && dy == 0) {
            completeScroll()
            setScrollState(SCROLL_STATE_IDLE)
            return
        }
        setScrollingCacheEnabled(true)
        mScrolling = true
        setScrollState(SCROLL_STATE_SETTLING)
        val pageDelta = Math.abs(dx).toFloat() / (width + mPageMargin)
        var duration = (pageDelta * 100).toInt()
        velocity = Math.abs(velocity)
        duration += if (velocity > 0) {
            (duration / (velocity / mBaseLineFlingVelocity) * mFlingVelocityInfluence).toInt()
        } else {
            100
        }
        duration = Math.min(duration, MAX_SETTLE_DURATION)
        mScroller!!.startScroll(sx, sy, dx, dy, duration)
        invalidate()
    }

    fun addNewItem(position: Int, index: Int) {
        val ii = ItemInfo()
        ii.position = position
        ii.`object` = mAdapter!!.instantiateItem(this, position)
        if (index < 0) {
            mItems.add(ii)
        } else {
            mItems.add(index, ii)
        }
    }

    fun dataSetChanged() {
        // This method only gets called if our observer is attached, so mAdapter is non-null.
        var needPopulate = mItems.size < 3 && mItems.size < mAdapter!!.count
        var newCurrItem = -1
        var i = 0
        while (i < mItems.size) {
            val ii = mItems[i]
            val newPos = mAdapter!!.getItemPosition(ii.`object`!!)
            if (newPos == PagerAdapter.POSITION_UNCHANGED) {
                i++
                continue
            }
            if (newPos == PagerAdapter.POSITION_NONE) {
                mItems.removeAt(i)
                i--
                mAdapter!!.destroyItem(this, ii.position, ii.`object`!!)
                needPopulate = true
                if (mCurItem == ii.position) {
                    // Keep the current item in the valid range
                    newCurrItem = Math.max(0, Math.min(mCurItem, mAdapter!!.count - 1))
                }
                i++
                continue
            }
            if (ii.position != newPos) {
                if (ii.position == mCurItem) {
                    // Our current item changed position. Follow it.
                    newCurrItem = newPos
                }
                ii.position = newPos
                needPopulate = true
            }
            i++
        }
        Collections.sort(mItems, COMPARATOR)
        if (newCurrItem >= 0) {
            // TODO This currently causes a jump.
            setCurrentItemInternal(newCurrItem, false, true)
            needPopulate = true
        }
        if (needPopulate) {
            populate()
            requestLayout()
        }
    }

    fun populate() {
        if (mAdapter == null) {
            return
        }

        // Bail now if we are waiting to populate.  This is to hold off
        // on creating views from the time the user releases their finger to
        // fling to a new position until we have finished the scroll to
        // that position, avoiding glitches from happening at that point.
        if (mPopulatePending) {
            if (DEBUG) Log.i(TAG, "populate is pending, skipping for now...")
            return
        }

        // Also, don't populate until we are attached to a window.  This is to
        // avoid trying to populate before we have restored our view hierarchy
        // state and conflicting with what is restored.
        if (windowToken == null) {
            return
        }
        mAdapter!!.startUpdate(this)
        val pageLimit = mOffscreenPageLimit
        val startPos = Math.max(0, mCurItem - pageLimit)
        val N = mAdapter!!.count
        val endPos = Math.min(N - 1, mCurItem + pageLimit)
        if (DEBUG) Log.v(TAG, "populating: startPos=$startPos endPos=$endPos")

        // Add and remove pages in the existing list.
        var lastPos = -1
        run {
            var i = 0
            while (i < mItems.size) {
                val ii = mItems[i]
                if ((ii.position < startPos || ii.position > endPos) && !ii.scrolling) {
                    if (DEBUG) Log.i(TAG, "removing: " + ii.position + " @ " + i)
                    mItems.removeAt(i)
                    i--
                    mAdapter!!.destroyItem(this, ii.position, ii.`object`!!)
                } else if (lastPos < endPos && ii.position > startPos) {
                    // The next item is outside of our range, but we have a gap
                    // between it and the last item where we want to have a page
                    // shown.  Fill in the gap.
                    lastPos++
                    if (lastPos < startPos) {
                        lastPos = startPos
                    }
                    while (lastPos <= endPos && lastPos < ii.position) {
                        if (DEBUG) Log.i(TAG, "inserting: $lastPos @ $i")
                        addNewItem(lastPos, i)
                        lastPos++
                        i++
                    }
                }
                lastPos = ii.position
                i++
            }
        }

        // Add any new pages we need at the end.
        lastPos = if (mItems.size > 0) mItems[mItems.size - 1].position else -1
        if (lastPos < endPos) {
            lastPos++
            lastPos = if (lastPos > startPos) lastPos else startPos
            while (lastPos <= endPos) {
                if (DEBUG) Log.i(TAG, "appending: $lastPos")
                addNewItem(lastPos, -1)
                lastPos++
            }
        }
        if (DEBUG) {
            Log.i(TAG, "Current page list:")
            for (i in mItems.indices) {
                Log.i(TAG, "#" + i + ": page " + mItems[i].position)
            }
        }
        var curItem: ItemInfo? = null
        for (i in mItems.indices) {
            if (mItems[i].position == mCurItem) {
                curItem = mItems[i]
                break
            }
        }
        mAdapter!!.setPrimaryItem(this, mCurItem, curItem?.`object`!!)
        mAdapter!!.finishUpdate(this)
        if (hasFocus()) {
            val currentFocused = findFocus()
            var ii = currentFocused?.let { infoForAnyChild(it) }
            if (ii == null || ii.position != mCurItem) {
                for (i in 0 until childCount) {
                    val child = getChildAt(i)
                    ii = infoForChild(child)
                    if (ii != null && ii.position == mCurItem) {
                        if (child.requestFocus(FOCUS_FORWARD)) {
                            break
                        }
                    }
                }
            }
        }
    }

    class SavedState : BaseSavedState {
        var position = 0
        var adapterState: Parcelable? = null
        var loader: ClassLoader? = null

        constructor(superState: Parcelable?) : super(superState) {}

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(position)
            out.writeParcelable(adapterState, flags)
        }

        override fun toString(): String {
            return ("FragmentPager.SavedState{"
                    + Integer.toHexString(System.identityHashCode(this))
                    + " position=" + position + "}")
        }

        internal constructor(`in`: Parcel, loader: ClassLoader?) : super(`in`) {
            var loader = loader
            if (loader == null) {
                loader = javaClass.classLoader
            }
            position = `in`.readInt()
            adapterState = `in`.readParcelable(loader)
            this.loader = loader
        }

        companion object {
            @JvmField
            val CREATOR =
                ParcelableCompat.newCreator(object : ParcelableCompatCreatorCallbacks<SavedState?> {
                    override fun createFromParcel(`in`: Parcel, loader: ClassLoader): SavedState? {
                        return SavedState(`in`, loader)
                    }

                    override fun newArray(size: Int): Array<SavedState?> {
                        return arrayOfNulls(size)
                    }
                })
        }
    }

    public override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        val ss = SavedState(superState)
        ss.position = mCurItem
        if (mAdapter != null) {
            ss.adapterState = mAdapter!!.saveState()
        }
        return ss
    }

    public override fun onRestoreInstanceState(state: Parcelable) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        val ss = state
        super.onRestoreInstanceState(ss.superState)
        if (mAdapter != null) {
            mAdapter!!.restoreState(ss.adapterState, ss.loader)
            setCurrentItemInternal(ss.position, false, true)
        } else {
            mRestoredCurItem = ss.position
            mRestoredAdapterState = ss.adapterState
            mRestoredClassLoader = ss.loader
        }
    }

    override fun addView(child: View, index: Int, params: LayoutParams) {
        if (mInLayout) {
            addViewInLayout(child, index, params)
            child.measure(mChildWidthMeasureSpec, mChildHeightMeasureSpec)
        } else {
            super.addView(child, index, params)
        }
        if (USE_CACHE) {
            if (child.visibility != GONE) {
                child.isDrawingCacheEnabled = mScrollingCacheEnabled
            } else {
                child.isDrawingCacheEnabled = false
            }
        }
    }

    fun infoForChild(child: View?): ItemInfo? {
        for (i in mItems.indices) {
            val ii = mItems[i]
            if (mAdapter!!.isViewFromObject(child!!, ii.`object`!!)) {
                return ii
            }
        }
        return null
    }

    fun infoForAnyChild(child: View): ItemInfo? {
        var child = child
        var parent: ViewParent?
        while (child.parent.also { parent = it } !== this) {
            if (parent == null || parent !is View) {
                return null
            }
            child = parent as View
        }
        return infoForChild(child)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mFirstLayout = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // For simple implementation, or internal size is always 0.
        // We depend on the container to specify the layout size of
        // our view.  We can't really know what it is since we will be
        // adding and removing different arbitrary views and do not
        // want the layout to change as this happens.
        setMeasuredDimension(
            getDefaultSize(0, widthMeasureSpec),
            getDefaultSize(0, heightMeasureSpec)
        )

        // Children are just made to fill our space.
        mChildWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
            measuredWidth -
                    paddingLeft - paddingRight, MeasureSpec.EXACTLY
        )
        mChildHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
            measuredHeight -
                    paddingTop - paddingBottom, MeasureSpec.EXACTLY
        )

        // Make sure we have created all fragments that we need to have shown.
        mInLayout = true
        populate()
        mInLayout = false

        // Make sure all children have been properly measured.
        val size = childCount
        for (i in 0 until size) {
            val child = getChildAt(i)
            if (child.visibility != GONE) {
                if (DEBUG) Log.v(
                    TAG, "Measuring #" + i + " " + child
                            + ": " + mChildWidthMeasureSpec
                )
                child.measure(mChildWidthMeasureSpec, mChildHeightMeasureSpec)
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Make sure scroll position is set correctly.
        if (w != oldw) {
            recomputeScrollPosition(w, oldw, mPageMargin, mPageMargin)
        }
    }

    private fun recomputeScrollPosition(width: Int, oldWidth: Int, margin: Int, oldMargin: Int) {
        val widthWithMargin = width + margin
        if (oldWidth > 0) {
            val oldScrollPos = scrollX
            val oldwwm = oldWidth + oldMargin
            val oldScrollItem = oldScrollPos / oldwwm
            val scrollOffset = (oldScrollPos % oldwwm).toFloat() / oldwwm
            val scrollPos = ((oldScrollItem + scrollOffset) * widthWithMargin).toInt()
            scrollTo(scrollPos, scrollY)
            if (!mScroller!!.isFinished) {
                // We now return to your regularly scheduled scroll, already in progress.
                val newDuration = mScroller!!.duration - mScroller!!.timePassed()
                mScroller!!.startScroll(scrollPos, 0, mCurItem * widthWithMargin, 0, newDuration)
            }
        } else {
            val scrollPos = mCurItem * widthWithMargin
            if (scrollPos != scrollX) {
                completeScroll()
                scrollTo(scrollPos, scrollY)
            }
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        mInLayout = true
        populate()
        mInLayout = false
        val count = childCount
        val width = r - l
        for (i in 0 until count) {
            val child = getChildAt(i)
            var ii: ItemInfo ?= null
            if (child.visibility != GONE && infoForChild(child).also { ii = it!! } != null) {
                val loff = (width + mPageMargin) * ii?.position!!
                val childLeft = paddingLeft + loff
                val childTop = paddingTop
                if (DEBUG) Log.v(
                    TAG, "Positioning #" + i + " " + child + " f=" + ii?.`object`
                            + ":" + childLeft + "," + childTop + " " + child.measuredWidth
                            + "x" + child.measuredHeight
                )
                child.layout(
                    childLeft, childTop,
                    childLeft + child.measuredWidth,
                    childTop + child.measuredHeight
                )
            }
        }
        mFirstLayout = false
    }

    override fun computeScroll() {
        if (DEBUG) Log.i(TAG, "computeScroll: finished=" + mScroller!!.isFinished)
        if (!mScroller!!.isFinished) {
            if (mScroller!!.computeScrollOffset()) {
                if (DEBUG) Log.i(TAG, "computeScroll: still scrolling")
                val oldX = scrollX
                val oldY = scrollY
                val x = mScroller!!.currX
                val y = mScroller!!.currY
                if (oldX != x || oldY != y) {
                    scrollTo(x, y)
                }
                if (mOnPageChangeListener != null) {
                    val widthWithMargin = width + mPageMargin
                    val position = x / widthWithMargin
                    val offsetPixels = x % widthWithMargin
                    val offset = offsetPixels.toFloat() / widthWithMargin
                    mOnPageChangeListener!!.onPageScrolled(position, offset, offsetPixels)
                }

                // Keep on drawing until the animation has finished.
                invalidate()
                return
            }
        }

        // Done with scroll, clean up state.
        completeScroll()
    }

    private fun completeScroll() {
        var needPopulate = mScrolling
        if (needPopulate) {
            // Done with scroll, no longer want to cache view drawing.
            setScrollingCacheEnabled(false)
            mScroller!!.abortAnimation()
            val oldX = scrollX
            val oldY = scrollY
            val x = mScroller!!.currX
            val y = mScroller!!.currY
            if (oldX != x || oldY != y) {
                scrollTo(x, y)
            }
            setScrollState(SCROLL_STATE_IDLE)
        }
        mPopulatePending = false
        mScrolling = false
        for (i in mItems.indices) {
            val ii = mItems[i]
            if (ii.scrolling) {
                needPopulate = true
                ii.scrolling = false
            }
        }
        if (needPopulate) {
            populate()
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        /*
         * This method JUST determines whether we want to intercept the motion.
         * If we return true, onMotionEvent will be called and we do the actual
         * scrolling there.
         */
        val action = ev.action and MotionEventCompat.ACTION_MASK

        // Always take care of the touch gesture being complete.
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            // Release the drag.
            if (DEBUG) Log.v(TAG, "Intercept done!")
            mIsBeingDragged = false
            mIsUnableToDrag = false
            mActivePointerId = INVALID_POINTER
            return false
        }

        // Nothing more to do here if we have decided whether or not we
        // are dragging.
        if (action != MotionEvent.ACTION_DOWN) {
            if (mIsBeingDragged) {
                if (DEBUG) Log.v(TAG, "Intercept returning true!")
                return true
            }
            if (mIsUnableToDrag) {
                if (DEBUG) Log.v(TAG, "Intercept returning false!")
                return false
            }
        }
        when (action) {
            MotionEvent.ACTION_MOVE -> {

                /*
                 * mIsBeingDragged == false, otherwise the shortcut would have caught it. Check
                 * whether the user has moved far enough from his original down touch.
                 */

                /*
                * Locally do absolute value. mLastMotionY is set to the y value
                * of the down event.
                */
                val activePointerId = mActivePointerId
                // If we don't have a valid id, the touch down wasn't on content.
                if (activePointerId == INVALID_POINTER) return false
                val pointerIndex = MotionEventCompat.findPointerIndex(ev, activePointerId)
                val x = MotionEventCompat.getX(ev, pointerIndex)
                val dx = x - mLastMotionX
                val xDiff = Math.abs(dx)
                val y = MotionEventCompat.getY(ev, pointerIndex)
                val yDiff = Math.abs(y - mLastMotionY)
                val scrollX = scrollX
                val atEdge =
                    dx > 0 && scrollX == 0 || dx < 0 && mAdapter != null && scrollX >= (mAdapter!!.count - 1) * width - 1
                if (DEBUG) Log.v(TAG, "Moved x to $x,$y diff=$xDiff,$yDiff")
                if (canScroll(this, false, dx.toInt(), x.toInt(), y.toInt())) {
                    // Nested view has scrollable area under this point. Let it be handled there.
                    mLastMotionX = x
                    mInitialMotionX = mLastMotionX
                    mLastMotionY = y
                    return false
                }
                if (xDiff > mTouchSlop && xDiff > yDiff) {
                    if (DEBUG) Log.v(TAG, "Starting drag!")
                    mIsBeingDragged = true
                    setScrollState(SCROLL_STATE_DRAGGING)
                    mLastMotionX = x
                    setScrollingCacheEnabled(true)
                } else {
                    if (yDiff > mTouchSlop) {
                        // The finger has moved enough in the vertical
                        // direction to be counted as a drag...  abort
                        // any attempt to drag horizontally, to work correctly
                        // with children that have scrolling containers.
                        if (DEBUG) Log.v(TAG, "Starting unable to drag!")
                        mIsUnableToDrag = true
                    }
                }
            }
            MotionEvent.ACTION_DOWN -> {

                /*
                 * Remember location of down touch.
                 * ACTION_DOWN always refers to pointer index 0.
                 */mInitialMotionX = ev.x
                mLastMotionX = mInitialMotionX
                mLastMotionY = ev.y
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0)
                if (mScrollState == SCROLL_STATE_SETTLING) {
                    // Let the user 'catch' the pager as it animates.
                    mIsBeingDragged = true
                    mIsUnableToDrag = false
                    setScrollState(SCROLL_STATE_DRAGGING)
                } else {
                    completeScroll()
                    mIsBeingDragged = false
                    mIsUnableToDrag = false
                }
                if (DEBUG) Log.v(
                    TAG, "Down at " + mLastMotionX + "," + mLastMotionY
                            + " mIsBeingDragged=" + mIsBeingDragged
                            + "mIsUnableToDrag=" + mIsUnableToDrag
                )
            }
            MotionEventCompat.ACTION_POINTER_UP -> onSecondaryPointerUp(ev)
        }

        /*
        * The only time we want to intercept motion events is if we are in the
        * drag mode.
        */return mIsBeingDragged
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (isFakeDragging) {
            // A fake drag is in progress already, ignore this real one
            // but still eat the touch events.
            // (It is likely that the user is multi-touching the screen.)
            return true
        }
        if (ev.action == MotionEvent.ACTION_DOWN && ev.edgeFlags != 0) {
            // Don't handle edge touches immediately -- they may actually belong to one of our
            // descendants.
            return false
        }
        if (mAdapter == null || mAdapter!!.count == 0) {
            // Nothing to present or scroll; nothing to touch.
            return false
        }
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain()
        }
        mVelocityTracker!!.addMovement(ev)
        val action = ev.action
        var needsInvalidate = false
        when (action and MotionEventCompat.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {

                /*
                 * If being flinged and user touches, stop the fling. isFinished
                 * will be false if being flinged.
                 */completeScroll()

                // Remember where the motion event started
                mInitialMotionX = ev.x
                mLastMotionX = mInitialMotionX
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!mIsBeingDragged) {
                    val pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId)
                    val x = MotionEventCompat.getX(ev, pointerIndex)
                    val xDiff = Math.abs(x - mLastMotionX)
                    val y = MotionEventCompat.getY(ev, pointerIndex)
                    val yDiff = Math.abs(y - mLastMotionY)
                    if (DEBUG) Log.v(TAG, "Moved x to $x,$y diff=$xDiff,$yDiff")
                    if (xDiff > mTouchSlop && xDiff > yDiff) {
                        if (DEBUG) Log.v(TAG, "Starting drag!")
                        mIsBeingDragged = true
                        mLastMotionX = x
                        setScrollState(SCROLL_STATE_DRAGGING)
                        setScrollingCacheEnabled(true)
                    }
                }
                if (mIsBeingDragged) {
                    // Scroll to follow the motion event
                    val activePointerIndex = MotionEventCompat.findPointerIndex(
                        ev, mActivePointerId
                    )
                    val x = MotionEventCompat.getX(ev, activePointerIndex)
                    val deltaX = mLastMotionX - x
                    mLastMotionX = x
                    val oldScrollX = scrollX.toFloat()
                    var scrollX = oldScrollX + deltaX
                    val width = width
                    val widthWithMargin = width + mPageMargin
                    val lastItemIndex = mAdapter!!.count - 1
                    val leftBound = Math.max(0, (mCurItem - 1) * widthWithMargin).toFloat()
                    val rightBound =
                        (Math.min(mCurItem + 1, lastItemIndex) * widthWithMargin).toFloat()
                    if (scrollX < leftBound) {
                        if (leftBound == 0f) {
                            val over = -scrollX
                            needsInvalidate = mLeftEdge!!.onPull(over / width)
                        }
                        scrollX = leftBound
                    } else if (scrollX > rightBound) {
                        if (rightBound == (lastItemIndex * widthWithMargin).toFloat()) {
                            val over = scrollX - rightBound
                            needsInvalidate = mRightEdge!!.onPull(over / width)
                        }
                        scrollX = rightBound
                    }
                    // Don't lose the rounded component
                    mLastMotionX += scrollX - scrollX.toInt()
                    scrollTo(scrollX.toInt(), scrollY)
                    if (mOnPageChangeListener != null) {
                        val position = scrollX.toInt() / widthWithMargin
                        val positionOffsetPixels = scrollX.toInt() % widthWithMargin
                        val positionOffset = positionOffsetPixels.toFloat() / widthWithMargin
                        mOnPageChangeListener!!.onPageScrolled(
                            position, positionOffset,
                            positionOffsetPixels
                        )
                    }
                }
            }
            MotionEvent.ACTION_UP -> if (mIsBeingDragged) {
                val velocityTracker = mVelocityTracker
                velocityTracker!!.computeCurrentVelocity(1000, mMaximumVelocity.toFloat())
                val initialVelocity = VelocityTrackerCompat.getXVelocity(
                    velocityTracker, mActivePointerId
                ).toInt()
                mPopulatePending = true
                val widthWithMargin = width + mPageMargin
                val scrollX = scrollX
                val currentPage = scrollX / widthWithMargin
                val nextPage = if (initialVelocity > 0) currentPage else currentPage + 1
                setCurrentItemInternal(nextPage, true, true, initialVelocity)
                mActivePointerId = INVALID_POINTER
                endDrag()
                needsInvalidate = mLeftEdge!!.onRelease() or mRightEdge!!.onRelease()
            }
            MotionEvent.ACTION_CANCEL -> if (mIsBeingDragged) {
                setCurrentItemInternal(mCurItem, true, true)
                mActivePointerId = INVALID_POINTER
                endDrag()
                needsInvalidate = mLeftEdge!!.onRelease() or mRightEdge!!.onRelease()
            }
            MotionEventCompat.ACTION_POINTER_DOWN -> {
                val index = MotionEventCompat.getActionIndex(ev)
                val x = MotionEventCompat.getX(ev, index)
                mLastMotionX = x
                mActivePointerId = MotionEventCompat.getPointerId(ev, index)
            }
            MotionEventCompat.ACTION_POINTER_UP -> {
                onSecondaryPointerUp(ev)
                mLastMotionX = MotionEventCompat.getX(
                    ev,
                    MotionEventCompat.findPointerIndex(ev, mActivePointerId)
                )
            }
        }
        if (needsInvalidate) {
            invalidate()
        }
        return true
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        var needsInvalidate = false
        val overScrollMode = ViewCompat.getOverScrollMode(this)
        if (overScrollMode == ViewCompat.OVER_SCROLL_ALWAYS ||
            (overScrollMode == ViewCompat.OVER_SCROLL_IF_CONTENT_SCROLLS &&
                    mAdapter != null && mAdapter!!.count > 1)) {
            if (!mLeftEdge!!.isFinished) {
                val restoreCount = canvas.save()
                val height = height - paddingTop - paddingBottom
                canvas.rotate(270f)
                canvas.translate((-height + paddingTop).toFloat(), 0f)
                mLeftEdge!!.setSize(height, width)
                needsInvalidate = needsInvalidate or mLeftEdge!!.draw(canvas)
                canvas.restoreToCount(restoreCount)
            }
            if (!mRightEdge!!.isFinished) {
                val restoreCount = canvas.save()
                val width = width
                val height = height - paddingTop - paddingBottom
                val itemCount = if (mAdapter != null) mAdapter!!.count else 1
                canvas.rotate(90f)
                canvas.translate(
                    -paddingTop.toFloat(),
                    (
                            -itemCount * (width + mPageMargin) + mPageMargin).toFloat()
                )
                mRightEdge!!.setSize(height, width)
                needsInvalidate = needsInvalidate or mRightEdge!!.draw(canvas)
                canvas.restoreToCount(restoreCount)
            }
        } else {
            mLeftEdge!!.finish()
            mRightEdge!!.finish()
        }
        if (needsInvalidate) {
            // Keep animating
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw the margin drawable if needed.
        if (mPageMargin > 0 && mMarginDrawable != null) {
            val scrollX = scrollX
            val width = width
            val offset = scrollX % (width + mPageMargin)
            if (offset != 0) {
                // Pages fit completely when settled; we only need to draw when in between
                val left = scrollX - offset + width
                mMarginDrawable!!.setBounds(left, 0, left + mPageMargin, height)
                mMarginDrawable!!.draw(canvas)
            }
        }
    }

    /**
     * Start a fake drag of the pager.
     *
     *
     *
     * A fake drag can be useful if you want to synchronize the motion of the ViewPager
     * with the touch scrolling of another view, while still letting the ViewPager
     * control the snapping motion and fling behavior. (e.g. parallax-scrolling tabs.)
     * Call [.fakeDragBy] to simulate the actual drag motion. Call
     * [.endFakeDrag] to complete the fake drag and fling as necessary.
     *
     *
     *
     * During a fake drag the ViewPager will ignore all touch events. If a real drag
     * is already in progress, this method will return false.
     *
     * @return true if the fake drag began successfully, false if it could not be started.
     * @see .fakeDragBy
     * @see .endFakeDrag
     */
    fun beginFakeDrag(): Boolean {
        if (mIsBeingDragged) {
            return false
        }
        isFakeDragging = true
        setScrollState(SCROLL_STATE_DRAGGING)
        mLastMotionX = 0f
        mInitialMotionX = mLastMotionX
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain()
        } else {
            mVelocityTracker!!.clear()
        }
        val time = SystemClock.uptimeMillis()
        val ev = MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, 0f, 0f, 0)
        mVelocityTracker!!.addMovement(ev)
        ev.recycle()
        mFakeDragBeginTime = time
        return true
    }

    /**
     * End a fake drag of the pager.
     *
     * @see .beginFakeDrag
     * @see .fakeDragBy
     */
    fun endFakeDrag() {
        check(isFakeDragging) { "No fake drag in progress. Call beginFakeDrag first." }
        val velocityTracker = mVelocityTracker
        velocityTracker!!.computeCurrentVelocity(1000, mMaximumVelocity.toFloat())
        val initialVelocity = VelocityTrackerCompat.getYVelocity(
            velocityTracker, mActivePointerId
        ).toInt()
        mPopulatePending = true
        if (Math.abs(initialVelocity) > mMinimumVelocity
            || Math.abs(mInitialMotionX - mLastMotionX) >= width / 3
        ) {
            if (mLastMotionX > mInitialMotionX) {
                setCurrentItemInternal(mCurItem - 1, true, true)
            } else {
                setCurrentItemInternal(mCurItem + 1, true, true)
            }
        } else {
            setCurrentItemInternal(mCurItem, true, true)
        }
        endDrag()
        isFakeDragging = false
    }

    /**
     * Fake drag by an offset in pixels. You must have called [.beginFakeDrag] first.
     *
     * @param xOffset Offset in pixels to drag by.
     * @see .beginFakeDrag
     * @see .endFakeDrag
     */
    fun fakeDragBy(xOffset: Float) {
        check(isFakeDragging) { "No fake drag in progress. Call beginFakeDrag first." }
        mLastMotionX += xOffset
        var scrollX = scrollX - xOffset
        val width = width
        val widthWithMargin = width + mPageMargin
        val leftBound = Math.max(0, (mCurItem - 1) * widthWithMargin).toFloat()
        val rightBound = (Math.min(mCurItem + 1, mAdapter!!.count - 1) * widthWithMargin).toFloat()
        if (scrollX < leftBound) {
            scrollX = leftBound
        } else if (scrollX > rightBound) {
            scrollX = rightBound
        }
        // Don't lose the rounded component
        mLastMotionX += scrollX - scrollX.toInt()
        scrollTo(scrollX.toInt(), scrollY)
        if (mOnPageChangeListener != null) {
            val position = scrollX.toInt() / widthWithMargin
            val positionOffsetPixels = scrollX.toInt() % widthWithMargin
            val positionOffset = positionOffsetPixels.toFloat() / widthWithMargin
            mOnPageChangeListener!!.onPageScrolled(
                position, positionOffset,
                positionOffsetPixels
            )
        }

        // Synthesize an event for the VelocityTracker.
        val time = SystemClock.uptimeMillis()
        val ev = MotionEvent.obtain(
            mFakeDragBeginTime, time, MotionEvent.ACTION_MOVE,
            mLastMotionX, 0f, 0
        )
        mVelocityTracker!!.addMovement(ev)
        ev.recycle()
    }

    private fun onSecondaryPointerUp(ev: MotionEvent) {
        val pointerIndex = MotionEventCompat.getActionIndex(ev)
        val pointerId = MotionEventCompat.getPointerId(ev, pointerIndex)
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            val newPointerIndex = if (pointerIndex == 0) 1 else 0
            mLastMotionX = MotionEventCompat.getX(ev, newPointerIndex)
            mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex)
            if (mVelocityTracker != null) {
                mVelocityTracker!!.clear()
            }
        }
    }

    private fun endDrag() {
        mIsBeingDragged = false
        mIsUnableToDrag = false
        if (mVelocityTracker != null) {
            mVelocityTracker!!.recycle()
            mVelocityTracker = null
        }
    }

    private fun setScrollingCacheEnabled(enabled: Boolean) {
        if (mScrollingCacheEnabled != enabled) {
            mScrollingCacheEnabled = enabled
            if (USE_CACHE) {
                val size = childCount
                for (i in 0 until size) {
                    val child = getChildAt(i)
                    if (child.visibility != GONE) {
                        child.isDrawingCacheEnabled = enabled
                    }
                }
            }
        }
    }

    /**
     * Tests scrollability within child views of v given a delta of dx.
     *
     * @param v      View to test for horizontal scrollability
     * @param checkV Whether the view v passed should itself be checked for scrollability (true),
     * or just its children (false).
     * @param dx     Delta scrolled in pixels
     * @param x      X coordinate of the active touch point
     * @param y      Y coordinate of the active touch point
     * @return true if child views of v can be scrolled by delta of dx.
     */
    protected fun canScroll(v: View, checkV: Boolean, dx: Int, x: Int, y: Int): Boolean {
        if (v is ViewGroup) {
            val group = v
            val scrollX = v.getScrollX()
            val scrollY = v.getScrollY()
            val count = group.childCount
            // Count backwards - let topmost views consume scroll distance first.
            for (i in count - 1 downTo 0) {
                // TODO: Add versioned support here for transformed views.
                // This will not work for transformed views in Honeycomb+
                val child = group.getChildAt(i)
                if (x + scrollX >= child.left && x + scrollX < child.right && y + scrollY >= child.top && y + scrollY < child.bottom &&
                    canScroll(
                        child, true, dx, x + scrollX - child.left,
                        y + scrollY - child.top
                    )
                ) {
                    return true
                }
            }
        }
        return checkV && ViewCompat.canScrollHorizontally(v, -dx)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Let the focused view and/or our descendants get the key first
        return super.dispatchKeyEvent(event) || executeKeyEvent(event)
    }

    /**
     * You can call this function yourself to have the scroll view perform
     * scrolling from a key event, just as if the event had been dispatched to
     * it by the view hierarchy.
     *
     * @param event The key event to execute.
     * @return Return true if the event was handled, else false.
     */
    fun executeKeyEvent(event: KeyEvent): Boolean {
        var handled = false
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> handled = arrowScroll(FOCUS_LEFT)
                KeyEvent.KEYCODE_DPAD_RIGHT -> handled = arrowScroll(FOCUS_RIGHT)
                KeyEvent.KEYCODE_TAB -> if (event.hasNoModifiers()) {
                    handled = arrowScroll(FOCUS_FORWARD)
                } else if (event.hasModifiers(KeyEvent.META_SHIFT_ON)) {
                    handled = arrowScroll(FOCUS_BACKWARD)
                }
            }
        }
        return handled
    }

    fun arrowScroll(direction: Int): Boolean {
        var currentFocused = findFocus()
        if (currentFocused === this) currentFocused = null
        var handled = false
        val nextFocused = FocusFinder.getInstance().findNextFocus(
            this, currentFocused,
            direction
        )
        if (nextFocused != null && nextFocused !== currentFocused) {
            if (direction == FOCUS_LEFT) {
                // If there is nothing to the left, or this is causing us to
                // jump to the right, then what we really want to do is page left.
                handled = if (currentFocused != null && nextFocused.left >= currentFocused.left) {
                    pageLeft()
                } else {
                    nextFocused.requestFocus()
                }
            } else if (direction == FOCUS_RIGHT) {
                // If there is nothing to the right, or this is causing us to
                // jump to the left, then what we really want to do is page right.
                handled = if (currentFocused != null && nextFocused.left <= currentFocused.left) {
                    pageRight()
                } else {
                    nextFocused.requestFocus()
                }
            }
        } else if (direction == FOCUS_LEFT || direction == FOCUS_BACKWARD) {
            // Trying to move left and nothing there; try to page.
            handled = pageLeft()
        } else if (direction == FOCUS_RIGHT || direction == FOCUS_FORWARD) {
            // Trying to move right and nothing there; try to page.
            handled = pageRight()
        }
        if (handled) {
            playSoundEffect(SoundEffectConstants.getContantForFocusDirection(direction))
        }
        return handled
    }

    fun pageLeft(): Boolean {
        if (mCurItem > 0) {
            setCurrentItem(mCurItem - 1, true)
            return true
        }
        return false
    }

    fun pageRight(): Boolean {
        if (mAdapter != null && mCurItem < mAdapter!!.count - 1) {
            setCurrentItem(mCurItem + 1, true)
            return true
        }
        return false
    }

    /**
     * We only want the current page that is being shown to be focusable.
     */
    override fun addFocusables(views: ArrayList<View>, direction: Int, focusableMode: Int) {
        val focusableCount = views.size
        val descendantFocusability = descendantFocusability
        if (descendantFocusability != FOCUS_BLOCK_DESCENDANTS) {
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (child.visibility == VISIBLE) {
                    val ii = infoForChild(child)
                    if (ii != null && ii.position == mCurItem) {
                        child.addFocusables(views, direction, focusableMode)
                    }
                }
            }
        }

        // we add ourselves (if focusable) in all cases except for when we are
        // FOCUS_AFTER_DESCENDANTS and there are some descendants focusable.  this is
        // to avoid the focus search finding layouts when a more precise search
        // among the focusable children would be more interesting.
        if (descendantFocusability != FOCUS_AFTER_DESCENDANTS || focusableCount == views.size) {
            // Note that we can't call the superclass here, because it will
            // add all views in.  So we need to do the same thing View does.
            if (!isFocusable) {
                return
            }
            if (focusableMode and FOCUSABLES_TOUCH_MODE == FOCUSABLES_TOUCH_MODE &&
                isInTouchMode && !isFocusableInTouchMode
            ) {
                return
            }
            if (views != null) {
                views.add(this)
            }
        }
    }

    /**
     * We only want the current page that is being shown to be touchable.
     */
    override fun addTouchables(views: ArrayList<View>) {
        // Note that we don't call super.addTouchables(), which means that
        // we don't call View.addTouchables().  This is okay because a ViewPager
        // is itself not touchable.
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == VISIBLE) {
                val ii = infoForChild(child)
                if (ii != null && ii.position == mCurItem) {
                    child.addTouchables(views)
                }
            }
        }
    }

    /**
     * We only want the current page that is being shown to be focusable.
     */
    override fun onRequestFocusInDescendants(
        direction: Int,
        previouslyFocusedRect: Rect
    ): Boolean {
        val index: Int
        val increment: Int
        val end: Int
        val count = childCount
        if (direction and FOCUS_FORWARD != 0) {
            index = 0
            increment = 1
            end = count
        } else {
            index = count - 1
            increment = -1
            end = -1
        }
        var i = index
        while (i != end) {
            val child = getChildAt(i)
            if (child.visibility == VISIBLE) {
                val ii = infoForChild(child)
                if (ii != null && ii.position == mCurItem) {
                    if (child.requestFocus(direction, previouslyFocusedRect)) {
                        return true
                    }
                }
            }
            i += increment
        }
        return false
    }

    override fun dispatchPopulateAccessibilityEvent(event: AccessibilityEvent): Boolean {
        // ViewPagers should only report accessibility info for the current page,
        // otherwise things get very confusing.

        // TODO: Should this note something about the paging container?
        val childCount = childCount
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == VISIBLE) {
                val ii = infoForChild(child)
                if (ii != null && ii.position == mCurItem &&
                    child.dispatchPopulateAccessibilityEvent(event)
                ) {
                    return true
                }
            }
        }
        return false
    }

    companion object {
        private const val TAG = "ViewPager"
        private const val DEBUG = false
        private const val USE_CACHE = false
        private const val DEFAULT_OFFSCREEN_PAGES = 0
        private const val MAX_SETTLE_DURATION = 600 // ms
        private val COMPARATOR = Comparator<ItemInfo> { lhs, rhs -> lhs.position - rhs.position }
        private val sInterpolator =
            Interpolator { t -> // _o(t) = t * t * ((tension + 1) * t + tension)
                // o(t) = _o(t - 1) + 1
                var t = t
                t -= 1.0f
                t * t * t + 1.0f
            }

        /**
         * Sentinel value for no current active pointer.
         * Used by [.mActivePointerId].
         */
        private const val INVALID_POINTER = -1

        /**
         * Indicates that the pager is in an idle, settled state. The current page
         * is fully in view and no animation is in progress.
         */
        const val SCROLL_STATE_IDLE = 0

        /**
         * Indicates that the pager is currently being dragged by the user.
         */
        const val SCROLL_STATE_DRAGGING = 1

        /**
         * Indicates that the pager is in the process of settling to a final position.
         */
        const val SCROLL_STATE_SETTLING = 2
    }
}
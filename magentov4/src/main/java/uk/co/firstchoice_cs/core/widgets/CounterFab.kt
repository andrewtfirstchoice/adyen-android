package uk.co.firstchoice_cs.core.widgets

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Property
import android.view.View
import android.view.animation.Interpolator
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.stateful.ExtendableSavedState
import uk.co.firstchoice_cs.firstchoice.R

class CounterFab(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : FloatingActionButton(context, attrs, defStyleAttr) {
    private val mContentBounds: Rect
    private val mTextPaint: Paint
    private val mTextSize: Float
    private val mCirclePaint: Paint
    private var mCircleBounds: Rect? = null
    private val mMaskPaint: Paint
    private val mAnimationDuration: Int
    private val mTextHeight: Float
    private var mAnimationFactor: Float
    private val animationProperty: Property<CounterFab?, Float> = object : Property<CounterFab?, Float>(Float::class.java, "animation") {
        override fun set(`object`: CounterFab?, value: Float) {
            mAnimationFactor = value
            postInvalidateOnAnimation()
        }

        override fun get(`object`: CounterFab?): Float {
            return 0f
        }
    }
    private var mCount = 0
    private var mText: String? = null
    private var mAnimator: ObjectAnimator? = null
    private var badgePosition = RIGHT_TOP_POSITION

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null) : this(context, attrs, 0) {
    }

    private val fadeAnim: ObjectAnimator? = null
    private var fadingOut = false
    private var fadingIn = false
    private fun cancelAnimations() {
        fadingIn = false
        fadingOut = false
        if (fadeAnim != null && fadeAnim.isStarted) fadeAnim.cancel()
    }

    fun restoreAlpha() {
        cancelAnimations()
        alpha = 1.0f
        visibility = View.VISIBLE
        isClickable = true
    }



    private fun setupFromStyledAttributes(context: Context, attrs: AttributeSet?) {
        val styledAttributes = context.theme
                .obtainStyledAttributes(attrs, R.styleable.CounterFab, 0, 0)
        mTextPaint.color = styledAttributes.getColor(R.styleable.CounterFab_badgeTextColor, Color.WHITE)
        mCirclePaint.color = ContextCompat.getColor(context,R.color.fcRed)
        badgePosition = styledAttributes.getInt(R.styleable.CounterFab_badgePosition, RIGHT_TOP_POSITION)
        styledAttributes.recycle()
    }

    private val isSizeMini: Boolean
        get() = size == SIZE_MINI

    /**
     * @return The current count value
     */
    /**
     * Set the count to show on badge
     *
     * @param count The count value starting from 0
     */
    var count: Int
        get() = mCount
        set(count) {
            if (count == mCount) return
            mCount = if (count > 0) count else 0
            onCountChanged()
            if (ViewCompat.isLaidOut(this)) {
                startAnimation()
            }
        }

    /**
     * Increase the current count value by 1
     */
    fun increase() {
        count = mCount + 1
    }

    /**
     * Decrease the current count value by 1
     */
    fun decrease() {
        count = if (mCount > 0) mCount - 1 else 0
    }

    private fun onCountChanged() {
        mText = if (isSizeMini) {
            if (mCount > MINI_MAX_COUNT) {
                MINI_MAX_COUNT_TEXT
            } else {
                mCount.toString()
            }
        } else {
            if (mCount > NORMAL_MAX_COUNT) {
                NORMAL_MAX_COUNT_TEXT
            } else {
                mCount.toString()
            }
        }
    }

    private fun startAnimation() {
        var start = 0f
        var end = 1f
        if (mCount == 0) {
            start = 1f
            end = 0f
        }
        if (isAnimating) {
            mAnimator!!.cancel()
        }
        mAnimator = ObjectAnimator.ofObject(this, animationProperty, null, start, end)
        mAnimator!!.interpolator = ANIMATION_INTERPOLATOR
        mAnimator!!.duration = mAnimationDuration.toLong()
        mAnimator!!.start()
    }

    private val isAnimating: Boolean
        get() = mAnimator != null && mAnimator!!.isRunning

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mCount > 0 || isAnimating) {
            if (getContentRect(mContentBounds)) {
                val newLeft: Int
                val newTop: Int
                when (badgePosition) {
                    LEFT_BOTTOM_POSITION -> {
                        newLeft = mContentBounds.left
                        newTop = mContentBounds.bottom - mCircleBounds!!.height()
                    }
                    LEFT_TOP_POSITION -> {
                        newLeft = mContentBounds.left
                        newTop = mContentBounds.top
                    }
                    RIGHT_BOTTOM_POSITION -> {
                        newLeft = mContentBounds.left + mContentBounds.width() - mCircleBounds!!.width()
                        newTop = mContentBounds.bottom - mCircleBounds!!.height()
                    }
                    RIGHT_TOP_POSITION -> {
                        newLeft = mContentBounds.left + mContentBounds.width() - mCircleBounds!!.width()
                        newTop = mContentBounds.top
                    }
                    else -> {
                        newLeft = mContentBounds.left + mContentBounds.width() - mCircleBounds!!.width()
                        newTop = mContentBounds.top
                    }
                }
                mCircleBounds!!.offsetTo(newLeft, newTop)
            }
            val cx = mCircleBounds!!.centerX().toFloat()
            val cy = mCircleBounds!!.centerY().toFloat()
            val radius = mCircleBounds!!.width() / 2f * mAnimationFactor
            // Solid circle
            canvas.drawCircle(cx, cy, radius, mCirclePaint)
            // Mask circle
            canvas.drawCircle(cx, cy, radius, mMaskPaint)
            // Count text
            mTextPaint.textSize = mTextSize * mAnimationFactor
            canvas.drawText(mText!!, cx, cy + mTextHeight / 2f, mTextPaint)
        }
    }

    public override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        val state = ExtendableSavedState(superState)
        val bundle = Bundle()
        bundle.putInt(COUNT_STATE, mCount)
        state.extendableStates.put(STATE_KEY, bundle)
        return state
    }

    public override fun onRestoreInstanceState(state: Parcelable) {
        if (state !is ExtendableSavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        super.onRestoreInstanceState(state.superState)
        val bundle = state.extendableStates[STATE_KEY]
        count = bundle!!.getInt(COUNT_STATE)
        requestLayout()
    }

    companion object {
        private val STATE_KEY = CounterFab::class.java.name + ".STATE"
        private const val COUNT_STATE = "COUNT"
        private const val NORMAL_MAX_COUNT = 99
        private const val NORMAL_MAX_COUNT_TEXT = "99+"
        private const val MINI_MAX_COUNT = 9
        private const val MINI_MAX_COUNT_TEXT = "9+"
        private const val TEXT_SIZE_DP = 11
        private const val TEXT_PADDING_DP = 2
        private val MASK_COLOR = Color.parseColor("#33000000") // Translucent black as mask color
        private val ANIMATION_INTERPOLATOR: Interpolator = OvershootInterpolator()
        private const val RIGHT_TOP_POSITION = 0
        private const val LEFT_BOTTOM_POSITION = 1
        private const val LEFT_TOP_POSITION = 2
        private const val RIGHT_BOTTOM_POSITION = 3
    }

    init {
        val density = resources.displayMetrics.density
        mTextSize = TEXT_SIZE_DP * density
        val textPadding = TEXT_PADDING_DP * density
        mAnimationDuration = resources.getInteger(android.R.integer.config_shortAnimTime)
        mAnimationFactor = 1f
        mTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mTextPaint.style = Paint.Style.FILL_AND_STROKE
        mTextPaint.textSize = mTextSize
        mTextPaint.textAlign = Paint.Align.CENTER
        mTextPaint.typeface = Typeface.SANS_SERIF
        mCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mCirclePaint.style = Paint.Style.FILL
        setupFromStyledAttributes(context, attrs)
        mMaskPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mMaskPaint.style = Paint.Style.FILL
        mMaskPaint.color = MASK_COLOR
        val textBounds = Rect()
        mTextPaint.getTextBounds(NORMAL_MAX_COUNT_TEXT, 0, NORMAL_MAX_COUNT_TEXT.length, textBounds)
        mTextHeight = textBounds.height().toFloat()
        val textWidth = mTextPaint.measureText(NORMAL_MAX_COUNT_TEXT)
        val circleRadius = Math.max(textWidth, mTextHeight) / 2f + textPadding
        val circleEnd = (circleRadius * 2).toInt()
        mCircleBounds = if (isSizeMini) {
            val circleStart = (circleRadius / 2).toInt()
            Rect(circleStart, circleStart, circleEnd, circleEnd)
        } else {
            val circleStart = 0
            Rect(circleStart, circleStart, (circleRadius * 2).toInt(), (circleRadius * 2).toInt())
        }
        mContentBounds = Rect()
        onCountChanged()
    }
}
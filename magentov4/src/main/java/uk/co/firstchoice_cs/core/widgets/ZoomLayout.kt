package uk.co.firstchoice_cs.core.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.OnScaleGestureListener
import android.view.View
import android.widget.FrameLayout
import org.slf4j.LoggerFactory
import kotlin.math.sign

private enum class Mode {
    NONE, DRAG, ZOOM
}

class ZoomLayout : FrameLayout, OnScaleGestureListener {
    private var mode = Mode.NONE
    private var scale = 1.0f
    private var lastScaleFactor = 0f
    private var startX = 0f
    private var startY = 0f
    private var dx = 0f
    private var dy = 0f
    private var prevDx = 0f
    private var prevDy = 0f
    private var zoomListener: OnZoomGestureListener? = null

    constructor(context: Context) : super(context) {
        if(!isInEditMode)
            init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        if(!isInEditMode)
            init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        if(!isInEditMode)
            init(context)
    }

    fun reset() {
        mode = Mode.NONE
        scale = 1.0f
        lastScaleFactor = 0f
        dx = 0f
        dy = 0f
        applyScaleAndTranslation()
    }

    fun set(scale: Float) {
        mode = Mode.NONE
        this.scale = scale
        lastScaleFactor = 0f
        dx = 0f
        dy = 0f
        applyScaleAndTranslation()
    }

    fun setOnZoomGestureListener(zoomListener: OnZoomGestureListener?) {
        this.zoomListener = zoomListener
    }

    private fun init(context: Context) {
        val scaleDetector = ScaleGestureDetector(context, this)
        val gestureDetector = GestureDetector(context, object : SimpleOnGestureListener() {
            override fun onDoubleTap(motionEvent: MotionEvent): Boolean {
                reset()
                if (zoomListener != null) {
                    zoomListener!!.onDoubleTap(motionEvent)
                }
                return true
            }

            override fun onSingleTapConfirmed(motionEvent: MotionEvent): Boolean {
                if (zoomListener != null) {
                    zoomListener!!.onSingleTap(motionEvent)
                }
                return true
            }
        })
        setOnTouchListener { view: View?, motionEvent: MotionEvent ->
            when (motionEvent.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    log.info("DOWN")
                    if (scale > MIN_ZOOM) {
                        mode = Mode.DRAG
                        startX = motionEvent.x - prevDx
                        startY = motionEvent.y - prevDy
                    }
                }
                MotionEvent.ACTION_MOVE -> if (mode == Mode.DRAG) {
                    dx = motionEvent.x - startX
                    dy = motionEvent.y - startY
                }
                MotionEvent.ACTION_POINTER_DOWN -> mode = Mode.ZOOM
                MotionEvent.ACTION_POINTER_UP -> {
                }
                MotionEvent.ACTION_UP -> {
                    log.info("UP")
                    mode = Mode.NONE
                    prevDx = dx
                    prevDy = dy
                    log.info("DX: {}, DY: {}", dx, dy)
                    view?.performClick()
                }
            }
            scaleDetector.onTouchEvent(motionEvent)
            gestureDetector.onTouchEvent(motionEvent)
            if (mode == Mode.DRAG && scale >= MIN_ZOOM || mode == Mode.ZOOM) {
                parent.requestDisallowInterceptTouchEvent(true)
                val maxDx = (child().width - child().width / scale) / 2 * scale
                val maxDy = (child().height - child().height / scale) / 2 * scale
                dx = dx.coerceAtLeast(-maxDx).coerceAtMost(maxDx)
                dy = dy.coerceAtLeast(-maxDy).coerceAtMost(maxDy)
                applyScaleAndTranslation()
            }
            true
        }
    }

    override fun onScaleBegin(scaleDetector: ScaleGestureDetector): Boolean {
        log.info("onScaleBegin")
        if (zoomListener != null) {
            zoomListener!!.onScaleStart()
        }
        return true
    }

    override fun onScale(scaleDetector: ScaleGestureDetector): Boolean {
        val scaleFactor = scaleDetector.scaleFactor
        log.info("onScale {}", scaleFactor)
        if (lastScaleFactor == 0f || sign(scaleFactor) == Math.signum(lastScaleFactor)) {
            scale *= scaleFactor
            scale = MIN_ZOOM.coerceAtLeast(scale.coerceAtMost(MAX_ZOOM))
            lastScaleFactor = scaleFactor
        } else {
            lastScaleFactor = 0f
        }
        return true
    }

    override fun onScaleEnd(scaleDetector: ScaleGestureDetector) {
        log.info("onScaleEnd")
        if (scale == 1.0f) {
            log.info("onScaleEnd Scale is zero")
            if (zoomListener != null) {
                zoomListener!!.onScaleEndZero()
            }
        }
    }

    private fun applyScaleAndTranslation() {
        child().scaleX = scale
        child().scaleY = scale
        child().translationX = dx
        child().translationY = dy
    }

    fun applyScaleAndTranslation(scale: Float, x: Float, y: Float) {
        child().scaleX = scale
        child().scaleY = scale
        child().translationX = x
        child().translationY = y
    }

    private fun child(): View {
        return getChildAt(0)
    }

    interface OnZoomGestureListener {
        fun onSingleTap(motionEvent: MotionEvent?)
        fun onDoubleTap(motionEvent: MotionEvent?)
        fun onScaleStart()
        fun onScaleEndZero()
    }

    companion object {
        private val log = LoggerFactory.getLogger(ZoomLayout::class.java)
        private const val MIN_ZOOM = 1.0f
        private const val MAX_ZOOM = 4.0f
    }
}
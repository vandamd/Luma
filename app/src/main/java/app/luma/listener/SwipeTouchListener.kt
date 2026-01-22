package app.luma.listener

import android.content.Context
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import app.luma.data.Constants
import java.util.*
import kotlin.concurrent.schedule
import kotlin.math.abs

/**
 * Unified swipe, tap, and long press touch listener.
 *
 * @param context The context
 * @param view Optional view reference for view-specific callbacks (onClick, onLongClick with view)
 * @param enableTripleTap Whether to enable triple-tap detection (adds slight delay to double-tap)
 * @param enableDelayedLongPress Whether to use delayed long press (500ms after system long press)
 */
internal open class SwipeTouchListener(
    context: Context?,
    private val view: View? = null,
    private val enableTripleTap: Boolean = false,
    private val enableDelayedLongPress: Boolean = false
) : OnTouchListener {

    private var longPressOn = false
    private var doubleTapOn = false
    private val gestureDetector: GestureDetector

    override fun onTouch(v: View, motionEvent: MotionEvent): Boolean {
        // Manage pressed state if we have a view reference
        if (view != null) {
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> view.isPressed = true
                MotionEvent.ACTION_UP -> view.isPressed = false
            }
        }

        if (motionEvent.action == MotionEvent.ACTION_UP) {
            longPressOn = false
        }

        return gestureDetector.onTouchEvent(motionEvent)
    }

    private inner class GestureListener : SimpleOnGestureListener() {
        private val SWIPE_THRESHOLD: Int = 100
        private val SWIPE_VELOCITY_THRESHOLD: Int = 100

        override fun onDown(e: MotionEvent): Boolean = true

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            if (enableTripleTap && doubleTapOn) {
                doubleTapOn = false
                onTripleClick()
            } else if (view != null) {
                onClick(view)
            }
            return super.onSingleTapUp(e)
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (enableTripleTap) {
                doubleTapOn = true
                Timer().schedule(Constants.TRIPLE_TAP_DELAY_MS.toLong()) {
                    if (doubleTapOn) {
                        doubleTapOn = false
                        onDoubleClick()
                    }
                }
            } else {
                onDoubleClick()
            }
            return super.onDoubleTap(e)
        }

        override fun onLongPress(e: MotionEvent) {
            if (enableDelayedLongPress) {
                longPressOn = true
                Timer().schedule(Constants.LONG_PRESS_DELAY_MS.toLong()) {
                    if (longPressOn) {
                        if (view != null) onLongClick(view) else onLongClick()
                    }
                }
            } else {
                if (view != null) onLongClick(view) else onLongClick()
            }
            super.onLongPress(e)
        }

        override fun onFling(
            event1: MotionEvent,
            event2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            try {
                val diffY = event2.y - event1.y
                val diffX = event2.x - event1.x
                if (abs(diffX) > abs(diffY)) {
                    if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) onSwipeRight() else onSwipeLeft()
                    }
                } else {
                    if (abs(diffY) > SWIPE_THRESHOLD && abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY < 0) onSwipeUp() else onSwipeDown()
                    }
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
            return false
        }
    }

    // Swipe callbacks
    open fun onSwipeRight() {}
    open fun onSwipeLeft() {}
    open fun onSwipeUp() {}
    open fun onSwipeDown() {}

    // Click callbacks (without view - for screen-level gestures)
    open fun onLongClick() {}
    open fun onDoubleClick() {}
    open fun onTripleClick() {}

    // Click callbacks (with view - for view-level gestures)
    open fun onLongClick(view: View) {}
    open fun onClick(view: View) {}

    init {
        gestureDetector = GestureDetector(context, GestureListener())
    }
}

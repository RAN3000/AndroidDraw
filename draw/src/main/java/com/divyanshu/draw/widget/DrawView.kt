package com.divyanshu.draw.widget

import android.content.Context
import android.graphics.*
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.util.*

class DrawView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private var mPaths = LinkedHashMap<MyPath, PaintOptions>()

    private var mLastPaths = LinkedHashMap<MyPath, PaintOptions>()
    private var mUndonePaths = LinkedHashMap<MyPath, PaintOptions>()

    private var mPaint = Paint()
    private var mPath = MyPath()
    private var mPaintOptions = PaintOptions()

    private var mCurX = 0f
    private var mCurY = 0f
    private var mStartX = 0f
    private var mStartY = 0f
    private var mIsSaving = false
    private var mIsStrokeWidthBarEnabled = false

    var isEraserOn = false
        private set

    init {
        mPaint.apply {
            color = mPaintOptions.color
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = mPaintOptions.strokeWidth
            isAntiAlias = true
        }
    }

    fun undo() {
        synchronized(mPaths) {
            if (mPaths.isEmpty() && mLastPaths.isNotEmpty()) {
                mPaths = mLastPaths.clone() as LinkedHashMap<MyPath, PaintOptions>
                mLastPaths.clear()
                invalidate()
                return
            }
            if (mPaths.isEmpty()) {
                return
            }
            val lastPath = mPaths.values.lastOrNull()
            val lastKey = mPaths.keys.lastOrNull()

            mPaths.remove(lastKey)
            if (lastPath != null && lastKey != null) {
                mUndonePaths[lastKey] = lastPath
            }
        }
        invalidate()
    }

    fun redo() {
        if (mUndonePaths.keys.isEmpty()) {
            return
        }

        val lastKey = mUndonePaths.keys.last()
        addPath(lastKey, mUndonePaths.values.last())
        mUndonePaths.remove(lastKey)
        invalidate()
    }

    fun setColor(newColor: Int) {
        @ColorInt
        val alphaColor = ColorUtils.setAlphaComponent(newColor, mPaintOptions.alpha)
        mPaintOptions.color = alphaColor
        if (mIsStrokeWidthBarEnabled) {
            invalidate()
        }
    }

    fun setAlpha(newAlpha: Int) {
        val alpha = (newAlpha*255)/100
        mPaintOptions.alpha = alpha
        setColor(mPaintOptions.color)
    }

    fun setStrokeWidth(newStrokeWidth: Float) {
        mPaintOptions.strokeWidth = newStrokeWidth
        if (mIsStrokeWidthBarEnabled) {
            invalidate()
        }
    }

    fun getBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        mIsSaving = true
        draw(canvas)
        mIsSaving = false
        return bitmap
    }

    fun addPath(path: MyPath, options: PaintOptions) {
        synchronized(mPaths) {
            mPaths[path] = options
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        synchronized(mPaths) {
            for ((key, value) in mPaths) {
                changePaint(value)
                canvas.drawPath(key, mPaint)
            }
        }

        changePaint(mPaintOptions)
        canvas.drawPath(mPath, mPaint)
    }

    private fun changePaint(paintOptions: PaintOptions) {
        mPaint.color = if (paintOptions.isEraserOn) Color.WHITE else paintOptions.color
        mPaint.strokeWidth = paintOptions.strokeWidth
    }

    fun clearCanvas() {
        mLastPaths = mPaths.clone() as LinkedHashMap<MyPath, PaintOptions>
        mPath.reset()
        synchronized(mPaths) {
            mPaths.clear()
        }
        invalidate()
    }

    private fun actionDown(x: Float, y: Float) {
        mPath.reset()
        mPath.moveTo(x, y)
        mCurX = x
        mCurY = y
    }

    private fun actionMove(x: Float, y: Float) {
        mPath.quadTo(mCurX, mCurY, (x + mCurX) / 2, (y + mCurY) / 2)
        mCurX = x
        mCurY = y
    }

    private fun actionUp() {
        mPath.lineTo(mCurX, mCurY)

        // draw a dot on click
        if (mStartX == mCurX && mStartY == mCurY) {
            mPath.lineTo(mCurX, mCurY + 2)
            mPath.lineTo(mCurX + 1, mCurY + 2)
            mPath.lineTo(mCurX + 1, mCurY)
        }

        synchronized(mPaths) {
            mPaths[mPath] = mPaintOptions
        }
        mPath = MyPath()
        mPaintOptions = PaintOptions(mPaintOptions.color, mPaintOptions.strokeWidth, mPaintOptions.alpha, mPaintOptions.isEraserOn)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mStartX = x
                mStartY = y
                actionDown(x, y)
                mUndonePaths.clear()
            }
            MotionEvent.ACTION_MOVE -> actionMove(x, y)
            MotionEvent.ACTION_UP -> actionUp()
        }

        invalidate()
        return true
    }

    fun toggleEraser() {
        isEraserOn = !isEraserOn
        mPaintOptions.isEraserOn = isEraserOn
        invalidate()
    }

}
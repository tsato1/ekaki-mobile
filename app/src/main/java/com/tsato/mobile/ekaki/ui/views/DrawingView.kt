package com.tsato.mobile.ekaki.ui.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.MotionEvent.*
import android.view.View
import com.tsato.mobile.ekaki.util.Constants
import java.util.*
import kotlin.math.abs

class DrawingView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null
) : View(context, attributeSet) {

    private var viewWidth: Int? = null
    private var viewHeight: Int? = null

    private var bitmap: Bitmap? = null
    private var canvas: Canvas? = null

    private var currX: Float? = null
    private var currY: Float? = null

    var smoothness = 5
    var isDrawing = false

    private var paint = Paint(Paint.DITHER_FLAG).apply {
        isDither = true
        isAntiAlias = true
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = Constants.DEFAULT_PAINT_THICKNESS
    }

    private var path = Path()
    private var paths = Stack<PathData>()
    private var pathDataChangedListener: ((Stack<PathData>) -> Unit)? = null

    fun setPathDataChangedListener(listener: (Stack<PathData>) -> Unit) {
        pathDataChangedListener = listener
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        viewWidth = w
        viewHeight = h
        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(bitmap!!)
    }

    /*
        called whenever the android system thinks that it's necessary to draw (could be many times a second)
     */
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        val initialColor = paint.color
        val initialThickness = paint.strokeWidth

        // draws all the paths in stack
        for (pathData in paths) {
            paint.apply {
                color = pathData.color
                strokeWidth = pathData.thickness
            }
            canvas?.drawPath(pathData.path, paint)
        }

        // draws what the player is currently drawing
        paint.apply {
            color = initialColor
            strokeWidth = initialThickness
        }
        canvas?.drawPath(path, paint)
    }

    /*
        called when the player touches the screen for the first time
     */
    private fun startedTouch(x: Float, y: Float) {
        path.reset()
        path.moveTo(x, y)
        currX = x
        currY = y
        invalidate() // triggers onDraw()
    }

    private fun movedTouch(toX: Float, toY: Float) {
        val deltaX = abs(toX - (currX ?: return))
        val deltaY = abs(toY - (currY ?: return))

        // drawing Bézier Curve
        // draw only if the diff is large enough. Larger the diff is the smoother the line will be *
        if (deltaX >= smoothness || deltaY >= smoothness) {
            isDrawing = true
            path.quadTo(currX!!, currY!!, (currX!! + toX) / 2f, (currY!! + toY) / 2f) // *

            currX = toX
            currY = toY
            invalidate()
        }
    }

    private fun releaseTouch() {
        isDrawing = false
        path.lineTo(currX ?: return, currY ?: return)
        paths.push(PathData(path, paint.color, paint.strokeWidth))
        pathDataChangedListener?.let { change ->
            change(paths)
        }
        path = Path()
        invalidate()
    }

    /*
        called when any kind of touch event happens
     */
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (!isEnabled) {
            return false
        }

        // disable the view if the view is not for the drawing player
        val newX = event?.x
        val newY = event?.y

        when (event?.action) {
            ACTION_DOWN -> startedTouch(newX ?: return false, newY ?: return false)
            ACTION_MOVE -> movedTouch(newX ?: return false, newY ?: return false)
            ACTION_UP -> releaseTouch()
        }

        return true //super.onTouchEvent(event) //no need this
    }

    fun setThickness(thickness: Float) {
        paint.strokeWidth = thickness
    }

    fun setColor(color: Int) {
        paint.color = color
    }

    fun clear() {
        canvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.MULTIPLY) // draws white on surface
        paths.clear()
    }

    data class PathData (val path: Path, val color: Int, val thickness: Float)
}
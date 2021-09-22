package com.tsato.mobile.ekaki.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatRadioButton
import com.tsato.mobile.ekaki.R
import kotlin.math.min
import kotlin.properties.Delegates

/*
JvmOverloads provides different constructors for this radioButton class.
It is one way of specifying a constructor
 */
class ColorRadioButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null // when using xml layout, use this
): AppCompatRadioButton(context, attrs) {

    private var buttonColor by Delegates.notNull<Int>() // lateinit var for primitive type
    private var radius = 25f

    private var viewWidth by Delegates.notNull<Int>()
    private var viewHeight by Delegates.notNull<Int>()

    private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val selectionPaint = Paint(Paint.ANTI_ALIAS_FLAG) // Paint for the stroke of the button

    init {
        // get the reference to the attributes in xml
        context.theme.obtainStyledAttributes(attrs, R.styleable.ColorRadioButton, 0, 0).apply {
            try {
                buttonColor = getColor(R.styleable.ColorRadioButton_buttonColor, Color.BLACK)
            }
            finally {
                recycle()
            }

            buttonPaint.apply {
                color = buttonColor
                style = Paint.Style.FILL
            }

            selectionPaint.apply {
                color = Color.BLACK // border around the selected button
                style = Paint.Style.STROKE
                strokeWidth = 12f
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        viewWidth = w
        viewHeight = h
        radius = min(w, h) / 2 * 0.8f
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.drawCircle(viewWidth / 2f, viewHeight / 2f, radius, buttonPaint)

        if (isChecked) {
            canvas?.drawCircle(viewWidth / 2f, viewHeight / 2f, radius * 1.1f, selectionPaint)
        }
    }
}
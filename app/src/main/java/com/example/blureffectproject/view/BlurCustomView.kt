package com.example.blureffectproject

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class BlurCustomView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var path = Path()
    private val paint = Paint()
    private val pathList = mutableListOf<Path>()

    private var eraserMode = false
    private var drawBitmap: Bitmap? = null
    private val drawCanvas: Canvas by lazy { Canvas(drawBitmap!!) }

    init {
        paint.isAntiAlias = true
        paint.strokeWidth = 50f
        paint.style = Paint.Style.STROKE
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND
    }

    fun setBitmap(bitmap: Bitmap) {
        drawBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        invalidate()
    }

    fun setEraserMode(eraser: Boolean) {
        eraserMode = eraser
        paint.xfermode = if (eraserMode) PorterDuffXfermode(PorterDuff.Mode.CLEAR) else null
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawBitmap?.let { canvas?.drawBitmap(it, 0f, 0f, null) }
        canvas?.drawPath(path, paint)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return false

        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                path.moveTo(x, y)
            }
            MotionEvent.ACTION_MOVE -> {
                path.lineTo(x, y)
                drawCanvas.drawPath(path, paint)
                pathList.add(Path(path))
            }
            MotionEvent.ACTION_UP -> {
                path.reset()
            }
        }

        invalidate()
        return true
    }

    fun getEditedBitmap(): Bitmap? {
        return drawBitmap
    }
}

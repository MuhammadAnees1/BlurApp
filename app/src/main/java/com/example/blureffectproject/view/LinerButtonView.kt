package com.example.blureffectproject.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Region
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.blureffectproject.utils.BlurUtils
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

class LinerButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    // Paints
    private val solidLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }

    private val dottedLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 4f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(20f, 10f), 0f)
    }
    private var originalBitmap: Bitmap? = null

    private val blurPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var blurredBitmap: Bitmap? = null
    private var bitmapShader: BitmapShader? = null
    private val shaderMatrix = Matrix()

    private var rotationAngle = 0f
    private var lastRotation = 0f
    private var initialRotation = 0f

    private var scaleFactor = 1f
    private val minScale = 0.5f
    private val maxScale = 5f
    private var lastDistance = 0f

    // Frame control
    private var frameOffsetX = 0f
    private var frameOffsetY = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    private var isDraggingFrame = false
    private var isRotating = false

    private var bottomLineOffsetX = 0f

    private var translationX = 0f
    private var translationY = 0f
    private var rotationDegrees = 0f

    // New logic variables
    private var areLinesVisible = true
    private val handler = Handler(Looper.getMainLooper())
    private val hideLinesRunnable = Runnable {
        areLinesVisible = false
        invalidate()
    }

    init {
        // Start hiding lines after 2 seconds initially
        startHideLinesTimer()
    }

    fun setBlurredBitmap(bitmap: Bitmap) {
        blurredBitmap = bitmap
        bitmapShader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        blurPaint.shader = bitmapShader
        invalidate()
    }

    fun setImageBitmap(bitmap: Bitmap) {
        originalBitmap = bitmap
        blurredBitmap = BlurUtils.applyLinearBlur(context, bitmap)
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {

            MotionEvent.ACTION_DOWN -> {
                isDraggingFrame = true
                lastTouchX = event.x
                lastTouchY = event.y

                showLinesImmediately() // Show lines as soon as you touch
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 2) {
                    isDraggingFrame = false
                    isRotating = true
                    initialRotation = calculateAngle(event)
                    lastRotation = rotationDegrees
                    lastDistance = calculateDistance(event)

                    showLinesImmediately() // Show lines as soon as you start rotating
                }
            }

            MotionEvent.ACTION_MOVE -> {
                // As long as you're moving, keep showing the lines.
                showLinesImmediately()

                if (isDraggingFrame && event.pointerCount == 1) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    translationX += dx
                    translationY += dy
                    lastTouchX = event.x
                    lastTouchY = event.y
                    invalidate()
                }

                if (isRotating && event.pointerCount == 2) {
                    val currentRotation = calculateAngle(event)
                    rotationDegrees = lastRotation + (currentRotation - initialRotation)

                    val currentDistance = calculateDistance(event)
                    val distanceDelta = currentDistance - lastDistance

                    scaleFactor += distanceDelta / 300f
                    scaleFactor = max(minScale, min(scaleFactor, maxScale))

                    lastDistance = currentDistance
                    invalidate()
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                if (event.pointerCount <= 1) {
                    isDraggingFrame = false
                    isRotating = false

                    startHideLinesTimer() // Only start hiding after you lift the fingers
                }
            }
        }

        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        originalBitmap?.let { original ->
            blurredBitmap?.let { blurred ->

                val scaleX = width.toFloat() / original.width
                val scaleY = height.toFloat() / original.height

                val centerX = width / 2f
                val centerY = height / 2f

                val halfGap = 200f / scaleFactor
                val topY = centerY - halfGap
                val bottomY = centerY + halfGap

                val extraGap = 80f
                val secondTopY = topY - extraGap
                val secondBottomY = bottomY + extraGap

                val originalMatrix = Matrix().apply {
                    setScale(scaleX, scaleY)
                }
                canvas.drawBitmap(original, originalMatrix, null)

                val saveLayer = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)

                val blurMatrix = Matrix().apply {
                    setScale(scaleX, scaleY)
                }
                canvas.drawBitmap(blurred, blurMatrix, blurPaint)

                val clearPath = Path()
                val infiniteLength = 10000f // Same as the lines

                val rect = RectF(-infiniteLength, topY, infiniteLength, bottomY)

                val matrix = Matrix().apply {
                    postTranslate(translationX, translationY)
                    postRotate(rotationDegrees, centerX + translationX, centerY + translationY)
                }

                clearPath.addRect(rect, Path.Direction.CW)
                clearPath.transform(matrix)

                blurPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)

                canvas.save()
                canvas.clipPath(clearPath, Region.Op.INTERSECT)
                canvas.drawPaint(blurPaint)
                canvas.restore()

                blurPaint.xfermode = null

                canvas.restoreToCount(saveLayer)

                if (areLinesVisible) {
                    canvas.save()

                    canvas.translate(translationX, translationY)
                    canvas.rotate(rotationDegrees, centerX, centerY)

                    val infiniteLength = 10000f

                    canvas.drawLine(-infiniteLength, topY, infiniteLength, topY, solidLinePaint)
                    canvas.drawLine(-infiniteLength, bottomY, infiniteLength, bottomY, solidLinePaint)

                    canvas.drawLine(-infiniteLength, secondTopY, infiniteLength, secondTopY, dottedLinePaint)
                    canvas.drawLine(-infiniteLength, secondBottomY, infiniteLength, secondBottomY, dottedLinePaint)

                    canvas.restore()
                }
            }
        }
    }

    private fun showLinesImmediately() {
        areLinesVisible = true
        invalidate()

        handler.removeCallbacks(hideLinesRunnable)
    }

    private fun startHideLinesTimer() {
        handler.removeCallbacks(hideLinesRunnable)
        handler.postDelayed(hideLinesRunnable, 2000)
    }

    private fun calculateAngle(event: MotionEvent): Float {
        return if (event.pointerCount >= 2) {
            val dx = event.getX(1) - event.getX(0)
            val dy = event.getY(1) - event.getY(0)
            Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
        } else 0f
    }

    private fun calculateDistance(event: MotionEvent): Float {
        return if (event.pointerCount >= 2) {
            val dx = event.getX(1) - event.getX(0)
            val dy = event.getY(1) - event.getY(0)
            hypot(dx, dy)
        } else 0f
    }
}

package com.example.blureffectproject.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
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
    private var lastRotation = 0f
    private var initialRotation = 0f

    private var scaleFactor = 1f
    private val minScale = 0.5f
    private val maxScale = 5f
    private var lastDistance = 0f

    // Frame control
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    private var isDraggingFrame = false
    private var isRotating = false

    private var translationX = 0f
    private var translationY = 0f
    private var rotationDegrees = 0f
    var bottomLineOffset = 5f
    var topLineOffset = 5f

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

    fun setImageBitmap(bitmap: Bitmap) {
        originalBitmap = bitmap
        blurredBitmap = BlurUtils.applyLinearBlur(context, bitmap)
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {

            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y

                if (isTouchInsideFrame(event.x, event.y)) {
                    isDraggingFrame = true
                } else {
                    isDraggingFrame = false
                    showLinesImmediately()
                }
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 2) {
                    isDraggingFrame = false
                    isRotating = true
                    initialRotation = calculateAngle(event)
                    lastRotation = rotationDegrees
                    lastDistance = calculateDistance(event)

                    showLinesImmediately()
                }
            }

            MotionEvent.ACTION_MOVE -> {
                // Keep showing lines as long as user is interacting
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

                    // Adjust scale factor for pinch in/out
                    scaleFactor -= distanceDelta / 300f
                    scaleFactor = max(minScale, min(scaleFactor, maxScale))

                    // Move top lines left, bottom lines right when dragging in/out
                    adjustLinePositions(distanceDelta)

                    lastDistance = currentDistance
                    invalidate()
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                if (event.pointerCount <= 1) {
                    isDraggingFrame = false
                    isRotating = false
                    startHideLinesTimer() // Hide lines after some time if no interaction
                }
            }
        }

        return true
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        originalBitmap?.let { original ->
            blurredBitmap?.let { blurred ->

                val viewWidth = width.toFloat()
                val viewHeight = height.toFloat()

                val bitmapWidth = original.width.toFloat()
                val bitmapHeight = original.height.toFloat()

                // Maintain aspect ratio by taking the smaller scale
                val scale = minOf(viewWidth / bitmapWidth, viewHeight / bitmapHeight)

                val dx = (viewWidth - bitmapWidth * scale) / 2f
                val dy = (viewHeight - bitmapHeight * scale) / 2f

                val centerX = viewWidth / 2f
                val centerY = viewHeight / 2f

                val halfGap = 200f / scaleFactor
                val topY = centerY - halfGap
                val bottomY = centerY + halfGap

                val extraGap = 80f
                val secondTopY = topY - extraGap
                val secondBottomY = bottomY + extraGap

                // Draw original bitmap scaled and centered
                val originalMatrix = Matrix().apply {
                    setScale(scale, scale)
                    postTranslate(dx, dy)
                }
                canvas.drawBitmap(original, originalMatrix, null)

                val saveLayer = canvas.saveLayer(0f, 0f, viewWidth, viewHeight, null)

                // Draw blurred bitmap scaled and centered
                val blurMatrix = Matrix().apply {
                    setScale(scale, scale)
                    postTranslate(dx, dy)
                }
                canvas.drawBitmap(blurred, blurMatrix, blurPaint)

                val infiniteLength = 10000f
                val matrix = Matrix().apply {
                    postTranslate(translationX, translationY)
                    postRotate(rotationDegrees, centerX + translationX, centerY + translationY)
                }

                // 1️⃣ Apply blur effect outside of dotted lines
                val outerBlurPath = Path().apply {
                    addRect(RectF(-infiniteLength, secondTopY, infiniteLength, secondBottomY), Path.Direction.CW)
                    transform(matrix)
                }
                blurPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                canvas.drawPath(outerBlurPath, blurPaint)
                blurPaint.xfermode = null

                // 2️⃣ Apply shade effect between dotted and solid lines with transformation
                val topShadePath = Path().apply {
                    addRect(RectF(-infiniteLength, secondTopY, infiniteLength, topY), Path.Direction.CW)
                    transform(matrix)
                }

                val bottomShadePath = Path().apply {
                    addRect(RectF(-infiniteLength, bottomY, infiniteLength, secondBottomY), Path.Direction.CW)
                    transform(matrix)
                }

                val shaderMatrix = Matrix().apply {
                    postTranslate(translationX, translationY)
                    postRotate(rotationDegrees, centerX + translationX, centerY + translationY)
                }

                // Top Shade Paint (Fade Downwards)
                val topShadePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        0f, secondTopY, 0f, topY,
                        Color.BLACK, Color.TRANSPARENT,
                        Shader.TileMode.CLAMP
                    ).apply {
                        setLocalMatrix(shaderMatrix)
                    }
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                }
                canvas.drawPath(topShadePath, topShadePaint)

                // Bottom Shade Paint (Fade Upwards)
                val bottomShadePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        0f, bottomY, 0f, secondBottomY,
                        Color.TRANSPARENT, Color.BLACK,
                        Shader.TileMode.CLAMP
                    ).apply {
                        setLocalMatrix(shaderMatrix)
                    }
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                }
                canvas.drawPath(bottomShadePath, bottomShadePaint)

                // 3️⃣ Clear blur effect inside solid lines
                val clearPath = Path().apply {
                    addRect(RectF(-infiniteLength, topY, infiniteLength, bottomY), Path.Direction.CW)
                    transform(matrix)
                }
                blurPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                canvas.drawPath(clearPath, blurPaint)
                blurPaint.xfermode = null

                canvas.restoreToCount(saveLayer)

                // 4️⃣ Draw the lines
                if (areLinesVisible) {
                    canvas.save()
                    canvas.translate(translationX, translationY)
                    canvas.rotate(rotationDegrees, centerX, centerY)

                    canvas.drawLine(-infiniteLength + topLineOffset, topY, infiniteLength + topLineOffset, topY, solidLinePaint)
                    canvas.drawLine(-infiniteLength + topLineOffset, secondTopY, infiniteLength + topLineOffset, secondTopY, dottedLinePaint)

                    canvas.drawLine(-infiniteLength + bottomLineOffset, bottomY, infiniteLength + bottomLineOffset, bottomY, solidLinePaint)
                    canvas.drawLine(-infiniteLength + bottomLineOffset, secondBottomY, infiniteLength + bottomLineOffset, secondBottomY, dottedLinePaint)

                    canvas.restore()
                }
            }
        }
    }

    private fun isTouchInsideFrame(touchX: Float, touchY: Float): Boolean {
        val centerX = width / 2f + translationX
        val centerY = height / 2f + translationY

        val halfGap = 200f / scaleFactor
        val extraGap = 80f

        val topY = centerY - halfGap
        val bottomY = centerY + halfGap

        val secondTopY = topY - extraGap
        val secondBottomY = bottomY + extraGap

        return touchY in secondTopY..secondBottomY
    }

    private fun adjustLinePositions(distanceDelta: Float) {
        val movementFactor = distanceDelta / 10 // Adjust sensitivity
        topLineOffset -= movementFactor // Move top lines left
        bottomLineOffset += movementFactor // Move bottom lines right
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

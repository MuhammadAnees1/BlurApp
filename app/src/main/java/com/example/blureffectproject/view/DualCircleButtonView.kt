package com.example.blureffectproject.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.example.blureffectproject.utils.BlurUtils
import kotlin.math.hypot

class DualCircleButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 5f
        pathEffect = DashPathEffect(floatArrayOf(15f, 10f), 0f)
    }

    private val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    private var outerRotationAngle = 0f
    private var innerRotationAngle = 0f

    private var centerX = 500f
    private var centerY = 500f

    private var outerRadius = 200f
    private var innerRadius = outerRadius * 0.6f

    private var outerScaleFactor = 1f
    private var innerScaleFactor = 1f

    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var activePointerId = MotionEvent.INVALID_POINTER_ID

    private var initialDistance = 0f

    private val rotationSensitivity = 0.5f

    private var showCircle = true
    private var isDragging = false
    private var toggleBlur = false

    // Handler to manage the delayed hide
    private val handler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable {
        showCircle = false
        invalidate()
    }


    private var originalBitmap: Bitmap? = null
    private var blurredBitmap: Bitmap? = null
    private val blurPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        showCircleIndefinitely()
    }

    fun setBitmap(bitmap: Bitmap) {
        originalBitmap = bitmap
        blurredBitmap = BlurUtils.blurBitmap(context, bitmap, 20f)

        post {
            centerX = width / 2f
            centerY = height / 2f
            invalidate()
        }


        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        originalBitmap?.let { original ->
            blurredBitmap?.let { blurred ->
                if (!toggleBlur) {
                    Log.d("TAG", "onDraw123456: "+toggleBlur)
                    drawInnerCircleBlurEffect(canvas, original, blurred)
                } else {
                    Log.d("TAG", "onDraw123: "+toggleBlur)

                    drawBlurEffect(canvas, original, blurred)
                }
            }
        }

        if (showCircle) {
            // Draw the rotating circles
            canvas.save()
            canvas.translate(centerX, centerY)

            canvas.save()
            canvas.rotate(outerRotationAngle)
            canvas.drawCircle(0f, 0f, outerRadius * outerScaleFactor, outerPaint)
            canvas.restore()

            canvas.save()
            canvas.rotate(innerRotationAngle)
            canvas.drawCircle(0f, 0f, innerRadius * innerScaleFactor, innerPaint)
            canvas.restore()

            canvas.restore()
        }
    }

    private fun drawInnerCircleBlurEffect(canvas: Canvas, original: Bitmap, blurred: Bitmap) {
        val saveLayer = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)

        val destRect = calculateCenteredRectF(original)

        // Draw original image as background
        canvas.drawBitmap(original, null, destRect, null)

        val scaledInnerRadius = innerRadius * innerScaleFactor
        val scaledOuterRadius = outerRadius * outerScaleFactor

        // Clip to outer circle (where blur will be applied)
        val blurPath = Path().apply {
            addCircle(centerX, centerY, scaledOuterRadius, Path.Direction.CCW)
        }

        canvas.save()
        canvas.clipPath(blurPath)

        // Draw blurred image over the area
        canvas.drawBitmap(blurred, null, destRect, null)

        // Create gradient mask (transparent on outer edge)
        val gradient = RadialGradient(
            centerX,
            centerY,
            scaledOuterRadius,
            intArrayOf(
                Color.BLACK,       // Fully visible in center
                Color.TRANSPARENT  // Fully transparent at outer radius
            ),
            floatArrayOf(
                scaledInnerRadius / scaledOuterRadius, // Start fading after inner radius
                1f                                     // End fading at outer radius
            ),
            Shader.TileMode.CLAMP
        )

        // Paint to apply mask using DST_IN blending mode
        val gradientMaskPaint = Paint().apply {
            isAntiAlias = true
            shader = gradient
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }

        // Draw gradient mask to fade edges
        canvas.drawCircle(centerX, centerY, scaledOuterRadius, gradientMaskPaint)

        canvas.restore()

        canvas.restoreToCount(saveLayer)
    }

    private fun drawBlurEffect(canvas: Canvas, original: Bitmap, blurred: Bitmap) {
        val saveLayer = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)

        val destRect = calculateCenteredRectF(original)

        // Draw the blurred background first
        canvas.drawBitmap(blurred, null, destRect, blurPaint)

        // Create a radial gradient shader for the fade effect
        val scaledOuterRadius = outerRadius * outerScaleFactor
        val scaledInnerRadius = innerRadius * innerScaleFactor

        val gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            // Radial gradient from opaque at inner radius to transparent at outer radius
            shader = android.graphics.RadialGradient(
                centerX,
                centerY,
                scaledOuterRadius,
                intArrayOf(Color.TRANSPARENT, Color.BLACK),
                floatArrayOf(scaledInnerRadius / scaledOuterRadius, 1f),
                android.graphics.Shader.TileMode.CLAMP
            )
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) // Mask out the blur with gradient alpha
        }

        // Apply the gradient mask to the blur
        canvas.drawCircle(centerX, centerY, scaledOuterRadius, gradientPaint)

        // Draw the original image on top, inside the inner circle
        val clipPath = Path().apply {
            addCircle(centerX, centerY, scaledInnerRadius, Path.Direction.CCW)
        }

        canvas.save()
        canvas.clipPath(clipPath)

//        canvas.drawBitmap(original, null, destRect, null)

        canvas.restore()

        canvas.restoreToCount(saveLayer)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {

            MotionEvent.ACTION_DOWN -> {
                val touchX = event.x
                val touchY = event.y
                val distanceToCenter = distance(touchX, touchY, centerX, centerY)

                showCircleIndefinitely()

                // 👉 Check if tap is inside the INNER circle
                if (distanceToCenter <= innerRadius * innerScaleFactor) {
                    toggleBlur = !toggleBlur // Toggle blur mode!
                    invalidate()
                    Log.d("DualCircleButtonView", "toggleBlur: $toggleBlur")
                    return true // Return here, so it doesn't start dragging
                }

                // 👉 Otherwise, check for OUTER circle to enable dragging
                if (distanceToCenter <= outerRadius * outerScaleFactor) {
                    isDragging = true
                    lastTouchX = touchX
                    lastTouchY = touchY
                    activePointerId = event.getPointerId(0)
                } else {
                    isDragging = false
                    activePointerId = MotionEvent.INVALID_POINTER_ID
                }

                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (!showCircle) return false

                showCircleIndefinitely() // Always keep showing while interacting

                if (event.pointerCount == 1 && isDragging && activePointerId != MotionEvent.INVALID_POINTER_ID) {
                    val pointerIndex = event.findPointerIndex(activePointerId)
                    val x = event.getX(pointerIndex)
                    val y = event.getY(pointerIndex)

                    val dx = x - lastTouchX
                    val dy = y - lastTouchY

                    centerX += dx
                    centerY += dy

                    val distanceMoved = hypot(dx, dy)

                    outerRotationAngle -= distanceMoved * rotationSensitivity
                    innerRotationAngle += distanceMoved * rotationSensitivity

                    lastTouchX = x
                    lastTouchY = y

                    invalidate()
                    return true
                } else if (event.pointerCount == 2) {
                    val x1 = event.getX(0)
                    val y1 = event.getY(0)
                    val x2 = event.getX(1)
                    val y2 = event.getY(1)

                    val newDistance = distance(x1, y1, x2, y2)

                    if (initialDistance != 0f) {
                        val scaleChange = newDistance / initialDistance

                        outerScaleFactor *= scaleChange
                        innerScaleFactor *= Math.pow(scaleChange.toDouble(), 1.3).toFloat()

                        outerScaleFactor = outerScaleFactor.coerceIn(0.2f, 5f)
                        innerScaleFactor = innerScaleFactor.coerceIn(0.1f, 5f)

                        val distanceBetweenFingers = distance(x1 - x2, y1 - y2, 0f, 0f)

                        if (newDistance < initialDistance) {
                            innerRotationAngle += distanceBetweenFingers * rotationSensitivity
                        } else {
                            innerRotationAngle -= distanceBetweenFingers * rotationSensitivity
                        }
                        invalidate()
                    }

                    initialDistance = newDistance
                }
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (!showCircle) return false

                showCircleIndefinitely()

                if (event.pointerCount == 2) {
                    initialDistance = distance(
                        event.getX(0), event.getY(0),
                        event.getX(1), event.getY(1)
                    )
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                if (!showCircle) return false

                if (event.getPointerId(event.actionIndex) == activePointerId) {
                    val newPointerIndex = if (event.pointerCount > 1) 1 - event.actionIndex else -1
                    if (newPointerIndex != -1) {
                        lastTouchX = event.getX(newPointerIndex)
                        lastTouchY = event.getY(newPointerIndex)
                        activePointerId = event.getPointerId(newPointerIndex)
                    } else {
                        activePointerId = MotionEvent.INVALID_POINTER_ID
                    }
                }

                initialDistance = 0f
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                activePointerId = MotionEvent.INVALID_POINTER_ID
                initialDistance = 0f
                Log.d("DualCircleButtonView", "Outer Circle Size: ${outerRadius * outerScaleFactor}, Inner Circle Size: ${innerRadius * innerScaleFactor}")

                // Start the hide countdown AFTER the user lifts their fingers
                startHideTimer()
            }
        }
        return true
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return hypot(x2 - x1, y2 - y1)
    }

    private fun calculateCenteredRectF(bitmap: Bitmap): RectF {
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        val bitmapWidth = bitmap.width.toFloat()
        val bitmapHeight = bitmap.height.toFloat()

        val viewRatio = viewWidth / viewHeight
        val bitmapRatio = bitmapWidth / bitmapHeight

        val destWidth: Float
        val destHeight: Float

        if (bitmapRatio > viewRatio) {
            destWidth = viewWidth
            destHeight = viewWidth / bitmapRatio
        } else {
            destHeight = viewHeight
            destWidth = viewHeight * bitmapRatio
        }

        val left = (viewWidth - destWidth) / 2f
        val top = (viewHeight - destHeight) / 2f
        val right = left + destWidth
        val bottom = top + destHeight

        return RectF(left, top, right, bottom)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (centerX == 0f && centerY == 0f) {
            centerX = w / 2f
            centerY = h / 2f
        }
    }

    private fun showCircleIndefinitely() {
        showCircle = true
        handler.removeCallbacks(hideRunnable)
        invalidate()
    }

    private fun startHideTimer(delayMillis: Long = 1000) {
        handler.removeCallbacks(hideRunnable)
        handler.postDelayed(hideRunnable, delayMillis)
    }

    fun setBlurMode(isBlur: Boolean) {
        toggleBlur = isBlur
        invalidate()
        Log.d("TAG", "setBlurMode: $toggleBlur")
    }
}

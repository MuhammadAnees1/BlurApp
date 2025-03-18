package com.example.blureffectproject.main.activities

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.blureffectproject.R
import com.example.blureffectproject.databinding.ActivityBlurBinding
import com.example.blureffectproject.main.adaptor.BlurAdapter
import com.example.blureffectproject.models.BlurModel
import com.example.blureffectproject.models.BlurType
import com.example.blureffectproject.utils.BlurUtils
import com.example.blureffectproject.utils.BlurUtils.blurBitmap
import com.example.blureffectproject.view.DualCircleButtonView
import com.example.blureffectproject.view.LinerButtonView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.Segmenter
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions

class BlurActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBlurBinding
    private lateinit var blurAdapter: BlurAdapter
    private var isEraserSelected = false
    private var isBlurOnFace = false
    private var blurRadius: Float = 10f
    private lateinit var segmenter: Segmenter
    private var originalBitmap: Bitmap? = null
    private var isEraserActive = false
    private var isBlurPaintActive = false
    private lateinit var mutableBlurredBitmap: Bitmap
    private lateinit var originalBitmapCopy: Bitmap
    private var motionBlurAngle = 0f
    private var motionBlurDistance = 10f
    private var currentBlurType: BlurType? = null
    private var lastX = -1f
    private var lastY = -1f
    private val THRESHOLD_DISTANCE = 50f // you can adjust this


    @SuppressLint("ClickableViewAccessibility")
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlurBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initSegmenter()

        // Initialize originalBitmap and mutableBlurredBitmap
        (binding.blurImageView.drawable as? BitmapDrawable)?.let { drawable ->
            originalBitmap = drawable.bitmap.copy(Bitmap.Config.ARGB_8888, true)
            mutableBlurredBitmap = drawable.bitmap.copy(Bitmap.Config.ARGB_8888, true)
            originalBitmapCopy = originalBitmap?.copy(Bitmap.Config.ARGB_8888, true)!!
        }

        val buttonList = listOf(
            BlurModel(R.drawable.bg1, "Blur"),
            BlurModel(R.drawable.bg2, "Circle"),
            BlurModel(R.drawable.bg3, "Linear"),
            BlurModel(R.drawable.bg4, "Radial"),
            BlurModel(R.drawable.bg5, "Motion"),
            BlurModel(R.drawable.bg6, "Zoom"),
        )

        blurAdapter = BlurAdapter(this, buttonList) { imageResId ->

            binding.BlurMotionLinearLayout.visibility = View.GONE
            binding.blurLinearLayout.visibility = View.VISIBLE
            binding.dualCircleButton.visibility = View.GONE
            binding.eraser.visibility = View.VISIBLE
            binding.blurPaintButton.visibility = View.VISIBLE

            // Clean up old views
            for (i in binding.blurContainer.childCount - 1 downTo 0) {
                val child = binding.blurContainer.getChildAt(i)
                if (child is DualCircleButtonView || child is LinerButtonView) {
                    binding.blurContainer.removeViewAt(i)
                }
            }

            val myImageView = binding.blurImageView
            val drawable = myImageView.drawable as? BitmapDrawable
            drawable?.let {
                val bitmap = it.bitmap
                when (imageResId) {
                    R.drawable.bg1 -> {
                        currentBlurType = BlurType.NORMAL
                        processImageWithSegmentation(bitmap, myImageView, BlurType.NORMAL)
                    }
                    R.drawable.bg2 -> {
                        val button = DualCircleButtonView(this)
                        binding.blurContainer.addView(button)
                        button.setBitmap(bitmap)

                        binding.eraser.visibility = View.GONE
                        binding.blurPaintButton.visibility = View.GONE
                    }
                    R.drawable.bg3 -> {
                        val button = LinerButtonView(this)
                        binding.blurContainer.addView(button)
                        button.setImageBitmap(bitmap)

                        binding.eraser.visibility = View.GONE
                        binding.blurPaintButton.visibility = View.GONE
                    }
                    R.drawable.bg4 -> {
                        currentBlurType = BlurType.RADIAL
                        processImageWithSegmentation(bitmap, myImageView, BlurType.RADIAL)
                    }
                    R.drawable.bg5 -> {
                        currentBlurType = BlurType.MOTION
                        processImageWithSegmentation(bitmap, myImageView, BlurType.MOTION)
                        binding.BlurMotionLinearLayout.visibility = View.VISIBLE
                        binding.blurLinearLayout.visibility = View.GONE
                    }
                    R.drawable.bg6 -> {
                        currentBlurType = BlurType.ZOOM
                        processImageWithSegmentation(bitmap, myImageView, BlurType.ZOOM)
                    }
                    else -> {
                        currentBlurType = null
                        Toast.makeText(this, "No blur type found for this button", Toast.LENGTH_SHORT).show()
                    }
                }
            } ?:

            Log.e("BlurEffect", "Drawable is not a BitmapDrawable!")
        }

        binding.blurRecycleView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.blurRecycleView.adapter = blurAdapter

        binding.eraser.setOnClickListener {
            isEraserActive = true
            isEraserSelected = true
            isBlurPaintActive = false

            updateButtonStates(eraserSelected = true)

            enableEraserTouch()
        }

        binding.blurPaintButton.setOnClickListener {
            isBlurPaintActive = !isBlurPaintActive
            isEraserActive = false
            isEraserSelected = false

            Toast.makeText(this, if (isBlurPaintActive) "Blur Paint Enabled" else "Blur Paint Disabled", Toast.LENGTH_SHORT).show()

            if (isBlurPaintActive) {
                if (!::mutableBlurredBitmap.isInitialized) {
                    mutableBlurredBitmap = originalBitmap!!.copy(Bitmap.Config.ARGB_8888, true)
                    binding.blurImageView.setImageBitmap(mutableBlurredBitmap)
                }
                enableBlurPaintTouch()
            } else {
                disableTouch()
            }

            updateButtonStates(blurPaintSelected = isBlurPaintActive)
        }

        binding.toggleBlurButton.setOnClickListener {
            isEraserActive = false
            isEraserSelected = false
            isBlurOnFace = !isBlurOnFace

            if (currentBlurType == null) {
                Toast.makeText(this@BlurActivity, "Select a blur type first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            originalBitmap?.let {
                processImageWithSegmentation(it, binding.blurImageView, currentBlurType!!)
            } ?: run {
                Toast.makeText(this@BlurActivity, "No image loaded!", Toast.LENGTH_SHORT).show()
            }
        }


        binding.reset.setOnClickListener {
            isEraserActive = false
            isEraserSelected = false
            isBlurOnFace = false

            originalBitmap?.let {
//                binding.blurImageView.setImageBitmap(it)
                processImageWithSegmentation(it, binding.blurImageView, currentBlurType!!)
                updateButtonStates()
            }
        }

        binding.imagePreview.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    originalBitmap?.let { binding.blurImageView.setImageBitmap(it) }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    originalBitmap?.let { processImageWithSegmentation(it, binding.blurImageView, BlurType.RADIAL) }
                    true
                }
                else -> false
            }
        }

        binding.blurSeekBar.max = 100
        binding.blurSeekBar.progress = 50
        binding.blurSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {


                if (currentBlurType == null) {
                    Toast.makeText(this@BlurActivity, "Select a blur type first", Toast.LENGTH_SHORT).show()
                    return
                }
                // Display SeekBar value as 0% to 100%
                binding.blurValueText.text = "$progress%"

                // Convert 0-100% to a brush size of 10-30
                val blurProgress = 10 + (progress / 100f) * 20  // Maps 0% → 10 and 100% → 30

                (binding.blurImageView.drawable as? BitmapDrawable)?.bitmap?.let { bitmap ->
                    when (currentBlurType) {
                        BlurType.RADIAL -> processImageWithSegmentation(bitmap, binding.blurImageView, BlurType.RADIAL, radialZoomAmount = blurProgress / 1000f, radialPasses = 15)
                        BlurType.ZOOM -> processImageWithSegmentation(bitmap, binding.blurImageView, BlurType.ZOOM, radialZoomAmount = blurProgress / 1000f, radialPasses = 10)
                        BlurType.NORMAL -> processImageWithSegmentation(bitmap, binding.blurImageView, BlurType.NORMAL)
                        else -> Toast.makeText(this@BlurActivity, "Unsupported blur type!", Toast.LENGTH_SHORT).show()
                    }
                } ?: Log.e("BlurEffect", "No valid image loaded")
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        binding.motionBlurSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (currentBlurType == null) {
                    Toast.makeText(this@BlurActivity, "Select a blur type first", Toast.LENGTH_SHORT).show()
                    return
                }
                val blurProgress = progress.toFloat()
                (binding.blurImageView.drawable as? BitmapDrawable)?.bitmap?.let { bitmap ->
                    when (currentBlurType) {
                        BlurType.MOTION -> processImageWithSegmentation(bitmap,binding.blurImageView, BlurType.MOTION, motionAngle = motionBlurAngle , motionDistancePerPass = blurProgress)
                        else -> Toast.makeText(this@BlurActivity, "Unsupported blur type!", Toast.LENGTH_SHORT).show()
                    }
                } ?: Log.e("BlurEffect", "No valid image loaded")
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Make sure your SeekBar is set up with max 360
        binding.motionAngleSeekbar.max = 360
        binding.motionAngleSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            @RequiresApi(Build.VERSION_CODES.S)
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Angle cycles through 0° to 360°
                motionBlurAngle = progress.toFloat() % 360

                // Optional: If you want the angle to reset to 0 after 360
                if (motionBlurAngle >= 360f) {
                    motionBlurAngle = 0f
                }

                reprocessMotionBlur() // Update with the new angle
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

    }


    private fun updateButtonStates(eraserSelected: Boolean = false, blurPaintSelected: Boolean = false) {
        binding.eraser.setImageResource(if (eraserSelected) R.drawable.eraser_selected else R.drawable.eraser)
        binding.eraser.setBackgroundResource(if (eraserSelected) R.drawable.buttons_background_selected else R.drawable.buttons_background)

        binding.blurPaintButton.setImageResource(if (blurPaintSelected) R.drawable.brush_2_selected else R.drawable.brush)
        binding.blurPaintButton.setBackgroundResource(if (blurPaintSelected) R.drawable.buttons_background_selected else R.drawable.buttons_background)
    }

    private fun initSegmenter() {
        val options = SelfieSegmenterOptions.Builder()
            .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
            .build()
        segmenter = Segmentation.getClient(options)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun processImageWithSegmentation(
        originalBitmap: Bitmap,
        targetImageView: ImageView, blurType: BlurType, motionAngle: Float = 10f, motionDistancePerPass: Float = 10f, radialZoomAmount: Float = 0.1f, radialPasses: Int = 10
    ) {

        val width = originalBitmap.width
        val height = originalBitmap.height
        val inputImage = InputImage.fromBitmap(originalBitmap, 0)

        segmenter.process(inputImage)
            .addOnSuccessListener { segmentationResult ->

                try {
                    val maskBuffer = segmentationResult.buffer
                    val maskWidth = segmentationResult.width
                    val maskHeight = segmentationResult.height

                    val maskBitmap = Bitmap.createBitmap(maskWidth, maskHeight, Bitmap.Config.ARGB_8888)
                    maskBuffer.rewind()

                    for (y in 0 until maskHeight) {
                        for (x in 0 until maskWidth) {
                            val foregroundConfidence = maskBuffer.float
                            val alpha: Int = if (!isBlurOnFace) {
                                (foregroundConfidence * 255).toInt().coerceIn(0, 255)
                            } else {
                                ((1f - foregroundConfidence) * 255).toInt().coerceIn(0, 255)
                            }

                            val color = Color.argb(alpha, 255, 255, 255)
                            maskBitmap.setPixel(x, y, color)
                        }
                    }

                    val scaledMask = Bitmap.createScaledBitmap(maskBitmap, width, height, true)

                    val blurredBackground = when (blurType) {
                        BlurType.RADIAL -> BlurUtils.applyRadialZoomBlur(
                            context = this,
                            originalBitmap,
                            width / 2f,
                            height / 2f,
                             radialZoomAmount, radialPasses
                        )

                        BlurType.MOTION -> BlurUtils.applyMotionBlur(
                            originalBitmap,
                            blurPasses = 10,
                            angleInDegrees = motionAngle,
                            distancePerPass = motionDistancePerPass
                        )

                        BlurType.ZOOM -> BlurUtils.createZoomBlur(
                            originalBitmap,
                            width / 2f,
                            height / 2f,
                            zoomAmount = radialZoomAmount,
                            blurPasses = radialPasses
                        )

                        BlurType.NORMAL -> BlurUtils.applyLinearBlur(this, originalBitmap)
                    }

                    val compositedBitmap = compositeBitmaps(originalBitmap, blurredBackground, scaledMask)
                    mutableBlurredBitmap = compositedBitmap.copy(Bitmap.Config.ARGB_8888, true)

                    targetImageView.setImageBitmap(mutableBlurredBitmap)

                    // 4. Update the currentBlurType after successful processing
                    currentBlurType = blurType

                } catch (e: Exception) {
                    Log.e("ProcessImage", "Error during blur process: ${e.message}")
                }
            }
            .addOnFailureListener { e ->
                Log.e("Segmentation", "Segmentation failed: ${e.message}")
            }
    }

    fun compositeBitmaps(original: Bitmap, blurred: Bitmap, mask: Bitmap): Bitmap {
        val width = original.width
        val height = original.height

        val adjustedBlurred = if (blurred.width != width || blurred.height != height)
            Bitmap.createScaledBitmap(blurred, width, height, true)
        else blurred

        val adjustedMask = if (mask.width != width || mask.height != height)
            Bitmap.createScaledBitmap(mask, width, height, true)
        else mask

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val origPixels = IntArray(width * height)
        val blurPixels = IntArray(width * height)
        val maskPixels = IntArray(width * height)

        original.getPixels(origPixels, 0, width, 0, 0, width, height)
        adjustedBlurred.getPixels(blurPixels, 0, width, 0, 0, width, height)
        adjustedMask.getPixels(maskPixels, 0, width, 0, 0, width, height)

        val resultPixels = IntArray(width * height)

        for (i in origPixels.indices) {
            val maskAlpha = Color.alpha(maskPixels[i]) / 255f
            val r = (Color.red(origPixels[i]) * maskAlpha + Color.red(blurPixels[i]) * (1 - maskAlpha)).toInt()
            val g = (Color.green(origPixels[i]) * maskAlpha + Color.green(blurPixels[i]) * (1 - maskAlpha)).toInt()
            val b = (Color.blue(origPixels[i]) * maskAlpha + Color.blue(blurPixels[i]) * (1 - maskAlpha)).toInt()
            val a = 255

            resultPixels[i] = Color.argb(a, r, g, b)
        }
        result.setPixels(resultPixels, 0, width, 0, 0, width, height)

        return result
    }

    private fun getBitmapCoordinates(imageView: ImageView, event: MotionEvent): PointF? {
        val drawable = imageView.drawable ?: return null
        val matrix = imageView.imageMatrix
        val values = FloatArray(9)
        matrix.getValues(values)

        val scaleX = values[Matrix.MSCALE_X]
        val scaleY = values[Matrix.MSCALE_Y]

        val transX = values[Matrix.MTRANS_X]
        val transY = values[Matrix.MTRANS_Y]

        val x = (event.x - transX) / scaleX
        val y = (event.y - transY) / scaleY

        if (x < 0 || y < 0 || x >= drawable.intrinsicWidth || y >= drawable.intrinsicHeight) {
            return null
        }

        return PointF(x, y)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun enableEraserTouch() {
        binding.blurImageView.setOnTouchListener { view, motionEvent ->
            if (!isEraserActive) {
                return@setOnTouchListener false
            }

            val bitmapPoint = getBitmapCoordinates(binding.blurImageView, motionEvent)

            if (bitmapPoint != null) {
                val x = bitmapPoint.x.toInt()
                val y = bitmapPoint.y.toInt()

                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN,
                    MotionEvent.ACTION_MOVE -> {
                        eraseBlurAtPoint(x, y)
                    }
                    MotionEvent.ACTION_UP -> {
                    }
                }
            }

            true
        }
    }

    private fun eraseBlurAtPoint(x: Int, y: Int) {
        val radius = 20

        val left = (x - radius).coerceAtLeast(0)
        val top = (y - radius).coerceAtLeast(0)
        val right = (x + radius).coerceAtMost(mutableBlurredBitmap.width - 1)
        val bottom = (y + radius).coerceAtMost(mutableBlurredBitmap.height - 1)

        for (i in left..right) {
            for (j in top..bottom) {
                val dx = i - x
                val dy = j - y
                if (dx * dx + dy * dy <= radius * radius) {
                    mutableBlurredBitmap.setPixel(i, j, originalBitmapCopy.getPixel(i, j))
                }
            }
        }
        // Update the ImageView
        binding.blurImageView.setImageBitmap(mutableBlurredBitmap)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun enableBlurPaintTouch() {
        binding.blurImageView.setOnTouchListener { view, motionEvent ->
            if (!isBlurPaintActive) {
                return@setOnTouchListener false
            }

            val bitmapPoint = getBitmapCoordinates(binding.blurImageView, motionEvent)

            if (bitmapPoint != null) {
                val x = bitmapPoint.x
                val y = bitmapPoint.y

                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> {
                        lastX = x
                        lastY = y
                        applyBlurAtPoint(x.toInt(), y.toInt())
                    }

                    MotionEvent.ACTION_MOVE -> {
                        drawSmoothLine(lastX, lastY, x, y)
                        lastX = x
                        lastY = y
                    }

                    MotionEvent.ACTION_UP -> {
                        lastX = -1f
                        lastY = -1f
                    }
                }
            }

            true
        }

    }

    private fun drawSmoothLine(startX: Float, startY: Float, endX: Float, endY: Float) {
        val distance = Math.hypot((endX - startX).toDouble(), (endY - startY).toDouble()).toFloat()
        val steps = distance.toInt()

        for (i in 0..steps) {
            val t = i / distance
            val x = lerp(startX, endX, t)
            val y = lerp(startY, endY, t)
            applyBlurAtPoint(x.toInt(), y.toInt())
        }
    }

    private fun lerp(a: Float, b: Float, t: Float): Float {
        return a + (b - a) * t
    }


    private fun applyBlurAtPoint(x: Int, y: Int) {
        val radius = 20f // increased radius for smoother edges
        val blurRadius = 50f // blur intensity

        if (originalBitmap == null) return

        val canvas = Canvas(mutableBlurredBitmap)

        val patchSize = (radius * 2).toInt()
        val left = (x - radius).toInt().coerceAtLeast(0)
        val top = (y - radius).toInt().coerceAtLeast(0)
        val right = (x + radius).toInt().coerceAtMost(originalBitmap!!.width)
        val bottom = (y + radius).toInt().coerceAtMost(originalBitmap!!.height)

        if (left >= right || top >= bottom) return

        // Extract the patch from the original bitmap
        val patch = Bitmap.createBitmap(originalBitmap!!, left, top, right - left, bottom - top)

        // Apply blur to the patch
        val blurredPatch = blurBitmap(this, patch, blurRadius)

        // Create a circular mask
        val mask = Bitmap.createBitmap(patch.width, patch.height, Bitmap.Config.ARGB_8888)
        val maskCanvas = Canvas(mask)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isAntiAlias = true
        }
        maskCanvas.drawCircle(
            patch.width / 2f,
            patch.height / 2f,
            radius,
            paint
        )

        // Prepare paint for blending
        val maskedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }

        // Mask the blurred patch
        val finalPatch = Bitmap.createBitmap(patch.width, patch.height, Bitmap.Config.ARGB_8888)
        val finalCanvas = Canvas(finalPatch)

        // Draw the blurred patch first
        finalCanvas.drawBitmap(blurredPatch, 0f, 0f, null)
        // Apply circular mask
        finalCanvas.drawBitmap(mask, 0f, 0f, maskedPaint)

        // Draw the final circular blurred patch on the mutable blurred bitmap
        canvas.drawBitmap(finalPatch, left.toFloat(), top.toFloat(), null)

        binding.blurImageView.setImageBitmap(mutableBlurredBitmap)
    }


    private fun disableTouch() {
        binding.blurImageView.setOnTouchListener(null)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun reprocessMotionBlur() {
        val myImageView = binding.blurImageView
        val drawable = myImageView.drawable

        if (drawable is BitmapDrawable) {
            val bitmap = drawable.bitmap
            processImageWithSegmentation(
                bitmap,
                myImageView,
                BlurType.MOTION,
                motionBlurAngle,
                motionBlurDistance
            )
        }
    }
}

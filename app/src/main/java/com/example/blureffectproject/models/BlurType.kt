package com.example.blureffectproject.models

enum class BlurType {
    RADIAL,
    MOTION,
    ZOOM,
    NORMAL
}

//
//private fun createRadialBlur(bitmap: Bitmap, blurRadius: Float): Bitmap {
//    // Create a mutable bitmap to draw on
//    val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
//    val canvas = Canvas(output)
//
//    // Set up paint for radial blur
//    val paint = Paint()
//    paint.isAntiAlias = true
//
//    // Example radial blur implementation
//    val shader = RadialGradient(
//        bitmap.width / 2f,
//        bitmap.height / 2f,
//        blurRadius * 10, // multiply radius for more visible effect
//        intArrayOf(0xFFFFFFFF.toInt(), 0x00FFFFFF),
//        floatArrayOf(0.0f, 1.0f),
//        Shader.TileMode.CLAMP
//    )
//
//    paint.shader = shader
//
//    // Draw blurred background
//    canvas.drawBitmap(bitmap, 0f, 0f, null)
//    canvas.drawRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), paint)
//
//    return output
//}
//
//fun createZoomBlur(bitmap: Bitmap, centerX: Float, centerY: Float, zoomAmount: Float = 1.05f, blurPasses: Int = 10): Bitmap {
//    val width = bitmap.width
//    val height = bitmap.height
//
//    val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
//    val canvas = Canvas(result)
//    val paint = Paint()
//    paint.isAntiAlias = true
//    paint.alpha = 255 / blurPasses
//
//    val matrix = Matrix()
//
//    // Draw the original image on canvas multiple times, each time scaling it a little.
//    for (i in 0 until blurPasses) {
//        val scale = 1 + (zoomAmount - 1) * (i.toFloat() / blurPasses)
//        matrix.reset()
//        matrix.postScale(scale, scale, centerX, centerY)
//        canvas.drawBitmap(bitmap, matrix, paint)
//    }
//
//    return result
//}
//    @RequiresApi(Build.VERSION_CODES.S)
//    private fun processImageWithSegmentation(originalBitmap: Bitmap, targetImageView: ImageView) {
//
//        val width = originalBitmap.width
//        val height = originalBitmap.height
//        val inputImage = InputImage.fromBitmap(originalBitmap, 0)
//
//        segmenter.process(inputImage)
//            .addOnSuccessListener { segmentationResult ->
//
//                val maskBuffer = segmentationResult.buffer
//                val maskWidth = segmentationResult.width
//                val maskHeight = segmentationResult.height
//
//                val maskBitmap = Bitmap.createBitmap(maskWidth, maskHeight, Bitmap.Config.ARGB_8888)
//                maskBuffer.rewind()
//
//                for (y in 0 until maskHeight) {
//                    for (x in 0 until maskWidth) {
//                        val foregroundConfidence = maskBuffer.float
//                        val alpha: Int = if (!isBlurOnFace) {
//                            // Blur background: mask focuses on the person
//                            (foregroundConfidence * 255).toInt().coerceIn(0, 255)
//                        } else {
//                            // Blur face: invert the mask
//                            ((1f - foregroundConfidence) * 255).toInt().coerceIn(0, 255)
//                        }
//
//                        val color = Color.argb(alpha, 255, 255, 255)
//                        maskBitmap.setPixel(x, y, color)
//                    }
//                }
//
//                val scaledMask = Bitmap.createScaledBitmap(maskBitmap, width, height, true)
////                val blurredBitmap = blurBitmap(this@BlurActivity, originalBitmap, blurRadius)
//
////                val compositedBitmap = compositeBitmaps(originalBitmap, blurredBitmap, scaledMask)
////                val compositedBitmap = compositeBitmaps(originalBitmap, radialBlurredBackground, scaledMask)
////                val centerX = originalBitmap.width / 2f
////                val centerY = originalBitmap.height / 2f
////                val zoomBlurredBackground = createZoomBlur(originalBitmap, centerX, centerY, zoomAmount = 1.05f, blurPasses = 15)
//
//                // ⏩ Instead of blurBitmap(), use radial blur on background!
//                val radialBlurredBackground = createRadialBlur(originalBitmap, blurRadius)
//
//                // Combine the original (foreground) with the radial blurred background
//                val compositedBitmap = compositeBitmaps(originalBitmap, radialBlurredBackground, scaledMask)
//                targetImageView.setImageBitmap(compositedBitmap)
//            }
//            .addOnFailureListener { e ->
//                Log.e("Segmentation", "Segmentation failed: ${e.message}")
//            }
//    }

//    @RequiresApi(Build.VERSION_CODES.S)
//    private fun processImageWithSegmentation(originalBitmap: Bitmap, targetImageView: ImageView) {
//
//        val width = originalBitmap.width
//        val height = originalBitmap.height
//        val inputImage = InputImage.fromBitmap(originalBitmap, 0)
//
//        segmenter.process(inputImage)
//            .addOnSuccessListener { segmentationResult ->
//
//                val maskBuffer = segmentationResult.buffer
//                val maskWidth = segmentationResult.width
//                val maskHeight = segmentationResult.height
//
//                val maskBitmap = Bitmap.createBitmap(maskWidth, maskHeight, Bitmap.Config.ARGB_8888)
//                maskBuffer.rewind()
//
//                for (y in 0 until maskHeight) {
//                    for (x in 0 until maskWidth) {
//                        val foregroundConfidence = maskBuffer.float
//                        val alpha: Int = if (!isBlurOnFace) {
//                            // Blur background: mask focuses on the person
//                            (foregroundConfidence * 255).toInt().coerceIn(0, 255)
//                        } else {
//                            // Blur face: invert the mask
//                            ((1f - foregroundConfidence) * 255).toInt().coerceIn(0, 255)
//                        }
//
//                        val color = Color.argb(alpha, 255, 255, 255)
//                        maskBitmap.setPixel(x, y, color)
//                    }
//                }
//
//                val scaledMask = Bitmap.createScaledBitmap(maskBitmap, width, height, true)
//
//                // ➡️ Apply radial blur from BlurUtils instead of createRadialBlur()
//                val radialBlurredBackground = BlurUtils.applyRadialBlur(
//                    originalBitmap,
//                    width / 2f,
//                    height / 2f,
//                    blurRadius
//                )
//
//                // Combine the original (foreground) with the radial blurred background
//                val compositedBitmap = compositeBitmaps(originalBitmap, radialBlurredBackground, scaledMask)
//
//                targetImageView.setImageBitmap(compositedBitmap)
//            }
//            .addOnFailureListener { e ->
//                Log.e("Segmentation", "Segmentation failed: ${e.message}")
//            }
//    }
//
//@SuppressLint("ClickableViewAccessibility")
//private fun enableBlurPaintTouch() {
//    binding.blurImageView.setOnTouchListener { view, motionEvent ->
//        if (!isBlurPaintActive) {
//            return@setOnTouchListener false
//        }
//
//        val bitmapPoint = getBitmapCoordinates(binding.blurImageView, motionEvent)
//
//        if (bitmapPoint != null) {
//            val x = bitmapPoint.x.toInt()
//            val y = bitmapPoint.y.toInt()
//
//            when (motionEvent.action) {
//                MotionEvent.ACTION_DOWN,
//                MotionEvent.ACTION_MOVE -> {
//                    applyBlurAtPoint(x, y)
//                }
//                MotionEvent.ACTION_UP -> {
//                }
//            }
//        }
//
//        true
//    }
//}
//
//private fun applyBlurAtPoint(x: Int, y: Int) {
//    val radius = 40f
//
//    val canvas = Canvas(mutableBlurredBitmap)
//
//    val patchSize = radius.toInt() * 2
//    val left = (x - radius).toInt().coerceAtLeast(0)
//    val top = (y - radius).toInt().coerceAtLeast(0)
//    val right = originalBitmap?.let { (x + radius).toInt().coerceAtMost(it.width) }
//    val bottom = originalBitmap?.let { (y + radius).toInt().coerceAtMost(it.height) }
//
//    if (originalBitmap == null || left >= right!! || top >= bottom!!) return
//
//    val patch = Bitmap.createBitmap(originalBitmap!!, left, top, right - left, bottom - top)
//
//    val blurredPatch = blurBitmap(this, patch, 25f)
//
//    canvas.drawBitmap(blurredPatch, left.toFloat(), top.toFloat(), null)
//
//    binding.blurImageView.setImageBitmap(mutableBlurredBitmap)
//}


// blurred patch is a blur effect
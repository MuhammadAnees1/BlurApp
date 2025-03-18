package com.example.blureffectproject.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.os.Build
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import androidx.annotation.RequiresApi
import androidx.core.graphics.createBitmap

object BlurUtils {

    fun blurBitmap(context: Context, bitmap: Bitmap, radius: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val inputBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val outputBitmap = createBitmap(width, height)

        val rs = RenderScript.create(context)
        val input = Allocation.createFromBitmap(rs, inputBitmap)
        val output = Allocation.createFromBitmap(rs, outputBitmap)

        val blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        blurScript.setRadius(radius.coerceIn(1f, 25f))
        blurScript.setInput(input)
        blurScript.forEach(output)

        output.copyTo(outputBitmap)
        rs.destroy()

        return outputBitmap
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun applyRadialZoomBlur(context: Context, bitmap: Bitmap, centerX: Float, centerY: Float, blurAmount: Float, blurPasses: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val outputBitmap = Bitmap.createBitmap(width, height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)

        // Draw the original bitmap first
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        val paint = Paint().apply {
            isAntiAlias = true
        }

        // Loop to create radial zoom blur with additional linear blur applied
        for (i in 1..blurPasses) {
            val scale = 1f + i * blurAmount
            val alpha = (255 / (i + 1)).coerceIn(0, 255)

            val matrix = Matrix().apply {
                postScale(scale, scale, centerX, centerY)
            }

            // Scale the bitmap
            val scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)

            // Apply linear blur to the scaled bitmap
            val blurredScaledBitmap = applyLinearBlur(context, scaledBitmap)

            val dx = (width - blurredScaledBitmap.width) / 2f
            val dy = (height - blurredScaledBitmap.height) / 2f

            paint.alpha = alpha
            canvas.drawBitmap(blurredScaledBitmap, dx, dy, paint)

            // Recycle temporary bitmaps
            scaledBitmap.recycle()
            blurredScaledBitmap.recycle()
        }

        return outputBitmap
    }


    fun applyMotionBlur(bitmap: Bitmap, blurPasses: Int, angleInDegrees: Float, distancePerPass: Float): Bitmap {
        val radians = Math.toRadians(angleInDegrees.toDouble())
        val offsetX = (Math.cos(radians) * distancePerPass).toFloat()
        val offsetY = (Math.sin(radians) * distancePerPass).toFloat()

        val outputBitmap =
            createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)
        val paint = Paint().apply {
            isAntiAlias = true
            alpha = (255 / blurPasses)
        }

        for (i in 0 until blurPasses) {
            val dx = offsetX * i
            val dy = offsetY * i
            canvas.drawBitmap(bitmap, dx, dy, paint)
        }

        return outputBitmap
    }

    fun createZoomBlur(bitmap: Bitmap, centerX: Float, centerY: Float, zoomAmount: Float, blurPasses: Int): Bitmap {
        if (bitmap.isRecycled) {
            throw IllegalArgumentException("Original bitmap is already recycled!")
        }

        val outputBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)
        val paint = Paint().apply {
            isAntiAlias = true
            alpha = (255 / blurPasses)
        }

        for (i in 0 until blurPasses) {
            val scale = 1f + zoomAmount * (i.toFloat() / blurPasses)
            val matrix = Matrix().apply {
                postScale(scale, scale, centerX, centerY)
            }

            val scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

            val dx = (bitmap.width - scaledBitmap.width) / 2f
            val dy = (bitmap.height - scaledBitmap.height) / 2f

            canvas.drawBitmap(scaledBitmap, dx, dy, paint)

            // Only recycle if it's not the same as the original
            if (scaledBitmap != bitmap && !scaledBitmap.isRecycled) {
                scaledBitmap.recycle()
            }
        }

        return outputBitmap
    }

    fun applyLinearBlur(context: Context, bitmap: Bitmap): Bitmap {
        val rs = RenderScript.create(context)

        val input = Allocation.createFromBitmap(rs, bitmap)
        val output = Allocation.createTyped(rs, input.type)
        val script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))

        script.setRadius(10f)
        script.setInput(input)
        script.forEach(output)

        val outputBitmap = createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)

        output.copyTo(outputBitmap)

        rs.destroy()

        return outputBitmap
    }


}

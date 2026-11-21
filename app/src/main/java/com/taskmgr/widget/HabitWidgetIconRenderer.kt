package com.taskmgr.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Build
import android.view.WindowManager
import androidx.core.content.res.ResourcesCompat
import com.jovij.taskmgr.R
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object HabitWidgetIconRenderer {

    private const val BYTES_PER_PIXEL_ARGB_8888 = 4L

    fun calculateWidgetMemoryCapBytes(context: Context): Long {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = runCatching {
                context.getSystemService(WindowManager::class.java)?.currentWindowMetrics?.bounds
            }.getOrNull()
            if (bounds != null && bounds.width() > 0 && bounds.height() > 0) {
                return 6L * bounds.width().toLong() * bounds.height().toLong()
            }
        }
        val dm = context.resources.displayMetrics
        return 6L * max(1, dm.widthPixels).toLong() * max(1, dm.heightPixels).toLong()
    }

    fun computeContrastContentColorArgb(backgroundArgb: Int): Int {
        val luminance = androidx.core.graphics.ColorUtils.calculateLuminance(backgroundArgb)
        return if (luminance > 0.34) 0xFF0F172A.toInt() else Color.WHITE
    }

    fun withAlpha(argb: Int, alphaFraction: Float): Int {
        val alpha = (alphaFraction.coerceIn(0f, 1f) * 255f).roundToInt()
        return Color.argb(alpha, Color.red(argb), Color.green(argb), Color.blue(argb))
    }

    fun renderHabitIconBitmap(
        context: Context,
        glyph: String,
        foregroundArgb: Int,
        containerFillArgb: Int,
        borderArgb: Int,
        isCompleted: Boolean,
        targetSizeDp: Int = 56
    ): Bitmap {
        val density = context.resources.displayMetrics.density.coerceAtLeast(1f)
        var sizePx = (targetSizeDp * density).roundToInt().coerceIn(72, 168)

        // Respect Android 17 widget bitmap memory cap (keep single icon tiny: <= cap / 16).
        val maxIconBytes = (calculateWidgetMemoryCapBytes(context) / 16L).coerceAtLeast(32_768L)
        while (sizePx > 48 && sizePx.toLong() * sizePx.toLong() * BYTES_PER_PIXEL_ARGB_8888 > maxIconBytes) {
            sizePx = (sizePx * 3) / 4
        }

        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = sizePx / 2f
        val cy = sizePx / 2f
        val strokeWidth = max(2f, sizePx * 0.032f)
        val radius = (min(cx, cy) - strokeWidth).coerceAtLeast(1f)

        // Tactile circular button background
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = containerFillArgb
        }
        canvas.drawCircle(cx, cy, radius, fillPaint)

        // Subtle circle border (stronger when completed)
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = if (isCompleted) strokeWidth * 1.45f else strokeWidth
            color = borderArgb
        }
        canvas.drawCircle(cx, cy, radius, borderPaint)

        // Centered Nerd Font glyph
        val typeface = runCatching {
            ResourcesCompat.getFont(context, R.font.ubuntu_sans_nerd_regular)
        }.getOrNull() ?: Typeface.DEFAULT

        val glyphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = foregroundArgb
            this.typeface = typeface
            textSize = sizePx * 0.48f
            textAlign = Paint.Align.LEFT
        }
        val textBounds = Rect()
        val resolvedGlyph = glyph.ifBlank { "\uF0238" }
        glyphPaint.getTextBounds(resolvedGlyph, 0, resolvedGlyph.length, textBounds)
        val textX = cx - textBounds.width() / 2f - textBounds.left
        val textY = cy + textBounds.height() / 2f - textBounds.bottom
        canvas.drawText(resolvedGlyph, textX, textY, glyphPaint)

        // Small check badge on bottom-right edge when habit is completed today
        if (isCompleted) {
            val badgeRadius = sizePx * 0.16f
            val badgeCx = sizePx - badgeRadius - strokeWidth
            val badgeCy = sizePx - badgeRadius - strokeWidth
            val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = foregroundArgb
            }
            canvas.drawCircle(badgeCx, badgeCy, badgeRadius, badgeBgPaint)

            val checkColor = if (foregroundArgb == Color.WHITE) 0xFF0F172A.toInt() else Color.WHITE
            val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                this.strokeWidth = max(2f, badgeRadius * 0.24f)
                color = checkColor
            }
            val r = badgeRadius * 0.46f
            canvas.drawLine(badgeCx - r * 0.85f, badgeCy + r * 0.05f, badgeCx - r * 0.15f, badgeCy + r * 0.72f, checkPaint)
            canvas.drawLine(badgeCx - r * 0.15f, badgeCy + r * 0.72f, badgeCx + r * 0.90f, badgeCy - r * 0.58f, checkPaint)
        }

        return bitmap
    }
}

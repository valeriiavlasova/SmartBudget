package pl.edu.finanseosobiste.ui

import android.graphics.Canvas
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.renderer.XAxisRendererHorizontalBarChart
import com.github.mikephil.charting.utils.MPPointF
import com.github.mikephil.charting.utils.Transformer
import com.github.mikephil.charting.utils.ViewPortHandler

class CategoryAxisRenderer(
    handler: ViewPortHandler,
    axis: XAxis,
    transformer: Transformer,
    chart: HorizontalBarChart,
    private val density: Float
) : XAxisRendererHorizontalBarChart(handler, axis, transformer, chart) {
    private val labelWidth get() = (104 * density).toInt()

    override fun computeSize() {
        super.computeSize()
        mXAxis.mLabelWidth = labelWidth
        mXAxis.mLabelRotatedWidth = labelWidth + (8 * density).toInt()
    }

    override fun drawLabel(c: Canvas, label: String, x: Float, y: Float, anchor: MPPointF, angle: Float) {
        if (label.isBlank()) return
        val paint = TextPaint(mAxisLabelPaint).apply { textAlign = android.graphics.Paint.Align.LEFT }
        val layout = StaticLayout.Builder.obtain(label, 0, label.length, paint, labelWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(false)
            .setMaxLines(3)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()
        val left = (x - labelWidth).coerceAtLeast(0f)
        val top = y - layout.height / 2f
        val saved = c.save()
        c.clipRect(left, y - 29 * density, x, y + 29 * density)
        c.translate(left, top)
        layout.draw(c)
        c.restoreToCount(saved)
    }
}

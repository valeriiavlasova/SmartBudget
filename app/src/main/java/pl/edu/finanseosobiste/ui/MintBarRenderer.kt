package pl.edu.finanseosobiste.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.renderer.BarChartRenderer
import com.github.mikephil.charting.utils.ViewPortHandler
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet

class MintBarRenderer(private val chart: BarChart, animator: ChartAnimator, handler: ViewPortHandler) : BarChartRenderer(chart, animator, handler) {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private val density=chart.resources.displayMetrics.density
    override fun drawDataSet(c: Canvas, dataSet: IBarDataSet, index: Int) {
        val transform=chart.getTransformer(dataSet.axisDependency)
        for(i in 0 until dataSet.entryCount) {
            val e=dataSet.getEntryForIndex(i)
            val rect=RectF(e.x-chart.barData.barWidth/2,e.y*mAnimator.phaseY,e.x+chart.barData.barWidth/2,0f)
            transform.rectValueToPixel(rect)
            paint.color=dataSet.getColor(i)
            c.drawRoundRect(rect,6*density,6*density,paint)
        }
    }
    override fun drawValues(c: Canvas) {
        val set=chart.barData?.getDataSetByIndex(0) ?: return
        if(set.entryCount==0) return
        val entry=set.getEntryForIndex(set.entryCount-1)
        if(entry.y<=0f) return
        val point=chart.getTransformer(set.axisDependency).getPixelForValues(entry.x,entry.y)
        val label=MintUi.money(entry.y.toDouble())
        paint.textSize=11*density; paint.typeface=android.graphics.Typeface.DEFAULT_BOLD
        val width=paint.measureText(label)+16*density
        val left=(point.x.toFloat()-width/2).coerceIn(0f,(chart.width-width).coerceAtLeast(0f))
        val bottom=(point.y.toFloat()-8*density).coerceAtLeast(26*density)
        paint.color=MintUi.primary(chart.context); c.drawRoundRect(RectF(left,bottom-25*density,left+width,bottom),8*density,8*density,paint)
        paint.color=MintUi.onPrimary(chart.context); c.drawText(label,left+8*density,bottom-8*density,paint)
        com.github.mikephil.charting.utils.MPPointD.recycleInstance(point)
    }
}

package pl.edu.finanseosobiste.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.action.GeneralClickAction
import androidx.test.espresso.action.Tap
import androidx.test.espresso.action.Press
import androidx.test.platform.app.InstrumentationRegistry
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.utils.MPPointD
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import pl.edu.finanseosobiste.R
import pl.edu.finanseosobiste.util.DataFiles
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class AnalyticsInteractionTest {
    @Test fun selectingOlderAndEmptyMonthsUpdatesAmountAndLabelsUseCategoryNames() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        if (android.os.Build.VERSION.SDK_INT >= 33) InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(context.packageName, android.Manifest.permission.POST_NOTIFICATIONS)
        val snapshot = java.io.ByteArrayOutputStream().also { DataFiles.backup(context,it) }.toByteArray()
        AppSettings.preferences(context).edit().putString("language","en").commit()
        val room = pl.edu.finanseosobiste.data.AppDatabase.getInstance(context)
        val sql = room.openHelper.writableDatabase
        val month = YearMonth.now()
        val zone = java.time.ZoneId.systemDefault()
        val start = month.minusMonths(5).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = month.minusMonths(4).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        room.runInTransaction { sql.execSQL("DELETE FROM expenses WHERE timestamp>=? AND timestamp<?",arrayOf(start,end)) }
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                onView(withId(R.id.nav_charts)).perform(click())
                onView(withId(R.id.nextMonth)).perform(click())
                onView(withId(R.id.categoryChartEmpty)).check(matches(isDisplayed()))
                onView(withId(R.id.categoryChartEmpty)).check(matches(withText(R.string.category_chart_empty)))
                onView(withId(R.id.categoryChartContent)).check(matches(withEffectiveVisibility(Visibility.GONE)))
                if (InstrumentationRegistry.getArguments().getString("captureEmpty")=="true") {
                    Thread.sleep(500)
                    val image=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
                    java.io.File(context.getExternalFilesDir(null),"SmartBudget-Empty-Month.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
                    image.recycle()
                }
                onView(withId(R.id.previousMonth)).perform(click())
                onView(withId(R.id.monthlyChart)).perform(scrollTo())
                fun tapMonth(index: Int) {
                    onView(withId(R.id.monthlyChart)).perform(GeneralClickAction(Tap.SINGLE, { view ->
                        val chart = view as BarChart
                        val point = chart.getTransformer(YAxis.AxisDependency.LEFT).getPixelForValues(index.toFloat(),0f)
                        val origin = IntArray(2); view.getLocationOnScreen(origin)
                        val coords = floatArrayOf(origin[0]+point.x.toFloat(),origin[1]+chart.viewPortHandler.contentBottom()-12f)
                        MPPointD.recycleInstance(point)
                        coords
                    }, Press.FINGER))
                }
                tapMonth(1)
                val past = month.minusMonths(4).format(DateTimeFormatter.ofPattern("MMMM yyyy",Locale.ENGLISH))
                onView(withId(R.id.monthlySelection)).check(matches(withText(org.hamcrest.Matchers.startsWith(past))))
                scenario.onActivity { activity ->
                    val chart = activity.findViewById<BarChart>(R.id.monthlyChart)
                    assertEquals(1,(chart.renderer as MintBarRenderer).selectedIndex)
                }
                tapMonth(0)
                val empty = month.minusMonths(5).format(DateTimeFormatter.ofPattern("MMMM yyyy",Locale.ENGLISH))
                onView(withId(R.id.monthlySelection)).check(matches(withText(org.hamcrest.Matchers.startsWith(empty+" · 0 "))))
                onView(withId(R.id.moreAnalytics)).perform(scrollTo(),click())
                onView(withId(R.id.barChartLimits)).perform(scrollTo())
                scenario.onActivity { activity ->
                    val chart=activity.findViewById<com.github.mikephil.charting.charts.HorizontalBarChart>(R.id.barChartLimits)
                    assertTrue(chart.xAxis.valueFormatter.getFormattedValue(0f).length>1)
                    assertTrue(chart.viewPortHandler.contentLeft()>100*activity.resources.displayMetrics.density)
                }
                onView(withId(R.id.historyForecast)).perform(scrollTo(),click())
                onView(withText(R.string.forecast_details)).check(matches(isDisplayed())).perform(click())
                onView(withText(org.hamcrest.Matchers.containsString("Average error in earlier months."))).check(matches(isDisplayed()))
            }
        } finally { DataFiles.restoreBytes(context,snapshot) }
    }
}

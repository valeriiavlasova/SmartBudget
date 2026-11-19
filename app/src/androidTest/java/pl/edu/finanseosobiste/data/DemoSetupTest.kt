package pl.edu.finanseosobiste.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import pl.edu.finanseosobiste.ui.AppSettings
import pl.edu.finanseosobiste.util.DataFiles

@RunWith(AndroidJUnit4::class)
class DemoSetupTest {
    @Test fun installDemoDataWhenExplicitlyRequested() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("loadDemo")=="true")
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        if (android.os.Build.VERSION.SDK_INT >= 33) InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(context.packageName, android.Manifest.permission.POST_NOTIFICATIONS)
        val dir=requireNotNull(context.getExternalFilesDir("demo"))
        val backup=java.io.File(dir,"SmartBudget-before-demo.json")
        backup.outputStream().use { DataFiles.backup(context,it) }
        val database=AppDatabase.getInstance(context)
        database.runInTransaction {
            val db=database.openHelper.writableDatabase
            for(table in listOf("cash_entries","expenses","recurring_payments","savings_goals","month_budgets","accounts","budget_config","categories")) db.execSQL("DELETE FROM $table")
            db.execSQL("DELETE FROM sqlite_sequence WHERE name IN ('cash_entries','expenses','recurring_payments','savings_goals','accounts','categories')")
            DemoData.seed(db)
        }
        AppSettings.preferences(context).edit().putString("currency","PLN").putString("language","pl").putString("savings_contribution","300").commit()
        AppSettings.configureMoney(context)
        context.getSystemService(android.app.NotificationManager::class.java).cancelAll()
        java.io.File(dir,"SmartBudget-demo.json").outputStream().use { DataFiles.backup(context,it) }
        database.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
        if (InstrumentationRegistry.getArguments().getString("captureDemo")=="true") {
            fun capture(name: String) {
                Thread.sleep(900)
                val image=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
                java.io.File(dir,name).outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
                image.recycle()
            }
            androidx.test.core.app.ActivityScenario.launch(pl.edu.finanseosobiste.ui.MainActivity::class.java).use { scenario ->
                fun click(id: Int) { androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(id)).perform(androidx.test.espresso.action.ViewActions.click()) }
                fun scroll(id: Int) { androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(id)).perform(androidx.test.espresso.action.ViewActions.scrollTo()) }
                capture("SmartBudget-Demo-Expenses.png")
                click(pl.edu.finanseosobiste.R.id.nav_charts)
                scroll(pl.edu.finanseosobiste.R.id.monthlyChart)
                scenario.onActivity { it.findViewById<com.github.mikephil.charting.charts.BarChart>(pl.edu.finanseosobiste.R.id.monthlyChart).highlightValue(3f,0,true) }
                capture("SmartBudget-Demo-Monthly.png")
                scroll(pl.edu.finanseosobiste.R.id.moreAnalytics); click(pl.edu.finanseosobiste.R.id.moreAnalytics)
                scroll(pl.edu.finanseosobiste.R.id.barChartLimits)
                capture("SmartBudget-Demo-Categories.png")
                scroll(pl.edu.finanseosobiste.R.id.historyForecast); click(pl.edu.finanseosobiste.R.id.historyForecast)
                capture("SmartBudget-Demo-Forecast.png")
                androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText(android.R.string.ok)).perform(androidx.test.espresso.action.ViewActions.click())
                click(pl.edu.finanseosobiste.R.id.nav_limits)
                capture("SmartBudget-Demo-Limits.png")
                click(pl.edu.finanseosobiste.R.id.nav_planner)
                capture("SmartBudget-Demo-Plans.png")
            }
        }

    }
}

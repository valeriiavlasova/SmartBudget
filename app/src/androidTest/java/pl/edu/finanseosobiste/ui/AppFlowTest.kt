package pl.edu.finanseosobiste.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import pl.edu.finanseosobiste.R
import pl.edu.finanseosobiste.data.AppDatabase
import pl.edu.finanseosobiste.util.DataFiles
import kotlinx.coroutines.runBlocking

@RunWith(AndroidJUnit4::class)
class AppFlowTest {
 @Test fun navigationIncomeSaveAndForecastWorkTogether() {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
        if (android.os.Build.VERSION.SDK_INT >= 33) InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(context.packageName, android.Manifest.permission.POST_NOTIFICATIONS)
  val before=java.io.ByteArrayOutputStream().also { DataFiles.backup(context,it) }.toByteArray()
  val prefs=AppSettings.preferences(context)
  prefs.edit().putString("language","en").commit()
  var scenario:ActivityScenario<MainActivity>?=null
  try {
   scenario=ActivityScenario.launch(MainActivity::class.java)
   onView(withId(R.id.nav_charts)).perform(click())
   onView(withText(R.string.mint_analytics)).check(matches(isDisplayed()))
   onView(withId(R.id.historyForecast)).perform(scrollTo(),click())
   onView(withText(android.R.string.ok)).perform(click())
   onView(withId(R.id.nav_limits)).perform(click())
   onView(withId(R.id.tvLimitsHeader)).check(matches(isDisplayed()))
   onView(withId(R.id.nav_planner)).perform(click())
   onView(withText(R.string.goals)).check(matches(isDisplayed()))
   onView(withContentDescription("More options")).perform(click())
   onView(withText(R.string.finance_title)).inRoot(androidx.test.espresso.matcher.RootMatchers.isPlatformPopup()).perform(click())
   onView(withText(R.string.finance_income)).perform(click())
   onView(withHint(R.string.plan_name)).perform(replaceText("Audit income"),closeSoftKeyboard())
   onView(withHint(R.string.hint_expense_amount)).perform(replaceText("10.20"),closeSoftKeyboard())
   onView(withText(R.string.btn_save)).perform(click())
   val sql=AppDatabase.getInstance(context).openHelper.readableDatabase
   var recorded=false
   for(attempt in 0..399) {
    recorded=sql.query("SELECT amountMinor FROM cash_entries WHERE title='Audit income' AND kind='INCOME'").use { it.moveToFirst() && it.getLong(0)==1020L }
    if(recorded) break
    androidx.test.espresso.Espresso.onIdle()
    Thread.sleep(50)
   }
   if(!recorded) {
    val image=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
    java.io.File(context.getExternalFilesDir(null),"income-failure.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
    image.recycle()
   }
   val lastRows=sql.query("SELECT title,amountMinor FROM cash_entries ORDER BY id DESC LIMIT 3").use { c -> buildString { while(c.moveToNext()) append(c.getString(0)+":"+c.getLong(1)+";") } }
   assertTrue("Income must persist from the UI; latest entries: $lastRows",recorded)
   onView(withId(R.id.nav_dashboard)).perform(click())
   onView(withId(R.id.addPurchase)).perform(click())
   onView(withId(R.id.expenseAccount)).check(matches(isDisplayed()))
   onView(withText(R.string.btn_cancel)).perform(click())
  } finally {
   scenario?.close()
   DataFiles.restoreBytes(context,before)
  }
 }
}

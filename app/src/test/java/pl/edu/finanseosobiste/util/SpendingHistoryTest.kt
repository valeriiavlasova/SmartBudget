package pl.edu.finanseosobiste.util
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
class SpendingHistoryTest {
 @Test fun exactMoneyAndInvalidPrecision() {
  assertEquals(30L,Money.parse("0.10")+Money.parse("0,20"))
  assertEquals(101L,Money.minor(1.005))
  for(value in listOf("NaN","-1","1.001","10000000000000000")) {
   try { Money.parse(value);fail(value) } catch(_:IllegalArgumentException) {} catch(_:ArithmeticException) {}
  }
 }
 @Test fun emptyHistoryIsNotAZeroPrediction() { val p=SpendingForecast.predict(emptyList(),LocalDate.of(2026,4,15));assertNull(p.historyMinor);assertNull(p.paceMinor);assertNull(p.historyMae) }
 @Test fun forecastExcludesFirstPartialMonthAndFutureRecords() {
  val rows=listOf(DailySpending(LocalDate.of(2026,1,20),99999),DailySpending(LocalDate.of(2026,2,1),10000),DailySpending(LocalDate.of(2026,3,1),20000),DailySpending(LocalDate.of(2026,4,1),1000),DailySpending(LocalDate.of(2026,4,30),999999))
  val p=SpendingForecast.predict(rows,LocalDate.of(2026,4,15))
  assertEquals(15000L,p.historyMinor);assertEquals(2000L,p.paceMinor)
  assertEquals(1,p.historyTests);assertEquals(100.0,p.historyMae!!,0.001)
 }
 @Test fun missingCompletedMonthCountsAsZero() {
  val p=SpendingForecast.predict(listOf(DailySpending(LocalDate.of(2026,1,1),10000),DailySpending(LocalDate.of(2026,3,1),20000)),LocalDate.of(2026,4,15))
  assertEquals(10000L,p.historyMinor)
 }

 @Test fun backtestDoesNotLearnFromTargetOrFutureMonths() {
  val past=(1..5).map { DailySpending(LocalDate.of(2026,it,1),10000L*it) }
  val before=SpendingForecast.predict(past,LocalDate.of(2026,6,15))
  val after=SpendingForecast.predict(past+DailySpending(LocalDate.of(2026,6,1),999999),LocalDate.of(2026,6,15))
  assertEquals(before.historyMae,after.historyMae);assertEquals(before.paceMae,after.paceMae)
  assertEquals(before.historyMinor,after.historyMinor)
 }
}

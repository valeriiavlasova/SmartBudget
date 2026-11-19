package pl.edu.finanseosobiste.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class PaymentScheduleTest {
    @Test fun monthlyPreservesAnchorAfterFebruary() {
        val feb = PaymentSchedule.next(LocalDate.of(2026, 1, 31), 1, 31)
        assertEquals(LocalDate.of(2026, 2, 28), feb)
        assertEquals(LocalDate.of(2026, 3, 31), PaymentSchedule.next(feb, 1, 31))
    }
    @Test fun weeklyCrossesYear() {
        assertEquals(LocalDate.of(2027, 1, 6), PaymentSchedule.next(LocalDate.of(2026, 12, 30), 0, 30))
    }
    @Test fun annualLeapDayReturnsInLeapYear() {
        var date = LocalDate.of(2024, 2, 29)
        repeat(4) { date = PaymentSchedule.next(date, 2, 29) }
        assertEquals(LocalDate.of(2028, 2, 29), date)
    }
}

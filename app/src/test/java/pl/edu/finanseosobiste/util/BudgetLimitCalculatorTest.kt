package pl.edu.finanseosobiste.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BudgetLimitCalculatorTest {

    @Test
    fun `evaluateLimit returns SAFE when spending is below 80 percent threshold`() {
        val result = BudgetLimitCalculator.evaluateLimit(
            spent = 450.0,
            limit = 1000.0,
            warningThresholdPercent = 80
        )

        assertEquals(LimitStatus.SAFE, result.status)
        assertEquals(45, result.percentage)
        assertEquals(550.0, result.remaining, 0.001)
    }

    @Test
    fun `evaluateLimit returns WARNING when spending reaches 80 percent threshold`() {
        val result = BudgetLimitCalculator.evaluateLimit(
            spent = 820.0,
            limit = 1000.0,
            warningThresholdPercent = 80
        )

        assertEquals(LimitStatus.WARNING, result.status)
        assertEquals(82, result.percentage)
        assertEquals(180.0, result.remaining, 0.001)
    }

    @Test
    fun `evaluateLimit returns EXCEEDED when spending reaches or exceeds 100 percent of limit`() {
        val result = BudgetLimitCalculator.evaluateLimit(
            spent = 1150.0,
            limit = 1000.0,
            warningThresholdPercent = 80
        )

        assertEquals(LimitStatus.EXCEEDED, result.status)
        assertEquals(115, result.percentage)
        assertEquals(-150.0, result.remaining, 0.001)
    }

    @Test
    fun `validateExpenseInput accepts comma and dot decimal separators`() {
        val amountWithComma = BudgetLimitCalculator.validateExpenseInput("Zakupy", "125,50")
        val amountWithDot = BudgetLimitCalculator.validateExpenseInput("Paliwo", "210.75")

        assertNotNull(amountWithComma)
        assertEquals(125.50, amountWithComma!!, 0.001)

        assertNotNull(amountWithDot)
        assertEquals(210.75, amountWithDot!!, 0.001)
    }

    @Test
    fun `validateExpenseInput rejects empty title or non-positive amount`() {
        assertNull(BudgetLimitCalculator.validateExpenseInput("", "50.0"))
        assertNull(BudgetLimitCalculator.validateExpenseInput("   ", "50.0"))
        assertNull(BudgetLimitCalculator.validateExpenseInput("Kawa", "0"))
        assertNull(BudgetLimitCalculator.validateExpenseInput("Kawa", "-15.0"))
        assertNull(BudgetLimitCalculator.validateExpenseInput("Kawa", "abc"))
    }

    @Test fun `rounded percentage does not trigger premature warnings`() {
        assertEquals(LimitStatus.SAFE,BudgetLimitCalculator.evaluateLimit(79.6,100.0).status)
        assertEquals(LimitStatus.WARNING,BudgetLimitCalculator.evaluateLimit(99.6,100.0).status)
        assertEquals(LimitStatus.EXCEEDED,BudgetLimitCalculator.evaluateLimit(100.0,100.0).status)
    }
    @Test(expected=IllegalArgumentException::class) fun `nonfinite budget is rejected`() {
        BudgetLimitCalculator.evaluateLimit(10.0,Double.NaN)
    }
    @Test fun `amount validation rejects excessive precision and huge values`() {
        assertNull(BudgetLimitCalculator.validateExpenseInput("Test","1.001"))
        assertNull(BudgetLimitCalculator.validateExpenseInput("Test","Infinity"))
        assertNull(BudgetLimitCalculator.validateExpenseInput("Test","10000000000000"))
    }
}

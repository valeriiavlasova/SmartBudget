package pl.edu.finanseosobiste.util

import org.junit.Assert.*
import org.junit.Test

class ForecastCalculatorTest {
    @Test fun surplusIncreasesBalance() {
        val points = ForecastCalculator.project(100.0, 2000.0, 1500.0, 30)
        assertEquals(100.0, points.first().balance, 0.001)
        assertEquals(600.0, points.last().balance, 0.001)
        assertEquals(1500.0, points.last().spending, 0.001)
    }
    @Test fun deficitCanGoNegative() {
        assertEquals(-400.0, ForecastCalculator.project(100.0, 500.0, 1000.0, 28).last().balance, 0.001)
    }
    @Test fun zeroFlowsPreserveDebt() {
        assertTrue(ForecastCalculator.project(-100.0, 0.0, 0.0, 31).all { it.balance == -100.0 })
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsNegativeSpending() {
        ForecastCalculator.project(0.0, 0.0, -1.0, 30)
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsNonFiniteIncome() {
        ForecastCalculator.project(0.0, Double.NaN, 0.0, 30)
    }
}

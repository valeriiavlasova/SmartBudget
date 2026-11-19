package pl.edu.finanseosobiste.util

import java.math.BigDecimal
import java.math.RoundingMode

object Money {
    const val MAX_MINOR = 100_000_000_000_000L
    fun parse(text: String, allowZero: Boolean = false): Long {
        val value=BigDecimal(text.trim().replace(',', '.')).setScale(2,RoundingMode.UNNECESSARY).movePointRight(2).longValueExact()
        require(value in (if(allowZero) 0L else 1L)..MAX_MINOR)
        return value
    }
    fun minor(value: Double): Long {
        require(value.isFinite() && value>=0)
        return BigDecimal.valueOf(value).setScale(2,RoundingMode.HALF_UP).movePointRight(2).longValueExact().also { require(it<=MAX_MINOR) }
    }
}

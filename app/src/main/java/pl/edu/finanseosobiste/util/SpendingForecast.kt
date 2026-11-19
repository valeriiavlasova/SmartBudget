package pl.edu.finanseosobiste.util

import java.time.LocalDate
import java.time.YearMonth

data class DailySpending(val date:LocalDate,val minor:Long)
data class SpendingPrediction(val historyMinor:Long?,val paceMinor:Long?,val historyMae:Double?,val paceMae:Double?,val historyTests:Int,val paceTests:Int)
object SpendingForecast {
    private fun average(values:List<Long>):Long? = if(values.isEmpty()) null else java.math.BigDecimal(values.fold(0L,Math::addExact)).divide(java.math.BigDecimal(values.size),0,java.math.RoundingMode.HALF_UP).longValueExact()
    private fun pace(total:Long,day:Int,length:Int):Long = java.math.BigDecimal(total).multiply(java.math.BigDecimal(length)).divide(java.math.BigDecimal(day),0,java.math.RoundingMode.HALF_UP).longValueExact()
    fun predict(rows:List<DailySpending>,asOf:LocalDate):SpendingPrediction {
        require(rows.all { it.minor>=0 && it.minor<=Money.MAX_MINOR && it.date.year in 1970..2100 })
        val data=rows.filter { !it.date.isAfter(asOf) };val month=YearMonth.from(asOf)
        val first=data.minOfOrNull { YearMonth.from(it.date) }
        val monthly=data.groupBy { YearMonth.from(it.date) }.mapValues { (_,entries) ->
            val prefix=LongArray(32)
            entries.forEach { prefix[it.date.dayOfMonth]=Math.addExact(prefix[it.date.dayOfMonth],it.minor) }
            for(day in 1..31) prefix[day]=Math.addExact(prefix[day],prefix[day-1])
            prefix
        }
        fun total(m:YearMonth,day:Int=m.lengthOfMonth())=monthly[m]?.get(day) ?: 0L
        fun prior(m:YearMonth)= (3 downTo 1).map { m.minusMonths(it.toLong()) }.filter { first!=null && it.isAfter(first) }.map { total(it) }
        val history=average(prior(month));val current=if(first!=null && first<=month) pace(total(month,asOf.dayOfMonth),asOf.dayOfMonth,month.lengthOfMonth()) else null
        val h=mutableListOf<Double>();val r=mutableListOf<Double>()
        if(first!=null) {
            var target=first.plusMonths(1)
            while(target<month) {
                val actual=total(target)
                average(prior(target))?.let { h.add(kotlin.math.abs(it-actual).toDouble()/100) }
                val day=minOf(asOf.dayOfMonth,target.lengthOfMonth())
                r.add(kotlin.math.abs(pace(total(target,day),day,target.lengthOfMonth())-actual).toDouble()/100)
                target=target.plusMonths(1)
            }
        }
        return SpendingPrediction(history,current,h.takeIf { it.isNotEmpty() }?.average(),r.takeIf { it.isNotEmpty() }?.average(),h.size,r.size)
    }
}

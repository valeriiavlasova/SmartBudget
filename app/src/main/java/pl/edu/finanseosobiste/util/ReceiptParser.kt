package pl.edu.finanseosobiste.util

data class ReceiptDraft(val title: String, val amount: Double?, val date: java.time.LocalDate?, val text: String, val currency: String?)

object ReceiptParser {
    fun parse(text: String, today: java.time.LocalDate = java.time.LocalDate.now()): ReceiptDraft {
        val lines=text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val money=Regex("(?<![\\d.,])(?:\\d{1,3}(?:[ \\u00a0]\\d{3})+|\\d+)[,.]\\d{2}(?![\\d.,])")
        val total=Regex("(?iu)\\b(grand total|total|razem|suma|do zapłaty|до сплати|сума|всього)\\b")
        var amount: Double?=null
        lines.forEachIndexed { index,line ->
            if(total.containsMatchIn(line) && !Regex("(?iu)sub.?total|subtotal|reszta|change|готівк|cash").containsMatchIn(line)) {
                val candidate=money.findAll(line).lastOrNull()?.value ?: lines.getOrNull(index+1)?.let { money.find(it)?.value }
                candidate?.replace(" ","")?.replace("\u00a0","")?.replace(',','.')?.toDoubleOrNull()?.takeIf { it.isFinite() && it>0 }?.let { amount=it }
            }
        }
        val dateRegex=Regex("\\b(\\d{4})[-/.](\\d{2})[-/.](\\d{2})\\b|\\b(\\d{2})[-/.](\\d{2})[-/.](\\d{4})\\b")
        val date=dateRegex.findAll(text).mapNotNull { match ->
            try {
                val g=match.groupValues
                val d=if(g[1].isNotEmpty()) java.time.LocalDate.of(g[1].toInt(),g[2].toInt(),g[3].toInt()) else java.time.LocalDate.of(g[6].toInt(),g[5].toInt(),g[4].toInt())
                d.takeIf { !it.isAfter(today) && it.year>=2000 }
            } catch(_: Exception) { null }
        }.firstOrNull()
        val currency=Regex("(?i)\\b(PLN|EUR|USD|UAH|GBP|CZK|CHF)\\b|zł|грн|€|\\$").find(text)?.value?.let {
            when(it.lowercase()) { "zł"->"PLN";"грн"->"UAH";"€"->"EUR";"$"->"USD";else->it.uppercase() }
        }
        return ReceiptDraft(lines.firstOrNull { line -> line.count { it.isLetter() }>=3 }?.take(100).orEmpty(),amount,date,text.take(20000),currency)
    }
}

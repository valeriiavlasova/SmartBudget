package pl.edu.finanseosobiste.util
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class ReceiptParserTest {
    @Test fun picksTotalInsteadOfSubtotalCashAndChange() {
        val receipt=ReceiptParser.parse("Shop\nSubtotal 100.00\nTOTAL 123.45 EUR\nCash 200.00\nChange 76.55\n11.11.2026",LocalDate.of(2026,11,11))
        assertEquals(123.45,receipt.amount!!,0.001)
        assertEquals("EUR",receipt.currency)
        assertEquals(LocalDate.of(2026,11,11),receipt.date)
    }
    @Test fun readsPolishGroupedAmountOnNextLine() {
        assertEquals(1234.56,ReceiptParser.parse("Sklep\nSUMA PLN\n1 234,56").amount!!,0.001)
    }
    @Test fun doesNotGuessTotalFromProductPrices() {
        assertNull(ReceiptParser.parse("Shop\nMilk 8.50\nBread 4.20").amount)
    }
    @Test fun rejectsInvalidAndFutureDates() {
        assertNull(ReceiptParser.parse("Shop\n31.02.2026\n2027-01-01",LocalDate.of(2026,11,11)).date)
    }
}

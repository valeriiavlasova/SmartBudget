package pl.edu.finanseosobiste.util

import org.junit.Assert.*
import org.junit.Test
import pl.edu.finanseosobiste.data.ExpenseWithCategory
import java.io.ByteArrayOutputStream

class CsvExportTest {
    @Test fun quotesCommasQuotesAndNewlinesAndKeepsUnicode() {
        val row=ExpenseWithCategory(1,"Їжа, \"Lunch\"",12.5,1,0,"Line 1\nLine 2","Food","#000000")
        val output=ByteArrayOutputStream()
        DataFiles.csv(listOf(row),"EUR",output)
        val csv=output.toString("UTF-8")
        assertTrue(csv.startsWith("\uFEFFDate,Title,Category,Amount,Currency,Note\r\n"))
        assertTrue(csv.contains("\"Їжа, \"\"Lunch\"\"\""))
        assertTrue(csv.contains("\"12.5\",\"EUR\""))
        assertTrue(csv.contains("\"Line 1\nLine 2\""))
    }
    @Test fun spreadsheetFormulasAreExportedAsText() {
        val row=ExpenseWithCategory(1,"=1+1",1.0,1,0,"@SUM(A1)","+Example","#000000")
        val output=ByteArrayOutputStream()
        DataFiles.csv(listOf(row),"USD",output)
        val csv=output.toString("UTF-8")
        assertTrue(csv.contains("\"'=1+1\""))
        assertTrue(csv.contains("\"'+Example\""))
        assertTrue(csv.contains("\"'@SUM(A1)\""))
    }
    @Test fun emptyExportStillContainsHeaders() {
        val output=ByteArrayOutputStream()
        DataFiles.csv(emptyList(),"PLN",output)
        assertEquals(1,output.toString("UTF-8").count { it=='\n' })
    }
}

package pl.edu.finanseosobiste.util

import android.content.Context
import android.net.Uri
import android.database.Cursor
import org.json.JSONArray
import org.json.JSONObject
import pl.edu.finanseosobiste.data.AppDatabase
import pl.edu.finanseosobiste.data.ExpenseWithCategory
import java.io.OutputStream

object DataFiles {
    private val tables=listOf("categories","budget_config","accounts","expenses","recurring_payments","savings_goals","month_budgets","cash_entries")
    fun backup(context: Context, output: OutputStream) {
        val database=AppDatabase.getInstance(context)
        val root=JSONObject().put("format","SmartBudget").put("version",2)
        database.runInTransaction {
            val db=database.openHelper.writableDatabase
            for(table in tables) {
                val rows=JSONArray()
                db.query("SELECT * FROM $table").use { c ->
                    while(c.moveToNext()) {
                        require(rows.length()<100000)
                        val row=JSONObject()
                        c.columnNames.forEachIndexed { index,name -> row.put(name,when(c.getType(index)) { Cursor.FIELD_TYPE_INTEGER -> c.getLong(index);Cursor.FIELD_TYPE_FLOAT -> c.getDouble(index);else -> c.getString(index) }) }
                        rows.put(row)
                    }
                }
                root.put(table,rows)
            }
        }
        root.put("currency",BudgetLimitCalculator.currencyCode)
        val prefs=pl.edu.finanseosobiste.ui.AppSettings.preferences(context)
        val settings=JSONObject()
        for(key in listOf("language","theme","dark","forecast_start","forecast_income","forecast_spend","savings_contribution")) {
            prefs.all[key]?.let { settings.put(key,it) }
        }
        for (key in listOf("notify_limits") + pl.edu.finanseosobiste.notifications.MoneyEvent.values().map { it.preferenceKey }) {
            settings.put(key, prefs.getBoolean(key, true))
        }
        root.put("settings",settings)
        val bytes=root.toString(2).toByteArray(Charsets.UTF_8)
        require(bytes.size<=10*1024*1024)
        output.write(bytes)
    }
    fun restore(context: Context, uri: Uri) {
        val bytes=context.contentResolver.openInputStream(uri)!!.use { input ->
            val buffer=java.io.ByteArrayOutputStream();val chunk=ByteArray(8192)
            while(true) { val count=input.read(chunk);if(count<0) break;buffer.write(chunk,0,count);require(buffer.size()<=10*1024*1024) };buffer.toByteArray() }
        require(bytes.size<=10*1024*1024)
        restoreBytes(context,bytes)
    }
    fun restoreBytes(context: Context, bytes: ByteArray) {
        require(bytes.size<=10*1024*1024)
        val root=JSONObject(String(bytes,Charsets.UTF_8))
        require(root.getString("format")=="SmartBudget" && root.getInt("version") in 1..2)
        if(root.getInt("version")==1) {
            root.put("accounts",JSONArray().put(JSONObject().put("id",1).put("name","Main account").put("openingMinor",0)))
            root.put("cash_entries",JSONArray())
            val expenses=root.getJSONArray("expenses")
            for(i in 0 until expenses.length()) expenses.getJSONObject(i).put("accountId",1)
        }
        val currency=root.getString("currency")
        require(currency in pl.edu.finanseosobiste.ui.AppSettings.currencies)
        val settings=root.optJSONObject("settings") ?: JSONObject()
        if(settings.has("language")) require(settings.getString("language") in listOf("en","uk","pl"))
        if(settings.has("theme")) require(settings.getInt("theme") in 0..12)
        if(settings.has("dark")) require(settings.get("dark") is Boolean)
        for(key in listOf("forecast_start","forecast_income","forecast_spend","savings_contribution")) if(settings.has(key)) require(settings.getString(key).toDouble().isFinite())
        val notificationKeys = listOf("notify_limits") + pl.edu.finanseosobiste.notifications.MoneyEvent.values().map { it.preferenceKey }
        for (key in notificationKeys) if (settings.has(key)) require(settings.get(key) is Boolean)
        val database=AppDatabase.getInstance(context)
        database.runInTransaction {
            val db=database.openHelper.writableDatabase
            // Validate every row before clearing anything; the transaction rolls back on any failure.
            val columns=mutableMapOf<String,List<String>>()
            for(table in tables) {
                val names=mutableListOf<String>()
                val types=mutableMapOf<String,String>()
                db.query("PRAGMA table_info($table)").use { c -> while(c.moveToNext()) { names.add(c.getString(1));types[c.getString(1)]=c.getString(2) } }
                columns[table]=names
                val rows=root.getJSONArray(table)
                require(rows.length()<=100000)
                for(i in 0 until rows.length()) {
                    val row=rows.getJSONObject(i)
                    require(row.keys().asSequence().toSet()==names.toSet())
                    names.forEach { name ->
                        val value=row.get(name)
                        if(types[name]=="TEXT") require(value is String)
                        else require(value is Number)
                        if(types[name]=="INTEGER") require(value is Number && value.toDouble()==value.toLong().toDouble())
                        if(value is Number) require(value.toDouble().isFinite())
                    }
                    listOf("amount","target","monthlyLimit","totalMonthlyLimit").filter { row.has(it) }.forEach { require(row.getDouble(it) in 0.0..1e12) }
                    if(table=="expenses") {
                        require(row.getDouble("amount")>0)
                        val time=row.getLong("timestamp")
                        require(time in 0..System.currentTimeMillis())
                        require(java.time.Instant.ofEpochMilli(time).atZone(java.time.ZoneId.systemDefault()).year in 1970..2100)
                    }
                    if(row.has("saved")) require(row.getDouble("saved")<=1e12)
                    if(row.has("title")) require(row.getString("title").isNotBlank())
                    if(row.has("name")) require(row.getString("name").isNotBlank())
                    if(row.has("id")) require(row.getLong("id")>0)
                    if(row.has("warningThresholdPercent")) { require(row.getInt("id")==1);require(row.getInt("warningThresholdPercent") in 0..100) }
                    if(row.has("target")) require(row.getDouble("target")>0)
                    if(row.has("saved")) require(row.getDouble("saved")>=0)
                    if(row.has("frequency")) { require(row.getInt("frequency") in 0..2);require(row.getInt("anchorDay") in 1..31);java.time.LocalDate.parse(row.getString("nextDate")) }
                    if(table=="accounts") require(row.getLong("openingMinor") in 0..Money.MAX_MINOR)
                    if(table=="cash_entries") {
                        val amount=row.getLong("amountMinor")
                        require(amount!=0L && amount in -Money.MAX_MINOR..Money.MAX_MINOR)
                        require(row.getLong("timestamp") in 0..System.currentTimeMillis())
                        require(row.getString("kind") in listOf("INCOME","TRANSFER"))
                        if(row.getString("kind")=="INCOME") require(amount>0 && row.getString("transferKey").isEmpty())
                        else require(row.getString("transferKey").isNotBlank())
                    }
                    if(row.has("month")) java.time.YearMonth.parse(row.getString("month"))
                }
            }
            val entries=root.getJSONArray("cash_entries")
            val transfers=(0 until entries.length()).map { entries.getJSONObject(it) }.filter { it.getString("kind")=="TRANSFER" }.groupBy { it.getString("transferKey") }
            transfers.values.forEach { pair ->
                require(pair.size==2 && pair[0].getLong("accountId")!=pair[1].getLong("accountId"))
                require(pair[0].getLong("amountMinor")==-pair[1].getLong("amountMinor"))
                require(pair[0].getLong("timestamp")==pair[1].getLong("timestamp"))
            }
            val accounts=root.getJSONArray("accounts")
            require((0 until accounts.length()).any { accounts.getJSONObject(it).getLong("id")==1L })
            for(table in tables.reversed()) db.execSQL("DELETE FROM $table")
            for(table in tables) {
                val rows=root.getJSONArray(table);val names=columns.getValue(table)
                for(i in 0 until rows.length()) {
                    val row=rows.getJSONObject(i)
                    val placeholders=names.joinToString(",") { "?" }
                    db.execSQL("INSERT INTO $table (${names.joinToString(",")}) VALUES ($placeholders)",names.map { row.get(it) }.toTypedArray())
                }
            }
            db.query("SELECT 1 FROM expenses WHERE accountId NOT IN (SELECT id FROM accounts)").use { require(!it.moveToFirst()) }
            db.query("PRAGMA foreign_key_check").use { require(!it.moveToFirst()) }
            db.query("SELECT 1 FROM recurring_payments WHERE categoryId NOT IN (SELECT id FROM categories)").use { require(!it.moveToFirst()) }
        }
        val editor=pl.edu.finanseosobiste.ui.AppSettings.preferences(context).edit().putString("currency",currency)
        for(key in listOf("language","forecast_start","forecast_income","forecast_spend","savings_contribution")) if(settings.has(key)) editor.putString(key,settings.getString(key))
        if(settings.has("theme")) editor.putInt("theme",settings.getInt("theme"))
        if(settings.has("dark")) editor.putBoolean("dark",settings.getBoolean("dark"))
        for (key in notificationKeys) editor.putBoolean(key, settings.optBoolean(key, true))
        editor.commit()
        pl.edu.finanseosobiste.ui.AppSettings.configureMoney(context)
    }
    fun csv(rows: List<ExpenseWithCategory>, currency: String, output: OutputStream) {
        fun quote(text: String): String {
            val safe=if(text.trimStart().firstOrNull() in listOf('=','+','-','@','\t','\r')) "'"+text else text
            return "\""+safe.replace("\"","\"\"")+"\""
        }
        output.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write("\uFEFFDate,Title,Category,Amount,Currency,Note\r\n")
            for(row in rows) writer.write(listOf(java.time.Instant.ofEpochMilli(row.timestamp).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString(),row.title,row.categoryName,row.amount.toString(),currency,row.note).joinToString(",") { quote(it) }+"\r\n")
        }
    }
    fun pdf(rows: List<ExpenseWithCategory>, currency: String, output: OutputStream) {
        val document=android.graphics.pdf.PdfDocument()
        val paint=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { textSize=11f }
        var page: android.graphics.pdf.PdfDocument.Page?=null
        var y=0f;var pageNumber=0
        fun nextPage() {
            page?.let { document.finishPage(it) }
            page=document.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(595,842,++pageNumber).create())
            y=42f;paint.textSize=17f;page!!.canvas.drawText("SmartBudget · Purchases · $currency",32f,y,paint);paint.textSize=11f;y+=26
        }
        fun line(text: String) {
            // Wrap by measured characters, including long unbroken titles.
            var rest=text.replace("\r", " ").replace("\n", " ")
            do {
                if(page==null || y>804) nextPage()
                val count=paint.breakText(rest,true,530f,null).coerceAtLeast(1).coerceAtMost(rest.length)
                page!!.canvas.drawText(rest.take(count),32f,y,paint);y+=16
                rest=rest.drop(count)
            } while(rest.isNotEmpty())
        }
        try {
            nextPage()
            line("Records: ${rows.size} · Total: ${rows.sumOf { it.amount }} $currency")
            for(row in rows) {
                y+=8;line("${BudgetLimitCalculator.formatDate(row.timestamp)} · ${row.title}")
                line("${row.categoryName} · ${row.amount} $currency")
                if(row.note.isNotBlank()) line(row.note)
            }
            page?.let { document.finishPage(it) };page=null;document.writeTo(output)
        } finally { page?.let { document.finishPage(it) }; document.close() }
    }
}

package pl.edu.finanseosobiste.data

import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

internal object DemoData {
    fun seed(db: SupportSQLiteDatabase, today: LocalDate = LocalDate.now()) {
        fun stamp(date: LocalDate) = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val current = YearMonth.from(today)
        val categories = listOf(
            CategoryEntity(1, "Jedzenie i zakupy", "#4CAF50", 1000.0),
            CategoryEntity(2, "Mieszkanie i rachunki", "#9C27B0", 1800.0),
            CategoryEntity(3, "Transport i paliwo", "#2196F3", 400.0),
            CategoryEntity(4, "Rozrywka i kultura", "#FF9800", 300.0),
            CategoryEntity(5, "Zdrowie i sport", "#E91E63", 350.0),
            CategoryEntity(6, "Inne wydatki", "#607D8B", 250.0)
        )
        categories.forEach {
            db.execSQL("INSERT INTO categories(id,name,colorHex,monthlyLimit) VALUES(?,?,?,?)",
                arrayOf(it.id,it.name,it.colorHex,it.monthlyLimit))
        }
        db.execSQL("INSERT INTO accounts(id,name,openingMinor) VALUES(1,'Main account',250000),(2,'Gotówka',40000),(3,'Oszczędności',600000)")
        db.execSQL("INSERT INTO budget_config(id,totalMonthlyLimit,warningThresholdPercent) VALUES(1,4100,80)")
        for (ago in 5 downTo -1) {
            val month = current.minusMonths(ago.toLong())
            val budget = when { ago >= 4 -> 3800; ago >= 2 -> 3950; ago >= 0 -> 4100; else -> 4300 }
            db.execSQL("INSERT INTO month_budgets(month,amount) VALUES(?,?)", arrayOf(month.toString(),budget))
            if (ago < 0) continue
            fun cash(day: Int, account: Long, title: String, minor: Long, kind: String = "INCOME", key: String = "") {
                val date = month.atDay(day)
                if (date <= today) db.execSQL("INSERT INTO cash_entries(accountId,title,amountMinor,timestamp,kind,transferKey) VALUES(?,?,?,?,?,?)",
                    arrayOf(account,title,minor,stamp(date),kind,key))
            }
            cash(1,1,"Wynagrodzenie",560000 + (5-ago)*5000L)
            cash(8,1,"Zlecenie dodatkowe",45000 + (ago%3)*5000L)
            cash(2,1,"Wypłata gotówki",-35000,"TRANSFER","demo-cash-$month")
            cash(2,2,"Wypłata gotówki",35000,"TRANSFER","demo-cash-$month")
            cash(3,1,"Przelew na oszczędności",-50000,"TRANSFER","demo-save-$month")
            cash(3,3,"Przelew na oszczędności",50000,"TRANSFER","demo-save-$month")
            fun expense(day: Int, title: String, amount: Double, category: Long, account: Long = 1) {
                val date = month.atDay(day)
                if (date <= today) db.execSQL("INSERT INTO expenses(title,amount,categoryId,timestamp,note,accountId) VALUES(?,?,?,?,?,?)",
                    arrayOf(title,amount,category,stamp(date),"Przykład demonstracyjny",account))
            }
            val variation = (ago%3)*12.0
            expense(1,"Czynsz za mieszkanie",1450.0,2)
            expense(2,"Zakupy spożywcze — Biedronka",186.40+variation,1)
            expense(3,"Bilet miesięczny",110.0,3)
            expense(4,"Internet i telefon",109.0,2)
            expense(5,"Karnet na siłownię",149.0,5)
            expense(6,"Lunch na uczelni",32.0,1,2)
            expense(7,"Zakupy spożywcze — Lidl",212.75-variation,1)
            expense(9,"Kino ze znajomymi",58.0,4,2)
            expense(10,"Paliwo",160.0+variation,3)
            expense(11,"Leki i witaminy",64.90,5)
            expense(12,"Książki i materiały do nauki",86.0+variation,6)
            expense(13,"Kawa i ciasto",27.50,1,2)
            expense(14,"Zakupy na tydzień",178.20+variation,1)
            expense(15,"Rachunek za prąd",172.40,2)
            expense(16,"Koncert",189.0,4)
            expense(17,"Warzywa i owoce",72.60,1,2)
            expense(18,"Dojazd na uczelnię",24.0,3,2)
            expense(20,"Zakupy spożywcze",163.80,1)
            expense(22,"Spotkanie ze znajomymi",78.0,4,2)
            expense(24,"Artykuły do domu",49.90,6)
            expense(26,"Wizyta u dentysty",120.0,5)
            expense(28,"Zakupy na weekend",145.30,1)
        }
        db.execSQL("INSERT INTO savings_goals(title,target,saved) VALUES('Poduszka finansowa',10000,4200),('Wakacje',4500,1800),('Nowy laptop',6000,1500)")
        fun recurring(title: String, amount: Double, category: Long, date: LocalDate, frequency: Int) {
            db.execSQL("INSERT INTO recurring_payments(title,amount,categoryId,nextDate,frequency,anchorDay) VALUES(?,?,?,?,?,?)",
                arrayOf(title,amount,category,date.toString(),frequency,date.dayOfMonth))
        }
        recurring("Internet i telefon",109.0,2,today,1)
        recurring("Czynsz za mieszkanie",1450.0,2,current.plusMonths(1).atDay(1),1)
        recurring("Zajęcia jogi",35.0,5,today.plusDays(4),0)
        recurring("Ubezpieczenie mieszkania",240.0,2,today.plusMonths(2),2)
    }
}

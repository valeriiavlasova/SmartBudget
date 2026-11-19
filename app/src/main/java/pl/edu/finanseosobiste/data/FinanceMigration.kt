package pl.edu.finanseosobiste.data

object FinanceMigration {
    val FROM_3 = object : androidx.room.migration.Migration(3,4) {
        override fun migrate(db:androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS accounts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,name TEXT NOT NULL,openingMinor INTEGER NOT NULL)")
            db.execSQL("INSERT INTO accounts(id,name,openingMinor) VALUES(1,'Main account',0)")
            db.execSQL("ALTER TABLE expenses ADD COLUMN accountId INTEGER NOT NULL DEFAULT 1")
            db.execSQL("CREATE TABLE IF NOT EXISTS cash_entries (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,accountId INTEGER NOT NULL,title TEXT NOT NULL,amountMinor INTEGER NOT NULL,timestamp INTEGER NOT NULL,kind TEXT NOT NULL,transferKey TEXT NOT NULL,FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE RESTRICT)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_cash_entries_accountId ON cash_entries(accountId)")
        }
    }
}

package pl.edu.finanseosobiste.data
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import kotlinx.coroutines.runBlocking
@RunWith(AndroidJUnit4::class)
class FinanceTest {
 private lateinit var db:AppDatabase
 @Before fun setup() { db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),AppDatabase::class.java).allowMainThreadQueries().build() }
 @After fun close() { db.close() }
 @Test fun transferIsAtomicAndNotAnExpense()=runBlocking {
  val dao=db.financeDao();val a=dao.addAccount(Account(name="Cash"));val b=dao.addAccount(Account(name="Card"))
  dao.income(a,"Salary",10000,0);dao.transfer(a,b,2500,0,"Move")
  db.openHelper.readableDatabase.query("SELECT SUM(amountMinor) FROM cash_entries").use { it.moveToFirst();assertEquals(10000L,it.getLong(0)) }
  assertEquals(0.0,db.budgetDao().getTotalSpentSince(0),0.0)
  try { dao.transfer(a,999,100,0,"Invalid");fail() } catch(_:IllegalArgumentException) {}
  db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM cash_entries").use { it.moveToFirst();assertEquals(3,it.getInt(0)) }
 }
 @Test fun migrationAddsAccountsAndPreservesExpenses() {
  // Run the migration against a minimal legacy v3 expense table in an isolated SQLite file.
  val context=ApplicationProvider.getApplicationContext<android.content.Context>()
  val file=java.io.File(context.cacheDir,"migration-"+java.util.UUID.randomUUID()+".db")
  val helper=androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory().create(androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context).name(file.absolutePath).callback(object:androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
   override fun onCreate(d:androidx.sqlite.db.SupportSQLiteDatabase) { d.execSQL("CREATE TABLE expenses(id INTEGER PRIMARY KEY, amount REAL)");d.execSQL("INSERT INTO expenses VALUES(7,12.34)") }
   override fun onUpgrade(d:androidx.sqlite.db.SupportSQLiteDatabase,a:Int,b:Int) {}
  }).build())
  try {
   val old=helper.writableDatabase;FinanceMigration.FROM_3.migrate(old)
   old.query("SELECT id,amount,accountId FROM expenses").use { it.moveToFirst();assertEquals(7,it.getInt(0));assertEquals(12.34,it.getDouble(1),0.0);assertEquals(1,it.getInt(2)) }
   old.query("SELECT openingMinor FROM accounts WHERE id=1").use { assertTrue(it.moveToFirst());assertEquals(0L,it.getLong(0)) }
  } finally { helper.close();file.delete() }
 }
 @Test fun backupRestoresAccountsAndRejectsBrokenTransfer() {
  val context=ApplicationProvider.getApplicationContext<android.content.Context>()
  val bytes=java.io.ByteArrayOutputStream().also { pl.edu.finanseosobiste.util.DataFiles.backup(context,it) }.toByteArray()
  pl.edu.finanseosobiste.util.DataFiles.restoreBytes(context,bytes)
  val bad=org.json.JSONObject(String(bytes,Charsets.UTF_8))
  bad.put("cash_entries",org.json.JSONArray().put(org.json.JSONObject().put("id",999).put("accountId",1).put("title","Bad").put("amountMinor",100).put("timestamp",0).put("kind","TRANSFER").put("transferKey","broken")))
  val before=java.io.ByteArrayOutputStream().also { pl.edu.finanseosobiste.util.DataFiles.backup(context,it) }.toByteArray()
  try { pl.edu.finanseosobiste.util.DataFiles.restoreBytes(context,bad.toString().toByteArray());fail() } catch(_:IllegalArgumentException) {}
  val after=java.io.ByteArrayOutputStream().also { pl.edu.finanseosobiste.util.DataFiles.backup(context,it) }.toByteArray()
  assertEquals(org.json.JSONObject(String(before)).getJSONArray("expenses").toString(),org.json.JSONObject(String(after)).getJSONArray("expenses").toString())
 }

 @Test fun balancesUseExactCentsAndExpenseEditsAndDeletes()=runBlocking {
  val f=db.financeDao();val a=f.addAccount(Account(name="Cash",openingMinor=10000));val b=f.addAccount(Account(name="Card"))
  val c=db.budgetDao().insertCategory(CategoryEntity(name="Food",colorHex="#000000",monthlyLimit=100.0))
  f.income(a,"Salary",50000,0);f.transfer(a,b,20000,0,"Move")
  val expense=ExpenseEntity(title="Coffee",amount=0.1,categoryId=c,accountId=b)
  val id=db.budgetDao().insertExpense(expense)
  db.budgetDao().insertExpense(expense.copy(amount=0.2))
  var values=f.balanceSnapshot().associateBy { it.id }
  assertEquals(40000L,values.getValue(a).balanceMinor);assertEquals(19970L,values.getValue(b).balanceMinor)
  db.budgetDao().updateExpense(expense.copy(id=id,amount=1.0,accountId=a))
  values=f.balanceSnapshot().associateBy { it.id }
  assertEquals(39900L,values.getValue(a).balanceMinor);assertEquals(19980L,values.getValue(b).balanceMinor)
  db.budgetDao().deleteExpenseById(id)
  assertEquals(40000L,f.balanceSnapshot().first { it.id==a }.balanceMinor)
 }
 @Test fun failingSecondTransferInsertRollsBackFirstLeg()=runBlocking {
  val f=db.financeDao();val a=f.addAccount(Account(name="Cash"));val b=f.addAccount(Account(name="Card"))
  db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_transfer BEFORE INSERT ON cash_entries WHEN NEW.amountMinor > 0 AND NEW.kind='TRANSFER' BEGIN SELECT RAISE(ABORT,'test failure'); END")
  try { f.transfer(a,b,100,0,"Move");fail() } catch(_:android.database.sqlite.SQLiteException) {}
  db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM cash_entries").use { it.moveToFirst();assertEquals(0,it.getInt(0)) }
 }
 @Test fun fullRoomMigrationValidatesLegacySchema() {
  val ctx=ApplicationProvider.getApplicationContext<android.content.Context>()
  val name="legacy-"+java.util.UUID.randomUUID()+".db"
  val statements=mutableListOf<String>()
  db.openHelper.readableDatabase.query("SELECT sql FROM sqlite_master WHERE type IN ('table','index') AND name NOT LIKE 'sqlite_%' AND name NOT IN ('room_master_table','android_metadata','accounts','cash_entries','index_cash_entries_accountId') AND sql IS NOT NULL ORDER BY type DESC").use {
   while(it.moveToNext()) statements.add(it.getString(0).replace("`accountId` INTEGER NOT NULL DEFAULT 1, ",""))
  }
  val helper=androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory().create(androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(ctx).name(name).callback(object:androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(3) {
   override fun onCreate(db:androidx.sqlite.db.SupportSQLiteDatabase) {
    statements.forEach { db.execSQL(it) }
    db.execSQL("INSERT INTO categories(id,name,colorHex,monthlyLimit) VALUES(1,'Food','#000000',100)")
    db.execSQL("INSERT INTO expenses(id,title,amount,categoryId,timestamp,note) VALUES(1,'Legacy',12.34,1,0,'keep')")
   }
   override fun onUpgrade(db:androidx.sqlite.db.SupportSQLiteDatabase,oldVersion:Int,newVersion:Int) {}
  }).build())
  helper.writableDatabase;helper.close()
  val migrated=Room.databaseBuilder(ctx,AppDatabase::class.java,name).addMigrations(FinanceMigration.FROM_3).allowMainThreadQueries().build()
  try { migrated.openHelper.readableDatabase.query("SELECT title,amount,accountId,note FROM expenses WHERE id=1").use {
   assertTrue(it.moveToFirst());assertEquals("Legacy",it.getString(0));assertEquals(12.34,it.getDouble(1),0.0);assertEquals(1L,it.getLong(2));assertEquals("keep",it.getString(3))
  } } finally { migrated.close();ctx.deleteDatabase(name) }
 }

 @Test fun legacyBackupImportsIntoMainAccount() {
  val ctx=ApplicationProvider.getApplicationContext<android.content.Context>()
  val original=java.io.ByteArrayOutputStream().also { pl.edu.finanseosobiste.util.DataFiles.backup(ctx,it) }.toByteArray()
  try {
   val root=org.json.JSONObject(String(original,Charsets.UTF_8)).put("version",1)
   root.remove("accounts");root.remove("cash_entries")
   val rows=root.getJSONArray("expenses")
   for(i in 0 until rows.length()) rows.getJSONObject(i).remove("accountId")
   pl.edu.finanseosobiste.util.DataFiles.restoreBytes(ctx,root.toString().toByteArray())
   val result=org.json.JSONObject(java.io.ByteArrayOutputStream().also { pl.edu.finanseosobiste.util.DataFiles.backup(ctx,it) }.toString("UTF-8"))
   assertEquals(1,result.getJSONArray("accounts").length());assertEquals(0,result.getJSONArray("cash_entries").length())
   val expenses=result.getJSONArray("expenses")
   assertEquals(rows.length(),expenses.length())
   for(i in 0 until expenses.length()) assertEquals(1L,expenses.getJSONObject(i).getLong("accountId"))
  } finally { pl.edu.finanseosobiste.util.DataFiles.restoreBytes(ctx,original) }
 }

 @Test fun firstOpenSeedsAllTablesBeforeReturning() {
  val seeded=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),AppDatabase::class.java)
   .allowMainThreadQueries().addCallback(AppDatabase.PrepopulateCallback()).build()
  try {
   val sql=seeded.openHelper.readableDatabase
   for((table,count) in listOf("accounts" to 1,"categories" to 6,"expenses" to 5,"budget_config" to 1)) {
    sql.query("SELECT COUNT(*) FROM $table").use { it.moveToFirst();assertEquals(count,it.getInt(0)) }
   }
   sql.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
  } finally { seeded.close() }
 }
 @Test fun savingsCannotOverflowLimit()=runBlocking {
  db.budgetDao().addGoal(SavingsGoal(id=1,title="Limit",target=100.0,saved=1e12-1))
  assertEquals(0,db.budgetDao().contribute(1,2.0))
  assertEquals(1,db.budgetDao().contribute(1,1.0))
 }
 @Test fun invalidTransferTitleAndDateLeaveNoEntries()=runBlocking {
  val f=db.financeDao();val a=f.addAccount(Account(name="A"));val b=f.addAccount(Account(name="B"))
  try { f.transfer(a,b,100,0," ");fail() } catch(_:IllegalArgumentException) {}
  try { f.income(a,"Salary",100,-1);fail() } catch(_:IllegalArgumentException) {}
  db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM cash_entries").use { it.moveToFirst();assertEquals(0,it.getInt(0)) }
 }
}

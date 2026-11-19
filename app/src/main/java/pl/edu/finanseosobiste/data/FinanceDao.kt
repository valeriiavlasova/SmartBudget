package pl.edu.finanseosobiste.data

import androidx.room.*
import androidx.lifecycle.LiveData

private const val BALANCE_SQL = "SELECT a.id,a.name,a.openingMinor + COALESCE((SELECT SUM(amountMinor) FROM cash_entries WHERE accountId=a.id),0) - COALESCE((SELECT SUM(CAST(ROUND(amount*100) AS INTEGER)) FROM expenses WHERE accountId=a.id),0) AS balanceMinor FROM accounts a ORDER BY a.id"

@Dao
interface FinanceDao {
    @Update suspend fun updateAccount(account:Account):Int
    @Insert suspend fun addAccount(account:Account):Long
    @Insert suspend fun insertEntry(entry:CashEntry):Long
    @Query("SELECT * FROM accounts ORDER BY id") fun accounts():LiveData<List<Account>>
    @Query("SELECT * FROM accounts WHERE id=:id") suspend fun account(id:Long):Account?
    @Query(BALANCE_SQL) fun balances():LiveData<List<AccountBalance>>
    @Query(BALANCE_SQL) suspend fun balanceSnapshot():List<AccountBalance>

    @Query("SELECT * FROM cash_entries ORDER BY timestamp DESC,id DESC") fun entries():LiveData<List<CashEntry>>
    @Query("DELETE FROM cash_entries WHERE id=:id AND kind='INCOME'") suspend fun deleteIncome(id:Long)
    @Query("DELETE FROM cash_entries WHERE transferKey=:key AND kind='TRANSFER' AND transferKey!=''") suspend fun deleteTransfer(key:String)
    @Query("SELECT * FROM expenses ORDER BY timestamp") suspend fun allExpenses():List<ExpenseEntity>
    @Transaction suspend fun income(accountId:Long,title:String,minor:Long,timestamp:Long) {
        require(account(accountId)!=null && title.isNotBlank() && minor in 1..pl.edu.finanseosobiste.util.Money.MAX_MINOR)
        require(timestamp in 0..System.currentTimeMillis())
        insertEntry(CashEntry(accountId=accountId,title=title.trim(),amountMinor=minor,timestamp=timestamp,kind="INCOME"))
    }
    @Transaction suspend fun transfer(from:Long,to:Long,minor:Long,timestamp:Long,title:String) {
        require(from!=to && account(from)!=null && account(to)!=null && minor in 1..pl.edu.finanseosobiste.util.Money.MAX_MINOR)
        require(timestamp in 0..System.currentTimeMillis())
        require(title.isNotBlank())
        val key=java.util.UUID.randomUUID().toString()
        insertEntry(CashEntry(accountId=from,title=title,amountMinor=-minor,timestamp=timestamp,kind="TRANSFER",transferKey=key))
        insertEntry(CashEntry(accountId=to,title=title,amountMinor=minor,timestamp=timestamp,kind="TRANSFER",transferKey=key))
    }
}

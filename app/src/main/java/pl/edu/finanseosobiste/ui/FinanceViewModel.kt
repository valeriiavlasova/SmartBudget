package pl.edu.finanseosobiste.ui

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.*
import pl.edu.finanseosobiste.data.*
import pl.edu.finanseosobiste.util.Money

class FinanceViewModel(app:Application):AndroidViewModel(app) {
    private val dao=AppDatabase.getInstance(app).financeDao()
    val accounts=dao.accounts();val balances=dao.balances();val entries=dao.entries()
    val message=MutableLiveData<Int?>()
    private val notifications=pl.edu.finanseosobiste.notifications.NotificationHelper(app)
    fun action(block:suspend ()->Unit) { viewModelScope.launch {
        try { withContext(Dispatchers.IO) { block() };message.value=pl.edu.finanseosobiste.R.string.saved_success }
        catch(e:CancellationException) { throw e }
        catch(_:Exception) { message.value=pl.edu.finanseosobiste.R.string.save_error }
    } }
    fun addAccount(name:String,opening:Long)=action { require(name.isNotBlank() && opening in 0..Money.MAX_MINOR);dao.addAccount(Account(name=name.trim(),openingMinor=opening)) }
    fun editAccount(account:Account)=action { require(account.name.isNotBlank() && account.openingMinor in 0..Money.MAX_MINOR);require(dao.updateAccount(account)>0) }
    fun income(id:Long,title:String,amount:Long,date:Long)=action { dao.income(id,title,amount,date);notifications.sendMoneyEvent(pl.edu.finanseosobiste.notifications.MoneyEvent.INCOME_RECORDED,title,amount/100.0) }
    fun transfer(from:Long,to:Long,amount:Long,date:Long,title:String)=action { dao.transfer(from,to,amount,date,title);notifications.sendMoneyEvent(pl.edu.finanseosobiste.notifications.MoneyEvent.TRANSFER_RECORDED,title,amount/100.0) }
    fun remove(entry:CashEntry)=action { if(entry.kind=="INCOME") dao.deleteIncome(entry.id) else dao.deleteTransfer(entry.transferKey) }
}

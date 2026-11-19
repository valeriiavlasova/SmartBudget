package pl.edu.finanseosobiste.ui

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import kotlinx.coroutines.*
import pl.edu.finanseosobiste.R
import pl.edu.finanseosobiste.util.*
import java.time.*

class HistoryForecastModel(app:android.app.Application):AndroidViewModel(app) {
    val result=MutableLiveData<SpendingPrediction?>();val failed=MutableLiveData(false)
    init { viewModelScope.launch {
        try { result.value=withContext(Dispatchers.IO) {
            val rows=pl.edu.finanseosobiste.data.AppDatabase.getInstance(app).financeDao().allExpenses()
            SpendingForecast.predict(rows.map { DailySpending(Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate(),Money.minor(it.amount)) },LocalDate.now())
        } } catch(e:CancellationException) { throw e } catch(_:Exception) { failed.value=true }
    } }
}
class HistoryForecastDialog:DialogFragment() {
    private val model:HistoryForecastModel by viewModels()
    override fun onCreateDialog(state:Bundle?):Dialog = AlertDialog.Builder(requireContext()).setTitle(R.string.history_forecast).setMessage(R.string.finance_loading).setPositiveButton(android.R.string.ok,null).setNeutralButton(R.string.forecast_details,null).create()
    override fun onStart() {
        super.onStart()
        (dialog as AlertDialog).getButton(AlertDialog.BUTTON_NEUTRAL).isEnabled=false
        model.failed.removeObservers(this)
        model.result.removeObservers(this)
        model.failed.observe(this) { if(it) (dialog as AlertDialog).setMessage(getString(R.string.save_error)) }
        model.result.observe(this) { p -> if(p!=null) {
            fun money(n:Long?)=n?.let { MintUi.money(it/100.0) } ?: getString(R.string.forecast_insufficient)
            fun error(n:Double?,count:Int)=n?.let { MintUi.money(it)+" ($count)" } ?: getString(R.string.forecast_insufficient)
            (dialog as AlertDialog).apply {
                getButton(AlertDialog.BUTTON_NEUTRAL).isEnabled=true
                setMessage(getString(R.string.history_forecast_explanation)+"\n\n"+
                    getString(R.string.forecast_history)+"\n"+money(p.historyMinor)+"\n\n"+
                    getString(R.string.forecast_pace)+"\n"+money(p.paceMinor)+"\n\n"+
                    getString(R.string.forecast_note))
                getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                    AlertDialog.Builder(requireContext()).setTitle(R.string.forecast_details)
                        .setMessage(getString(R.string.forecast_accuracy_help)+"\n\n"+
                            getString(R.string.forecast_history)+": "+error(p.historyMae,p.historyTests)+"\n\n"+
                            getString(R.string.forecast_pace)+": "+error(p.paceMae,p.paceTests))
                        .setPositiveButton(android.R.string.ok,null).show()
                }
            }
        } }
    }
}

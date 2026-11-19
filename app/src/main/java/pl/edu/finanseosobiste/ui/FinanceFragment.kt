package pl.edu.finanseosobiste.ui

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.appcompat.app.AlertDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import pl.edu.finanseosobiste.R
import pl.edu.finanseosobiste.data.*
import pl.edu.finanseosobiste.util.Money

class FinanceFragment:Fragment() {
    private val model:FinanceViewModel by viewModels()
    private var content:LinearLayout?=null
    private fun dp(n:Int)=(n*resources.displayMetrics.density).toInt()
    private fun column()=LinearLayout(requireContext()).apply { orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(12),dp(16),dp(12)) }
    private fun text(s:String,size:Float=14f)=TextView(requireContext()).apply { text=s;textSize=size;if(size>=18f) setTypeface(typeface,android.graphics.Typeface.BOLD);setTextColor(requireContext().getColor(R.color.text_primary));setPadding(0,dp(5),0,dp(5)) }
    private fun button(label:Int,run:()->Unit)=MaterialButton(requireContext()).apply { setText(label);setOnClickListener { run() } }
    override fun onCreateView(i:LayoutInflater,c:ViewGroup?,state:Bundle?):View = androidx.core.widget.NestedScrollView(requireContext()).apply { val layout=column();content=layout;addView(layout) }
    override fun onViewCreated(view:View,state:Bundle?) {
        model.accounts.observe(viewLifecycleOwner) { render() }
        model.balances.observe(viewLifecycleOwner) { render() };model.entries.observe(viewLifecycleOwner) { render() }
        model.message.observe(viewLifecycleOwner) { it?.let { Toast.makeText(requireContext(),it,Toast.LENGTH_LONG).show();model.message.value=null } }
    }
    private fun render() {
        val root=content ?: return;root.removeAllViews()
        root.addView(text(getString(R.string.finance_title),28f));root.addView(text(getString(R.string.finance_help)))
        val balances=model.balances.value.orEmpty()
        root.addView(text(getString(R.string.finance_total)+": "+MintUi.money(balances.sumOf { it.balanceMinor }/100.0),24f))
        balances.forEach { row -> root.addView(MaterialCardView(requireContext()).apply {
            radius=dp(20).toFloat();setCardBackgroundColor(requireContext().getColor(R.color.surface_white))
            strokeWidth=0
            addView(column().apply { addView(text(row.name,18f));addView(text(MintUi.money(row.balanceMinor/100.0),24f)) })
            setOnClickListener { model.accounts.value?.firstOrNull { it.id==row.id }?.let { accountForm(it) } }
        },LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(10) }) }
        root.addView(button(R.string.finance_new_account) { accountForm() })
        root.addView(button(R.string.finance_income) { entryForm(false) })
        root.addView(button(R.string.finance_transfer) { entryForm(true) })
        root.addView(text(getString(R.string.finance_history),20f))
        val entries=model.entries.value.orEmpty().filter { it.kind=="INCOME" || it.amountMinor<0 }.take(100)
        if(entries.isEmpty()) root.addView(text(getString(R.string.finance_empty)))
        entries.forEach { entry ->
            val account=balances.firstOrNull { it.id==entry.accountId }?.name.orEmpty()
            val date=java.time.Instant.ofEpochMilli(entry.timestamp).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            val kind=getString(if(entry.kind=="INCOME") R.string.finance_income else R.string.finance_transfer)
            root.addView(text("$kind · ${entry.title}\n$account · $date · ${MintUi.money(kotlin.math.abs(entry.amountMinor)/100.0)}").apply {
                setOnLongClickListener { AlertDialog.Builder(requireContext()).setMessage(R.string.finance_remove)
                    .setPositiveButton(R.string.btn_delete) { _,_->model.remove(entry) }.setNegativeButton(R.string.btn_cancel,null).show();true }
            })
        }
    }
    private fun field(layout:LinearLayout,label:Int,numeric:Boolean=false)=EditText(requireContext()).apply {
        hint=getString(label);inputType=if(numeric) android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL else android.text.InputType.TYPE_CLASS_TEXT
        layout.addView(this)
    }
    private fun form(title:Int,layout:LinearLayout,save:()->Unit) {
        val dialog=AlertDialog.Builder(requireContext()).setTitle(title).setView(ScrollView(requireContext()).apply { addView(layout) }).setPositiveButton(R.string.btn_save,null).setNegativeButton(R.string.btn_cancel,null).create()
        dialog.setOnShowListener { dialog.getButton(-1).setOnClickListener {
            try { save();dialog.dismiss() } catch(_:Exception) { Toast.makeText(requireContext(),R.string.error_invalid_input,Toast.LENGTH_LONG).show() }
        } };dialog.show()
    }
    private fun accountForm(existing:Account?=null) {
        val layout=column();val name=field(layout,R.string.plan_name).apply { setText(existing?.name.orEmpty()) };val opening=field(layout,R.string.finance_opening,true).apply { setText(java.math.BigDecimal.valueOf(existing?.openingMinor ?: 0L,2).toPlainString()) }
        form(R.string.finance_new_account,layout) { require(name.text.isNotBlank());val amount=Money.parse(opening.text.toString(),true);if(existing==null) model.addAccount(name.text.toString(),amount) else model.editAccount(existing.copy(name=name.text.toString().trim(),openingMinor=amount)) }
    }
    private fun entryForm(transfer:Boolean) {
        val accounts=model.accounts.value.orEmpty().toList()
        if(accounts.size<(if(transfer) 2 else 1)) { Toast.makeText(requireContext(),R.string.finance_need_accounts,Toast.LENGTH_LONG).show();return }
        val layout=column()
        fun selector(label:Int):Spinner { layout.addView(text(getString(label)));return Spinner(requireContext()).apply {
            adapter=ArrayAdapter(requireContext(),android.R.layout.simple_spinner_dropdown_item,accounts.map { it.name });layout.addView(this)
        } }
        val from=selector(if(transfer) R.string.finance_from else R.string.finance_account)
        val to=if(transfer) selector(R.string.finance_to).apply { setSelection(1) } else null
        val title=field(layout,R.string.plan_name);val amount=field(layout,R.string.hint_expense_amount,true)
        var date=java.time.LocalDate.now()
        layout.addView(button(R.string.purchase_date) { android.app.DatePickerDialog(requireContext(),{ _,y,m,d->date=java.time.LocalDate.of(y,m+1,d) },date.year,date.monthValue-1,date.dayOfMonth).apply { datePicker.maxDate=System.currentTimeMillis() }.show() }.apply {
            text=date.toString();setOnClickListener { android.app.DatePickerDialog(requireContext(),{ _,y,m,d->date=java.time.LocalDate.of(y,m+1,d);text=date.toString() },date.year,date.monthValue-1,date.dayOfMonth).apply { datePicker.maxDate=System.currentTimeMillis() }.show() }
        })
        form(if(transfer) R.string.finance_transfer else R.string.finance_income,layout) {
            require(title.text.isNotBlank());val minor=Money.parse(amount.text.toString());val time=date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            if(transfer) { require(from.selectedItemPosition!=to!!.selectedItemPosition);model.transfer(accounts[from.selectedItemPosition].id,accounts[to.selectedItemPosition].id,minor,time,title.text.toString()) }
            else model.income(accounts[from.selectedItemPosition].id,title.text.toString(),minor,time)
        }
    }
    override fun onDestroyView() { content=null;super.onDestroyView() }
}

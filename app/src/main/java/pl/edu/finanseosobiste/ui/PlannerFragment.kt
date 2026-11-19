package pl.edu.finanseosobiste.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputEditText
import pl.edu.finanseosobiste.R
import pl.edu.finanseosobiste.data.*
import pl.edu.finanseosobiste.util.BudgetLimitCalculator
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class PlannerFragment : Fragment() {
    private val model: BudgetViewModel by activityViewModels()
    private var payments: LinearLayout? = null
    private var goals: LinearLayout? = null
    private var categories: List<CategoryEntity> = emptyList()
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun column() = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(12), dp(16), dp(12)) }
    private fun text(value: String, size: Float = 15f) = TextView(requireContext()).apply { text = value; textSize = size; setTextColor(androidx.core.content.ContextCompat.getColor(context,R.color.text_primary)); setPadding(0, dp(2), 0, dp(2)) }
    private fun button(label: Int, action: () -> Unit) = MaterialButton(requireContext()).apply { setText(label); cornerRadius=dp(16); insetTop=0; insetBottom=0; setTypeface(typeface,android.graphics.Typeface.BOLD); setOnClickListener { action() } }
    private fun date(date: LocalDate) = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(resources.configuration.locales[0]))
    private fun money(value: Double) = BudgetLimitCalculator.formatCurrency(value)
    private fun input(parent: LinearLayout, label: Int, numeric: Boolean = false): TextInputEditText {
        val box = TextInputLayout(requireContext()).apply { hint = getString(label) + if (numeric) " (" + BudgetLimitCalculator.currencyCode + ")" else "" }
        val field = TextInputEditText(requireContext()).apply {
            inputType = if (numeric) InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        box.addView(field); parent.addView(box); return field
    }
    private fun amount(field: EditText, zero: Boolean = false): Double? {
        val value = field.text.toString().trim().replace(',', '.').toDoubleOrNull()
        return value?.takeIf { it.isFinite() && it <= 1e12 && if (zero) it >= 0 else it > 0 }
    }
    private fun invalid() { Toast.makeText(requireContext(), R.string.error_invalid_input, Toast.LENGTH_LONG).show() }
    private fun form(title: Int, layout: LinearLayout, save: () -> Boolean) {
        val scroll = ScrollView(requireContext()).apply { addView(layout) }
        val dialog = AlertDialog.Builder(requireContext()).setTitle(title).setView(scroll)
            .setPositiveButton(R.string.btn_save, null).setNegativeButton(R.string.btn_cancel, null).create()
        dialog.setOnShowListener { dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { if (save()) dialog.dismiss() } }
        dialog.show()
    }
    private fun card(parent: LinearLayout): LinearLayout {
        val card = MaterialCardView(requireContext()).apply { radius = dp(20).toFloat(); strokeWidth=0; cardElevation = dp(1).toFloat(); setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.surface_white)); setContentPadding(0, dp(4), 0, dp(4)) }
        val content = column(); card.addView(content)
        parent.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(16) })
        return content
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        val root = column().apply { setPadding(dp(18),dp(12),dp(18),dp(20)) }
        root.addView(text(getString(R.string.goals),28f).apply { typeface=android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.BOLD) })
        root.addView(text(getString(R.string.mint_goals_help),14f).apply { setTextColor(androidx.core.content.ContextCompat.getColor(context,R.color.text_secondary)) })
        goals=LinearLayout(requireContext()).apply { orientation=LinearLayout.VERTICAL }
        root.addView(goals,LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(18) })
        root.addView(button(R.string.add_goal) { addGoal() }.apply { setIconResource(R.drawable.ic_add); iconGravity=MaterialButton.ICON_GRAVITY_TEXT_START },LinearLayout.LayoutParams(-1,dp(52)).apply { topMargin=dp(12) })
        val paymentSection=column().apply { visibility=View.GONE }
        paymentSection.addView(text(getString(R.string.plan_help),13f))
        paymentSection.addView(button(R.string.add_recurring) { addRecurring() })
        payments=LinearLayout(requireContext()).apply { orientation=LinearLayout.VERTICAL }; paymentSection.addView(payments)
        root.addView(button(R.string.recurring) { paymentSection.visibility=if(paymentSection.visibility==View.VISIBLE) View.GONE else View.VISIBLE }.apply {
            backgroundTintList=android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
            setTextColor(MintUi.primary(context))
        },LinearLayout.LayoutParams(-1,dp(48)))
        root.addView(paymentSection)
        return androidx.core.widget.NestedScrollView(requireContext()).apply { addView(root) }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        model.allCategories.observe(viewLifecycleOwner) { categories = it }
        model.recurringPayments.observe(viewLifecycleOwner) { list ->
            val parent = payments ?: return@observe
            parent.removeAllViews()
            if (list.isEmpty()) parent.addView(text(getString(R.string.empty_plans)))
            list.forEach { payment ->
                val due = LocalDate.parse(payment.nextDate)
                val row = card(parent)
                row.addView(text("💳 " + payment.title, 19f))
                row.addView(text(money(payment.amount) + " · " + getString(listOf(R.string.weekly, R.string.monthly, R.string.yearly)[payment.frequency])))
                row.addView(text(getString(R.string.due_date, date(due))))
                row.addView(button(R.string.mark_paid) {
                    AlertDialog.Builder(requireContext()).setMessage(getString(R.string.pay_confirm, payment.title, date(due)))
                        .setPositiveButton(R.string.mark_paid) { _, _ -> model.pay(payment) }
                        .setNegativeButton(R.string.btn_cancel, null).show()
                }.apply { isEnabled = !due.isAfter(LocalDate.now()) })
                overflow(row) { model.removeRecurring(payment.id) }
            }
        }
        model.savingsGoals.observe(viewLifecycleOwner) { list ->
            val parent = goals ?: return@observe
            parent.removeAllViews()
            if (list.isEmpty()) parent.addView(text(getString(R.string.empty_plans)))
            list.forEachIndexed { index, goal ->
                val row = card(parent)
                val header=LinearLayout(requireContext()).apply { gravity=android.view.Gravity.CENTER_VERTICAL }
                header.addView(ImageView(requireContext()).apply {
                    setImageResource(if(index % 2 == 0) R.drawable.ic_mint_plane else R.drawable.ic_mint_pig)
                    setBackgroundResource(if(index % 2 == 0) R.drawable.mint_chip else R.drawable.mint_lavender)
                    setPadding(dp(11),dp(11),dp(11),dp(11))
                },LinearLayout.LayoutParams(dp(48),dp(48)))
                val titles=LinearLayout(requireContext()).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(14),0,0,0) }
                titles.addView(text(goal.title,18f).apply { setTypeface(typeface,android.graphics.Typeface.BOLD) })
                titles.addView(text(getString(when { goal.title.contains("Japan",true) -> R.string.mint_goal_journey; goal.title.contains("Emergency",true) -> R.string.mint_goal_safety; else -> R.string.mint_goal_description }),12f).apply { setTextColor(androidx.core.content.ContextCompat.getColor(context,R.color.text_secondary)) })
                header.addView(titles,LinearLayout.LayoutParams(0,-2,1f))
                header.addView(ImageButton(requireContext()).apply {
                    setImageResource(R.drawable.ic_mint_more); contentDescription=getString(R.string.mint_goal_actions)
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    setOnClickListener { confirmDelete { model.removeGoal(goal.id) } }
                },LinearLayout.LayoutParams(dp(40),dp(48)))
                row.addView(header)
                row.addView(text("",26f).apply { text=MintUi.amountPair(context,goal.saved,goal.target,26,17) },LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(14); bottomMargin=dp(8) })
                val percent=(goal.saved/goal.target*100).coerceIn(0.0,100.0).toInt()
                val progressRow=LinearLayout(requireContext()).apply { gravity=android.view.Gravity.CENTER_VERTICAL }
                progressRow.addView(com.google.android.material.progressindicator.LinearProgressIndicator(requireContext()).apply {
                    max=100; progress=percent; indicatorTrackGapSize=0; trackStopIndicatorSize=0; trackThickness=dp(14); trackCornerRadius=dp(7)
                    setIndicatorColor(MintUi.palette(requireContext())[if(index%2==0) 0 else 2])
                    trackColor=if(index%2==0) MintUi.container(context) else MintUi.secondaryContainer(context)
                },LinearLayout.LayoutParams(0,dp(20),1f))
                progressRow.addView(text("$percent%",14f).apply { setPadding(dp(16),0,0,0) })
                row.addView(progressRow)
                if(goal.saved>=goal.target) row.addView(text(getString(R.string.goal_complete)))
                row.addView(button(R.string.contribute) {
                    val layout=column(); val sum=input(layout,R.string.hint_expense_amount,true)
                    form(R.string.contribute,layout) {
                        val value=amount(sum)
                        if(value==null || goal.saved+value>1e12) { invalid(); false }
                        else { model.contribute(goal.id,value); true }
                    }
                }.apply {
                    setIconResource(R.drawable.ic_add); iconGravity=MaterialButton.ICON_GRAVITY_TEXT_START
                    backgroundTintList=android.content.res.ColorStateList.valueOf(MintUi.container(context))
                    setTextColor(androidx.core.content.ContextCompat.getColor(context,R.color.text_primary)); iconTint=android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(context,R.color.text_primary))
                },LinearLayout.LayoutParams(-1,dp(52)).apply { topMargin=dp(14) })
            }

        }
    }
    private fun overflow(row: LinearLayout, action: () -> Unit) {
        row.addView(android.widget.ImageButton(requireContext()).apply {
            setImageResource(R.drawable.ic_mint_more); contentDescription=getString(R.string.settings)
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setColorFilter(androidx.core.content.ContextCompat.getColor(context,R.color.text_secondary))
            setOnClickListener { anchor -> android.widget.PopupMenu(requireContext(),anchor).apply { menu.add(R.string.btn_delete).setOnMenuItemClickListener { confirmDelete(action); true }; show() } }
        },LinearLayout.LayoutParams(dp(48),dp(48)).apply { gravity=android.view.Gravity.END })
    }
    private fun confirmDelete(action: () -> Unit) {
        AlertDialog.Builder(requireContext()).setMessage(R.string.remove_plan)
            .setPositiveButton(R.string.btn_delete) { _, _ -> action() }.setNegativeButton(R.string.btn_cancel, null).show()
    }
    private fun addGoal() {
        val layout = column()
        val name = input(layout, R.string.plan_name)
        val target = input(layout, R.string.target_amount, true)
        val saved = input(layout, R.string.saved_amount, true).apply { setText("0") }
        form(R.string.add_goal, layout) {
            val title = name.text.toString().trim(); val goal = amount(target); val initial = amount(saved, true)
            if (title.isBlank() || goal == null || initial == null) { invalid(); false }
            else { model.addGoal(SavingsGoal(title = title, target = goal, saved = initial)); true }
        }
    }
    private fun addRecurring() {
        if (categories.isEmpty()) { Toast.makeText(requireContext(), R.string.loading_categories, Toast.LENGTH_SHORT).show(); return }
        val choices = categories.toList()
        val layout = column()
        val title = input(layout, R.string.plan_name)
        val sum = input(layout, R.string.hint_expense_amount, true)
        layout.addView(text(getString(R.string.label_select_category)))
        val category = Spinner(requireContext()).apply { adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, choices.map { AppSettings.category(requireContext(), it.name) }) }
        layout.addView(category)
        val frequency = Spinner(requireContext()).apply { adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, listOf(getString(R.string.weekly), getString(R.string.monthly), getString(R.string.yearly))); setSelection(1) }
        layout.addView(frequency)
        var due = LocalDate.now()
        val dateButton = MaterialButton(requireContext())
        fun updateDate() { dateButton.text = getString(R.string.due_date, date(due)) }
        updateDate()
        dateButton.setOnClickListener {
            DatePickerDialog(requireContext(), { _, y, m, d -> due = LocalDate.of(y, m + 1, d); updateDate() }, due.year, due.monthValue - 1, due.dayOfMonth).show()
        }
        layout.addView(dateButton)
        form(R.string.add_recurring, layout) {
            val name = title.text.toString().trim(); val value = amount(sum)
            if (name.isBlank() || value == null || category.selectedItemPosition !in choices.indices) { invalid(); false }
            else { model.addRecurring(RecurringPayment(title = name, amount = value, categoryId = choices[category.selectedItemPosition].id, nextDate = due.toString(), frequency = frequency.selectedItemPosition, anchorDay = due.dayOfMonth)); true }
        }
    }
    override fun onDestroyView() { payments = null; goals = null; super.onDestroyView() }
}

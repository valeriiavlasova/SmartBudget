package pl.edu.finanseosobiste.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import pl.edu.finanseosobiste.R
import pl.edu.finanseosobiste.databinding.DialogEditLimitBinding
import pl.edu.finanseosobiste.databinding.FragmentDashboardBinding
import pl.edu.finanseosobiste.util.BudgetLimitCalculator
import pl.edu.finanseosobiste.util.LimitStatus

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BudgetViewModel by activityViewModels()
    private lateinit var expenseAdapter: ExpenseAdapter

    private var monthRows: List<pl.edu.finanseosobiste.data.ExpenseWithCategory> = emptyList()
    private var showAllRows = false
    private var categoryFilter: Long? = null
    private var minimum = 0.0
    private var maximum = Double.MAX_VALUE
    private var baseLimit = 3500.0
    private var monthLimit: Double? = null
    private var currentTotalSpent: Double = 0.0
    private var currentGlobalLimit: Double = 3500.0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.addPurchase.setOnClickListener { (requireActivity() as MainActivity).showAddExpenseDialog() }
        binding.scanPurchase.setOnClickListener { (requireActivity() as MainActivity).receiptSources() }

        binding.btnMonth.setOnClickListener { chooseMonth() }
        binding.previousMonth.setOnClickListener { viewModel.selectedMonth.value = viewModel.selectedMonth.value!!.minusMonths(1) }
        binding.nextMonth.setOnClickListener { viewModel.selectedMonth.value = viewModel.selectedMonth.value!!.plusMonths(1) }
        binding.cardMonthlySummary.setOnClickListener { if (viewModel.selectedMonth.value == java.time.YearMonth.now()) showEditGlobalBudgetDialog() }
        viewModel.selectedMonth.observe(viewLifecycleOwner) { month ->
            binding.btnEditGlobalBudget.isEnabled = month == java.time.YearMonth.now()
            binding.btnMonth.text = month.format(java.time.format.DateTimeFormatter.ofPattern("LLLL yyyy", resources.configuration.locales[0]))
            binding.tvMonthlySummaryTitle.setText(R.string.global_budget)
        }
        setupRecyclerView()
        binding.searchPurchases.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { applyFilters() }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        binding.filterPurchases.setOnClickListener {
            if(!showAllRows) { showAllRows=true; binding.filterPurchases.setText(R.string.filters); applyFilters() }
            else showFilters()
        }
        binding.searchPurchases.setOnLongClickListener { showFilters(); true }
        observeData()

        binding.btnEditGlobalBudget.setOnClickListener {
            showEditGlobalBudgetDialog()
        }
    }

    private fun chooseMonth() {
        val current = viewModel.selectedMonth.value ?: java.time.YearMonth.now()
        val layout = android.widget.LinearLayout(requireContext()).apply { orientation = android.widget.LinearLayout.VERTICAL; setPadding(32, 16, 32, 16) }
        val year = android.widget.NumberPicker(requireContext()).apply { minValue = 2000; maxValue = 2100; value = current.year.coerceIn(2000,2100); wrapSelectorWheel = false }
        val month = android.widget.NumberPicker(requireContext()).apply {
            minValue=1; maxValue=12
            displayedValues=(1..12).map { java.time.Month.of(it).getDisplayName(java.time.format.TextStyle.FULL_STANDALONE, resources.configuration.locales[0]) }.toTypedArray()
            value=current.monthValue
        }
        layout.orientation = android.widget.LinearLayout.HORIZONTAL
        layout.addView(year); layout.addView(month)
        val dialog = AlertDialog.Builder(requireContext()).setTitle(R.string.choose_month).setView(layout)
            .setPositiveButton(R.string.btn_save, null).setNegativeButton(R.string.btn_cancel, null).create()
        dialog.setOnShowListener { dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val y = year.value
            
            viewModel.selectedMonth.value = java.time.YearMonth.of(y, month.value)
            dialog.dismiss()
        } }
        dialog.show()
    }

    private fun setupRecyclerView() {
        expenseAdapter = ExpenseAdapter(onEditClick = { (requireActivity() as MainActivity).showAddExpenseDialog(it) }) { expense ->
            viewModel.deleteWithUndo(expense) {
                if (_binding != null) com.google.android.material.snackbar.Snackbar.make(binding.root, getString(R.string.purchase_deleted, expense.title), com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                    .setAction(R.string.undo) { viewModel.restoreExpense(expense) }.show()
            }
        }
        binding.rvExpenses.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = expenseAdapter
        }
    }

    private fun applyFilters() {
        val query=binding.searchPurchases.text.toString().trim()
        val rows=monthRows.filter { item ->
            (categoryFilter == null || item.categoryId == categoryFilter) && item.amount >= minimum && item.amount <= maximum &&
            (query.isBlank() || listOf(item.title,item.note,AppSettings.category(requireContext(),item.categoryName)).any { it.contains(query,ignoreCase=true) })
        }
        expenseAdapter.submitList(if(!showAllRows && query.isBlank() && categoryFilter==null && minimum==0.0 && maximum==Double.MAX_VALUE) rows.take(3) else rows)
        viewModel.exportRows=rows.toList()
        binding.tvEmptyExpenses.visibility=if(rows.isEmpty()) View.VISIBLE else View.GONE
        binding.tvRecentExpensesHeader.text=if(query.isBlank() && categoryFilter==null && minimum==0.0 && maximum==Double.MAX_VALUE) getString(R.string.recent_expenses_title) else getString(R.string.filtered_count,rows.size,MintUi.money(rows.sumOf { it.amount }))
    }
    private fun showFilters() {
        val categories=viewModel.allCategories.value.orEmpty()
        val layout=android.widget.LinearLayout(requireContext()).apply { orientation=android.widget.LinearLayout.VERTICAL; setPadding(32,16,32,16) }
        val category=android.widget.Spinner(requireContext()).apply {
            adapter=android.widget.ArrayAdapter(requireContext(),android.R.layout.simple_spinner_dropdown_item,listOf(getString(R.string.all_categories_filter))+categories.map { AppSettings.category(requireContext(),it.name) })
            setSelection(categories.indexOfFirst { it.id==categoryFilter }+1)
        }
        fun input(label: Int, value: Double)=android.widget.EditText(requireContext()).apply { hint=getString(label); inputType=android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL; if(value>0 && value<Double.MAX_VALUE) setText(value.toString()) }
        val min=input(R.string.minimum_amount,minimum);val max=input(R.string.maximum_amount,maximum)
        layout.addView(category);layout.addView(min);layout.addView(max)
        val dialog=AlertDialog.Builder(requireContext()).setTitle(R.string.filters).setView(layout).setPositiveButton(R.string.btn_save,null).setNegativeButton(R.string.btn_cancel,null).setNeutralButton(R.string.clear_filters) { _,_ -> categoryFilter=null;minimum=0.0;maximum=Double.MAX_VALUE;binding.searchPurchases.setText("");applyFilters() }.create()
        dialog.setOnShowListener { dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val lo=if(min.text.isBlank()) 0.0 else min.text.toString().replace(',','.').toDoubleOrNull()
            val hi=if(max.text.isBlank()) Double.MAX_VALUE else max.text.toString().replace(',','.').toDoubleOrNull()
            if(lo==null || hi==null || !lo.isFinite() || !hi.isFinite() || lo<0 || hi<lo) { min.error=getString(R.string.error_invalid_input);return@setOnClickListener }
            minimum=lo;maximum=hi;categoryFilter=categories.getOrNull(category.selectedItemPosition-1)?.id;applyFilters();dialog.dismiss()
        } };dialog.show()
    }

    private fun observeData() {
        viewModel.history.observe(viewLifecycleOwner) { expenses ->
            monthRows=expenses
            applyFilters()
            currentTotalSpent = expenses.sumOf { it.amount }
            updateSummaryCard()
        }

        viewModel.effectiveBudget.observe(viewLifecycleOwner) { limit ->
            monthLimit = limit; currentGlobalLimit = limit ?: baseLimit; updateSummaryCard()
        }
        viewModel.budgetConfig.observe(viewLifecycleOwner) { config ->
            if (config != null) {
                baseLimit = config.totalMonthlyLimit
                currentGlobalLimit = monthLimit ?: baseLimit
                updateSummaryCard()
            }
        }
    }

    private fun updateSummaryCard() {
        val eval = BudgetLimitCalculator.evaluateLimit(
            spent = currentTotalSpent,
            limit = currentGlobalLimit
        )

        binding.tvTotalSpentAmount.text = MintUi.amountPair(requireContext(), currentTotalSpent, currentGlobalLimit, 24, 15)
        binding.remainingAmount.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_mint_goal,0,0,0)
        binding.remainingAmount.compoundDrawablePadding = (4*resources.displayMetrics.density).toInt()
        binding.remainingAmount.text = MintUi.money(eval.remaining) + "\n" + getString(R.string.mint_remaining)
        binding.progressTotalBudget.progress = eval.percentage.coerceIn(0, 100)
        binding.progressTotalBudget.setIndicatorColor(when(eval.status) {
            LimitStatus.SAFE -> MintUi.primary(requireContext())
            LimitStatus.WARNING -> ContextCompat.getColor(requireContext(),R.color.status_warning)
            LimitStatus.EXCEEDED -> ContextCompat.getColor(requireContext(),R.color.status_exceeded)
        })
        binding.tvBudgetStatusInfo.text = getString(R.string.mint_used, eval.percentage)
    }

    private fun showEditGlobalBudgetDialog() {
        val dialogBinding = DialogEditLimitBinding.inflate(layoutInflater)
        dialogBinding.tilCategoryName.visibility = View.GONE
        dialogBinding.etMonthlyLimit.setText(currentGlobalLimit.toString())

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.global_budget))
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.btn_save) { _, _ ->
                val text = dialogBinding.etMonthlyLimit.text?.toString().orEmpty()
                val newLimit = text.replace(',', '.').toDoubleOrNull()
                if (newLimit != null && newLimit.isFinite() && newLimit > 0.0 && newLimit<=1e12) {
                    viewModel.updateGlobalMonthlyLimit(newLimit)
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.invalid_limit),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

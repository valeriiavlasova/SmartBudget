package pl.edu.finanseosobiste.ui

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import pl.edu.finanseosobiste.R
import pl.edu.finanseosobiste.data.CategoryEntity
import pl.edu.finanseosobiste.databinding.ActivityMainBinding
import pl.edu.finanseosobiste.databinding.DialogAddExpenseBinding
import pl.edu.finanseosobiste.util.BudgetLimitCalculator

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: BudgetViewModel by viewModels()
    private var cachedAccounts: List<pl.edu.finanseosobiste.data.Account> = emptyList()
    private var cachedCategories: List<CategoryEntity> = emptyList()

    private var receiptCameraUri: android.net.Uri?=null
    private val receiptPhoto=registerForActivityResult(ActivityResultContracts.GetContent()) { uri -> if(uri!=null) viewModel.scanReceipt(uri) }
    private val receiptCamera=registerForActivityResult(ActivityResultContracts.TakePicture()) { success -> if(success) receiptCameraUri?.let { viewModel.scanReceipt(it) } }
    fun receiptSources() {
        if(viewModel.receiptBusy.value==true) return
        AlertDialog.Builder(this).setTitle(R.string.scan_receipt)
            .setItems(arrayOf(getString(R.string.receipt_camera),getString(R.string.receipt_photo))) { _,which ->
                try {
                    if(which==0) {
                        val folder=java.io.File(cacheDir,"receipts").apply { mkdirs() }
                        val file=java.io.File.createTempFile("receipt-",".jpg",folder)
                        receiptCameraUri=androidx.core.content.FileProvider.getUriForFile(this,packageName+".receipts",file)
                        receiptCamera.launch(requireNotNull(receiptCameraUri))
                    } else receiptPhoto.launch("image/*")
                } catch(_: Exception) { Toast.makeText(this,R.string.receipt_error,Toast.LENGTH_LONG).show() }
            }.show()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("receipt_camera_uri",receiptCameraUri?.toString())
        super.onSaveInstanceState(outState)
    }
    private fun consumeReceipt() {
        val draft=viewModel.receiptDraft.value ?: return
        if(cachedCategories.isEmpty()) return
        viewModel.receiptDraft.value=null
        showAddExpenseDialog(draft=draft)
    }
    private val csvFile=registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri -> if(uri!=null) writeFile(uri,"csv") }
    private val pdfFile=registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri -> if(uri!=null) writeFile(uri,"pdf") }
    private val backupFile=registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> if(uri!=null) writeFile(uri,"backup") }
    private val restoreFile=registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if(uri!=null) lifecycleScope.launch {
            try {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { pl.edu.finanseosobiste.util.DataFiles.restore(this@MainActivity,uri) }
                Toast.makeText(this@MainActivity,R.string.saved_success,Toast.LENGTH_LONG).show();recreate()
            } catch(e: kotlinx.coroutines.CancellationException) { throw e }
            catch(_: Exception) { Toast.makeText(this@MainActivity,R.string.file_error,Toast.LENGTH_LONG).show() }
        }
    }
    private fun writeFile(uri: android.net.Uri, type: String) {
        val rows=viewModel.exportRows.map { it.copy(categoryName=AppSettings.category(this,it.categoryName)) }
        val currency=pl.edu.finanseosobiste.util.BudgetLimitCalculator.currencyCode
        lifecycleScope.launch {
            try {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    contentResolver.openOutputStream(uri,"wt")!!.use { output ->
                        when(type) {
                            "csv" -> pl.edu.finanseosobiste.util.DataFiles.csv(rows,currency,output)
                            "pdf" -> pl.edu.finanseosobiste.util.DataFiles.pdf(rows,currency,output)
                            else -> pl.edu.finanseosobiste.util.DataFiles.backup(this@MainActivity,output)
                        }
                    }
                }
                Toast.makeText(this@MainActivity,R.string.file_done,Toast.LENGTH_LONG).show()
            } catch(e: kotlinx.coroutines.CancellationException) { throw e }
            catch(_: Exception) { Toast.makeText(this@MainActivity,R.string.file_error,Toast.LENGTH_LONG).show() }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(AppSettings.localized(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(if (AppSettings.preferences(this).getBoolean("dark", false)) androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES else androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
        setTheme(AppSettings.theme(this))
        AppSettings.configureMoney(this)
        super.onCreate(savedInstanceState)
        receiptCameraUri=savedInstanceState?.getString("receipt_camera_uri")?.let { android.net.Uri.parse(it) }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        androidx.core.view.WindowCompat.getInsetsController(window,binding.root).isAppearanceLightNavigationBars = resources.getBoolean(R.bool.light_system_bars)
        binding.topAppBar.subtitle = null
        for (i in 0 until binding.topAppBar.childCount) {
            (binding.topAppBar.getChildAt(i) as? android.widget.TextView)?.apply {
                textSize=18f; typeface=android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.BOLD)
                setPadding(10,0,0,0)
            }
        }
        binding.topAppBar.overflowIcon = ContextCompat.getDrawable(this, R.drawable.ic_mint_bell)
        binding.bottomNavigation.isItemActiveIndicatorEnabled = false
        binding.topAppBar.menu.add(R.string.notification_settings).setOnMenuItemClickListener { showNotificationSettings(); true }
        binding.topAppBar.setSubtitleTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        binding.fabAddExpense.hide()

        val scanItem=binding.topAppBar.menu.add(R.string.scan_receipt).setOnMenuItemClickListener { receiptSources();true }
        viewModel.receiptBusy.observe(this) { busy -> scanItem.isEnabled=!busy; if(busy) Toast.makeText(this,R.string.receipt_progress,Toast.LENGTH_SHORT).show() }
        viewModel.receiptDraft.observe(this) { consumeReceipt() }
        pl.edu.finanseosobiste.data.AppDatabase.getInstance(this).financeDao().accounts().observe(this) { cachedAccounts=it }
        binding.topAppBar.menu.add(R.string.finance_title).setOnMenuItemClickListener { replaceFragment(FinanceFragment()); true }
        binding.topAppBar.menu.add("Files").setOnMenuItemClickListener {
            AlertDialog.Builder(this).setMessage(R.string.backup_notice).setPositiveButton("Continue") { _,_ ->
                AlertDialog.Builder(this).setItems(arrayOf(getString(R.string.export_csv),getString(R.string.export_pdf),getString(R.string.backup_create),getString(R.string.backup_restore))) { _,which ->
                    val month=viewModel.selectedMonth.value.toString()
                    when(which) {
                        0 -> csvFile.launch("SmartBudget-$month.csv")
                        1 -> pdfFile.launch("SmartBudget-$month.pdf")
                        2 -> backupFile.launch("SmartBudget-backup-${java.time.LocalDate.now()}.json")
                        3 -> AlertDialog.Builder(this).setMessage(R.string.restore_warning).setPositiveButton(R.string.backup_restore) { _,_ -> restoreFile.launch(arrayOf("application/json","text/plain","application/octet-stream")) }.setNegativeButton(R.string.btn_cancel,null).show()
                    }
                }.show()
            }.setNegativeButton(R.string.btn_cancel,null).show()
            true
        }
        binding.topAppBar.menu.add(R.string.settings).setOnMenuItemClickListener {
            AlertDialog.Builder(this).setTitle(R.string.settings)
                .setItems(arrayOf(getString(R.string.language), getString(R.string.theme), getString(R.string.dark_mode), getString(R.string.future_budget), getString(R.string.currency_setting), getString(R.string.notification_settings))) { _, which ->
                    val prefs = AppSettings.preferences(this)
                    if (which == 5) {
                        showNotificationSettings()
                    } else if (which == 4) {
                        val codes=AppSettings.currencies
                        AlertDialog.Builder(this).setTitle(R.string.currency_setting).setMessage(R.string.currency_note)
                            .setPositiveButton(R.string.btn_save) { _, _ ->
                                AlertDialog.Builder(this).setTitle(R.string.currency_setting)
                                    .setSingleChoiceItems(codes,codes.indexOf(prefs.getString("currency","PLN"))) { d, index ->
                                        prefs.edit().putString("currency",codes[index]).apply()
                                        AppSettings.configureMoney(this); d.dismiss(); recreate()
                                    }.setNegativeButton(R.string.btn_cancel,null).show()
                            }.setNegativeButton(R.string.btn_cancel,null).show()
                    } else if (which == 2) {
                        prefs.edit().putBoolean("dark", !prefs.getBoolean("dark", false)).apply(); recreate()
                    } else if (which == 3) {
                        val input = android.widget.EditText(this).apply { inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL; hint = getString(R.string.hint_monthly_limit) }
                        val dialog = AlertDialog.Builder(this).setTitle(R.string.future_budget).setMessage(R.string.future_help).setView(input).setPositiveButton(R.string.btn_save, null).setNegativeButton(R.string.btn_cancel, null).create()
                        dialog.setOnShowListener { dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                            val value = input.text.toString().replace(',', '.').toDoubleOrNull()
                            if (value == null || !value.isFinite() || value <= 0) input.error = getString(R.string.invalid_limit)
                            else { viewModel.setFutureBudget(value); dialog.dismiss() }
                        } }; dialog.show()
                    } else if (which == 0) {
                        val codes = arrayOf("en", "uk", "pl")
                        AlertDialog.Builder(this).setTitle(R.string.language)
                            .setSingleChoiceItems(arrayOf("English", "Українська", "Polski"), codes.indexOf(prefs.getString("language", "en"))) { d, i ->
                                prefs.edit().putString("language", codes[i]).apply()
                                d.dismiss(); recreate()
                            }.setNegativeButton(R.string.btn_cancel, null).show()
                    } else {
                        AlertDialog.Builder(this).setTitle(R.string.theme)
                            .setSingleChoiceItems(arrayOf(getString(R.string.theme_blue), getString(R.string.theme_purple), getString(R.string.theme_coral), getString(R.string.theme_amber), getString(R.string.theme_rose), getString(R.string.theme_indigo), getString(R.string.theme_sky), getString(R.string.theme_teal), getString(R.string.theme_emerald), getString(R.string.theme_plum), getString(R.string.theme_cocoa), getString(R.string.theme_slate), getString(R.string.theme_crimson)), prefs.getInt("theme", 0)) { d, i ->
                                prefs.edit().putInt("theme", i).apply()
                                d.dismiss(); recreate()
                            }.setNegativeButton(R.string.btn_cancel, null).show()
                    }
                }.show()
            true
        }
        viewModel.plannerMessage.observe(this) { id ->
            if (id != null) {
                Toast.makeText(this, id, Toast.LENGTH_SHORT).show()
                viewModel.plannerMessage.value = null
            }
        }
        requestNotificationPermissionIfNeeded()

        viewModel.allCategories.observe(this) { categories ->
            cachedCategories = categories
            consumeReceipt()
        }

        if (savedInstanceState == null) {
            replaceFragment(DashboardFragment())
        }

        if (savedInstanceState != null && supportFragmentManager.findFragmentById(R.id.fragmentContainer) !is DashboardFragment) binding.fabAddExpense.hide()
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    binding.fabAddExpense.hide()
                    replaceFragment(DashboardFragment())
                    true
                }
                R.id.nav_charts -> {
                    binding.fabAddExpense.hide(); replaceFragment(ChartsFragment())
                    true
                }
                R.id.nav_limits -> {
                    binding.fabAddExpense.hide(); replaceFragment(LimitsFragment())
                    true
                }
                R.id.nav_planner -> { binding.fabAddExpense.hide(); replaceFragment(PlannerFragment()); true }
                else -> false
            }
        }

        binding.fabAddExpense.setOnClickListener {
            showAddExpenseDialog()
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    fun showAddExpenseDialog(existing: pl.edu.finanseosobiste.data.ExpenseWithCategory? = null, draft: pl.edu.finanseosobiste.util.ReceiptDraft? = null) {
        if (cachedCategories.isEmpty()) {
            Toast.makeText(
                this,
                getString(R.string.loading_categories),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val dialogBinding = DialogAddExpenseBinding.inflate(layoutInflater)
        val accounts=cachedAccounts.toList()
        if(accounts.isEmpty()) { Toast.makeText(this,R.string.loading_categories,Toast.LENGTH_SHORT).show();return }
        dialogBinding.expenseAccount.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,accounts.map { it.name })
        dialogBinding.expenseAccount.setSelection(accounts.indexOfFirst { it.id==(existing?.accountId ?: 1L) }.coerceAtLeast(0))
        val selectedDate = java.util.Calendar.getInstance()
        if (existing != null) selectedDate.timeInMillis = existing.timestamp
        draft?.date?.let { selectedDate.timeInMillis=it.atTime(12,0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() }
        fun updateDateLabel() {
            dialogBinding.btnPurchaseDate.text = getString(R.string.purchase_date,
                java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, resources.configuration.locales[0]).format(selectedDate.time))
        }
        updateDateLabel()
        dialogBinding.btnPurchaseDate.setOnClickListener {
            android.app.DatePickerDialog(this, { _, year, month, day ->
                selectedDate.set(year, month, day, 12, 0, 0)
                selectedDate.set(java.util.Calendar.MILLISECOND, 0)
                updateDateLabel()
            }, selectedDate.get(java.util.Calendar.YEAR), selectedDate.get(java.util.Calendar.MONTH), selectedDate.get(java.util.Calendar.DAY_OF_MONTH)).apply {
                datePicker.maxDate = System.currentTimeMillis()
            }.show()
        }
        val categoryNames = cachedCategories.map { AppSettings.category(this, it.name) }
        val spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            categoryNames
        )
        dialogBinding.spinnerCategory.adapter = spinnerAdapter
        if (existing != null) {
            dialogBinding.etExpenseTitle.setText(existing.title)
            dialogBinding.etExpenseAmount.setText(existing.amount.toString())
            dialogBinding.etExpenseNote.setText(existing.note)
            dialogBinding.spinnerCategory.setSelection(cachedCategories.indexOfFirst { it.id == existing.categoryId }.coerceAtLeast(0))
        }

        if(draft!=null) {
            dialogBinding.etExpenseTitle.setText(draft.title)
            draft.amount?.let { dialogBinding.etExpenseAmount.setText(it.toString()) }
            dialogBinding.etExpenseNote.setText(draft.text)
        }
        val review=if(draft==null) null else getString(R.string.receipt_review) + if(draft.currency!=null && draft.currency!=pl.edu.finanseosobiste.util.BudgetLimitCalculator.currencyCode) "\n"+getString(R.string.receipt_currency,draft.currency,pl.edu.finanseosobiste.util.BudgetLimitCalculator.currencyCode) else ""
        val dialog = AlertDialog.Builder(this)
            .setMessage(review)
            .setTitle(if (existing == null) R.string.dialog_add_expense_title else R.string.edit_purchase)
            .setView(dialogBinding.root)
            .setNeutralButton(if(existing==null && draft==null) getString(R.string.scan_receipt) else null) { _,_ -> receiptSources() }
            .setPositiveButton(R.string.btn_save, null)
            .setNegativeButton(R.string.btn_cancel, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val title = dialogBinding.etExpenseTitle.text?.toString().orEmpty()
                val amountText = dialogBinding.etExpenseAmount.text?.toString().orEmpty()
                val note = dialogBinding.etExpenseNote.text?.toString().orEmpty()
                val selectedPosition = dialogBinding.spinnerCategory.selectedItemPosition

                val validAmount = if(title.isBlank()) null else try { pl.edu.finanseosobiste.util.Money.parse(amountText)/100.0 } catch(_:Exception) { null }
                if (validAmount == null || selectedPosition !in cachedCategories.indices) {
                    Toast.makeText(
                        this,
                        getString(R.string.error_invalid_input),
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    dialog.dismiss()
                    selectedDate.timeInMillis = minOf(selectedDate.timeInMillis,System.currentTimeMillis())
                    val selectedCategory = cachedCategories[selectedPosition]
                    if (existing != null) viewModel.editExpense(pl.edu.finanseosobiste.data.ExpenseEntity(existing.id, title.trim(), validAmount, selectedCategory.id, selectedDate.timeInMillis, note.trim(), accounts[dialogBinding.expenseAccount.selectedItemPosition].id))
                    else viewModel.addExpense(
                        title = title,
                        amount = validAmount,
                        categoryId = selectedCategory.id,
                        note = note,
                        timestamp = selectedDate.timeInMillis,
                        accountId = accounts[dialogBinding.expenseAccount.selectedItemPosition].id
                    )

                }
            }
        }
        dialog.show()
    }

    private fun showNotificationSettings() {
        val events = pl.edu.finanseosobiste.notifications.MoneyEvent.values()
        val prefs = AppSettings.preferences(this)
        val keys = listOf("notify_limits") + events.map { it.preferenceKey }
        val labels = arrayOf(getString(R.string.notify_limits)) + events.map { getString(it.label) }
        val checked = keys.map { prefs.getBoolean(it, true) }.toBooleanArray()
        AlertDialog.Builder(this)
            .setTitle(R.string.notification_settings)
            .setMultiChoiceItems(labels, checked) { _, index, enabled -> checked[index] = enabled }
            .setPositiveButton(R.string.btn_save) { _, _ ->
                prefs.edit().apply { keys.forEachIndexed { index, key -> putBoolean(key, checked[index]) } }.apply()
                if (checked.any { it }) requestNotificationPermissionIfNeeded()
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .setNeutralButton(R.string.notification_system_settings) { _, _ ->
                startActivity(android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName))
            }.show()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}


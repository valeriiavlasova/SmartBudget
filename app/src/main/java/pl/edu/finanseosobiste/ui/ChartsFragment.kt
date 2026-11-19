package pl.edu.finanseosobiste.ui

import android.graphics.Color
import kotlin.math.roundToInt
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import pl.edu.finanseosobiste.R
import pl.edu.finanseosobiste.data.CategoryExpenseSummary
import pl.edu.finanseosobiste.databinding.FragmentChartsBinding

class ChartsFragment : Fragment() {

    private var _binding: FragmentChartsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BudgetViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChartsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.historyForecast.setOnClickListener { HistoryForecastDialog().show(parentFragmentManager,"historyForecast") }
        setupPieChartAppearance()
        setupBarChartAppearance()
        setupSavings()

        binding.chartMonth.setOnClickListener { viewModel.selectedMonth.value=java.time.YearMonth.now() }
        binding.previousMonth.setOnClickListener { viewModel.selectedMonth.value = viewModel.selectedMonth.value!!.minusMonths(1) }
        binding.nextMonth.setOnClickListener { viewModel.selectedMonth.value = viewModel.selectedMonth.value!!.plusMonths(1) }
        viewModel.selectedMonth.observe(viewLifecycleOwner) { month ->
            binding.chartMonth.text = month.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", resources.configuration.locales[0]))
        }
        binding.moreAnalytics.setOnClickListener { binding.advancedAnalytics.visibility = if(binding.advancedAnalytics.visibility == View.VISIBLE) View.GONE else View.VISIBLE }
        viewModel.history.observe(viewLifecycleOwner) { updateSelectedCategories() }
        viewModel.allCategories.observe(viewLifecycleOwner) { updateSelectedCategories() }
        viewModel.chartExpenses.observe(viewLifecycleOwner) { rows ->
            val selected = viewModel.selectedMonth.value ?: java.time.YearMonth.now()
            val months = (5 downTo 0).map { selected.minusMonths(it.toLong()) }
            val totals = rows.groupBy { java.time.YearMonth.from(java.time.Instant.ofEpochMilli(it.timestamp).atZone(java.time.ZoneId.systemDefault())) }
            val entries = months.mapIndexed { index, month -> BarEntry(index.toFloat(), totals[month].orEmpty().sumOf { it.amount }.toFloat()) }
            val set = BarDataSet(entries, "").apply {
                colors = months.map { if(it == selected) MintUi.palette(requireContext())[0] else MintUi.secondaryContainer(requireContext()) }
                setDrawValues(false)
            }
            binding.monthlyChart.apply {
                description.isEnabled = false; legend.isEnabled = false; axisRight.isEnabled = false
                setScaleEnabled(false); setPinchZoom(false); setDrawGridBackground(false)
                axisLeft.axisMinimum = 0f; axisLeft.axisMaximum = (kotlin.math.ceil((entries.maxOfOrNull { it.y } ?: 0f).coerceAtLeast(1f)/1000f)*1000f).coerceAtLeast(1000f)
                axisLeft.textColor = androidx.core.content.ContextCompat.getColor(context,R.color.text_secondary)
                axisLeft.gridColor = androidx.core.content.ContextCompat.getColor(context,R.color.divider_color); axisLeft.setDrawAxisLine(false)
                axisLeft.textSize = 10f
                xAxis.position = XAxis.XAxisPosition.BOTTOM; xAxis.setDrawGridLines(false); xAxis.setDrawAxisLine(false)
                xAxis.granularity = 1f; xAxis.textSize = 11f; xAxis.textColor = androidx.core.content.ContextCompat.getColor(context,R.color.text_secondary)
                xAxis.valueFormatter = IndexAxisValueFormatter(months.map { it.format(java.time.format.DateTimeFormatter.ofPattern("MMM", resources.configuration.locales[0])) })
                data = BarData(set).apply { barWidth = 0.62f }
                renderer = MintBarRenderer(this, animator, viewPortHandler)
                setExtraOffsets(0f,22f,0f,8f); invalidate()
            }
        }
    }
    private fun updateSelectedCategories() {
        val rows = viewModel.history.value.orEmpty()
        val summaries = viewModel.allCategories.value.orEmpty().map { c ->
            CategoryExpenseSummary(c.id,c.name,c.colorHex,c.monthlyLimit,rows.filter { it.categoryId == c.id }.sumOf { it.amount })
        }
        updatePieChart(summaries); updateBarChart(summaries)
    }

    private fun setupPieChartAppearance() {
        binding.pieChartCategories.apply {
            setUsePercentValues(true)
            setHoleColor(androidx.core.content.ContextCompat.getColor(context, R.color.surface_white))
            setCenterTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.text_primary))
            setHighlightPerTapEnabled(true)
            description.isEnabled = false
            isDrawHoleEnabled = true
            setDrawEntryLabels(false)
            holeRadius = 64f
            setMinOffset(0f)
            setExtraOffsets(0f,0f,0f,0f)
            transparentCircleRadius = 64f
            setEntryLabelColor(Color.DKGRAY)
            setEntryLabelTextSize(11f)
            setNoDataText(getString(R.string.no_chart_data))
            legend.apply {
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
                isWordWrapEnabled = true
                isEnabled = false
            }
        }
    }

    private fun setupBarChartAppearance() {
        binding.barChartLimits.apply {
            description.isEnabled = false
            setPinchZoom(false)
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setNoDataText(getString(R.string.no_chart_data))

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setCenterAxisLabels(true)
                setDrawGridLines(false)
                textSize = 10f
            }
            axisLeft.apply {
                axisMinimum = 0f
                textSize = 11f
            }
            axisRight.isEnabled = false
            legend.isEnabled = true
            legend.isWordWrapEnabled = true
            legend.textColor = androidx.core.content.ContextCompat.getColor(context, R.color.text_primary)
            xAxis.textColor = legend.textColor; axisLeft.textColor = legend.textColor
            setExtraOffsets(12f, 8f, 24f, 16f)
        }
    }

    private fun updatePieChart(summaries: List<CategoryExpenseSummary>) {
        binding.pieLegend.removeAllViews()
        val activeCategories = summaries.filter { it.totalSpent > 0.0 }.sortedBy { it.categoryId }
        if (activeCategories.isEmpty()) {
            binding.pieChartCategories.clear()
            binding.pieChartCategories.invalidate()
            return
        }

        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()

        for ((index, summary) in activeCategories.withIndex()) {
            entries.add(PieEntry(summary.totalSpent.toFloat(), AppSettings.category(requireContext(), summary.categoryName)))
            val color = MintUi.palette(requireContext())[index % MintUi.palette(requireContext()).size]
            colors.add(color)
            val row = android.widget.LinearLayout(requireContext()).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, (8*resources.displayMetrics.density).toInt(), 0, (8*resources.displayMetrics.density).toInt())
            }
            val label = android.widget.TextView(requireContext()).apply {

                text = "●  ${AppSettings.category(context, summary.categoryName)}"
                maxLines=2; ellipsize=android.text.TextUtils.TruncateAt.END
                textSize = 10f; setTextColor(androidx.core.content.ContextCompat.getColor(context,R.color.text_primary))
                val styled = android.text.SpannableString(text)
                styled.setSpan(android.text.style.ForegroundColorSpan(color),0,1,0); text=styled
            }
            row.addView(label,android.widget.LinearLayout.LayoutParams(0,-2,1f).apply { rightMargin=(4*resources.displayMetrics.density).toInt() })
            row.addView(android.widget.TextView(requireContext()).apply {
                text = MintUi.money(summary.totalSpent) + "  " + (summary.totalSpent / activeCategories.sumOf { it.totalSpent } * 100).roundToInt() + "%"
                textSize=10f; setTextColor(androidx.core.content.ContextCompat.getColor(context,R.color.text_primary))
            })
            binding.pieLegend.addView(row)
        }

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            sliceSpace = 0f
            selectionShift = 4f
            setDrawValues(false)
            valueTextSize = 12f
            valueTextColor = Color.WHITE
        }

        val pieData = PieData(dataSet).apply {
            setValueFormatter(PercentFormatter(binding.pieChartCategories))
        }

        binding.pieChartCategories.apply {
            data = pieData
            val total = MintUi.money(activeCategories.sumOf { it.totalSpent })
            setOnChartValueSelectedListener(object : com.github.mikephil.charting.listener.OnChartValueSelectedListener {
                override fun onValueSelected(e: com.github.mikephil.charting.data.Entry?, h: com.github.mikephil.charting.highlight.Highlight?) {
                    val entry = e as? PieEntry ?: return
                    centerText = entry.label + "\n" + MintUi.money(entry.value.toDouble())
                }
                override fun onNothingSelected() { centerText = total }
            })
            centerText = MintUi.money(activeCategories.sumOf { it.totalSpent }).replace(" ", "\n")
            setCenterTextSize(14f)
            animateY(700)
            invalidate()
        }
    }

    private fun updateBarChart(summaries: List<CategoryExpenseSummary>) {
        if (summaries.isEmpty()) {
            binding.barChartLimits.clear()
            binding.barChartLimits.invalidate()
            return
        }

        val spentEntries = ArrayList<BarEntry>()
        val limitEntries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        summaries.forEachIndexed { index, summary ->
            spentEntries.add(BarEntry(index.toFloat(), summary.totalSpent.toFloat()))
            limitEntries.add(BarEntry(index.toFloat(), summary.monthlyLimit.toFloat()))
            labels.add("${index + 1}")
        }

        binding.barLegend.removeAllViews()
        summaries.forEachIndexed { index, item ->
            binding.barLegend.addView(android.widget.TextView(requireContext()).apply {
                text = "${index + 1}. ${AppSettings.category(context, item.categoryName)}"
                setPadding(0,8,0,8)
            })
        }
        val spentDataSet = BarDataSet(spentEntries, getString(R.string.spent)).apply {
            color = MintUi.palette(requireContext())[2]
            setDrawValues(false)
            valueTextSize = 9f
        }

        val limitDataSet = BarDataSet(limitEntries, getString(R.string.limit)).apply {
            color = com.google.android.material.color.MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorPrimary)
            setDrawValues(false)
            valueTextSize = 9f
        }

        val groupSpace = 0.24f
        val barSpace = 0.06f
        val barWidth = 0.32f

        val barData = BarData(spentDataSet, limitDataSet).apply {
            this.barWidth = barWidth
        }

        binding.barChartLimits.apply {
            layoutParams = layoutParams.apply { height = (maxOf(280, summaries.size * 60) * resources.displayMetrics.density).toInt() }
            data = barData
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.axisMinimum = 0f
            xAxis.axisMaximum =
                0f + barData.getGroupWidth(groupSpace, barSpace) * summaries.size
            groupBars(0f, groupSpace, barSpace)
            animateY(700)
            invalidate()
        }
    }

    private var goals: List<pl.edu.finanseosobiste.data.SavingsGoal> = emptyList()
    private fun setupSavings() {
        viewModel.savingsGoals.observe(viewLifecycleOwner) { items ->
            goals = items
            binding.goalSpinner.adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, items.map { "🎯 " + it.title })
            renderSavings()
        }
        binding.goalSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) { renderSavings() }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
        binding.updateForecast.setOnClickListener { renderSavings() }
        binding.forecastIncome.setText(AppSettings.preferences(requireContext()).getString("savings_contribution", "0"))
    }
    private fun renderSavings() {
        val goal = goals.getOrNull(binding.goalSpinner.selectedItemPosition)
        if (goal == null) { binding.forecastChart.clear(); binding.forecastSummary.setText(R.string.empty_plans); return }
        val contribution = binding.forecastIncome.text.toString().replace(',', '.').toDoubleOrNull()
        if (contribution == null || !contribution.isFinite() || contribution < 0 || contribution > 1e12) { binding.forecastIncome.error=getString(R.string.forecast_invalid); return }
        AppSettings.preferences(requireContext()).edit().putString("savings_contribution", contribution.toString()).apply()
        val line = com.github.mikephil.charting.data.LineDataSet((0..12).map {
            com.github.mikephil.charting.data.Entry(it.toFloat(), (goal.saved + contribution * it).toFloat())
        }, goal.title).apply { color=com.google.android.material.color.MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorPrimary); lineWidth=3f; setDrawValues(false); setDrawCircles(false) }
        binding.forecastChart.apply {
            description.isEnabled=false; axisRight.isEnabled=false; legend.isEnabled=false
            val ink=androidx.core.content.ContextCompat.getColor(context,R.color.text_primary)
            xAxis.textColor=ink; axisLeft.textColor=ink
            xAxis.position=XAxis.XAxisPosition.BOTTOM; xAxis.granularity=1f; xAxis.setLabelCount(4,false)
            xAxis.valueFormatter=object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getFormattedValue(value: Float): String = java.time.YearMonth.now().plusMonths(value.toLong()).format(java.time.format.DateTimeFormatter.ofPattern("MMM yy", resources.configuration.locales[0]))
            }
            axisLeft.axisMinimum=0f; axisLeft.axisMaximum=(maxOf(goal.target, goal.saved + contribution*12, 1.0)*1.1).toFloat()
            axisLeft.removeAllLimitLines()
            axisLeft.addLimitLine(com.github.mikephil.charting.components.LimitLine(goal.target.toFloat()).apply { lineColor=ink; enableDashedLine(8f,4f,0f) })
            data=com.github.mikephil.charting.data.LineData(line); invalidate()
        }
        binding.forecastSummary.text=getString(R.string.goal_forecast_result, MintUi.money(goal.saved + contribution*12), MintUi.money(goal.target))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


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
            var selectedIndex = months.lastIndex
            val set = BarDataSet(entries, "").apply {
                colors = months.map { if(it == selected) MintUi.palette(requireContext())[0] else MintUi.secondaryContainer(requireContext()) }
                setDrawValues(false)
                highLightAlpha = 0
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
                val mintRenderer = MintBarRenderer(this, animator, viewPortHandler)
                renderer = mintRenderer
                fun showMonth(index: Int) {
                    selectedIndex = index.coerceIn(months.indices)
                    mintRenderer.selectedIndex = selectedIndex
                    set.colors = months.indices.map { if (it == selectedIndex) MintUi.primary(context) else MintUi.secondaryContainer(context) }
                    binding.monthlySelection.text = getString(R.string.month_spending_value,
                        months[selectedIndex].format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", resources.configuration.locales[0])),
                        MintUi.money(totals[months[selectedIndex]].orEmpty().sumOf { it.amount }))
                    invalidate()
                }
                setOnChartValueSelectedListener(object : com.github.mikephil.charting.listener.OnChartValueSelectedListener {
                    override fun onValueSelected(e: com.github.mikephil.charting.data.Entry?, h: com.github.mikephil.charting.highlight.Highlight?) {
                        if (e != null) showMonth(e.x.roundToInt())
                    }
                    override fun onNothingSelected() { showMonth(selectedIndex) }
                })
                onChartGestureListener = object : com.github.mikephil.charting.listener.OnChartGestureListener {
                    override fun onChartSingleTapped(e: android.view.MotionEvent) {
                        if (!viewPortHandler.isInBounds(e.x, e.y)) return
                        val point = getTransformer(com.github.mikephil.charting.components.YAxis.AxisDependency.LEFT).getValuesByTouchPoint(e.x, e.y)
                        showMonth(point.x.roundToInt())
                        com.github.mikephil.charting.utils.MPPointD.recycleInstance(point)
                    }
                    override fun onChartGestureStart(e: android.view.MotionEvent?, g: com.github.mikephil.charting.listener.ChartTouchListener.ChartGesture?) {}
                    override fun onChartGestureEnd(e: android.view.MotionEvent?, g: com.github.mikephil.charting.listener.ChartTouchListener.ChartGesture?) {}
                    override fun onChartLongPressed(e: android.view.MotionEvent?) {}
                    override fun onChartDoubleTapped(e: android.view.MotionEvent?) {}
                    override fun onChartFling(a: android.view.MotionEvent?, b: android.view.MotionEvent?, x: Float, y: Float) {}
                    override fun onChartScale(e: android.view.MotionEvent?, x: Float, y: Float) {}
                    override fun onChartTranslate(e: android.view.MotionEvent?, x: Float, y: Float) {}
                }
                showMonth(selectedIndex)
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
            setScaleEnabled(false)
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setNoDataText(getString(R.string.no_chart_data))

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setCenterAxisLabels(true)
                setDrawGridLines(false)
                textSize = 11f
                setDrawAxisLine(false)
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
            setExtraOffsets(4f, 8f, 12f, 12f)
            setXAxisRenderer(CategoryAxisRenderer(viewPortHandler, xAxis,
                getTransformer(com.github.mikephil.charting.components.YAxis.AxisDependency.LEFT),
                this, resources.displayMetrics.density))
            axisLeft.setLabelCount(4, false)
        }
    }

    private fun updatePieChart(summaries: List<CategoryExpenseSummary>) {
        binding.pieLegend.removeAllViews()
        val activeCategories = summaries.filter { it.totalSpent > 0.0 }.sortedBy { it.categoryId }
        binding.categoryChartEmpty.visibility = if (activeCategories.isEmpty()) View.VISIBLE else View.GONE
        binding.categoryChartContent.visibility = if (activeCategories.isEmpty()) View.GONE else View.VISIBLE
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
            labels.add(AppSettings.category(requireContext(), summary.categoryName))
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
            layoutParams = layoutParams.apply { height = (maxOf(280, summaries.size * 66) * resources.displayMetrics.density).toInt() }
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


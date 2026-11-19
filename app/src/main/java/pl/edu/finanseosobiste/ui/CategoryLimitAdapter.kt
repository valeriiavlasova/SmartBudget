package pl.edu.finanseosobiste.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import pl.edu.finanseosobiste.R
import pl.edu.finanseosobiste.data.CategoryExpenseSummary
import pl.edu.finanseosobiste.databinding.ItemCategoryLimitBinding
import pl.edu.finanseosobiste.util.BudgetLimitCalculator
import pl.edu.finanseosobiste.util.LimitStatus

class CategoryLimitAdapter(
    private val onCategoryClick: (CategoryExpenseSummary) -> Unit
) : ListAdapter<CategoryExpenseSummary, CategoryLimitAdapter.CategoryLimitViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryLimitViewHolder {
        val binding = ItemCategoryLimitBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryLimitViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryLimitViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategoryLimitViewHolder(
        private val binding: ItemCategoryLimitBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(summary: CategoryExpenseSummary) {
            val context = binding.root.context
            val eval = BudgetLimitCalculator.evaluateLimit(
                spent = summary.totalSpent,
                limit = summary.monthlyLimit
            )

            binding.tvCategoryName.text = AppSettings.category(context, summary.categoryName)
            binding.tvLimitPercentage.text = "${eval.percentage}%"

            val spentText = BudgetLimitCalculator.formatCurrency(summary.totalSpent)
            val limitText = BudgetLimitCalculator.formatCurrency(summary.monthlyLimit)
            binding.tvSpentVsLimit.text = context.getString(R.string.spent_limit, spentText, limitText)

            binding.progressCategoryLimit.progress = eval.percentage.coerceIn(0, 100)

            try {
                binding.viewDotColor.setBackgroundColor(Color.parseColor(summary.colorHex))
            } catch (_: IllegalArgumentException) {
                binding.viewDotColor.setBackgroundColor(Color.GRAY)
            }

            val statusColor = when (eval.status) {
                LimitStatus.SAFE -> MintUi.primary(context)
                LimitStatus.WARNING -> ContextCompat.getColor(context, R.color.status_warning)
                LimitStatus.EXCEEDED -> ContextCompat.getColor(context, R.color.status_exceeded)
            }

            binding.tvLimitPercentage.setTextColor(statusColor)
            binding.tvStatusMessage.setTextColor(statusColor)
            binding.progressCategoryLimit.setIndicatorColor(statusColor)

            binding.tvStatusMessage.text = when (eval.status) {
                LimitStatus.SAFE -> {
                    val rem = BudgetLimitCalculator.formatCurrency(eval.remaining)
                    context.getString(R.string.remaining_safe, rem)
                }
                LimitStatus.WARNING -> {
                    val rem = BudgetLimitCalculator.formatCurrency(eval.remaining)
                    context.getString(R.string.remaining_warning, rem)
                }
                LimitStatus.EXCEEDED -> {
                    val over = BudgetLimitCalculator.formatCurrency(-eval.remaining)
                    context.getString(R.string.budget_exceeded, over)
                }
            }

            binding.root.setOnClickListener {
                onCategoryClick(summary)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<CategoryExpenseSummary>() {
        override fun areItemsTheSame(
            oldItem: CategoryExpenseSummary,
            newItem: CategoryExpenseSummary
        ): Boolean = oldItem.categoryId == newItem.categoryId

        override fun areContentsTheSame(
            oldItem: CategoryExpenseSummary,
            newItem: CategoryExpenseSummary
        ): Boolean = oldItem == newItem
    }
}

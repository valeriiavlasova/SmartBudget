package pl.edu.finanseosobiste.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import pl.edu.finanseosobiste.databinding.ItemExpenseBinding
import pl.edu.finanseosobiste.data.ExpenseWithCategory
import pl.edu.finanseosobiste.util.BudgetLimitCalculator

class ExpenseAdapter(
    private val onEditClick: (ExpenseWithCategory) -> Unit,
    private val onDeleteClick: (ExpenseWithCategory) -> Unit
) : ListAdapter<ExpenseWithCategory, ExpenseAdapter.ExpenseViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = ItemExpenseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ExpenseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ExpenseViewHolder(
        private val binding: ItemExpenseBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ExpenseWithCategory) {
            binding.root.setOnClickListener { onEditClick(item) }
            binding.root.contentDescription = item.title + ". " + binding.root.context.getString(pl.edu.finanseosobiste.R.string.edit_purchase)
            binding.tvExpenseTitle.text = item.title
            binding.tvExpenseCategoryAndDate.text = java.time.Instant.ofEpochMilli(item.timestamp).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy",binding.root.resources.configuration.locales[0]))
            binding.tvExpenseAmount.text = MintUi.money(item.amount)
            binding.tvExpenseNote.visibility = View.GONE
            binding.viewCategoryColor.setImageResource(when {
                item.title.contains("coffee",true) || item.title.contains("kawa",true) -> pl.edu.finanseosobiste.R.drawable.ic_mint_coffee
                item.title.contains("internet",true) -> pl.edu.finanseosobiste.R.drawable.ic_mint_wifi
                item.categoryId == 1L -> pl.edu.finanseosobiste.R.drawable.ic_mint_basket
                else -> pl.edu.finanseosobiste.R.drawable.ic_mint_wallet
            })
            binding.root.setOnLongClickListener {
                androidx.appcompat.app.AlertDialog.Builder(binding.root.context)
                    .setTitle(item.title).setItems(arrayOf(binding.root.context.getString(pl.edu.finanseosobiste.R.string.edit_purchase), binding.root.context.getString(pl.edu.finanseosobiste.R.string.btn_delete))) { _, which ->
                        if(which == 0) onEditClick(item) else onDeleteClick(item)
                    }.show()
                true
            }

            binding.btnDeleteExpense.setOnClickListener {
                onDeleteClick(item)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ExpenseWithCategory>() {
        override fun areItemsTheSame(
            oldItem: ExpenseWithCategory,
            newItem: ExpenseWithCategory
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: ExpenseWithCategory,
            newItem: ExpenseWithCategory
        ): Boolean = oldItem == newItem
    }
}

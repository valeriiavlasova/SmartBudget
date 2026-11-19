package pl.edu.finanseosobiste.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import pl.edu.finanseosobiste.R
import pl.edu.finanseosobiste.data.CategoryExpenseSummary
import pl.edu.finanseosobiste.databinding.DialogEditLimitBinding
import pl.edu.finanseosobiste.databinding.FragmentLimitsBinding

class LimitsFragment : Fragment() {

    private var _binding: FragmentLimitsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BudgetViewModel by activityViewModels()
    private lateinit var categoryLimitAdapter: CategoryLimitAdapter

    private val paletteColors = listOf(
        "#00897B", "#3949AB", "#D81B60", "#FB8C00", "#8E24AA", "#43A047"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLimitsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        categoryLimitAdapter = CategoryLimitAdapter { summary ->
            showEditCategoryLimitDialog(summary)
        }

        binding.rvCategoryLimits.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryLimitAdapter
        }

        viewModel.categorySummaries.observe(viewLifecycleOwner) { summaries ->
            categoryLimitAdapter.submitList(summaries)
        }

        binding.btnAddCategory.setOnClickListener {
            showAddCategoryDialog()
        }
    }

    private fun showEditCategoryLimitDialog(summary: CategoryExpenseSummary) {
        val dialogBinding = DialogEditLimitBinding.inflate(layoutInflater)
        dialogBinding.tilCategoryName.visibility = View.GONE
        dialogBinding.etMonthlyLimit.setText(summary.monthlyLimit.toString())

        AlertDialog.Builder(requireContext())
            .setTitle("${getString(R.string.dialog_edit_limit_title)}: ${AppSettings.category(requireContext(), summary.categoryName)}")
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.btn_save) { _, _ ->
                val limitText = dialogBinding.etMonthlyLimit.text?.toString().orEmpty()
                val newLimit = limitText.replace(',', '.').toDoubleOrNull()
                if (newLimit != null && newLimit.isFinite() && newLimit > 0.0 && newLimit<=1e12) {
                    viewModel.updateCategoryLimit(summary.categoryId, newLimit)
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.limit_updated, AppSettings.category(requireContext(), summary.categoryName)),
                        Toast.LENGTH_SHORT
                    ).show()
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

    private fun showAddCategoryDialog() {
        val dialogBinding = DialogEditLimitBinding.inflate(layoutInflater)
        dialogBinding.tilCategoryName.visibility = View.VISIBLE

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_add_category_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.btn_save) { _, _ ->
                val name = dialogBinding.etCategoryName.text?.toString().orEmpty().trim()
                val limitText = dialogBinding.etMonthlyLimit.text?.toString().orEmpty()
                val limit = limitText.replace(',', '.').toDoubleOrNull()

                if (name.isNotEmpty() && limit != null && limit.isFinite() && limit > 0.0 && limit<=1e12) {
                    val colorHex = paletteColors.random()
                    viewModel.addCategory(name, limit, colorHex)
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.invalid_category),
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

package pl.edu.finanseosobiste.ui

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.StyleSpan
import android.text.style.ForegroundColorSpan
import pl.edu.finanseosobiste.R
import pl.edu.finanseosobiste.util.BudgetLimitCalculator

object MintUi {
    fun color(context: Context, attr: Int): Int {
        val value = android.util.TypedValue()
        check(context.theme.resolveAttribute(attr, value, true)) { "Missing theme color: $attr" }
        return if (value.resourceId != 0) androidx.core.content.ContextCompat.getColor(context, value.resourceId) else value.data
    }
    fun primary(context: Context) = color(context, com.google.android.material.R.attr.colorPrimary)
    fun container(context: Context) = color(context, com.google.android.material.R.attr.colorPrimaryContainer)
    fun secondaryContainer(context: Context) = color(context, com.google.android.material.R.attr.colorSecondaryContainer)
    fun onPrimary(context: Context) = color(context, com.google.android.material.R.attr.colorOnPrimary)
    fun palette(context: Context): List<Int> {
        val main = primary(context)
        val second = color(context, com.google.android.material.R.attr.colorSecondary)
        return listOf(main, androidx.core.graphics.ColorUtils.blendARGB(main, container(context), 0.38f), second,
            color(context,R.attr.budgetTrackColor), androidx.core.graphics.ColorUtils.blendARGB(main,second,0.5f),
            androidx.core.graphics.ColorUtils.blendARGB(second,secondaryContainer(context),0.4f))
    }
    fun money(amount: Double): String = java.text.NumberFormat.getNumberInstance(BudgetLimitCalculator.displayLocale).apply { minimumFractionDigits=0; maximumFractionDigits=2 }.format(amount) + " " + BudgetLimitCalculator.currencyCode
    fun amountPair(context: Context, amount: Double, target: Double, large: Int, small: Int): SpannableString {
        val first = money(amount)
        val text = SpannableString(first + " / " + money(target))
        text.setSpan(AbsoluteSizeSpan(large,true),0,first.lastIndexOf(" "),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        text.setSpan(AbsoluteSizeSpan(small,true),first.lastIndexOf(" "),first.length,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        text.setSpan(StyleSpan(android.graphics.Typeface.BOLD),0,first.length,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        text.setSpan(AbsoluteSizeSpan(small,true),first.length,text.length,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        text.setSpan(ForegroundColorSpan(androidx.core.content.ContextCompat.getColor(context,R.color.text_secondary)),first.length,text.length,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return text
    }
}

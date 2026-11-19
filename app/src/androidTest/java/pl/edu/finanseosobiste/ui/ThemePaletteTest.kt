package pl.edu.finanseosobiste.ui

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.core.graphics.ColorUtils
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import pl.edu.finanseosobiste.R

@RunWith(AndroidJUnit4::class)
class ThemePaletteTest {
    private val styles = listOf(R.style.Theme_FinanseOsobiste, R.style.Theme_FinanseOsobiste_Lavender,
        R.style.Theme_FinanseOsobiste_Coral, R.style.Theme_FinanseOsobiste_Amber, R.style.Theme_FinanseOsobiste_Rose,
        R.style.Theme_FinanseOsobiste_Indigo, R.style.Theme_FinanseOsobiste_Sky, R.style.Theme_FinanseOsobiste_Teal,
        R.style.Theme_FinanseOsobiste_Emerald, R.style.Theme_FinanseOsobiste_Plum, R.style.Theme_FinanseOsobiste_Cocoa,
        R.style.Theme_FinanseOsobiste_Slate, R.style.Theme_FinanseOsobiste_Crimson)

    private fun context(style: Int, night: Boolean): Context {
        val app = ApplicationProvider.getApplicationContext<Context>()
        val config = Configuration(app.resources.configuration)
        config.uiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
            if(night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        return ContextThemeWrapper(app.createConfigurationContext(config), style)
    }

    private fun chipPixel(context: Context): Int {
        val drawable = requireNotNull(androidx.appcompat.content.res.AppCompatResources.getDrawable(context,R.drawable.mint_chip))
        val bitmap = Bitmap.createBitmap(64,64,Bitmap.Config.ARGB_8888)
        drawable.setBounds(0,0,64,64); drawable.draw(Canvas(bitmap))
        val color=bitmap.getPixel(32,32);bitmap.recycle();return color
    }

    @Test fun eachThemeRecolorsRealChipAndChartInBothModes() {
        for(night in listOf(false,true)) {
            val chips = styles.map { style ->
                val ctx=context(style,night)
                assertEquals(MintUi.container(ctx),chipPixel(ctx))
                assertEquals(MintUi.primary(ctx),MintUi.palette(ctx).first())
                assertTrue(ColorUtils.calculateContrast(MintUi.onPrimary(ctx),MintUi.primary(ctx)) >= 4.5)
                assertTrue(ColorUtils.calculateContrast(ctx.getColor(R.color.text_primary),MintUi.container(ctx)) >= 4.5)
                chipPixel(ctx)
            }
            assertEquals(styles.size,chips.distinct().size)
        }
    }

    @Test fun switchingBackDoesNotReuseAnotherThemesPalette() {
        val first=context(R.style.Theme_FinanseOsobiste_Lavender,false)
        val other=context(R.style.Theme_FinanseOsobiste_Coral,false)
        assertNotEquals(chipPixel(first),chipPixel(other))
        assertEquals(chipPixel(first),chipPixel(context(R.style.Theme_FinanseOsobiste_Lavender,false)))
        assertNotEquals(chipPixel(first),chipPixel(context(R.style.Theme_FinanseOsobiste_Lavender,true)))
    }
}

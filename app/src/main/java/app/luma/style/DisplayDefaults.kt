package app.luma.style

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.util.DisplayMetrics
import app.luma.data.Prefs
import kotlin.math.abs

/**
 * The UI is tuned for a specific physical size on the Light Phone. Override system scaling so
 * both font scale and display density stay pinned to those expectations.
 */
object DisplayDefaults {
    private const val EPSILON = 0.01f

    fun Context.withDisplayDefaults(): Context {
        val configuration = Configuration(resources.configuration)
        val option = Prefs(this).fontSizeOption
        return if (configuration.applyDisplayDefaults(option)) {
            createConfigurationContext(configuration)
        } else {
            this
        }
    }

    fun Configuration?.withDisplayDefaults(context: Context): Configuration? {
        this ?: return null
        val option = Prefs(context).fontSizeOption
        applyDisplayDefaults(option)
        return this
    }

    private fun Configuration.applyDisplayDefaults(option: FontSizeOption): Boolean {
        var changed = false

        if (abs(fontScale - option.fontScale) >= EPSILON) {
            fontScale = option.fontScale
            changed = true
        }

        val targetDensity = targetDensityDpi()
        if (densityDpi != targetDensity) {
            densityDpi = targetDensity
            changed = true
        }

        return changed
    }

    private fun targetDensityDpi(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            DisplayMetrics.DENSITY_DEVICE_STABLE
        } else {
            Resources.getSystem().displayMetrics.densityDpi
        }
    }
}

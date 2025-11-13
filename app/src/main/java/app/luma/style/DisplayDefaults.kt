package app.luma.style

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.util.DisplayMetrics
import kotlin.math.abs

/**
 * The UI is tuned for a specific physical size on the Light Phone. Override system scaling so
 * both font scale and display density stay pinned to those expectations.
 */
object DisplayDefaults {
    private const val TARGET_FONT_SCALE = 0.72f
    private const val EPSILON = 0.01f

    fun Context.withDisplayDefaults(): Context {
        val configuration = Configuration(resources.configuration)
        return if (configuration.applyDisplayDefaults()) {
            createConfigurationContext(configuration)
        } else {
            this
        }
    }

    fun Configuration?.withDisplayDefaults(): Configuration? {
        this ?: return null
        applyDisplayDefaults()
        return this
    }

    private fun Configuration.applyDisplayDefaults(): Boolean {
        var changed = false

        if (abs(fontScale - TARGET_FONT_SCALE) >= EPSILON) {
            fontScale = TARGET_FONT_SCALE
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

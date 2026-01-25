package app.luma.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import app.luma.data.AppModel
import app.luma.data.Prefs
import app.luma.databinding.AdapterAppDrawerBinding
import app.luma.helper.performHapticFeedback
import java.text.Normalizer

data class AppDrawerConfig(
    val gravity: Int,
    val clickListener: (AppModel) -> Unit,
    val appLongPressListener: ((AppModel) -> Unit)? = null,
)

class AppDrawerAdapter(
    private val context: Context,
    private val config: AppDrawerConfig,
) : RecyclerView.Adapter<AppDrawerAdapter.ViewHolder>(),
    Filterable {
    companion object {
        private val DIACRITICAL_REGEX = Regex("\\p{InCombiningDiacriticalMarks}+")
        private val SEPARATOR_REGEX = Regex("[-_+,. ]")

        private fun normalizeForSearch(text: String): String =
            Normalizer
                .normalize(text, Normalizer.Form.NFD)
                .replace(DIACRITICAL_REGEX, "")
                .replace(SEPARATOR_REGEX, "")
    }

    private var appFilter = createAppFilter()
    var appsList: MutableList<AppModel> = mutableListOf()
    var appFilteredList: MutableList<AppModel> = mutableListOf()
    private val normalizedNameCache = mutableMapOf<String, String>()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val binding = AdapterAppDrawerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        if (appFilteredList.size == 0) return
        val appModel = appFilteredList[holder.absoluteAdapterPosition]
        holder.bind(
            config.gravity,
            appModel,
            config.clickListener,
            config.appLongPressListener,
        )
    }

    override fun getItemCount(): Int = appFilteredList.size

    override fun getFilter(): Filter = this.appFilter

    private fun createAppFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val searchChars = constraint.toString()
                val appFilteredList =
                    if (searchChars.isEmpty()) {
                        appsList
                    } else {
                        appsList.filter { app ->
                            val displayName = app.appAlias.ifEmpty { app.appLabel }
                            appLabelMatches(displayName, searchChars)
                        }
                    }

                val filterResults = FilterResults()
                filterResults.values = appFilteredList
                return filterResults
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(
                constraint: CharSequence?,
                results: FilterResults?,
            ) {
                appFilteredList = (results?.values as? List<AppModel>)?.toMutableList() ?: mutableListOf()
                notifyDataSetChanged()
            }
        }
    }

    private fun appLabelMatches(
        appLabel: String,
        searchChars: String,
    ): Boolean {
        if (appLabel.contains(searchChars, ignoreCase = true)) return true
        val normalized = normalizedNameCache.getOrPut(appLabel) { normalizeForSearch(appLabel) }
        return normalized.contains(searchChars, ignoreCase = true)
    }

    fun setAppList(appsList: MutableList<AppModel>) {
        normalizedNameCache.clear()
        this.appsList = appsList
        this.appFilteredList = this.appsList
        notifyDataSetChanged()
    }

    class ViewHolder(
        val binding: AdapterAppDrawerBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            appLabelGravity: Int,
            appModel: AppModel,
            listener: (AppModel) -> Unit,
            appLongPressListener: ((AppModel) -> Unit)? = null,
        ) {
            val context = itemView.context
            configureAppTitle(context, appModel, appLabelGravity)
            setupClickListeners(context, appModel, listener, appLongPressListener)
        }

        private fun configureAppTitle(
            context: Context,
            appModel: AppModel,
            gravity: Int,
        ) {
            val appName = appModel.appAlias.ifEmpty { appModel.appLabel }
            val showIndicator = Prefs.getInstance(context).showNotificationIndicator && appModel.hasNotification
            val displayName = if (showIndicator) "$appName*" else appName

            binding.appTitle.text = displayName

            val params = binding.appTitle.layoutParams as FrameLayout.LayoutParams
            params.gravity = gravity
            binding.appTitle.layoutParams = params
        }

        private fun setupClickListeners(
            context: Context,
            appModel: AppModel,
            listener: (AppModel) -> Unit,
            appLongPressListener: ((AppModel) -> Unit)? = null,
        ) {
            binding.appTitleFrame.isHapticFeedbackEnabled = false
            binding.appTitleFrame.setOnClickListener {
                performHapticFeedback(context)
                listener(appModel)
            }
            binding.appTitleFrame.setOnLongClickListener {
                performHapticFeedback(context)
                appLongPressListener?.invoke(appModel)
                true
            }
        }
    }
}

package app.luma.ui

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Filter
import android.widget.Filterable
import android.widget.FrameLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import app.luma.R
import app.luma.data.AppModel
import app.luma.data.Constants
import app.luma.data.Constants.AppDrawerFlag
import app.luma.data.Prefs
import app.luma.databinding.AdapterAppDrawerBinding
import app.luma.helper.performHapticFeedback
import app.luma.helper.uninstallApp
import java.text.Normalizer

data class AppDrawerConfig(
    val flag: AppDrawerFlag,
    val gravity: Int,
    val clickListener: (AppModel) -> Unit,
    val appInfoListener: (AppModel) -> Unit,
    val appHideListener: (AppDrawerFlag, AppModel) -> Unit,
    val appDeleteShortcutListener: (AppModel) -> Unit,
    val appRenameListener: (String, String) -> Unit,
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
        val deleteShortcutAction = {
            appFilteredList.removeAt(holder.absoluteAdapterPosition)
            appsList.remove(appModel)
            notifyItemRemoved(holder.absoluteAdapterPosition)
            config.appDeleteShortcutListener(appModel)
        }
        holder.bind(
            config.flag,
            config.gravity,
            appModel,
            config.clickListener,
            config.appInfoListener,
            deleteShortcutAction,
            config.appLongPressListener,
        )
        setupHideButton(holder, appModel)
        setupRenameListeners(holder, appModel)
    }

    private fun setupHideButton(
        holder: ViewHolder,
        appModel: AppModel,
    ) {
        holder.binding.appHide.setOnClickListener {
            appFilteredList.removeAt(holder.absoluteAdapterPosition)
            appsList.remove(appModel)
            notifyItemRemoved(holder.absoluteAdapterPosition)
            config.appHideListener(config.flag, appModel)
        }
    }

    private fun setupRenameListeners(
        holder: ViewHolder,
        appModel: AppModel,
    ) {
        val commitRename = {
            val name =
                holder.binding.appRenameEdit.text
                    .toString()
                    .trim()
            appModel.appAlias = name
            notifyItemChanged(holder.absoluteAdapterPosition)
            config.appRenameListener(appModel.appPackage, appModel.appAlias)
            dismissKeyboard(holder.binding.appRenameEdit)
        }

        holder.binding.appRename.setOnClickListener { commitRename() }
        holder.binding.appRenameEdit.setOnEditorActionListener { _, actionId, event ->
            val isDoneAction = actionId == EditorInfo.IME_ACTION_DONE
            val isEnterKey = event != null && event.action == KeyEvent.ACTION_DOWN
            if (isDoneAction || isEnterKey) {
                commitRename()
                true
            } else {
                false
            }
        }
    }

    private fun dismissKeyboard(editText: EditText) {
        val imm = editText.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(editText.windowToken, 0)
    }

    override fun getItemCount(): Int = appFilteredList.size

    override fun getFilter(): Filter = this.appFilter

    private fun createAppFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val searchChars = constraint.toString()
                val appFilteredList = (
                    if (searchChars.isEmpty()) {
                        appsList
                    } else {
                        appsList.filter { app ->
                            if (app.appAlias.isEmpty()) {
                                appLabelMatches(app.appLabel, searchChars)
                            } else {
                                appLabelMatches(app.appAlias, searchChars)
                            }
                        } as MutableList<AppModel>
                    }
                )

                val filterResults = FilterResults()
                filterResults.values = appFilteredList
                return filterResults
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(
                constraint: CharSequence?,
                results: FilterResults?,
            ) {
                appFilteredList = results?.values as MutableList<AppModel>
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
        private var textWatcher: TextWatcher? = null

        fun bind(
            flag: AppDrawerFlag,
            appLabelGravity: Int,
            appModel: AppModel,
            listener: (AppModel) -> Unit,
            appInfoListener: (AppModel) -> Unit,
            deleteShortcutAction: () -> Unit,
            appLongPressListener: ((AppModel) -> Unit)? = null,
        ) {
            val context = itemView.context
            binding.appHideLayout.visibility = View.GONE

            configureHideIcon(context, flag)
            setupTextWatcher(context, appModel)
            configureAppTitle(context, appModel, appLabelGravity)
            setupClickListeners(context, appModel, listener, appInfoListener, deleteShortcutAction, appLongPressListener)
        }

        private fun configureHideIcon(
            context: Context,
            flag: AppDrawerFlag,
        ) {
            val drawable =
                if (flag == AppDrawerFlag.HiddenApps) {
                    R.drawable.visibility
                } else {
                    R.drawable.visibility_off
                }
            binding.appHide.setImageDrawable(AppCompatResources.getDrawable(context, drawable))
        }

        private fun setupTextWatcher(
            context: Context,
            appModel: AppModel,
        ) {
            textWatcher?.let { binding.appRenameEdit.removeTextChangedListener(it) }
            textWatcher =
                object : TextWatcher {
                    override fun afterTextChanged(s: Editable) {}

                    override fun beforeTextChanged(
                        s: CharSequence,
                        start: Int,
                        count: Int,
                        after: Int,
                    ) {}

                    override fun onTextChanged(
                        s: CharSequence,
                        start: Int,
                        before: Int,
                        count: Int,
                    ) {
                        binding.appRename.text = computeRenameButtonText(context, appModel)
                    }
                }
            binding.appRenameEdit.addTextChangedListener(textWatcher)
        }

        private fun computeRenameButtonText(
            context: Context,
            appModel: AppModel,
        ): String {
            val currentText = binding.appRenameEdit.text.toString()
            return when {
                currentText.isEmpty() -> context.getString(R.string.app_drawer_reset)
                currentText == appModel.appAlias || currentText == appModel.appLabel -> context.getString(R.string.app_drawer_cancel)
                else -> context.getString(R.string.app_drawer_rename)
            }
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
            binding.appRenameEdit.text = Editable.Factory.getInstance().newEditable(appName)

            val params = binding.appTitle.layoutParams as FrameLayout.LayoutParams
            params.gravity = gravity
            binding.appTitle.layoutParams = params
        }

        private fun setupClickListeners(
            context: Context,
            appModel: AppModel,
            listener: (AppModel) -> Unit,
            appInfoListener: (AppModel) -> Unit,
            deleteShortcutAction: () -> Unit,
            appLongPressListener: ((AppModel) -> Unit)? = null,
        ) {
            binding.appTitleFrame.isHapticFeedbackEnabled = false
            binding.appTitleFrame.setOnClickListener {
                performHapticFeedback(context)
                listener(appModel)
            }
            binding.appTitleFrame.setOnLongClickListener {
                performHapticFeedback(context)
                appLongPressListener?.invoke(appModel) ?: run {
                    binding.appHideLayout.visibility = View.VISIBLE
                }
                true
            }

            val isPinnedShortcut = appModel.appPackage == Constants.PINNED_SHORTCUT_PACKAGE
            if (isPinnedShortcut) {
                binding.appInfo.setImageDrawable(
                    AppCompatResources.getDrawable(context, R.drawable.ic_outline_delete_24),
                )
                binding.appInfo.setOnClickListener { deleteShortcutAction() }
                binding.appInfo.setOnLongClickListener { true }
            } else {
                binding.appInfo.setImageDrawable(
                    AppCompatResources.getDrawable(context, R.drawable.ic_outline_info_24),
                )
                binding.appInfo.setOnClickListener { appInfoListener(appModel) }
                binding.appInfo.setOnLongClickListener {
                    uninstallApp(context, appModel.appPackage)
                    true
                }
            }

            binding.appHideLayout.setOnClickListener { binding.appHideLayout.visibility = View.GONE }
        }
    }
}

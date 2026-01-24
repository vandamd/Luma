package app.luma.ui

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
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
import app.luma.data.Constants.AppDrawerFlag
import app.luma.data.Prefs
import app.luma.databinding.AdapterAppDrawerBinding
import app.luma.helper.dp2px
import app.luma.helper.performHapticFeedback
import app.luma.helper.uninstallApp
import java.text.Normalizer

data class AppDrawerConfig(
    val flag: AppDrawerFlag,
    val gravity: Int,
    val clickListener: (AppModel) -> Unit,
    val appInfoListener: (AppModel) -> Unit,
    val appHideListener: (AppDrawerFlag, AppModel) -> Unit,
    val appRenameListener: (String, String) -> Unit,
)

class AppDrawerAdapter(
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
        holder.bind(config.flag, config.gravity, appModel, config.clickListener, config.appInfoListener)
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
        if (config.flag == AppDrawerFlag.SetHomeApp && appsList.isNotEmpty()) {
            val first = appsList[0]
            val pseudo =
                AppModel(
                    appLabel = "Rename",
                    key = first.key,
                    appPackage = "__rename__",
                    appActivityName = "",
                    user = first.user,
                    appAlias = "",
                )
            this.appsList.add(0, pseudo)
        }
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
        ) {
            val context = itemView.context
            binding.appHideLayout.visibility = View.GONE

            configureHideIcon(context, flag)
            setupTextWatcher(appModel)
            configureAppTitle(context, appModel, appLabelGravity)
            configureWorkProfileIcon(context, appModel, appLabelGravity)
            setupClickListeners(context, appModel, listener, appInfoListener)
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

        private fun setupTextWatcher(appModel: AppModel) {
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
                        binding.appRename.text = computeRenameButtonText(appModel)
                    }
                }
            binding.appRenameEdit.addTextChangedListener(textWatcher)
        }

        private fun computeRenameButtonText(appModel: AppModel): String {
            val currentText = binding.appRenameEdit.text.toString()
            return when {
                currentText.isEmpty() -> "Reset"
                currentText == appModel.appAlias || currentText == appModel.appLabel -> "Cancel"
                else -> "Rename"
            }
        }

        private fun configureAppTitle(
            context: Context,
            appModel: AppModel,
            gravity: Int,
        ) {
            val appName = appModel.appAlias.ifEmpty { appModel.appLabel }
            val showIndicator = Prefs(context).showNotificationIndicator && appModel.hasNotification
            val displayName = if (showIndicator) "$appName*" else appName

            binding.appTitle.text = displayName
            binding.appRenameEdit.text = Editable.Factory.getInstance().newEditable(appName)

            val params = binding.appTitle.layoutParams as FrameLayout.LayoutParams
            params.gravity = gravity
            binding.appTitle.layoutParams = params
        }

        private fun configureWorkProfileIcon(
            context: Context,
            appModel: AppModel,
            gravity: Int,
        ) {
            val isWorkProfile = appModel.user != android.os.Process.myUserHandle()
            if (!isWorkProfile) {
                binding.appTitle.setCompoundDrawables(null, null, null, null)
                return
            }

            val icon = AppCompatResources.getDrawable(context, R.drawable.work_profile)
            val px = dp2px(itemView.resources, 41)
            icon?.setBounds(0, 0, px, px)

            if (gravity == Gravity.LEFT) {
                binding.appTitle.setCompoundDrawables(null, null, icon, null)
            } else {
                binding.appTitle.setCompoundDrawables(icon, null, null, null)
            }
            binding.appTitle.compoundDrawablePadding = 20
        }

        private fun setupClickListeners(
            context: Context,
            appModel: AppModel,
            listener: (AppModel) -> Unit,
            appInfoListener: (AppModel) -> Unit,
        ) {
            binding.appTitleFrame.isHapticFeedbackEnabled = false
            binding.appTitleFrame.setOnClickListener {
                performHapticFeedback(context)
                listener(appModel)
            }
            binding.appTitleFrame.setOnLongClickListener {
                if (appModel.appPackage == "__rename__") {
                    false
                } else {
                    performHapticFeedback(context)
                    binding.appHideLayout.visibility = View.VISIBLE
                    true
                }
            }

            binding.appInfo.setOnClickListener { appInfoListener(appModel) }
            binding.appInfo.setOnLongClickListener {
                uninstallApp(context, appModel.appPackage)
                true
            }

            binding.appHideLayout.setOnClickListener { binding.appHideLayout.visibility = View.GONE }
        }
    }
}

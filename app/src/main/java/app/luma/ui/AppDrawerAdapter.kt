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
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
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

    private lateinit var prefs: Prefs
    private var appFilter = createAppFilter()
    var appsList: MutableList<AppModel> = mutableListOf()
    var appFilteredList: MutableList<AppModel> = mutableListOf()
    private lateinit var binding: AdapterAppDrawerBinding
    private val normalizedNameCache = mutableMapOf<String, String>()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        binding = AdapterAppDrawerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        prefs = Prefs(parent.context)
        binding.appTitle.textSize = 41f
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
        holder.appHideButton.setOnClickListener {
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
                holder.appRenameEdit.text
                    .toString()
                    .trim()
            appModel.appAlias = name
            notifyItemChanged(holder.absoluteAdapterPosition)
            config.appRenameListener(appModel.appPackage, appModel.appAlias)
            dismissKeyboard(holder.appRenameEdit)
        }

        holder.appRenameButton.setOnClickListener { commitRename() }
        holder.appRenameEdit.setOnEditorActionListener { _, actionId, event ->
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
        private val binding: AdapterAppDrawerBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        val appHideButton: ImageView = binding.appHide
        val appRenameButton: TextView = binding.appRename
        val appRenameEdit: EditText = binding.appRenameEdit
        private val appHideLayout: ConstraintLayout = binding.appHideLayout
        private val appTitle: TextView = binding.appTitle
        private val appTitleFrame: FrameLayout = binding.appTitleFrame
        private val appInfo: ImageView = binding.appInfo
        private var textWatcher: TextWatcher? = null

        fun bind(
            flag: AppDrawerFlag,
            appLabelGravity: Int,
            appModel: AppModel,
            listener: (AppModel) -> Unit,
            appInfoListener: (AppModel) -> Unit,
        ) {
            val context = itemView.context
            appHideLayout.visibility = View.GONE

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
            appHideButton.setImageDrawable(AppCompatResources.getDrawable(context, drawable))
        }

        private fun setupTextWatcher(appModel: AppModel) {
            textWatcher?.let { appRenameEdit.removeTextChangedListener(it) }
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
                        appRenameButton.text = computeRenameButtonText(appModel)
                    }
                }
            appRenameEdit.addTextChangedListener(textWatcher)
        }

        private fun computeRenameButtonText(appModel: AppModel): String {
            val currentText = appRenameEdit.text.toString()
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

            appTitle.text = displayName
            appRenameEdit.text = Editable.Factory.getInstance().newEditable(appName)

            val params = appTitle.layoutParams as FrameLayout.LayoutParams
            params.gravity = gravity
            appTitle.layoutParams = params
        }

        private fun configureWorkProfileIcon(
            context: Context,
            appModel: AppModel,
            gravity: Int,
        ) {
            val isWorkProfile = appModel.user != android.os.Process.myUserHandle()
            if (!isWorkProfile) {
                appTitle.setCompoundDrawables(null, null, null, null)
                return
            }

            val icon = AppCompatResources.getDrawable(context, R.drawable.work_profile)
            val px = dp2px(itemView.resources, 41)
            icon?.setBounds(0, 0, px, px)

            if (gravity == Gravity.LEFT) {
                appTitle.setCompoundDrawables(null, null, icon, null)
            } else {
                appTitle.setCompoundDrawables(icon, null, null, null)
            }
            appTitle.compoundDrawablePadding = 20
        }

        private fun setupClickListeners(
            context: Context,
            appModel: AppModel,
            listener: (AppModel) -> Unit,
            appInfoListener: (AppModel) -> Unit,
        ) {
            appTitleFrame.isHapticFeedbackEnabled = false
            appTitleFrame.setOnClickListener {
                performHapticFeedback(context)
                listener(appModel)
            }
            appTitleFrame.setOnLongClickListener {
                if (appModel.appPackage == "__rename__") {
                    false
                } else {
                    performHapticFeedback(context)
                    appHideLayout.visibility = View.VISIBLE
                    true
                }
            }

            appInfo.setOnClickListener { appInfoListener(appModel) }
            appInfo.setOnLongClickListener {
                uninstallApp(context, appModel.appPackage)
                true
            }

            appHideLayout.setOnClickListener { appHideLayout.visibility = View.GONE }
        }
    }
}

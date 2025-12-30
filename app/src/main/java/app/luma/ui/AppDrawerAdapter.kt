package app.luma.ui

import android.content.Context
import android.content.res.Resources
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.addTextChangedListener
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


class AppDrawerAdapter(
    private var flag: AppDrawerFlag,
    private val gravity: Int,
    private val clickListener: (AppModel) -> Unit,
    private val appInfoListener: (AppModel) -> Unit,
    private val appHideListener: (AppDrawerFlag, AppModel) -> Unit,
    private val appRenameListener: (String, String) -> Unit
) : RecyclerView.Adapter<AppDrawerAdapter.ViewHolder>(), Filterable {

    private lateinit var prefs: Prefs
    private var appFilter = createAppFilter()
    var appsList: MutableList<AppModel> = mutableListOf()
    var appFilteredList: MutableList<AppModel> = mutableListOf()
    private lateinit var binding: AdapterAppDrawerBinding

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        //val view = LayoutInflater.from(parent.context)
        //    .inflate(R.layout.adapter_app_drawer, parent, false)

        binding = AdapterAppDrawerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        //val view = binding.root
        prefs = Prefs(parent.context)
        binding.appTitle.textSize = 41f

        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (appFilteredList.size == 0) return
        val appModel = appFilteredList[holder.absoluteAdapterPosition]
        holder.bind(flag, gravity, appModel, clickListener, appInfoListener)

        holder.appHideButton.setOnClickListener {
            appFilteredList.removeAt(holder.absoluteAdapterPosition)
            appsList.remove(appModel)
            notifyItemRemoved(holder.absoluteAdapterPosition)
            appHideListener(flag, appModel)
        }


        val renameCommit = {
            val name = holder.appRenameEdit.text.toString().trim()
            appModel.appAlias = name
            notifyItemChanged(holder.absoluteAdapterPosition)
            appRenameListener(appModel.appPackage, appModel.appAlias)
            
            // Dismiss the keyboard
            val imm = holder.appRenameEdit.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(holder.appRenameEdit.windowToken, 0)
        }

        holder.appRenameButton.setOnClickListener { renameCommit() }
        holder.appRenameEdit.setOnEditorActionListener { _, actionId, event ->
            if ( actionId == EditorInfo.IME_ACTION_DONE ||
                event != null &&
                event.action == KeyEvent.ACTION_DOWN) {
                renameCommit()
                return@setOnEditorActionListener true
            }
            false
        }

    }

    override fun getItemCount(): Int = appFilteredList.size

    override fun getFilter(): Filter = this.appFilter

    private fun createAppFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val searchChars = constraint.toString()
/*                 val appFilteredList = (if (searchChars.isEmpty()) appsList
 *                 else appsList.filter { app -> appLabelMatches(app.appLabel, searchChars) } as MutableList<AppModel>)
 *  */
                val appFilteredList = (if (searchChars.isEmpty()) appsList
                else appsList.filter { app ->
                    if (app.appAlias.isEmpty()) {
                        appLabelMatches(app.appLabel, searchChars)
                    } else {
                        appLabelMatches(app.appAlias, searchChars)
                    }
                } as MutableList<AppModel>)

                val filterResults = FilterResults()
                filterResults.values = appFilteredList
                return filterResults
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                appFilteredList = results?.values as MutableList<AppModel>
                notifyDataSetChanged()
            }
        }
    }

    private fun appLabelMatches(appLabel: String, searchChars: String): Boolean {
        return (appLabel.contains(searchChars, true) or
                Normalizer.normalize(appLabel, Normalizer.Form.NFD)
                    .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
                    .replace(Regex("[-_+,. ]"), "")
                    .contains(searchChars, true))
    }

    fun setAppList(appsList: MutableList<AppModel>) {
        this.appsList = appsList
        if (flag == AppDrawerFlag.SetHomeApp && appsList.isNotEmpty()) {
            val first = appsList[0]
            val pseudo = AppModel(
                appLabel = "Rename",
                key = first.key,
                appPackage = "__rename__",
                appActivityName = "",
                user = first.user,
                appAlias = ""
            )
            this.appsList.add(0, pseudo)
        }
        this.appFilteredList = this.appsList
        notifyDataSetChanged()
    }

    fun launchFirstInList() {
        if (appFilteredList.size > 0)
            clickListener(appFilteredList[0])
    }

    class ViewHolder(itemView: AdapterAppDrawerBinding) : RecyclerView.ViewHolder(itemView.root) {
        val appHideButton: ImageView = itemView.appHide
        val appRenameButton: TextView = itemView.appRename
        val appRenameEdit: EditText = itemView.appRenameEdit
        private val appHideLayout: ConstraintLayout = itemView.appHideLayout
        private val appTitle: TextView = itemView.appTitle
        private val appTitleFrame: FrameLayout = itemView.appTitleFrame
        private val appInfo: ImageView = itemView.appInfo

        fun bind(
            flag: AppDrawerFlag,
            appLabelGravity: Int,
            appModel: AppModel,
            listener: (AppModel) -> Unit,
            appInfoListener: (AppModel) -> Unit
        ) =
            with(itemView) {
                appHideLayout.visibility = View.GONE

                // set show/hide icon
                val drawable = if (flag == AppDrawerFlag.HiddenApps) { R.drawable.visibility } else { R.drawable.visibility_off }
                appHideButton.setImageDrawable(AppCompatResources.getDrawable(context, drawable))

                appRenameEdit.addTextChangedListener(object : TextWatcher {

                    override fun afterTextChanged(s: Editable) {}

                    override fun beforeTextChanged(
                        s: CharSequence, start: Int,
                        count: Int, after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence, start: Int,
                        before: Int, count: Int
                    ) {
                        if (appRenameEdit.text.isEmpty()) {
                            appRenameButton.text = context.getString(R.string.reset)
                        } else if (appRenameEdit.text.toString() == appModel.appAlias || appRenameEdit.text.toString() == appModel.appLabel) {
                            appRenameButton.text = context.getString(R.string.cancel)
                        } else {
                            appRenameButton.text = context.getString(R.string.rename)
                        }
                    }
                })

                val appName = appModel.appAlias.ifEmpty {
                    appModel.appLabel
                }

                val displayName = if (Prefs(context).showNotificationIndicator && appModel.hasNotification) "$appName*" else appName
                appTitle.text = displayName

                // set current name as default text in EditText
                appRenameEdit.text = Editable.Factory.getInstance().newEditable(appName)

                // set text gravity
                val params = appTitle.layoutParams as FrameLayout.LayoutParams
                params.gravity = appLabelGravity
                appTitle.layoutParams = params


                // add icon next to app name to indicate that this app is installed on another profile
                if (appModel.user != android.os.Process.myUserHandle()) {
                    val icon = AppCompatResources.getDrawable(context, R.drawable.work_profile)
                    val px = dp2px(resources, 41)
                    icon?.setBounds(0, 0, px, px)
                    if (appLabelGravity == Gravity.LEFT) {
                        appTitle.setCompoundDrawables(null, null, icon, null)
                    } else {
                        appTitle.setCompoundDrawables(icon, null, null, null)
                    }
                    appTitle.compoundDrawablePadding = 20
                } else {
                    appTitle.setCompoundDrawables(null, null, null, null)
                }

                appTitleFrame.setOnClickListener {
                     performHapticFeedback(context)
                     listener(appModel)
                }

                appTitleFrame.setOnLongClickListener {
                     // Don't allow long press on pseudo rename app
                     if (appModel.appPackage == "__rename__") {
                         false
                     } else {
                         performHapticFeedback(context)
                         appHideLayout.visibility = View.VISIBLE
                         true
                     }
                }

                // Disable system haptic feedback for this view
                appTitleFrame.isHapticFeedbackEnabled = false
                appInfo.apply {
                    setOnClickListener { appInfoListener(appModel) }
                    setOnLongClickListener {
                        uninstallApp(context, appModel.appPackage)
                        true
                    }
                }
                appHideLayout.setOnClickListener { appHideLayout.visibility = View.GONE }
            }
    }
}

package app.luma.ui

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import app.luma.MainViewModel
import app.luma.R
import app.luma.data.AppModel
import app.luma.data.Constants.Action
import app.luma.data.Constants.AppDrawerFlag
import app.luma.data.Prefs
import app.luma.databinding.FragmentHomeBinding
import app.luma.helper.*
import app.luma.listener.OnSwipeTouchListener
import app.luma.listener.ViewSwipeTouchListener
import kotlinx.coroutines.launch


class HomeFragment : Fragment(), View.OnClickListener, View.OnLongClickListener {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var deviceManager: DevicePolicyManager
    private lateinit var vibrator: Vibrator

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        val view = binding.root
        prefs = Prefs(requireContext())

        if (prefs.firstSettingsOpen()) {
            binding.firstRunTips.visibility = View.VISIBLE
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        viewModel = activity?.run {
            ViewModelProvider(this)[MainViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        deviceManager = context?.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        vibrator = context?.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        initObservers()

        initSwipeTouchListener()
        initClickListeners()
    }

    override fun onStart() {
        super.onStart()
        hideStatusBar(requireActivity())


    }

    override fun onResume() {
        super.onResume()
    }

    override fun onClick(view: View) {
        when (view.id) {
            else -> {
                try { // Launch app
                    val appLocation = view.id.toString().toInt()
                    homeAppClicked(appLocation)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onLongClick(view: View): Boolean {
        if (prefs.homeLocked) return true

        val n = view.id
        val name = prefs.getHomeAppModel(n).appLabel
        showAppList(AppDrawerFlag.SetHomeApp, name.isNotEmpty(), n)
        return true
    }

    private fun initSwipeTouchListener() {
        val context = requireContext()
        binding.touchArea.setOnTouchListener(getHomeScreenGestureListener(context))
    }

    private fun initClickListeners() {
        // removed setDefaultLauncher click listener
    }

    private fun initObservers() {
        with(viewModel) {
            homeAppsAlignment.observe(viewLifecycleOwner) { (gravity, onBottom) ->
                val horizontalAlignment = if (onBottom) Gravity.BOTTOM else Gravity.CENTER_VERTICAL
                binding.homeAppsLayout.gravity = gravity.value() or horizontalAlignment

                binding.homeAppsLayout.children.forEach { view ->
                    (view as TextView).gravity = gravity.value()
                }
            }
            homeAppsCount.observe(viewLifecycleOwner) {
                updateAppCount(it)
            }
        }
    }

    private fun homeAppClicked(location: Int) {
        if (prefs.getAppName(location).isEmpty()) showLongPressToast()
        else launchApp(prefs.getHomeAppModel(location))
    }

    private fun launchApp(appModel: AppModel) {
        viewModel.selectedApp(appModel, AppDrawerFlag.LaunchApp)
    }

    private fun showAppList(flag: AppDrawerFlag, showHiddenApps: Boolean = false, n: Int = 0) {
        viewModel.getAppList(showHiddenApps)
        lifecycleScope.launch {
            try {
                findNavController().navigate(
                    R.id.action_mainFragment_to_appListFragment,
                    bundleOf("flag" to flag.toString(), "n" to n)
                )
            } catch (e: Exception) {
                findNavController().navigate(
                    R.id.appListFragment,
                    bundleOf("flag" to flag.toString())
                )
                e.printStackTrace()
            }
        }
    }

    private fun openSwipeRightApp() {
        if (prefs.appSwipeRight.appPackage.isNotEmpty())
            launchApp(prefs.appSwipeRight)
        else openDialerApp(requireContext())
    }

    private fun openSwipeDownApp() {
        if (prefs.appSwipeDown.appPackage.isNotEmpty())
            launchApp(prefs.appSwipeDown)
        else openDialerApp(requireContext())
    }

    private fun openSwipeUpApp() {
        if (prefs.appSwipeUp.appPackage.isNotEmpty())
            launchApp(prefs.appSwipeUp)
        else showAppList(AppDrawerFlag.LaunchApp)
    }

    private fun openClickClockApp() {
        if (prefs.appClickClock.appPackage.isNotEmpty())
            launchApp(prefs.appClickClock)
        else openAlarmApp(requireContext())
    }

    private fun openClickDateApp() {
        if (prefs.appClickDate.appPackage.isNotEmpty())
            launchApp(prefs.appClickDate)
        else openCalendar(requireContext())
    }

    private fun openSwipeLeftApp() {
        if (prefs.appSwipeLeft.appPackage.isNotEmpty())
            launchApp(prefs.appSwipeLeft)
        else openCameraApp(requireContext())
    }

    private fun openDoubleTapApp() {
        if (prefs.appDoubleTap.appPackage.isNotEmpty())
            launchApp(prefs.appDoubleTap)
        else openCameraApp(requireContext())
    }

    // This function handles all swipe actions that a independent of the actual swipe direction
    @SuppressLint("NewApi")
    private fun handleOtherAction(action: Action) {
        when(action) {
            Action.ShowNotification -> expandNotificationDrawer(requireContext())
            Action.LockScreen -> lockPhone()
            Action.ShowAppList -> showAppList(AppDrawerFlag.LaunchApp)
            Action.OpenApp -> {} // this should be handled in the respective onSwipe[Down,Right,Left] functions
            Action.OpenQuickSettings -> expandQuickSettings(requireContext())
            Action.ShowRecents -> initActionService(requireContext())?.showRecents()
            Action.Disabled -> {}
        }
    }

    private fun lockPhone() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val actionService = ActionService.instance()
            if (actionService != null) {
                actionService.lockScreen()
            } else {
                openAccessibilitySettings(requireContext())
            }
        } else {
            requireActivity().runOnUiThread {
                try {
                    deviceManager.lockNow()
                } catch (e: SecurityException) {
                    showToastLong(requireContext(), "App does not have the permission to lock the device")
                } catch (e: Exception) {
                    showToastLong(requireContext(), "Luma failed to lock device.\nPlease check your app settings.")
                    prefs.lockModeOn = false
                }
            }
        }
    }

    private fun showLongPressToast() = showToastShort(requireContext(), "Long press to select app")

    private fun textOnClick(view: View) = onClick(view)

    private fun textOnLongClick(view: View) = onLongClick(view)

    private fun getHomeScreenGestureListener(context: Context): View.OnTouchListener {
        return object : OnSwipeTouchListener(context) {
            override fun onSwipeLeft() {
                super.onSwipeLeft()
                when(val action = prefs.swipeLeftAction) {
                    Action.OpenApp -> openSwipeLeftApp()
                    else -> handleOtherAction(action)
                }
            }

            override fun onSwipeRight() {
                super.onSwipeRight()
                when(val action = prefs.swipeRightAction) {
                    Action.OpenApp -> openSwipeRightApp()
                    else -> handleOtherAction(action)
                }
            }

            override fun onSwipeUp() {
                super.onSwipeUp()
                when(val action = prefs.swipeUpAction) {
                    Action.OpenApp -> openSwipeUpApp()
                    else -> handleOtherAction(action)
                }
            }

            override fun onSwipeDown() {
                super.onSwipeDown()
                when(val action = prefs.swipeDownAction) {
                    Action.OpenApp -> openSwipeDownApp()
                    else -> handleOtherAction(action)
                }
            }

            override fun onLongClick() {
                super.onLongClick()
                try {
                    findNavController().navigate(R.id.action_mainFragment_to_settingsFragment)
                    // viewModel.firstOpen(false)
                } catch (e: java.lang.Exception) {
                }
            }

            override fun onDoubleClick() {
                super.onDoubleClick()
                when(val action = prefs.doubleTapAction) {
                    Action.OpenApp -> openDoubleTapApp()
                    else -> handleOtherAction(action)
                }
            }
        }
    }

    private fun getHomeAppsGestureListener(context: Context, view: View): View.OnTouchListener {
        return object : ViewSwipeTouchListener(context, view) {
            override fun onLongClick(view: View) {
                super.onLongClick(view)
                textOnLongClick(view)
            }

            override fun onClick(view: View) {
                super.onClick(view)
                textOnClick(view)
            }
            
            override fun onSwipeLeft() {
                super.onSwipeLeft()
                when(val action = prefs.swipeLeftAction) {
                    Action.OpenApp -> openSwipeLeftApp()
                    else -> handleOtherAction(action)
                }
            }

            override fun onSwipeRight() {
                super.onSwipeRight()
                when(val action = prefs.swipeRightAction) {
                    Action.OpenApp -> openSwipeRightApp()
                    else -> handleOtherAction(action)
                }
            }

            override fun onSwipeUp() {
                super.onSwipeUp()
                when(val action = prefs.swipeUpAction) {
                    Action.OpenApp -> openSwipeUpApp()
                    else -> handleOtherAction(action)
                }
            }

            override fun onSwipeDown() {
                super.onSwipeDown()
                when(val action = prefs.swipeDownAction) {
                    Action.OpenApp -> openSwipeDownApp()
                    else -> handleOtherAction(action)
                }
            }
        }
    }

    // updates number of apps visible on home screen
    // does nothing if number has not changed
    private fun updateAppCount(newAppsNum: Int) {
        val oldAppsNum = binding.homeAppsLayout.size // current number
        val diff = oldAppsNum - newAppsNum

        if (diff in 1 until oldAppsNum) { // 1 <= diff <= oldNumApps
            binding.homeAppsLayout.children.drop(diff)
        } else if (diff < 0) {
            val alignment = prefs.homeAlignment.value() // make only one call to prefs and store here

            // add all missing apps to list
            for (i in oldAppsNum until newAppsNum) {
                val view = layoutInflater.inflate(R.layout.home_app_button, null) as TextView
                view.apply {
                    textSize = prefs.textSize.toFloat()
                    id = i
                    text = prefs.getHomeAppModel(i).appLabel
                    setOnTouchListener(getHomeAppsGestureListener(context, this))
                    if (!prefs.extendHomeAppsArea) {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    }
                    gravity = alignment
                }
                binding.homeAppsLayout.addView(view)
            }
        }
    }
}

package app.luma.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.UserManager
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import app.luma.MainViewModel
import app.luma.R
import app.luma.data.AppModel
import app.luma.data.Constants.Action
import app.luma.data.Constants.AppDrawerFlag
import app.luma.data.GestureType
import app.luma.data.HomeLayout
import app.luma.data.Prefs
import app.luma.data.StatusBarSectionType
import app.luma.databinding.FragmentHomeBinding
import app.luma.helper.*
import app.luma.helper.LumaNotificationListener
import app.luma.listener.SwipeTouchListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

private const val TAG = "HomeFragment"

class HomeFragment :
    Fragment(),
    View.OnClickListener,
    View.OnLongClickListener {
    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private var currentPage = 0
    private var totalPages = 1
    private var pageIndicatorLayout: LinearLayout? = null
    private var colonVisible = true
    private var batteryReceiver: BroadcastReceiver? = null
    private var bluetoothReceiver: BroadcastReceiver? = null
    private var telephonyCallback: TelephonyCallback? = null
    private var wifiNetworkCallback: ConnectivityManager.NetworkCallback? = null
    private var notificationDotView: TextView? = null

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        val view = binding.root
        prefs = Prefs.getInstance(requireContext())

        if (prefs.firstSettingsOpen()) {
            binding.firstRunTips.visibility = View.VISIBLE
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        notificationDotView = null
        _binding = null
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        initObservers()
        initPageNavigation()
        initSwipeTouchListener()
        initStatusBarClickListeners()
        observeNotificationChanges()
        startClock()
    }

    override fun onStart() {
        super.onStart()
        hideStatusBar(requireActivity())
    }

    override fun onResume() {
        super.onResume()
        HomeCleanupHelper.setOnHomeCleanupCallback { refreshAppNames() }
        totalPages = prefs.homePages
        if (currentPage >= totalPages) currentPage = totalPages - 1
        pageIndicatorLayout = null
        updatePageIndicator()
        refreshAppNames()
        startBatteryMonitor()
        startConnectivityMonitors()
    }

    override fun onPause() {
        super.onPause()
        HomeCleanupHelper.setOnHomeCleanupCallback(null)
        stopBatteryMonitor()
        stopConnectivityMonitors()
    }

    override fun onClick(view: View) {
        try {
            val appLocation = view.id
            performHapticFeedback(requireContext())
            homeAppClicked(appLocation)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling app click", e)
        }
    }

    override fun onLongClick(view: View): Boolean {
        performHapticFeedback(requireContext())
        val position = view.id
        val appModel = prefs.getHomeAppModel(position)

        if (appModel.appLabel.isEmpty()) {
            showAppList(AppDrawerFlag.SetHomeApp, position)
        } else {
            val userManager = requireContext().getSystemService(android.content.Context.USER_SERVICE) as UserManager
            findNavController().navigate(
                R.id.appActionsFragment,
                bundleOf(
                    "appPackage" to appModel.appPackage,
                    "appLabel" to appModel.appLabel,
                    "appAlias" to appModel.appAlias,
                    "appActivityName" to appModel.appActivityName,
                    "homePosition" to position,
                    "userSerial" to userManager.getSerialNumberForUser(appModel.user),
                ),
            )
        }
        return true
    }

    private fun initSwipeTouchListener() {
        binding.touchArea.setOnTouchListener(
            createGestureListener(
                onLongClick = {
                    try {
                        findNavController().navigate(R.id.action_mainFragment_to_settingsFragment)
                    } catch (_: Exception) {
                        // Navigation already in progress, ignore
                    }
                },
            ),
        )
    }

    private fun initStatusBarClickListeners() {
        binding.statusConnectivityLayout.setOnClickListener { handleSectionPress(StatusBarSectionType.CELLULAR) }
        binding.statusClock.setOnClickListener { handleSectionPress(StatusBarSectionType.TIME) }
        binding.statusBatteryLayout.setOnClickListener { handleSectionPress(StatusBarSectionType.BATTERY) }
    }

    private fun handleSectionPress(section: StatusBarSectionType) {
        val action = prefs.getSectionAction(section)
        if (action == Action.Disabled) return
        performHapticFeedback(requireContext())
        if (action == Action.OpenApp) {
            val app = prefs.getSectionApp(section)
            if (app.appPackage.isNotEmpty()) launchApp(app)
        } else {
            handleOtherAction(action)
        }
    }

    private fun initPageNavigation() {
        totalPages = prefs.homePages
        if (currentPage >= totalPages) currentPage = totalPages - 1
        updatePageIndicator()
        refreshAppNames()
    }

    private fun updatePageIndicator() {
        // Remove any existing indicator to avoid duplicates
        binding.mainLayout.findViewWithTag<View>("pageIndicator")?.let {
            binding.mainLayout.removeView(it)
            if (it === pageIndicatorLayout) pageIndicatorLayout = null
        }

        // Only show indicator if there are 2 or more pages and not hidden
        if (totalPages < 2) {
            currentPage = 0
            pageIndicatorLayout = null
            return
        }

        // If hidden, just return without creating indicators but keep page logic
        if (prefs.pageIndicatorPosition == Prefs.PageIndicatorPosition.Hidden) {
            pageIndicatorLayout = null
            return
        }

        // Always create a fresh indicator layout
        val newLayout =
            LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                tag = "pageIndicator"
            }

        val density = resources.displayMetrics.density
        val circleSize = (11.6 * density).toInt()
        val circleMargin = (0.8 * density).toInt()
        val circleVerticalMargin = (7.8 * density).toInt()

        // Add circles for each page
        for (i in 0 until totalPages) {
            val index = i
            val circle =
                View(requireContext()).apply {
                    layoutParams =
                        LinearLayout.LayoutParams(circleSize, circleSize).apply {
                            setMargins(circleMargin, circleVerticalMargin, circleMargin, circleVerticalMargin)
                        }
                    isClickable = true
                    isFocusable = true
                    setOnClickListener { switchToPage(index) }
                    setBackgroundResource(if (index == currentPage) R.drawable.filled_circle else R.drawable.hollow_circle)
                }
            newLayout.addView(circle)
        }

        val layoutParams =
            FrameLayout
                .LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    when (prefs.pageIndicatorPosition) {
                        Prefs.PageIndicatorPosition.Left -> {
                            gravity = Gravity.START or Gravity.CENTER_VERTICAL
                            marginStart = (15.5 * density).toInt()
                            topMargin = (-7.0 * density).toInt()
                        }

                        Prefs.PageIndicatorPosition.Right -> {
                            gravity = Gravity.END or Gravity.CENTER_VERTICAL
                            marginEnd = (15.5 * density).toInt()
                            topMargin = (-7.0 * density).toInt()
                        }

                        Prefs.PageIndicatorPosition.Hidden -> { }
                    }
                }

        binding.mainLayout.addView(newLayout, layoutParams)
        pageIndicatorLayout = newLayout
    }

    private fun switchToPage(page: Int) {
        if (page >= 0 && page < totalPages) {
            currentPage = page
            refreshAppNames()
            updatePageIndicator()
        }
    }

    private fun initObservers() {
        binding.homeAppsLayout.gravity = android.view.Gravity.CENTER
    }

    private fun observeNotificationChanges() {
        viewLifecycleOwner.lifecycleScope.launch {
            var lastPackages: Set<String> = emptySet()
            LumaNotificationListener.changeVersion.collect {
                val current = LumaNotificationListener.getActiveNotificationPackages()
                if (current != lastPackages) {
                    lastPackages = current
                    refreshAppNames()
                    updateNotificationDot(current.isNotEmpty())
                }
            }
        }
    }

    private fun createNotificationDot(): TextView =
        TextView(requireContext()).apply {
            typeface = resources.getFont(R.font.public_sans)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            val ta = requireContext().obtainStyledAttributes(intArrayOf(R.attr.primaryColor))
            setTextColor(ta.getColor(0, 0))
            ta.recycle()
            text = "∗"
            visibility = View.GONE
        }

    private fun hasDotIn(layout: ViewGroup): Boolean =
        notificationDotView?.let { it.parent == layout && it.visibility == View.VISIBLE } == true

    private fun updateNotificationDot(hasNotifications: Boolean) {
        val show = hasNotifications && prefs.statusBarEnabled && prefs.showStatusBarNotificationIndicator
        val dot = notificationDotView ?: createNotificationDot().also { notificationDotView = it }

        val oldParent = dot.parent as? ViewGroup
        oldParent?.removeView(dot)

        if (!show) {
            dot.visibility = View.GONE
            refreshSectionVisibility(oldParent)
            return
        }

        dot.visibility = View.VISIBLE
        val section = prefs.notificationIndicatorSection
        val before = prefs.notificationIndicatorAlignment == Prefs.NotificationIndicatorAlignment.Before
        val marginLp =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
        val dp4 = (4 * resources.displayMetrics.density).toInt()

        when (section) {
            Prefs.NotificationIndicatorSection.Cellular -> {
                if (before) {
                    marginLp.marginEnd = dp4
                    binding.statusConnectivityLayout.addView(dot, 0, marginLp)
                } else {
                    marginLp.marginStart = dp4
                    binding.statusConnectivityLayout.addView(dot, marginLp)
                }
                binding.statusConnectivityLayout.visibility = View.VISIBLE
            }

            Prefs.NotificationIndicatorSection.Time -> {
                val clockIndex = binding.statusBar.indexOfChild(binding.statusClock)
                if (before) {
                    marginLp.marginEnd = dp4
                    binding.statusBar.addView(dot, clockIndex, marginLp)
                } else {
                    marginLp.marginStart = dp4
                    binding.statusBar.addView(dot, clockIndex + 1, marginLp)
                }
            }

            Prefs.NotificationIndicatorSection.Battery -> {
                if (before) {
                    marginLp.marginEnd = dp4
                    binding.statusBatteryLayout.addView(dot, 0, marginLp)
                } else {
                    marginLp.marginStart = dp4
                    binding.statusBatteryLayout.addView(dot, marginLp)
                }
                binding.statusBatteryLayout.visibility = View.VISIBLE
            }
        }
        refreshSectionVisibility(oldParent)
    }

    private fun refreshSectionVisibility(oldParent: ViewGroup?) {
        if (oldParent == binding.statusConnectivityLayout && !hasDotIn(binding.statusConnectivityLayout)) {
            val anyEnabled = prefs.cellularEnabled || prefs.wifiEnabled || prefs.bluetoothEnabled
            binding.statusConnectivityLayout.visibility = if (anyEnabled) View.VISIBLE else View.INVISIBLE
        }
        if (oldParent == binding.statusBatteryLayout && !hasDotIn(binding.statusBatteryLayout)) {
            binding.statusBatteryLayout.visibility = if (prefs.batteryEnabled) View.VISIBLE else View.INVISIBLE
        }
    }

    private fun startClock() {
        viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                if (prefs.statusBarEnabled && prefs.timeEnabled) {
                    binding.statusClock.visibility = View.VISIBLE
                    val is24Hour = prefs.timeFormat == Prefs.TimeFormat.TwentyFourHour
                    val showSec = prefs.showSeconds
                    val cal = Calendar.getInstance()
                    val hour =
                        if (is24Hour) {
                            cal.get(Calendar.HOUR_OF_DAY)
                        } else {
                            cal.get(Calendar.HOUR).let { if (it == 0) 12 else it }
                        }
                    val min = cal.get(Calendar.MINUTE)
                    val sec = cal.get(Calendar.SECOND)
                    val sep = if (prefs.flashingSeconds && !colonVisible) " " else ":"
                    val hStr =
                        if (is24Hour || prefs.leadingZero) {
                            "%02d".format(hour)
                        } else {
                            hour.toString()
                        }
                    val time =
                        buildString {
                            append("$hStr$sep${"%02d".format(min)}")
                            if (showSec) append("$sep${"%02d".format(sec)}")
                            if (!is24Hour) append(if (cal.get(Calendar.AM_PM) == Calendar.AM) " AM" else " PM")
                        }
                    binding.statusClock.text = time
                    colonVisible = !colonVisible
                } else {
                    binding.statusClock.visibility = View.GONE
                }
                val now = System.currentTimeMillis()
                delay(1000 - (now % 1000))
            }
        }
    }

    private fun startBatteryMonitor() {
        if (!prefs.statusBarEnabled || !prefs.batteryEnabled) {
            binding.statusBatteryLayout.visibility =
                if (hasDotIn(binding.statusBatteryLayout)) View.VISIBLE else View.INVISIBLE
            return
        }
        binding.statusBatteryLayout.visibility = View.VISIBLE
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context,
                    intent: Intent,
                ) {
                    updateBatteryIcon(intent)
                }
            }
        batteryReceiver = receiver
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val sticky = requireContext().registerReceiver(receiver, filter)
        if (sticky != null) updateBatteryIcon(sticky)
    }

    private fun stopBatteryMonitor() {
        batteryReceiver?.let {
            requireContext().unregisterReceiver(it)
            batteryReceiver = null
        }
    }

    private fun updateBatteryIcon(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return
        val pct = level * 100 / scale
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging =
            status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val icon =
            if (charging) {
                R.drawable.battery_charging
            } else {
                when {
                    pct >= 95 -> R.drawable.battery_full
                    pct >= 60 -> R.drawable.battery_75
                    pct >= 40 -> R.drawable.battery_50
                    pct >= 20 -> R.drawable.battery_low
                    pct >= 5 -> R.drawable.battery_very_low
                    else -> R.drawable.battery_empty
                }
            }
        binding.statusBatteryText.visibility = if (prefs.batteryPercentage) View.VISIBLE else View.GONE
        binding.statusBatteryText.text = "$pct%"
        binding.statusBattery.visibility = if (prefs.batteryIcon) View.VISIBLE else View.GONE
        binding.statusBattery.setImageResource(icon)
        binding.statusBattery.scaleX = if (charging) 1f else -1f
        binding.statusBattery.setColorFilter(binding.statusClock.currentTextColor)
    }

    private fun startConnectivityMonitors() {
        if (!prefs.statusBarEnabled) {
            binding.statusConnectivityLayout.visibility =
                if (hasDotIn(binding.statusConnectivityLayout)) View.VISIBLE else View.INVISIBLE
            return
        }
        val anyEnabled = prefs.cellularEnabled || prefs.wifiEnabled || prefs.bluetoothEnabled
        binding.statusConnectivityLayout.visibility =
            if (anyEnabled || hasDotIn(binding.statusConnectivityLayout)) View.VISIBLE else View.INVISIBLE
        if (prefs.cellularEnabled) startCellularMonitor() else hideCellular()
        if (prefs.wifiEnabled) startWifiMonitor() else hideWifi()
        if (prefs.bluetoothEnabled) startBluetoothMonitor() else hideBluetooth()
    }

    private fun stopConnectivityMonitors() {
        stopCellularMonitor()
        stopWifiMonitor()
        stopBluetoothMonitor()
    }

    private fun startCellularMonitor() {
        val tm = requireContext().getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val callback =
            object :
                TelephonyCallback(),
                TelephonyCallback.SignalStrengthsListener,
                TelephonyCallback.DataConnectionStateListener {
                override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                    if (_binding == null) return
                    updateSignalIcon(signalStrength.level)
                }

                override fun onDataConnectionStateChanged(
                    state: Int,
                    networkType: Int,
                ) {
                    if (_binding == null) return
                    updateNetworkTypeFromInt(networkType)
                }
            }
        telephonyCallback = callback
        tm.registerTelephonyCallback(requireContext().mainExecutor, callback)
    }

    private fun stopCellularMonitor() {
        telephonyCallback?.let {
            val tm = requireContext().getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            tm.unregisterTelephonyCallback(it)
            telephonyCallback = null
        }
    }

    private fun updateSignalIcon(level: Int) {
        val icon =
            when (level) {
                0 -> R.drawable.signal_0
                1 -> R.drawable.signal_1
                2 -> R.drawable.signal_2
                3 -> R.drawable.signal_3
                else -> R.drawable.signal_4
            }
        binding.statusSignal.visibility = View.VISIBLE
        binding.statusSignal.setImageResource(icon)
        binding.statusSignal.setColorFilter(binding.statusClock.currentTextColor)
    }

    private fun updateNetworkTypeFromInt(type: Int) {
        val label =
            when (type) {
                TelephonyManager.NETWORK_TYPE_NR -> "5G"

                TelephonyManager.NETWORK_TYPE_LTE -> "LTE"

                TelephonyManager.NETWORK_TYPE_HSPAP,
                TelephonyManager.NETWORK_TYPE_HSPA,
                TelephonyManager.NETWORK_TYPE_HSDPA,
                TelephonyManager.NETWORK_TYPE_HSUPA,
                TelephonyManager.NETWORK_TYPE_UMTS,
                -> "3G"

                TelephonyManager.NETWORK_TYPE_EDGE -> "E"

                TelephonyManager.NETWORK_TYPE_GPRS -> "G"

                TelephonyManager.NETWORK_TYPE_UNKNOWN -> ""

                else -> ""
            }
        if (label.isNotEmpty()) {
            binding.statusNetworkType.visibility = View.VISIBLE
            binding.statusNetworkType.text = label
        } else {
            binding.statusNetworkType.visibility = View.GONE
        }
    }

    private fun hideCellular() {
        binding.statusSignal.visibility = View.GONE
        binding.statusNetworkType.visibility = View.GONE
    }

    private fun startWifiMonitor() {
        val cm = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wm = requireContext().getSystemService(Context.WIFI_SERVICE) as WifiManager
        val request =
            NetworkRequest
                .Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()
        val callback =
            object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(
                    network: Network,
                    caps: NetworkCapabilities,
                ) {
                    if (_binding == null) return
                    val level = wm.calculateSignalLevel(caps.signalStrength)
                    binding.statusWifi.post { if (_binding != null) updateWifiIcon(level) }
                }

                override fun onLost(network: Network) {
                    if (_binding == null) return
                    binding.statusWifi.post { if (_binding != null) hideWifi() }
                }
            }
        wifiNetworkCallback = callback
        cm.registerNetworkCallback(request, callback)
        val activeNet = cm.activeNetwork
        val activeCaps = if (activeNet != null) cm.getNetworkCapabilities(activeNet) else null
        if (activeCaps != null && activeCaps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            updateWifiIcon(wm.calculateSignalLevel(activeCaps.signalStrength))
        } else {
            hideWifi()
        }
    }

    private fun stopWifiMonitor() {
        wifiNetworkCallback?.let {
            val cm = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            cm.unregisterNetworkCallback(it)
            wifiNetworkCallback = null
        }
    }

    private fun updateWifiIcon(level: Int) {
        val icon =
            when (level) {
                0 -> R.drawable.wifi_1
                1 -> R.drawable.wifi_1
                2 -> R.drawable.wifi_2
                else -> R.drawable.wifi_full
            }
        binding.statusWifi.visibility = View.VISIBLE
        binding.statusWifi.setImageResource(icon)
        binding.statusWifi.setColorFilter(binding.statusClock.currentTextColor)
    }

    private fun hideWifi() {
        binding.statusWifi.visibility = View.GONE
    }

    private fun startBluetoothMonitor() {
        val bm = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bm?.adapter
        if (adapter == null) {
            hideBluetooth()
            return
        }
        try {
            if (adapter.isEnabled) showBluetooth() else hideBluetooth()
        } catch (_: SecurityException) {
            hideBluetooth()
        }
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context,
                    intent: Intent,
                ) {
                    if (_binding == null) return
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)
                    if (state == BluetoothAdapter.STATE_ON) showBluetooth() else hideBluetooth()
                }
            }
        bluetoothReceiver = receiver
        requireContext().registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
    }

    private fun stopBluetoothMonitor() {
        bluetoothReceiver?.let {
            requireContext().unregisterReceiver(it)
            bluetoothReceiver = null
        }
    }

    private fun showBluetooth() {
        binding.statusBluetooth.visibility = View.VISIBLE
        binding.statusBluetooth.setImageResource(R.drawable.bluetooth)
        binding.statusBluetooth.setColorFilter(binding.statusClock.currentTextColor)
    }

    private fun hideBluetooth() {
        binding.statusBluetooth.visibility = View.GONE
    }

    private fun homeAppClicked(location: Int) {
        val appModel = prefs.getHomeAppModel(location)
        if (appModel.appLabel.isEmpty()) {
            showLongPressToast()
        } else {
            launchApp(appModel)
        }
    }

    private fun launchApp(appModel: AppModel) {
        viewModel.selectedApp(appModel, AppDrawerFlag.LaunchApp)
    }

    private fun showAppList(
        flag: AppDrawerFlag,
        n: Int = 0,
    ) {
        viewModel.getAppList()
        findNavController().navigate(
            R.id.appListFragment,
            bundleOf("flag" to flag.toString(), "n" to n),
        )
    }

    private fun openGestureApp(gestureType: GestureType) {
        val app = prefs.getGestureApp(gestureType)
        if (app.appPackage.isNotEmpty()) {
            launchApp(app)
        }
    }

    // This function handles all swipe actions that a independent of the actual swipe direction
    @SuppressLint("NewApi")
    private fun handleOtherAction(action: Action) {
        when (action) {
            Action.ShowNotification -> {
                expandNotificationDrawer(requireContext())
            }

            Action.LockScreen -> {
                lockPhone()
            }

            Action.ShowAppList -> {
                showAppList(AppDrawerFlag.LaunchApp)
            }

            Action.OpenApp -> {}

            // this should be handled in the respective onSwipe[Down,Right,Left] functions
            Action.OpenQuickSettings -> {
                expandQuickSettings(requireContext())
            }

            Action.ShowRecents -> {
                initActionService(requireContext())?.showRecents()
            }

            Action.ShowNotificationList -> {
                try {
                    findNavController().navigate(R.id.action_mainFragment_to_notificationListFragment)
                } catch (_: Exception) {
                }
            }

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
            showToast(requireContext(), getString(R.string.toast_lock_requires_android_9), Toast.LENGTH_LONG)
        }
    }

    private fun showLongPressToast() = showToast(requireContext(), getString(R.string.toast_long_press_to_select))

    private fun handleGesture(gestureType: GestureType) {
        val action = prefs.getGestureAction(gestureType)
        if (action == Action.OpenApp) {
            openGestureApp(gestureType)
        } else {
            handleOtherAction(action)
        }
    }

    private fun handleSwipeUp() {
        if (totalPages > 1 && currentPage < totalPages - 1) {
            switchToPage(currentPage + 1)
        } else {
            handleGesture(GestureType.SWIPE_UP)
        }
    }

    private fun handleSwipeDown() {
        if (totalPages > 1 && currentPage > 0) {
            switchToPage(currentPage - 1)
        } else {
            handleGesture(GestureType.SWIPE_DOWN)
        }
    }

    private fun createGestureListener(
        view: View? = null,
        onLongClick: () -> Unit = {},
        onClick: (View) -> Unit = {},
    ): View.OnTouchListener =
        object : SwipeTouchListener(requireContext(), view) {
            override fun onSwipeLeft() = handleGesture(GestureType.SWIPE_LEFT)

            override fun onSwipeRight() = handleGesture(GestureType.SWIPE_RIGHT)

            override fun onSwipeUp() = handleSwipeUp()

            override fun onSwipeDown() = handleSwipeDown()

            override fun onDoubleClick() = handleGesture(GestureType.DOUBLE_TAP)

            override fun onLongClick() = onLongClick()

            override fun onLongClick(view: View) {
                this@HomeFragment.onLongClick(view)
            }

            override fun onClick(view: View) = onClick(view)
        }

    // Update the number of app buttons displayed for the current page
    private fun updateAppCountForPage(appsCount: Int) {
        val currentAppCount = binding.homeAppsLayout.childCount

        if (currentAppCount < appsCount) {
            // Add more app buttons
            for (i in currentAppCount until appsCount) {
                val view = layoutInflater.inflate(R.layout.home_app_button, null) as TextView
                view.apply {
                    textSize = 41f
                    setOnTouchListener(
                        createGestureListener(
                            view = this,
                            onClick = { v -> this@HomeFragment.onClick(v) },
                        ),
                    )
                    layoutParams =
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                        )
                    gravity = android.view.Gravity.CENTER
                }
                binding.homeAppsLayout.addView(view)
            }
        } else if (currentAppCount > appsCount) {
            // Remove excess app buttons
            binding.homeAppsLayout.removeViews(appsCount, currentAppCount - appsCount)
        }
    }

    // Helper function to get app display name with notification indicator
    private fun getAppDisplayName(appModel: AppModel): String {
        val appName = if (appModel.appAlias.isNotEmpty()) appModel.appAlias else appModel.appLabel
        if (!prefs.showNotificationIndicator) return appName

        val packagesWithNotifications = LumaNotificationListener.getActiveNotificationPackages()
        val hasNotification = packagesWithNotifications.contains(appModel.appPackage)
        return if (hasNotification) "$appName*" else appName
    }

    private fun refreshAppNames() {
        val appsPerPage = prefs.getAppsPerPage(currentPage + 1)
        val startIndex = currentPage * HomeLayout.APPS_PER_PAGE

        // Update the number of app buttons if needed
        updateAppCountForPage(appsPerPage)

        for (i in 0 until appsPerPage) {
            val appIndex = startIndex + i
            val view = binding.homeAppsLayout.getChildAt(i)
            if (view is TextView) {
                val appModel = prefs.getHomeAppModel(appIndex)
                view.text = getAppDisplayName(appModel)
                view.id = appIndex
            }
        }

        updatePageIndicator()
    }
}

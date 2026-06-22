package com.example.yjx_clockin

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.graphics.scale
import androidx.fragment.app.Fragment
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.MapsInitializer
import com.amap.api.maps.model.*
import com.example.yjx_clockin.utils.ApiService
import com.example.yjx_clockin.utils.Constants
import com.example.yjx_clockin.utils.DeviceUtils
import com.example.yjx_clockin.utils.ImageUtils
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import androidx.core.graphics.createBitmap
import java.util.concurrent.atomic.AtomicBoolean
import com.example.yjx_clockin.utils.DialogUtils
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// 时间规则数据类
data class PunchTimeRule(
    val checkInDeadline: String,
    val checkOutStart: String,
    val lateMinutes: Int,
    val earlyLeaveMinutes: Int
)

class ClockInSample : Fragment() {
    private lateinit var tvUsername: TextView
    private lateinit var tvDepartment: TextView
    private lateinit var tvCheckInTime: TextView
    private lateinit var tvCheckOutTime: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnCheckIn: Button
    private lateinit var btnCheckOut: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView
    private lateinit var avatarIv: ImageView
    private lateinit var mapView: MapView
    private var aMap: AMap? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private var currentEmpId: String? = null
    private var currentEmpName: String? = null
    private var authToken: String? = null
    private var mLocationClient: AMapLocationClient? = null
    private var userLocationMarker: Marker? = null
    private val mLocationListener = AMapLocationListener { location ->
        if (location != null && location.errorCode == 0) {
            if (location.isMock) {
                activity?.runOnUiThread {
                    showDialog("提示", "检测到虚拟定位，请关闭模拟位置软件后重试")
                }
                return@AMapLocationListener
            }
            currentLatitude = location.latitude
            currentLongitude = location.longitude
            currentAddress = location.address
            activity?.runOnUiThread {
                updateUserLocationMarker(location.latitude, location.longitude)
            }
        }
    }
    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null
    private var currentAddress: String? = null
    private var isBinding = false

    private val punchPoints = mutableListOf<JSONObject>()
    private var pendingCheckType: Int? = null
    private var pendingLatitude: Double? = null
    private var pendingLongitude: Double? = null
    private var pendingAddress: String? = null

    private lateinit var takePictureLauncher: androidx.activity.result.ActivityResultLauncher<Intent>
    private var progressDialog: Dialog? = null

    // 新增：时间规则与打卡防重
    private var timeRule: PunchTimeRule? = null
    private var isClockInProgress = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        MapsInitializer.updatePrivacyShow(requireContext(), true, true)
        MapsInitializer.updatePrivacyAgree(requireContext(), true)
        MapsInitializer.initialize(requireContext())
        val view = inflater.inflate(R.layout.activity_clock_in_sample, container, false)
        mapView = view.findViewById(R.id.map_view)
        mapView.onCreate(savedInstanceState)
        aMap = mapView.map
        val transparentBitmap = createBitmap(1, 1, Bitmap.Config.ALPHA_8)
        val transparentIcon = BitmapDescriptorFactory.fromBitmap(transparentBitmap)
        val myLocationStyle = MyLocationStyle()
        myLocationStyle.myLocationIcon(transparentIcon)
        myLocationStyle.showMyLocation(false)
        myLocationStyle.anchor(0f, 0f)
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE)
        aMap?.myLocationStyle = myLocationStyle
        aMap?.isMyLocationEnabled = true
        aMap?.uiSettings?.isMyLocationButtonEnabled = true
        aMap?.setOnMapLoadedListener {
            fetchPunchPointsAndDrawFence()
        }
        return view
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val data: Intent? = result.data
                val imageBitmap = data?.extras?.get("data") as? Bitmap
                if (imageBitmap != null) {
                    val base64 = bitmapToBase64(imageBitmap)
                    val checkType = pendingCheckType ?: return@registerForActivityResult
                    val lat = pendingLatitude ?: return@registerForActivityResult
                    val lng = pendingLongitude ?: return@registerForActivityResult
                    val addr = pendingAddress ?: return@registerForActivityResult

                    // 下班打卡需要早退确认
                    if (checkType == 2) {
                        if (timeRule == null) loadTimeRuleForPoint(null)
                        if (isEarlyLeave()) {
                            showEarlyLeaveConfirmDialog { confirmed ->
                                if (confirmed) {
                                    submitCheck(checkType, lat, lng, addr, base64, true)
                                } else {
                                    isClockInProgress = false
                                }
                            }
                        } else {
                            submitCheck(checkType, lat, lng, addr, base64, false)
                        }
                    } else {
                        // 上班打卡直接提交
                        submitCheck(checkType, lat, lng, addr, base64, false)
                    }
                } else {
                    showDialog("提示", "拍照失败，请重试")
                    isClockInProgress = false
                }
            } else {
                showDialog("提示", "取消拍照")
                isClockInProgress = false
            }
            // 清理临时变量
            pendingCheckType = null
            pendingLatitude = null
            pendingLongitude = null
            pendingAddress = null
        }
        tvUsername = view.findViewById(R.id.tv_username)
        tvDepartment = view.findViewById(R.id.tv_dep)
        tvCheckInTime = view.findViewById(R.id.tv_check_in_time)
        tvCheckOutTime = view.findViewById(R.id.tv_check_out_time)
        tvStatus = view.findViewById(R.id.tv_status)
        btnCheckIn = view.findViewById(R.id.btn_check_in)
        btnCheckOut = view.findViewById(R.id.btn_check_out)
        progressBar = view.findViewById(R.id.progress_bar)
        tvError = view.findViewById(R.id.tv_error)
        avatarIv = view.findViewById(R.id.iv_avatar)

        val tvDate = view.findViewById<TextView>(R.id.tv_date)
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val week = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            1 -> "星期日"
            2 -> "星期一"
            3 -> "星期二"
            4 -> "星期三"
            5 -> "星期四"
            else -> "星期六"
        }
        tvDate.text = "今天是 $year 年 $month 月 $day 日 $week"

        val prefs = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        authToken = prefs.getString("token", null)
        loadCurrentUserInfo()
        initAmapLocation()
        btnCheckIn.setOnClickListener { performCheckIn() }
        btnCheckOut.setOnClickListener { performCheckOut() }
        testTodaySchedule()
    }

    // ========== 通用弹窗 ==========
    private fun showDialog(title: String, msg: String, isSuccess: Boolean = false) {
        val iconRes = if (isSuccess) R.drawable.ic_dialog_success else R.drawable.ic_dialog_info
        activity?.let {
            DialogUtils.showCustomDialog(
                context = it,
                title = title,
                message = msg,
                iconRes = iconRes,
                positiveText = "确定"
            )
        }
    }

    private fun showConfirmDialog(title: String, msg: String, onConfirm: () -> Unit) {
        activity?.let {
            DialogUtils.showCustomDialog(
                context = it,
                title = title,
                message = msg,
                positiveText = "确定",
                negativeText = "取消",
                onPositive = onConfirm
            )
        }
    }

    private fun showProgressDialog(message: String) {
        activity?.runOnUiThread {
            if (progressDialog == null) {
                val dialog = Dialog(requireContext(), android.R.style.Theme_Translucent_NoTitleBar)
                val textView = TextView(requireContext()).apply {
                    text = message
                    textSize = 24f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.progress_text))
                    gravity = android.view.Gravity.CENTER
                    setPadding(48, 48, 48, 48)
                    background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_progress_dialog)
                }
                dialog.setContentView(textView)
                dialog.setCancelable(false)
                dialog.window?.setLayout(
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                )
                dialog.window?.setGravity(android.view.Gravity.CENTER)
                progressDialog = dialog
            } else {
                val textView: TextView = progressDialog?.findViewById(android.R.id.message) ?: run {
                    TextView(requireContext()).apply {
                        text = message
                        textSize = 24f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.progress_text))
                        gravity = android.view.Gravity.CENTER
                        setPadding(48, 48, 48, 48)
                        background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_progress_dialog)
                    }
                }
                textView.text = message
                progressDialog?.setContentView(textView)
            }
            progressDialog?.show()
        }
    }

    private fun dismissProgressDialog() {
        activity?.runOnUiThread {
            progressDialog?.dismiss()
            progressDialog = null
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    // ========== 网络辅助 ==========
    private fun getAuthToken(): String? = authToken
    private fun buildAuthenticatedRequest(url: String): Request.Builder {
        val builder = Request.Builder().url(url)
        val token = getAuthToken()
        if (!token.isNullOrEmpty()) {
            builder.addHeader("Authorization", "Bearer $token")
        }
        return builder
    }

    // ========== 高德定位 ==========
    private fun initAmapLocation() {
        mLocationClient = AMapLocationClient(requireContext())
        val option = AMapLocationClientOption().apply {
            locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
            isOnceLocation = true
            isNeedAddress = true
            isMockEnable = false //禁止模拟位置
        }
        mLocationClient?.setLocationOption(option)
        mLocationClient?.setLocationListener(mLocationListener)
    }

    private fun updateUserLocationMarker(lat: Double, lng: Double) {
        val position = LatLng(lat, lng)
        val originBitmap = BitmapFactory.decodeResource(resources, R.drawable.clock_in_map_badge_local)
        val scaledBitmap = originBitmap.scale(64, 64, false)
        val icon = BitmapDescriptorFactory.fromBitmap(scaledBitmap)
        originBitmap.recycle()
        if (userLocationMarker == null) {
            val markerOptions = MarkerOptions()
                .position(position)
                .icon(icon)
                .title("我的位置")
                .anchor(0.5f, 0.5f)
            userLocationMarker = aMap?.addMarker(markerOptions)
        } else {
            userLocationMarker?.position = position
            userLocationMarker?.setIcon(icon)
        }
    }

    private fun fetchPunchPointsAndDrawFence() {
        val request = buildAuthenticatedRequest("${ApiService.BASE_URL}/api/emp/punch_points")
            .get()
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ClockInSample", "获取打卡点失败: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    try {
                        val json = JSONObject(body)
                        if (json.optInt("code") == 200) {
                            val points = json.optJSONArray("data")
                            drawCirclesOnMap(points)
                        }
                    } catch (e: Exception) {
                        Log.e("ClockInSample", "解析打卡点失败: ${e.message}")
                    }
                }
            }
        })
    }

    private fun drawCirclesOnMap(points: JSONArray?) {
        activity?.runOnUiThread {
            if (points == null || points.length() == 0) return@runOnUiThread
            punchPoints.clear()
            val latLngList = mutableListOf<LatLng>()
            for (i in 0 until points.length()) {
                val point = points.getJSONObject(i)
                punchPoints.add(point)
                val name = point.optString("name")
                val longitude = point.optDouble("longitude")
                val latitude = point.optDouble("latitude")
                val radius = point.optInt("radius")
                val center = LatLng(latitude, longitude)
                val circleOptions = CircleOptions()
                    .center(center)
                    .radius(radius.toDouble())
                    .strokeColor(ContextCompat.getColor(requireContext(), R.color.fence_stroke))
                    .strokeWidth(4f)
                    .fillColor(ContextCompat.getColor(requireContext(), R.color.fence_fill))
                aMap?.addCircle(circleOptions)
                val originBitmap = BitmapFactory.decodeResource(resources, R.drawable.clock_in_local)
                val scaledBitmap = originBitmap.scale(64, 64, false)
                val bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(scaledBitmap)
                originBitmap.recycle()
                val markerOptions = MarkerOptions()
                    .position(center)
                    .title(name)
                    .icon(bitmapDescriptor)
                    .anchor(0.5f, 0.5f)
                aMap?.addMarker(markerOptions)
                latLngList.add(center)
            }
            if (latLngList.isNotEmpty()) {
                aMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngList.first(), 17f))
            }
        }
    }

    private fun loadCurrentUserInfo() {
        val prefs = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val name = prefs.getString("emp_name", "")
        val empId = prefs.getString("emp_id", "")
        if (name.isNullOrEmpty()) {
            tvUsername.text = "未登录"
            tvDepartment.text = "请重新登录"
            return
        }
        currentEmpId = empId
        currentEmpName = name
        tvUsername.text = name
        tvDepartment.text = "部门：加载中..."
        fetchEmployeeDetail(empId!!)
        loadTodayCheckRecord(empId)
    }

    private fun fetchEmployeeDetail(empId: String) {
        ApiService.getEmployeeDetail(empId) { empJson ->
            activity?.runOnUiThread {
                if (empJson != null) {
                    val department = empJson.optString("department")
                    val faceImageBase64 = empJson.optString("face_image")
                    tvDepartment.text = department.takeIf { it.isNotEmpty() } ?: "未分配"
                    if (faceImageBase64.isNotEmpty()) {
                        displayAvatarFromBase64(faceImageBase64)
                    } else {
                        avatarIv.setImageResource(R.drawable.name_image)
                    }
                } else {
                    tvDepartment.text = "未分配"
                    avatarIv.setImageResource(R.drawable.name_image)
                }
            }
        }
    }

    private fun displayAvatarFromBase64(base64Str: String) {
        ImageUtils.setAvatarFromBase64(base64Str, avatarIv)
    }

    private fun loadTodayCheckRecord(empId: String) {
        showLoading(true)
        val jsonBody = "{\"emp_id\":\"$empId\"}"
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val request = buildAuthenticatedRequest("${ApiService.BASE_URL}/api/get_daily_record")
            .post(jsonBody.toRequestBody(mediaType))
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    showLoading(false)
                    showDialog("提示", "加载打卡记录失败：${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val respBody = response.body?.string()
                activity?.runOnUiThread {
                    showLoading(false)
                    try {
                        val json = JSONObject(respBody ?: "{}")
                        if (json.optInt("code") == 200) {
                            val data = json.optJSONObject("data")
                            val checkInTime = data?.optString("check_in_time")?.takeIf { it != "null" && it.isNotEmpty() } ?: "未打卡"
                            val checkOutTime = data?.optString("check_out_time")?.takeIf { it != "null" && it.isNotEmpty() } ?: "未打卡"
                            tvCheckInTime.text = checkInTime
                            tvCheckOutTime.text = checkOutTime
                            val status = when {
                                checkInTime != "未打卡" && checkOutTime != "未打卡" -> "已完成"
                                checkInTime != "未打卡" && checkOutTime == "未打卡" -> "已上班，未下班"
                                else -> "未打卡"
                            }
                            tvStatus.text = status
                            btnCheckIn.isEnabled = checkInTime == "未打卡"
                            btnCheckOut.isEnabled = checkInTime != "未打卡" && checkOutTime == "未打卡"
                        } else {
                            tvCheckInTime.text = "未打卡"
                            tvCheckOutTime.text = "未打卡"
                            tvStatus.text = "未打卡"
                            btnCheckIn.isEnabled = true
                            btnCheckOut.isEnabled = false
                        }
                    } catch (_: Exception) {
                        showDialog("提示", "解析打卡记录失败")
                    }
                }
            }
        })
    }

    // ========== 设备绑定 ==========
    private fun ensureDeviceBindingAndProceed(onComplete: () -> Unit, onError: (String) -> Unit) {
        if (isBinding) {
            onError("设备绑定中，请稍后...")
            return
        }
        val deviceId = DeviceUtils.getAndroidDeviceId(requireContext())
        if (!DeviceUtils.isDeviceIdValid(deviceId)) {
            onError("无法获取设备ID，请检查系统设置")
            return
        }
        val empId = currentEmpId
        if (empId.isNullOrEmpty()) {
            onError("员工信息缺失")
            return
        }

        val prefs = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val cachedBound = prefs.getBoolean(Constants.KEY_DEVICE_BOUND, false)
        val cachedDeviceId = prefs.getString(Constants.KEY_DEVICE_ID, "")
        if (cachedBound && cachedDeviceId == deviceId) {
            onComplete()
            return
        }

        showLoading(true)
        val request = buildAuthenticatedRequest("${ApiService.BASE_URL}/api/employees")
            .get()
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    showLoading(false)
                    onError("获取员工信息失败：${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                activity?.runOnUiThread {
                    showLoading(false)
                    try {
                        val json = JSONObject(body ?: "{}")
                        if (json.optInt("code") == 200) {
                            val employees = json.optJSONArray("data")
                            var boundDeviceId: String? = null
                            if (employees != null) {
                                for (i in 0 until employees.length()) {
                                    val emp = employees.getJSONObject(i)
                                    if (emp.optString("emp_id") == empId) {
                                        boundDeviceId = emp.optString("device_id")
                                        break
                                    }
                                }
                            }
                            if (boundDeviceId.isNullOrEmpty()) {
                                doBindDevice(deviceId, empId, onComplete, onError)
                            } else {
                                if (boundDeviceId == deviceId) {
                                    prefs.edit().putBoolean(Constants.KEY_DEVICE_BOUND, true)
                                        .putString(Constants.KEY_DEVICE_ID, deviceId).apply()
                                    onComplete()
                                } else {
                                    onError("该账号已绑定其他设备，请使用原绑定设备打卡。如需更换设备，请联系管理员解绑。")
                                }
                            }
                        } else {
                            onError(json.optString("msg", "获取员工信息失败"))
                        }
                    } catch (e: Exception) {
                        onError("解析响应失败：${e.message}")
                    }
                }
            }
        })
    }

    private fun doBindDevice(deviceId: String, empId: String, onComplete: () -> Unit, onError: (String) -> Unit) {
        isBinding = true
        showLoading(true)
        val bindJson = JSONObject().apply {
            put("emp_id", empId)
            put("device_id", deviceId)
            put("force", false)
        }.toString()
        val request = buildAuthenticatedRequest("${ApiService.BASE_URL}/api/bind_device")
            .post(bindJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                isBinding = false
                activity?.runOnUiThread {
                    showLoading(false)
                    onError("设备绑定失败：${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                isBinding = false
                val body = response.body?.string()
                try {
                    val json = JSONObject(body ?: "{}")
                    if (json.optInt("code") == 200) {
                        val prefs = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                        prefs.edit()
                            .putBoolean(Constants.KEY_DEVICE_BOUND, true)
                            .putString(Constants.KEY_DEVICE_ID, deviceId)
                            .apply()
                        activity?.runOnUiThread {
                            showLoading(false)
                            onComplete()
                        }
                    } else {
                        val msg = json.optString("msg", "设备绑定失败")
                        activity?.runOnUiThread {
                            showLoading(false)
                            onError(msg)
                        }
                    }
                } catch (_: Exception) {
                    activity?.runOnUiThread {
                        showLoading(false)
                        onError("绑定响应解析失败")
                    }
                }
            }
        })
    }

    // ========== 定位获取 ==========
    private fun getCurrentLocation(callback: (Double?, Double?, String?) -> Unit) {
        if (!checkLocationPermission()) {
            callback(null, null, null)
            return
        }
        if (mLocationClient == null) {
            callback(null, null, null)
            showDialog("提示", "定位服务未初始化")
            return
        }

        val callbackInvoked = AtomicBoolean(false)
        lateinit var locationListener: AMapLocationListener
        locationListener = AMapLocationListener { location ->
            if (!callbackInvoked.getAndSet(true)) {
                if (location != null && location.errorCode == 0) {
                    if (location.isMock) {
                        activity?.runOnUiThread {
                            showDialog("提示", "检测到虚拟定位，请关闭模拟位置软件后重试")
                        }
                        callback(null, null, null)
                        mLocationClient?.stopLocation()
                        mLocationClient?.unRegisterLocationListener(locationListener)
                        return@AMapLocationListener
                    }
                    callback(location.latitude, location.longitude, location.address)
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude
                    currentAddress = location.address
                    updateUserLocationMarker(location.latitude, location.longitude)
                } else {
                    callback(null, null, null)
                }
                mLocationClient?.stopLocation()
                mLocationClient?.unRegisterLocationListener(locationListener)
            }
        }

        mLocationClient?.setLocationListener(locationListener)
        mLocationClient?.startLocation()

        Handler(Looper.getMainLooper()).postDelayed({
            if (!callbackInvoked.getAndSet(true)) {
                callback(null, null, null)
                mLocationClient?.stopLocation()
                mLocationClient?.unRegisterLocationListener(locationListener)
            }
        }, 10000)
    }

    // ========== 打卡点与时间规则 ==========
    private fun isFaceRequiredAtLocation(lat: Double, lng: Double): Boolean {
        var minDistance = Double.MAX_VALUE
        var nearestPoint: JSONObject? = null
        for (point in punchPoints) {
            val pointLat = point.optDouble("latitude")
            val pointLng = point.optDouble("longitude")
            if (pointLat == 0.0 && pointLng == 0.0) continue
            val distance = calculateDistance(lat, lng, pointLat, pointLng)
            val radius = point.optInt("radius", 0)
            if (distance <= radius && distance < minDistance) {
                minDistance = distance
                nearestPoint = point
            }
        }
        return nearestPoint?.optInt("face_required", 0) == 1
    }

    private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private fun loadTimeRuleForPoint(pointId: Int?) {
        val url = if (pointId != null) {
            "${ApiService.BASE_URL}/api/emp/time_rule?point_id=$pointId"
        } else {
            "${ApiService.BASE_URL}/api/emp/time_rule"
        }
        val request = buildAuthenticatedRequest(url).get().build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ClockInSample", "获取时间规则失败: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    try {
                        val json = JSONObject(body)
                        if (json.optInt("code") == 200) {
                            val data = json.getJSONObject("data")
                            timeRule = PunchTimeRule(
                                checkInDeadline = data.getString("check_in_deadline"),
                                checkOutStart = data.getString("check_out_start"),
                                lateMinutes = data.getInt("late_minutes"),
                                earlyLeaveMinutes = data.getInt("early_leave_minutes")
                            )
                            Log.d("ClockInSample", "时间规则加载成功: 下班开始时间 ${timeRule?.checkOutStart}")
                        }
                    } catch (e: Exception) {
                        Log.e("ClockInSample", "解析时间规则失败: ${e.message}")
                    }
                }
            }
        })
    }

    @SuppressLint("DefaultLocale")
    private fun isEarlyLeave(): Boolean {
        if (timeRule == null) return false
        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val parts = timeRule!!.checkOutStart.split(":")
        val ruleMinutes = parts[0].toInt() * 60 + parts[1].toInt()
        return currentMinutes < ruleMinutes
    }

    private fun showEarlyLeaveConfirmDialog(onResult: (Boolean) -> Unit) {
        activity?.let {
            DialogUtils.showCustomDialog(
                context = it,
                title = "早退确认",
                message = "当前时间未到下班时间，是否确认打卡？（将记为早退）",
                positiveText = "确认早退",
                negativeText = "取消",
                onPositive = { onResult(true) },
                onNegative = { onResult(false) }
            )
        }
    }

    // ========== 打卡逻辑 ==========
    private fun performCheckIn() {
        if (isClockInProgress) {
            showDialog("提示", "打卡处理中，请稍后...")
            return
        }
        if (currentEmpId.isNullOrEmpty()) {
            showDialog("提示", "员工信息缺失")
            return
        }
        if (!checkLocationPermission()) {
            requestLocationPermission()
            return
        }
        isClockInProgress = true
        ensureDeviceBindingAndProceed(
            onComplete = {
                getCurrentLocation { lat, lng, addr ->
                    if (lat != null && lng != null) {
                        if (isFaceRequiredAtLocation(lat, lng)) {
                            pendingCheckType = 1
                            pendingLatitude = lat
                            pendingLongitude = lng
                            pendingAddress = addr ?: ""
                            openCamera()
                        } else {
                            submitCheck(1, lat, lng, addr ?: "", "", false)
                        }
                    } else {
                        showDialog("提示", "无法获取位置，请检查定位权限")
                        isClockInProgress = false
                    }
                }
            },
            onError = { errorMsg ->
                showDialog("提示", errorMsg)
                isClockInProgress = false
            }
        )
    }

    private fun performCheckOut() {
        if (isClockInProgress) {
            showDialog("提示", "打卡处理中，请稍后...")
            return
        }
        if (currentEmpId.isNullOrEmpty()) {
            showDialog("提示", "员工信息缺失")
            return
        }
        if (!checkLocationPermission()) {
            requestLocationPermission()
            return
        }
        isClockInProgress = true
        ensureDeviceBindingAndProceed(
            onComplete = {
                getCurrentLocation { lat, lng, addr ->
                    if (lat != null && lng != null) {
                        // 下班也需要人脸验证
                        if (isFaceRequiredAtLocation(lat, lng)) {
                            pendingCheckType = 2
                            pendingLatitude = lat
                            pendingLongitude = lng
                            pendingAddress = addr ?: ""
                            openCamera()
                        } else {
                            // 不需要人脸，走原有早退确认流程
                            if (timeRule == null) loadTimeRuleForPoint(null)
                            if (isEarlyLeave()) {
                                showEarlyLeaveConfirmDialog { confirmed ->
                                    if (confirmed) {
                                        submitCheck(2, lat, lng, addr ?: "", "", true)
                                    } else {
                                        isClockInProgress = false
                                    }
                                }
                            } else {
                                submitCheck(2, lat, lng, addr ?: "", "", false)
                            }
                        }
                    } else {
                        showDialog("提示", "无法获取位置，请检查定位权限")
                        isClockInProgress = false
                    }
                }
            },
            onError = { errorMsg ->
                showDialog("提示", errorMsg)
                isClockInProgress = false
            }
        )
    }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), Constants.CAMERA_PERMISSION_REQUEST_CODE)
            return
        }
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureLauncher.launch(intent)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        return ImageUtils.bitmapToBase64(bitmap)
    }

    private fun submitCheck(checkType: Int, latitude: Double, longitude: Double, address: String, faceBase64: String, confirmEarly: Boolean) {
        showProgressDialog("打卡中...")
        showLoading(true)
        val deviceId = DeviceUtils.getAndroidDeviceId(requireContext())
        val jsonBody = JSONObject().apply {
            put("emp_id", currentEmpId)
            put("check_type", checkType)
            put("longitude", longitude)
            put("latitude", latitude)
            put("device_id", deviceId)
            put("address", address)
            put("wifi_mac", "")
            put("face_base64", faceBase64)
            put("confirm_early", confirmEarly)
        }.toString()
        val request = buildAuthenticatedRequest("${ApiService.BASE_URL}/api/upload_check")
            .post(jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    showLoading(false)
                    dismissProgressDialog()
                    showDialog("打卡失败", "打卡失败：${e.message}")
                    isClockInProgress = false
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val respBody = response.body?.string()
                activity?.runOnUiThread {
                    showLoading(false)
                    dismissProgressDialog()
                    try {
                        val json = JSONObject(respBody ?: "{}")
                        if (json.optInt("code") == 200) {
                            showDialog("打卡成功", "打卡成功！", isSuccess = true)
                            currentEmpId?.let { loadTodayCheckRecord(it) }
                        } else {
                            showDialog("打卡失败", json.optString("msg", "打卡失败"))
                        }
                    } catch (_: Exception) {
                        showDialog("提示", "打卡响应解析失败")
                    } finally {
                        isClockInProgress = false
                    }
                }
            }
        })
    }

    private fun testTodaySchedule() {
        val empId = currentEmpId ?: return
        val request = buildAuthenticatedRequest("${ApiService.BASE_URL}/api/my_schedule_today")
            .get()
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ClockInSample", "获取今日排班失败: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                Log.d("ClockInSample", "今日排班接口返回: $body")
            }
        })
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        requestPermissions(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            Constants.LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            Constants.LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    showDialog("提示", "定位权限被拒绝，无法打卡")
                }
            }
            Constants.CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera()
                } else {
                    showDialog("提示", "相机权限被拒绝，无法进行人脸验证打卡")
                }
            }
        }
    }

    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
    override fun onDestroyView() {
        super.onDestroyView()
        userLocationMarker?.remove()
        mapView.onDestroy()
        mLocationClient?.onDestroy()
        dismissProgressDialog()
    }

    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); mapView.onSaveInstanceState(outState) }

    companion object {
        // SharedPreferences keys are now in Constants
        // Permission request codes are now in Constants
    }
}
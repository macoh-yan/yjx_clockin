package com.example.yjx_clockin

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.yjx_clockin.databinding.ActivityLoginBinding
import com.example.yjx_clockin.utils.ApiService
import com.example.yjx_clockin.utils.Constants
import com.example.yjx_clockin.utils.DeviceUtils
import com.example.yjx_clockin.utils.EncryptionUtils
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 沉浸式状态栏设置（保持原有）
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 加载保存的账号密码（如果已记住）
        loadSavedCredentials()

        binding.btnLogin.setOnClickListener {
            val empId = binding.etEmpId.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            if (validateInput(empId, password)) {
                // 先处理设备绑定检查，再执行登录
                checkAndBindDevice(empId, password)
            }
        }
    }

    private fun loadSavedCredentials() {
        val prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
        val savedEmpId = prefs.getString(Constants.KEY_SAVED_EMP_ID, "")
        val savedPassword = prefs.getString(Constants.KEY_SAVED_PASSWORD, "")
        val isRemembered = prefs.getBoolean(Constants.KEY_REMEMBER_PASSWORD, false)

        if (isRemembered && !savedEmpId.isNullOrEmpty() && !savedPassword.isNullOrEmpty()) {
            binding.etEmpId.setText(savedEmpId)
            binding.etPassword.setText(EncryptionUtils.decrypt(savedPassword))
            binding.cbRemember.isChecked = true
        }
    }

    private fun saveCredentials(empId: String, password: String) {
        val prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
        val editor = prefs.edit()
        if (binding.cbRemember.isChecked) {
            editor.putString(Constants.KEY_SAVED_EMP_ID, empId)
            editor.putString(Constants.KEY_SAVED_PASSWORD, EncryptionUtils.encrypt(password))
            editor.putBoolean(Constants.KEY_REMEMBER_PASSWORD, true)
        } else {
            editor.remove(Constants.KEY_SAVED_EMP_ID)
            editor.remove(Constants.KEY_SAVED_PASSWORD)
            editor.putBoolean(Constants.KEY_REMEMBER_PASSWORD, false)
        }
        editor.apply()
    }

    private fun validateInput(empId: String, password: String): Boolean {
        if (TextUtils.isEmpty(empId)) {
            showError("请输入员工编号")
            return false
        }
        if (TextUtils.isEmpty(password)) {
            showError("请输入密码")
            return false
        }
        return true
    }

    /**
     * 检查设备绑定状态，必要时自动绑定
     */
    private fun checkAndBindDevice(empId: String, password: String) {
        val deviceId = DeviceUtils.getAndroidDeviceId(this)
        if (!DeviceUtils.isDeviceIdValid(deviceId)) {
            showError("无法获取设备ID，请检查系统设置")
            return
        }

        val prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
        val cachedBound = prefs.getBoolean(Constants.KEY_DEVICE_BOUND, false)
        val cachedDeviceId = prefs.getString(Constants.KEY_DEVICE_ID, "")

        // 快速缓存命中
        if (cachedBound && cachedDeviceId == deviceId) {
            // 直接执行登录
            performLogin(empId, password)
            return
        }

        setLoading(true)
        // 获取员工列表，查找当前员工的设备绑定状态
        ApiService.getEmployeeList { list ->
            runOnUiThread {
                setLoading(false)
                val currentEmp = list.find { it["emp_id"] == empId }
                if (currentEmp == null) {
                    showError("员工信息不存在，请检查工号")
                    return@runOnUiThread
                }
                val boundDeviceId = currentEmp["device_id"] as? String ?: ""
                if (boundDeviceId.isEmpty()) {
                    // 未绑定设备，自动绑定当前设备
                    autoBindDevice(empId, deviceId) { success ->
                        if (success) {
                            // 绑定成功，保存缓存并登录
                            prefs.edit().putBoolean(Constants.KEY_DEVICE_BOUND, true)
                                .putString(Constants.KEY_DEVICE_ID, deviceId).apply()
                            performLogin(empId, password)
                        } else {
                            showError("设备绑定失败，请重试")
                        }
                    }
                } else if (boundDeviceId == deviceId) {
                    // 已绑定且一致，更新本地缓存并登录
                    prefs.edit().putBoolean(Constants.KEY_DEVICE_BOUND, true)
                        .putString(Constants.KEY_DEVICE_ID, deviceId).apply()
                    performLogin(empId, password)
                } else {
                    // 已绑定其他设备，拒绝登录
                    showError("该账号已绑定其他设备，请使用原绑定设备登录。如需更换设备，请联系管理员解绑。")
                }
            }
        }
    }

    private fun autoBindDevice(empId: String, deviceId: String, callback: (Boolean) -> Unit) {
        setLoading(true)
        val bindJson = JSONObject().apply {
            put("emp_id", empId)
            put("device_id", deviceId)
            put("force", false) // 仅用于未绑定场景，不强制覆盖
        }.toString()
        val request = Request.Builder()
            .url("${ApiService.BASE_URL}/api/bind_device")
            .post(bindJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                runOnUiThread {
                    setLoading(false)
                    callback(false)
                }
            }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val body = response.body?.string()
                runOnUiThread {
                    setLoading(false)
                    try {
                        val json = JSONObject(body ?: "{}")
                        callback(json.optInt("code") == 200)
                    } catch (e: Exception) {
                        callback(false)
                    }
                }
            }
        })
    }

    private fun performLogin(empId: String, password: String) {
        setLoading(true)
        val deviceId = DeviceUtils.getAndroidDeviceId(this)

        ApiService.login(empId, password, deviceId) { success, json, cookie ->
            runOnUiThread {
                setLoading(false)
                if (success && json != null && json.optInt("code") == 200) {
                    val data = json.optJSONObject("data")
                    val token = data?.optString("token") ?: ""
                    val empIdFromData = data?.optString("emp_id") ?: ""
                    val name = data?.optString("name") ?: ""
                    Log.e("===LoginActivity的Token", token)

                    val prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
                    val editor = prefs.edit()
                    editor.putString(Constants.KEY_TOKEN, token)
                    editor.putString(Constants.KEY_EMP_ID, empIdFromData)
                    editor.putString(Constants.KEY_EMP_NAME, name)
                    if (!cookie.isNullOrEmpty()) {
                        val cookieValue = cookie.split(";").firstOrNull()?.trim() ?: cookie
                        editor.putString(Constants.KEY_COOKIE, cookieValue)
                    }
                    editor.apply()

                    // 保存记住的密码
                    saveCredentials(empId, password)

                    ApiService.setToken(token)

                    startActivity(Intent(this, ClockInSamplePage::class.java))
                    finish()
                } else {
                    val msg = json?.optString("msg") ?: "登录失败，请稍后重试"
                    showError(msg)
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnLogin.isEnabled = !loading
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun showError(msg: String) {
        binding.tvError.text = msg
        binding.tvError.visibility = View.VISIBLE
        binding.tvError.postDelayed({ binding.tvError.visibility = View.GONE }, 3000)
    }
}
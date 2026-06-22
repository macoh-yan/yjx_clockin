package com.example.yjx_clockin

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.yjx_clockin.databinding.ActivityLoginBinding
import com.example.yjx_clockin.utils.ApiService
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
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val savedEmpId = prefs.getString("saved_emp_id", "")
        val isRemembered = prefs.getBoolean("remember_password", false)

        if (isRemembered && !savedEmpId.isNullOrEmpty()) {
            binding.etEmpId.setText(savedEmpId)
            binding.cbRemember.isChecked = true
            // 密码不再明文持久化：只预填工号，密码由用户手动输入
            binding.etPassword.requestFocus()
        }
    }

    private fun saveCredentials(empId: String) {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val editor = prefs.edit()
        if (binding.cbRemember.isChecked) {
            editor.putString("saved_emp_id", empId)
            editor.putBoolean("remember_password", true)
        } else {
            editor.remove("saved_emp_id")
            editor.putBoolean("remember_password", false)
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

    @SuppressLint("HardwareIds")
    private fun getAndroidDeviceId(): String {
        return try {
            Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
        } catch (e: Exception) {
            "unknown_device"
        }
    }

    /**
     * 检查设备绑定状态，必要时自动绑定
     */
    private fun checkAndBindDevice(empId: String, password: String) {
        val deviceId = getAndroidDeviceId()
        if (deviceId.isEmpty() || deviceId == "unknown_device") {
            showError("无法获取设备ID，请检查系统设置")
            return
        }

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val cachedBound = prefs.getBoolean("device_bound", false)
        val cachedDeviceId = prefs.getString("device_id", "")

        // 快速缓存命中
        if (cachedBound && cachedDeviceId == deviceId) {
            // 直接执行登录
            performLogin(empId, password)
            return
        }

        setLoading(true)
        // 仅拉取当前员工信息，避免泄露全员数据
        ApiService.verifyEmployeeExists(empId) { exists, boundDeviceId ->
            runOnUiThread {
                setLoading(false)
                if (!exists) {
                    showError("员工信息不存在，请检查工号")
                    return@runOnUiThread
                }
                if (boundDeviceId.isEmpty()) {
                    // 未绑定设备，自动绑定当前设备
                    autoBindDevice(empId, deviceId) { success ->
                        if (success) {
                            prefs.edit().putBoolean("device_bound", true)
                                .putString("device_id", deviceId).apply()
                            performLogin(empId, password)
                        } else {
                            showError("设备绑定失败，请重试")
                        }
                    }
                } else if (boundDeviceId == deviceId) {
                    prefs.edit().putBoolean("device_bound", true)
                        .putString("device_id", deviceId).apply()
                    performLogin(empId, password)
                } else {
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
        val deviceId = getAndroidDeviceId()

        ApiService.login(empId, password, deviceId) { success, json, cookie ->
            runOnUiThread {
                setLoading(false)
                if (success && json != null && json.optInt("code") == 200) {
                    val data = json.optJSONObject("data")
                    val token = data?.optString("token") ?: ""
                    val empIdFromData = data?.optString("emp_id") ?: ""
                    val name = data?.optString("name") ?: ""
                    Log.e("===LoginActivity的Token", token)

                    val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                    val editor = prefs.edit()
                    editor.putString("token", token)
                    editor.putString("emp_id", empIdFromData)
                    editor.putString("emp_name", name)
                    if (!cookie.isNullOrEmpty()) {
                        val cookieValue = cookie.split(";").firstOrNull()?.trim() ?: cookie
                        editor.putString("cookie", cookieValue)
                    }
                    editor.apply()

                    // 仅记住工号，密码不再明文持久化
                    saveCredentials(empId)

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
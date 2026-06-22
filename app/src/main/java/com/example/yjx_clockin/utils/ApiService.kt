package com.example.yjx_clockin.utils

import com.example.yjx_clockin.model.MenuButton
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

object ApiService {
    const val BASE_URL = Constants.BASE_URL
    private const val API_URL_EMPLOYEES = "$BASE_URL/api/employees"
    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request()
            Log.d("OkHttp", "请求 URL: ${request.url}")
            val response = chain.proceed(request)
            Log.d("OkHttp", "响应码: ${response.code}")
            response
        }
        .build()
    // 全局 Token，登录后调用 setToken() 设置
    private var authToken: String? = null

    /**
     * 设置认证 Token (Bearer)
     */
    fun setToken(token: String) {
        authToken = token
    }

    /**
     * 清除 Token
     */
    fun clearToken() {
        authToken = null
    }

    /**
     * 构建带认证头的请求
     */
    private fun buildRequest(url: String, method: String = "GET", body: RequestBody? = null): Request {
        val builder = Request.Builder().url(url).method(method, body)
        authToken?.let {
            builder.addHeader("Authorization", "Bearer $it")
        }
        builder.addHeader("Content-Type", "application/json")
        return builder.build()
    }

    /**
     * 执行通用 GET 请求并返回 JSONObject
     */
    private fun getJson(url: String, onResult: (JSONObject?) -> Unit) {
        val request = buildRequest(url)
        Log.d("===ApiService", "请求 URL: $url")
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("===ApiService", "请求失败: ${e.message}")
                onResult(null)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                Log.d("===ApiService", "响应: $body")
                if (!response.isSuccessful || body == null) {
                    onResult(null)
                    return
                }
                try {
                    val json = JSONObject(body)
                    onResult(json)
                } catch (e: Exception) {
                    Log.e("===ApiService", "JSON 解析失败: ${e.message}")
                    onResult(null)
                }
            }
        })
    }

    // ==================== 新增：登录接口（可提取 Set-Cookie） ====================
    /**
     * 登录接口，返回完整的 Response 对象（用于提取 Cookie）
     * @param empId 员工编号
     * @param password 密码
     * @param deviceId 设备ID
     * @param callback 回调，参数为 (是否成功, 响应体JSON, Cookie字符串)
     */
    fun login(
        empId: String,
        password: String,
        deviceId: String,
        callback: (success: Boolean, json: JSONObject?, cookie: String?) -> Unit
    ) {
        val url = "$BASE_URL/api/employee/login"
        val jsonBody = JSONObject().apply {
            put("emp_id", empId)
            put("password", password)
            put("device_id", deviceId)
        }
        val body = RequestBody.create("application/json".toMediaTypeOrNull(), jsonBody.toString())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ApiService", "登录请求失败: ${e.message}")
                callback(false, null, null)
            }

            override fun onResponse(call: Call, response: Response) {
                val bodyString = response.body?.string()
                val setCookie = response.header("Set-Cookie")  // 提取 Cookie
                Log.d("ApiService", "Set-Cookie: $setCookie")
                Log.d("ApiService", "登录响应: $bodyString")

                if (!response.isSuccessful || bodyString == null) {
                    callback(false, null, setCookie)
                    return
                }
                try {
                    val json = JSONObject(bodyString)
                    callback(true, json, setCookie)
                } catch (e: Exception) {
                    Log.e("ApiService", "JSON解析失败: ${e.message}")
                    callback(false, null, setCookie)
                }
            }
        })
    }

    // ==================== 1. 获取员工列表 ====================
    fun getEmployeeList(onResult: (List<Map<String, String>>) -> Unit) {
        val request = Request.Builder().url(API_URL_EMPLOYEES).build()
        Log.d("===ApiService", "请求 URL: $API_URL_EMPLOYEES")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                Log.e("===ApiService", "网络请求失败: ${e.message}")
                onResult(emptyList())
            }

            override fun onResponse(call: Call, response: Response) {
                val json = response.body?.string()
                Log.d("===ApiService", "原始响应 JSON: $json")
                val list = mutableListOf<Map<String, String>>()
                try {
                    val jsonObject = JSONObject(json)
                    val arr = jsonObject.getJSONArray("data")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(mapOf(
                            "emp_id" to obj.getString("emp_id"),
                            "name" to obj.getString("name"),
                            "department" to obj.getString("department")
                        ))
                    }
                    Log.d("===ApiService", "解析成功，共 ${list.size} 条数据")
                } catch (e: Exception) {
                    Log.e("===ApiService", "JSON 解析失败: ${e.message}")
                    e.printStackTrace()
                }
                onResult(list)
            }
        })
    }

    // ==================== 3. 获取移动端菜单（/employee/mobile） ====================

    fun getMobileMenu(onResult: (List<MenuButton>) -> Unit) {
        val url = "$BASE_URL/employee/mobile"
        getJson(url) { json ->
            val menus = mutableListOf<MenuButton>()
            if (json != null && json.optInt("code") == 200) {
                val data = json.optJSONArray("buttons") ?: json.optJSONArray("data")
                if (data != null) {
                    for (i in 0 until data.length()) {
                        val obj = data.getJSONObject(i)
                        val name = obj.optString("name", "")
                        val icon = obj.optString("icon", "fa-circle")
                        val url = obj.optString("url", "#")
                        menus.add(MenuButton(name, icon, url))
                    }
                }
            }
            onResult(menus)
        }
    }

    // ==================== 3. 获取当前用户信息（含角色） ====================
    fun getCurrentUser(onResult: (JSONObject) -> Unit) {
        val url = "$BASE_URL/api/current_user"
        getJson(url) { json ->
            if (json != null && json.optInt("code") == 200) {
                val data = json.optJSONObject("data")
                onResult(data ?: JSONObject())
            } else {
                onResult(JSONObject())
            }
        }
    }

    // ==================== 4. 个人记录 ====================
    // 请假记录
    fun getMyLeaves(onResult: (List<JSONObject>) -> Unit) {
        val url = "$BASE_URL/api/leave/my"
        getJson(url) { json ->
            val list = mutableListOf<JSONObject>()
            if (json != null && json.optInt("code") == 200) {
                val data = json.optJSONArray("data")
                if (data != null) {
                    for (i in 0 until data.length()) {
                        list.add(data.getJSONObject(i))
                    }
                }
            }
            onResult(list)
        }
    }

    // 调休记录
    fun getMyExchanges(onResult: (List<JSONObject>) -> Unit) {
        val url = "$BASE_URL/api/exchange/my"
        getJson(url) { json ->
            val list = mutableListOf<JSONObject>()
            if (json != null && json.optInt("code") == 200) {
                val data = json.optJSONArray("data")
                if (data != null) {
                    for (i in 0 until data.length()) {
                        list.add(data.getJSONObject(i))
                    }
                }
            }
            onResult(list)
        }
    }

    // 报销记录
    fun getMyExpenses(onResult: (List<JSONObject>) -> Unit) {
        val url = "$BASE_URL/api/expense/my"
        getJson(url) { json ->
            val list = mutableListOf<JSONObject>()
            if (json != null && json.optInt("code") == 200) {
                val data = json.optJSONArray("data")
                if (data != null) {
                    for (i in 0 until data.length()) {
                        list.add(data.getJSONObject(i))
                    }
                }
            }
            onResult(list)
        }
    }

    // ==================== 5. 待审批数量 ====================
    fun getLeaveWaitCount(onResult: (Int) -> Unit) {
        val url = "$BASE_URL/api/leave/wait_count"
        getJson(url) { json ->
            var count = 0
            if (json != null && json.optInt("code") == 200) {
                count = json.optInt("data", 0)
            }
            onResult(count)
        }
    }

    fun getExchangeWaitCount(onResult: (Int) -> Unit) {
        val url = "$BASE_URL/api/exchange/wait_count"
        getJson(url) { json ->
            var count = 0
            if (json != null && json.optInt("code") == 200) {
                count = json.optInt("data", 0)
            }
            onResult(count)
        }
    }

    fun getExpenseWaitCount(onResult: (Int) -> Unit) {
        val url = "$BASE_URL/api/expense/wait_count"
        getJson(url) { json ->
            var count = 0
            if (json != null && json.optInt("code") == 200) {
                count = json.optInt("data", 0)
            }
            onResult(count)
        }
    }

    fun getOutApplyWaitCount(onResult: (Int) -> Unit) {
        val url = "$BASE_URL/api/out_apply/wait_count"
        getJson(url) { json ->
            var count = 0
            if (json != null && json.optInt("code") == 200) {
                count = json.optInt("data", 0)
            }
            onResult(count)
        }
    }

    fun getPurchaseWaitCount(onResult: (Int) -> Unit) {
        val url = "$BASE_URL/api/purchase/wait_count"
        getJson(url) { json ->
            var count = 0
            if (json != null && json.optInt("code") == 200) {
                count = json.optInt("data", 0)
            }
            onResult(count)
        }
    }
    // ==================== 获取单个员工详细信息 ====================
    fun getEmployeeDetail(empId: String, onResult: (JSONObject?) -> Unit) {
        val url = "$BASE_URL/api/employees?keyword=$empId"
        getJson(url) { json ->
            if (json != null && json.optInt("code") == 200) {
                val data = json.optJSONArray("data")
                if (data != null && data.length() > 0) {
                    onResult(data.getJSONObject(0))
                } else {
                    onResult(null)
                }
            } else {
                onResult(null)
            }
        }
    }

    // ==================== 修改密码 ====================
    fun changePassword(oldPassword: String, newPassword: String, onResult: (Boolean, String) -> Unit) {
        val url = "$BASE_URL/api/employee/change_password"
        val jsonBody = JSONObject().apply {
            put("old_password", oldPassword)
            put("new_password", newPassword)
        }.toString()
        val request = buildRequest(url, "POST", jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onResult(false, e.message ?: "网络错误")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                try {
                    val json = JSONObject(body ?: "{}")
                    if (json.optInt("code") == 200) {
                        onResult(true, json.optString("msg", "修改成功"))
                    } else {
                        onResult(false, json.optString("msg", "修改失败"))
                    }
                } catch (e: Exception) {
                    onResult(false, "解析失败")
                }
            }
        })
    }
}
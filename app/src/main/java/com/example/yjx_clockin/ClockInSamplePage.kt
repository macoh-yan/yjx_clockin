package com.example.yjx_clockin

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.yjx_clockin.utils.ApiService
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import java.util.ArrayList

class ClockInSamplePage : AppCompatActivity() {

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    private val tabTitles = arrayOf("打卡", "工作台", "我的")
    private val tabIcons = arrayOf(
        R.drawable.ic_clock,
        R.drawable.ic_tab_workbench,
        R.drawable.ic_tab_profile
    )

    private var tvUsername: TextView? = null
    private var tvDepartment: TextView? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 清除旧标志
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        // 2. 允许绘制到系统栏
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        // 3. 设置颜色透明
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        // 4. 让内容延伸到系统栏
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // 5. 设置状态栏图标颜色（根据背景调整）
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true

        // 6. 设置布局
        setContentView(R.layout.activity_clock_in_sample_page)

        // 绑定控件
        tvUsername = findViewById(R.id.tv_username)
        tvDepartment = findViewById(R.id.tv_dep)

        // ViewPager2
        val viewPager2 = findViewById<ViewPager2>(R.id.viewPager2)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)

        val fragmentList: ArrayList<Fragment> = ArrayList()
        fragmentList.add(ClockInSample())
        fragmentList.add(WorkbenchContent())
        fragmentList.add(ProfileContent())

        val adapter = FragmentAdapter(this, fragmentList)
        viewPager2.adapter = adapter
        viewPager2.isUserInputEnabled = false

        TabLayoutMediator(tabLayout, viewPager2) { tab, position ->
            tab.setText(tabTitles[position])
            tab.setIcon(tabIcons[position])
        }.attach()

        // 加载当前登录用户的信息
        loadCurrentUserInfo()

        // 检查并请求位置权限（地图依赖）
        checkAndRequestLocationPermission()
    }

    /**
     * 检查并动态申请位置权限
     */
    private fun checkAndRequestLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // 已有权限，无需操作
            }
            else -> {
                // 请求位置权限（同时请求粗略和精确位置）
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    /**
     * 处理权限请求结果
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 权限已授予，如果需要可以通知地图Fragment刷新（可选，此处省略具体实现）
                    // 因为地图SDK内部会监听权限变化，或通过重新进入Fragment时重新加载
                } else {
                    // 权限被拒绝，可提示用户地图功能可能受限（可选）
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadCurrentUserInfo() {
        // 从 SharedPreferences 获取当前登录用户的 emp_id
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val currentEmpId = prefs.getString("emp_id", null)

        if (currentEmpId.isNullOrEmpty()) {
            tvUsername?.text = "未登录"
            tvDepartment?.text = "请重新登录"
            return
        }

        ApiService.getEmployeeList { list ->
            runOnUiThread {
                // 在员工列表中查找当前登录的用户
                val currentUser = list.find { it["emp_id"] == currentEmpId }
                if (currentUser != null) {
                    tvUsername?.text = "欢迎：${currentUser["name"]}"
                    tvDepartment?.text = "部门：${currentUser["department"]}"
                } else {
                    tvUsername?.text = "用户信息不存在"
                    tvDepartment?.text = "请检查网络或联系管理员"
                }
            }
        }
    }
}
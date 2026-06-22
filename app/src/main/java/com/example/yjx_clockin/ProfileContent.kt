package com.example.yjx_clockin

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import com.example.yjx_clockin.utils.ApiService
import com.example.yjx_clockin.utils.Constants
import com.example.yjx_clockin.utils.DeviceUtils
import com.example.yjx_clockin.utils.DialogUtils
import com.example.yjx_clockin.utils.ImageUtils
import de.hdodenhof.circleimageview.CircleImageView

class ProfileContent : Fragment() {

    private lateinit var ivAvatar: CircleImageView
    private lateinit var tvUsername: TextView
    private lateinit var tvDepartment: TextView
    private lateinit var tvEmpId: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvHireDate: TextView
    private lateinit var tvDeviceStatus: TextView
    private lateinit var btnChangePassword: Button
    private lateinit var btnAbout: Button
    private lateinit var btnLogout: Button

    private lateinit var prefs: android.content.SharedPreferences
    private var currentEmpId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_profile_content, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 绑定视图
        ivAvatar = view.findViewById(R.id.iv_avatar)
        tvUsername = view.findViewById(R.id.tv_username)
        tvDepartment = view.findViewById(R.id.tv_department)
        tvEmpId = view.findViewById(R.id.tv_emp_id)
        tvPhone = view.findViewById(R.id.tv_phone)
        tvEmail = view.findViewById(R.id.tv_email)
        tvHireDate = view.findViewById(R.id.tv_hire_date)
        tvDeviceStatus = view.findViewById(R.id.tv_device_status)
        btnChangePassword = view.findViewById(R.id.btn_change_password)
        btnAbout = view.findViewById(R.id.btn_about)
        btnLogout = view.findViewById(R.id.btn_logout)

        prefs = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        currentEmpId = prefs.getString("emp_id", null)

        // 先从本地缓存加载基本信息（快速显示）
        loadUserInfoFromLocal()
        // 然后从网络拉取详细数据（覆盖并加载头像）
        fetchUserDetail()

        // 设置按钮点击事件
        btnChangePassword.setOnClickListener { showChangePasswordDialog() }
        btnAbout.setOnClickListener { showAboutDialog() }
        btnLogout.setOnClickListener { logout() }
    }

    /**
     * 从本地 SharedPreferences 加载缓存信息，快速展示
     */
    private fun loadUserInfoFromLocal() {
        val name = prefs.getString(Constants.KEY_EMP_NAME, "")
        val empId = prefs.getString(Constants.KEY_EMP_ID, "")
        tvUsername.text = if (name.isNullOrEmpty()) "员工" else name
        tvEmpId.text = empId ?: "--"
        tvDepartment.text = "加载中..."
        tvPhone.text = "--"
        tvEmail.text = "--"
        tvHireDate.text = "--"

        // 显示设备绑定状态
        val deviceId = DeviceUtils.getAndroidDeviceId(requireContext())
        val cachedBound = prefs.getBoolean(Constants.KEY_DEVICE_BOUND, false)
        val cachedDeviceId = prefs.getString(Constants.KEY_DEVICE_ID, "")
        tvDeviceStatus.text = if (cachedBound && cachedDeviceId == deviceId) "已绑定" else "未绑定"
    }

    /**
     * 从服务器获取员工详细信息（包含头像 Base64）
     */
    private fun fetchUserDetail() {
        val empId = currentEmpId ?: return
        ApiService.getEmployeeDetail(empId) { empJson ->
            activity?.runOnUiThread {
                if (empJson != null) {
                    val name = empJson.optString("name")
                    val department = empJson.optString("department")
                    val phone = empJson.optString("phone")
                    val email = empJson.optString("email")
                    val hireDate = empJson.optString("hire_date")
                    val faceImageBase64 = empJson.optString("face_image")

                    tvUsername.text = name.takeIf { it.isNotEmpty() } ?: tvUsername.text
                    tvDepartment.text = department.takeIf { it.isNotEmpty() } ?: "未分配"
                    tvPhone.text = phone.takeIf { it.isNotEmpty() } ?: "--"
                    tvEmail.text = email.takeIf { it.isNotEmpty() } ?: "--"
                    tvHireDate.text = hireDate.takeIf { it.isNotEmpty() } ?: "--"

                    // 加载头像：优先使用 face_image，否则使用默认图
                    if (faceImageBase64.isNotEmpty()) {
                        displayAvatarFromBase64(faceImageBase64)
                    } else {
                        ivAvatar.setImageResource(R.drawable.name_image)
                    }
                } else {
                    ivAvatar.setImageResource(R.drawable.name_image)
                }
            }
        }
    }

    /**
     * 将 Base64 字符串解码为 Bitmap 并设置到 CircleImageView
     */
    private fun displayAvatarFromBase64(base64Str: String) {
        ImageUtils.setAvatarFromBase64(base64Str, ivAvatar)
    }

    /**
     * 显示修改密码对话框（使用美化后的 DialogUtils）
     */
    @SuppressLint("MissingInflatedId")
    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null)
        val etOldPassword = dialogView.findViewById<EditText>(R.id.et_old_password)
        val etNewPassword = dialogView.findViewById<EditText>(R.id.et_new_password)
        val etConfirmPassword = dialogView.findViewById<EditText>(R.id.et_confirm_password)

        // 创建一个临时的 AlertDialog 来展示自定义视图（因为 DialogUtils 不支持嵌入复杂视图）
        // 为了保持风格一致，这里仍然使用 AlertDialog.Builder，但我们将背景设置为透明并附加圆角背景
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        // 设置按钮点击
        val positiveButton = dialogView.findViewById<Button>(R.id.btn_positive)
        val negativeButton = dialogView.findViewById<Button>(R.id.btn_negative)

        positiveButton.text = "确定"
        negativeButton.text = "取消"

        positiveButton.setOnClickListener {
            val oldPwd = etOldPassword.text.toString().trim()
            val newPwd = etNewPassword.text.toString().trim()
            val confirmPwd = etConfirmPassword.text.toString().trim()

            when {
                oldPwd.isEmpty() -> Toast.makeText(requireContext(), "请输入原密码", Toast.LENGTH_SHORT).show()
                newPwd.isEmpty() -> Toast.makeText(requireContext(), "请输入新密码", Toast.LENGTH_SHORT).show()
                newPwd.length < 6 -> Toast.makeText(requireContext(), "新密码长度至少6位", Toast.LENGTH_SHORT).show()
                newPwd != confirmPwd -> Toast.makeText(requireContext(), "两次输入的新密码不一致", Toast.LENGTH_SHORT).show()
                else -> {
                    ApiService.changePassword(oldPwd, newPwd) { success, msg ->
                        activity?.runOnUiThread {
                            if (success) {
                                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                                dialog.dismiss()
                            } else {
                                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }

        negativeButton.setOnClickListener {
            dialog.dismiss()
        }
    }

    /**
     * 显示关于系统对话框（使用美化后的 DialogUtils）
     */
    private fun showAboutDialog() {
        val versionName = try {
            requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName
        } catch (e: Exception) {
            "1.0"
        }
        val message = """
            亿杰智企综合管理平台
            版本：$versionName
            开发者：陕西亿杰鑫信息技术有限公司
            支持：智慧考勤 · 财务报销 · 采购审批 · 移动办公
        """.trimIndent()

        DialogUtils.showCustomDialog(
            context = requireContext(),
            title = "关于系统",
            message = message,
            iconRes = R.drawable.ic_dialog_info,   // 确保该资源存在，否则可传 null
            positiveText = "确定"
        )
    }

    /**
     * 退出登录确认对话框（使用美化后的 DialogUtils）
     */
    private fun logout() {
        DialogUtils.showCustomDialog(
            context = requireContext(),
            title = "提示",
            message = "确定要退出登录吗？",
            positiveText = "确定",
            negativeText = "取消",
            onPositive = {
                prefs.edit().clear().apply()
                ApiService.clearToken()
                startActivity(Intent(requireContext(), LoginActivity::class.java))
                requireActivity().finish()
            }
        )
    }
}
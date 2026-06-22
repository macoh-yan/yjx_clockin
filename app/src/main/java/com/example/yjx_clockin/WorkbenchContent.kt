package com.example.yjx_clockin

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.yjx_clockin.databinding.FragmentWorkbenchBinding
import com.example.yjx_clockin.utils.ApiService
import com.example.yjx_clockin.utils.WorkbenchAdapter

class WorkbenchContent : Fragment() {

    private var _binding: FragmentWorkbenchBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: WorkbenchAdapter
    private lateinit var sharedPref: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkbenchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 状态栏避让
        ViewCompat.setOnApplyWindowInsetsListener(binding.swipeRefreshLayout) { v, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBarsInsets.top, 0, 0)
            insets
        }
        ViewCompat.requestApplyInsets(binding.swipeRefreshLayout)

        sharedPref = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val token = sharedPref.getString("token", null)
        if (token.isNullOrEmpty()) {
            Log.e("===Workbench", "Token为空，跳转登录页")
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            return
        }
        ApiService.setToken(token)

        adapter = WorkbenchAdapter(
            onPersonalItemClick = { item -> openListPage(item) },
            onMenuItemClick = { menu -> openWebPage(menu.url, menu.name) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.swipeRefreshLayout.setOnRefreshListener { loadAllData() }
        loadAllData()
    }

    private fun loadAllData() {
        ApiService.getCurrentUser { userJson ->
            val userName = userJson.optString("name", "员工")
            val roles = mutableListOf<String>()
            val rolesArray = userJson.optJSONArray("roles")
            if (rolesArray != null) {
                for (i in 0 until rolesArray.length()) {
                    roles.add(rolesArray.getString(i))
                }
            }

            val personalItems = listOf(
                WorkbenchAdapter.PersonalGridItem("报销记录", R.drawable.ic_expense, "/employee/expense_list"),
                WorkbenchAdapter.PersonalGridItem("请假记录", R.drawable.ic_leave, "/employee/leave_list"),
                WorkbenchAdapter.PersonalGridItem("调休记录", R.drawable.ic_exchange, "/employee/exchange_list"),
                WorkbenchAdapter.PersonalGridItem("外出记录", R.drawable.ic_out_apply, "/employee/out_list"),
                WorkbenchAdapter.PersonalGridItem("采购记录", R.drawable.ic_purchase, "/employee/purchase_list"),
                WorkbenchAdapter.PersonalGridItem("加班记录", R.drawable.ic_overtime, "/employee/overtime_list"),
                WorkbenchAdapter.PersonalGridItem("费用申请记录", R.drawable.ic_cost, "/employee/cost_list"),
                WorkbenchAdapter.PersonalGridItem("工资条", R.drawable.ic_salary, "/employee/salary")
            )

            val applyMenus = buildApplyMenus()
            val approvalMenus = buildApprovalMenusByRoles(roles)

            requireActivity().runOnUiThread {
                adapter.setData(userName, personalItems, applyMenus, approvalMenus)
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun buildApplyMenus(): List<WorkbenchAdapter.MenuItem> {
        return listOf(
            WorkbenchAdapter.MenuItem("请假申请", R.drawable.ic_leave, "/employee/leave_apply", android.R.color.holo_blue_light),
            WorkbenchAdapter.MenuItem("调休申请", R.drawable.ic_exchange, "/employee/exchange_apply", android.R.color.holo_orange_light),
            WorkbenchAdapter.MenuItem("报销申请", R.drawable.ic_expense, "/employee/expense_apply", android.R.color.holo_green_light),
            WorkbenchAdapter.MenuItem("外出申请", R.drawable.ic_out_apply, "/employee/out_apply", android.R.color.holo_purple),
            WorkbenchAdapter.MenuItem("加班申请", R.drawable.ic_overtime, "/employee/overtime_apply", android.R.color.holo_red_light),
            WorkbenchAdapter.MenuItem("采购申请", R.drawable.ic_purchase, "/employee/purchase_apply", android.R.color.holo_blue_dark),
            WorkbenchAdapter.MenuItem("费用申请", R.drawable.ic_cost, "/employee/cost_apply", android.R.color.holo_green_dark)
        )
    }

    private fun buildApprovalMenusByRoles(roles: List<String>): List<WorkbenchAdapter.MenuItem> {
        val menus = mutableListOf<WorkbenchAdapter.MenuItem>()
        data class ApprovalRule(
            val name: String,
            val iconRes: Int,
            val url: String,
            val requiredRoles: List<String>,
            val iconTint: Int
        )
        val approvalRules = listOf(
            ApprovalRule("请假审批", R.drawable.ic_leave_approval, "/admin/leave_approval", listOf("admin", "manager", "supervisor", "general_manager", "tech_supervisor", "hr_supervisor"), android.R.color.holo_blue_light),
            ApprovalRule("调休审批", R.drawable.ic_exchange_approval, "/admin/exchange_approval", listOf("admin", "manager", "supervisor", "general_manager", "tech_supervisor", "hr_supervisor"), android.R.color.holo_orange_light),
            ApprovalRule("外出审批", R.drawable.ic_out_approval, "/admin/out_apply", listOf("admin", "manager", "supervisor", "general_manager", "tech_supervisor", "hr_supervisor"), android.R.color.holo_purple),
            ApprovalRule("加班审批", R.drawable.ic_overtime_approval, "/admin/overtime_approval", listOf("admin", "manager", "supervisor", "general_manager", "tech_supervisor", "hr_supervisor"), android.R.color.holo_red_light),
            ApprovalRule("报销审批", R.drawable.ic_expense_approval, "/admin/expense_manage", listOf("admin", "finance", "accountant", "general_manager", "supervisor", "manager", "tech_supervisor"), android.R.color.holo_green_light),
            ApprovalRule("采购审批", R.drawable.ic_purchase_approval, "/admin/purchase_manage", listOf("admin", "manager", "supervisor", "general_manager", "tech_supervisor", "purchaser", "storekeeper"), android.R.color.holo_blue_dark),
            ApprovalRule("费用审批", R.drawable.ic_cost_approval, "/admin/cost_approval", listOf("admin", "finance", "accountant", "general_manager", "supervisor", "tech_supervisor"), android.R.color.holo_green_dark)
        )
        for (rule in approvalRules) {
            if (rule.requiredRoles.any { roles.contains(it) }) {
                menus.add(WorkbenchAdapter.MenuItem(rule.name, rule.iconRes, rule.url, rule.iconTint))
            }
        }
        return menus
    }

    private fun openWebPage(url: String, title: String) {
        val token = sharedPref.getString("token", null)
        if (token.isNullOrEmpty()) {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            return
        }
        val separator = if (url.contains("?")) "&" else "?"
        val fullUrl = "${ApiService.BASE_URL}$url$separator" + "token=${token}"
        Log.e("===WorkbenchContent fullUrl", fullUrl)
        val intent = Intent(requireContext(), WebViewActivity::class.java)
        intent.putExtra("url", fullUrl)
        intent.putExtra("title", title)
        startActivity(intent)
    }

    private fun openListPage(item: WorkbenchAdapter.PersonalGridItem) {
        val title = when (item.name) {
            "报销记录" -> "我的报销记录"
            "请假记录" -> "我的请假记录"
            "调休记录" -> "我的调休记录"
            "外出记录" -> "我的外出记录"
            "采购记录" -> "我的采购申请"
            "加班记录" -> "我的加班记录"
            "费用申请记录" -> "我的费用申请"
            "工资条" -> "我的工资条"
            else -> item.name
        }
        openWebPage(item.url, title)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/**
 * WebView 容器，处理附件下载认证
 */
class WebViewActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var toolbar: Toolbar
    private lateinit var sharedPref: SharedPreferences
    private var currentPageUrl: String = ""

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 沉浸式状态栏
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true

        setContentView(R.layout.activity_webview)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        val title = intent.getStringExtra("title") ?: "详情"
        supportActionBar?.title = title

        webView = findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.builtInZoomControls = true
        webView.settings.displayZoomControls = false
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true

        // Cookie 配置（保持不变）
        CookieManager.getInstance().setAcceptCookie(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        }
        sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val cookie = sharedPref.getString("cookie", null)
        val baseUrl = ApiService.BASE_URL
        val domain = baseUrl.replace("http://", "").replace("https://", "").split(":").firstOrNull() ?: ""
        if (!cookie.isNullOrEmpty() && domain.isNotEmpty()) {
            CookieManager.getInstance().setCookie("http://$domain", cookie)
            CookieManager.getInstance().setCookie("https://$domain", cookie)
            CookieManager.getInstance().flush()
            Log.d("WebViewActivity", "Cookie set: $cookie")
        }

        // ========== 下载监听 - 为附件URL添加Token ==========
        webView.setDownloadListener { url, _, contentDisposition, mimeType, _ ->
            val token = sharedPref.getString("token", null)
            if (token.isNullOrEmpty()) {
                Toast.makeText(this, "登录已过期，请重新登录", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                return@setDownloadListener
            }

            // 处理相对路径：拼接当前页面的Base URL
            var downloadUrl = url
            if (!downloadUrl.startsWith("http")) {
                val base = currentPageUrl.ifEmpty { ApiService.BASE_URL }
                val baseUri = Uri.parse(base)
                downloadUrl = Uri.parse(baseUri.scheme + "://" + baseUri.host + ":" + baseUri.port + downloadUrl).toString()
            }

            // 添加token参数
            val separator = if (downloadUrl.contains("?")) "&" else "?"
            val finalUrl = "$downloadUrl$separator" + "token=${token}"
            Log.d("WebViewActivity", "下载附件（已加Token）: $finalUrl")
            downloadFile(finalUrl, contentDisposition, mimeType)
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                url?.let { currentPageUrl = it }
                injectExportInterceptor()
                addTokenToAttachmentLinks()
                cleanPageHeader()
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                // 对于页面内点击的链接，确保带上Token（防止某些链接遗漏）
                val url = request?.url.toString()
                val token = sharedPref.getString("token", null)
                if (!token.isNullOrEmpty() && url.startsWith(ApiService.BASE_URL) && !url.contains("token=")) {
                    val separator = if (url.contains("?")) "&" else "?"
                    val newUrl = "$url$separator" + "token=${token}"
                    view?.loadUrl(newUrl)
                    return true
                }
                return super.shouldOverrideUrlLoading(view, request)
            }
        }

        // 返回键后退
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    finish()
                }
            }
        })

        val url = intent.getStringExtra("url") ?: ""
        val fullUrl = if (url.startsWith("http")) url else "${ApiService.BASE_URL}$url"
        webView.loadUrl(fullUrl)
    }

    /**
     * 为页面内所有附件链接（包含"attachment"关键字的a标签）添加token参数
     */
    private fun addTokenToAttachmentLinks() {
        val token = sharedPref.getString("token", null) ?: return
        val js = """
            (function() {
                var links = document.querySelectorAll('a[href*="attachment"], a[href*="export"]');
                links.forEach(function(link) {
                    var href = link.getAttribute('href');
                    if (href && href.indexOf('token=') === -1) {
                        var separator = href.indexOf('?') === -1 ? '?' : '&';
                        link.setAttribute('href', href + separator + 'token=${token}');
                    }
                });
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    /**
     * 拦截表单提交（如导出Excel），为action添加token参数
     */
    private fun injectExportInterceptor() {
        val token = sharedPref.getString("token", null) ?: return
        val js = """
            (function() {
                var forms = document.querySelectorAll('form[action*="export"], form[action*="excel"]');
                forms.forEach(function(form) {
                    if (form.getAttribute('data-export-intercepted') === 'true') return;
                    form.setAttribute('data-export-intercepted', 'true');
                    var originalAction = form.action;
                    var separator = originalAction.indexOf('?') === -1 ? '?' : '&';
                    form.action = originalAction + separator + 'token=${token}';
                    form.addEventListener('submit', function(e) {
                        // 不需要额外处理，action已修改
                    });
                });
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    private fun cleanPageHeader() {
        val js = """
            (function() {
                var headers = document.querySelectorAll('.header, .mobile-header, .pc-header');
                headers.forEach(function(h) { h.remove(); });
                var contents = document.querySelectorAll('.content, .mobile-content');
                contents.forEach(function(c) {
                    c.style.marginTop = '0';
                    c.style.paddingTop = '0';
                });
                var containers = document.querySelectorAll('.container');
                containers.forEach(function(c) {
                    c.style.marginTop = '0';
                    c.style.paddingTop = '0';
                });
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    private fun downloadFile(url: String, contentDisposition: String, mimeType: String) {
        try {
            val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            var token = prefs.getString("token", null)
            var finalUrl = url

            // 补全 BASE_URL（如果是相对路径）
            if (!finalUrl.startsWith("http")) {
                finalUrl = ApiService.BASE_URL + finalUrl
            }

            // 添加 token 参数（避免重复）
            if (!token.isNullOrEmpty() && !finalUrl.contains("token=")) {
                val separator = if (finalUrl.contains("?")) "&" else "?"
                finalUrl = "$finalUrl$separator" + "token=$token"
            }

            // 生成正确的文件名
            var fileName = generateFileName(url, contentDisposition, mimeType)

            // 根据实际文件扩展名重新获取 MIME 类型（覆盖后端返回的不准确类型）
            val finalMimeType = getMimeTypeFromExtension(fileName) ?: mimeType

            val request = DownloadManager.Request(Uri.parse(finalUrl)).apply {
                setMimeType(finalMimeType)   // 使用修正后的 MIME 类型
                setTitle(fileName)
                setDescription("正在下载附件")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalFilesDir(this@WebViewActivity, Environment.DIRECTORY_DOWNLOADS, fileName)
                allowScanningByMediaScanner()
            }

            val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
            Toast.makeText(this, "开始下载：$fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("WebViewActivity", "下载失败", e)
            Toast.makeText(this, "下载失败：" + e.message, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 根据 Content-Disposition、MIME 类型、URL 路径生成合适的文件名（带正确扩展名）
     */
    private fun generateFileName(url: String, contentDisposition: String, mimeType: String): String {
        // 1. 尝试从 Content-Disposition 解析文件名（最准确）
        var fileName = parseFileNameFromContentDisposition(contentDisposition)
        if (!fileName.isNullOrEmpty()) {
            // 确保有扩展名（防止后端返回无扩展名的文件名）
            return ensureExtension(fileName, mimeType)
        }

        // 2. 从 URL 路径中提取文件名（例如 .../attachment/xxx? 中的 xxx 可能不是文件名，但可能有扩展名）
        val urlPath = URLUtil.guessFileName(url, null, null)
        if (urlPath.isNotEmpty() && urlPath != "1.bin" && urlPath.contains(".")) {
            return ensureExtension(urlPath, mimeType)
        }

        // 3. 根据 MIME 类型生成默认文件名（带正确扩展名）
        val extension = getExtensionFromMimeType(mimeType)
        return "attachment_${System.currentTimeMillis()}$extension"
    }

    /**
     * 解析 Content-Disposition 中的文件名（支持普通 filename 和 RFC 5987 filename*）
     * 支持中文，自动处理 URL 编码和 ISO-8859-1 乱码转换
     */
    private fun parseFileNameFromContentDisposition(contentDisposition: String?): String? {
        if (contentDisposition.isNullOrEmpty()) return null

        // 1. RFC 5987 格式：filename*=UTF-8''%E4%B8%AD%E6%96%87.xlsx
        val rfc5987Pattern = "filename\\*=(?:UTF-8|utf-8)''([^;]+)".toRegex(RegexOption.IGNORE_CASE)
        rfc5987Pattern.find(contentDisposition)?.let {
            val encoded = it.groupValues[1]
            return try {
                java.net.URLDecoder.decode(encoded, "UTF-8")
            } catch (e: Exception) {
                // 解码失败，返回原始编码字符串（后续 ensureExtension 会处理扩展名）
                encoded
            }
        }

        // 2. 普通 filename="xxx" 或 filename=xxx
        val pattern = "filename[^;=\\n]*=((['\"]).*?\\2|[^;\\n]*)".toRegex(RegexOption.IGNORE_CASE)
        val match = pattern.find(contentDisposition)
        var fileName = match?.groupValues?.get(1)?.trim('"', '\'') ?: return null
        if (fileName.isEmpty()) return null

        // 3. 处理可能存在的 URL 编码（部分服务器会对 filename 进行编码）
        if (fileName.contains("%")) {
            fileName = try {
                java.net.URLDecoder.decode(fileName, "UTF-8")
            } catch (e: Exception) {
                fileName
            }
        } else {
            // 4. 检测是否为 ISO-8859-1 乱码（例如 "ÖÐÎÄ" 本应为 "中文"）
            // 当字符串包含连续的高位字节特征且看起来像乱码时进行转换
            if (fileName.any { it.code in 128..255 } &&
                !fileName.matches(Regex("[\\u4e00-\\u9fa5]+"))) {
                fileName = try {
                    val bytes = fileName.toByteArray(Charsets.ISO_8859_1)
                    String(bytes, Charsets.UTF_8)
                } catch (e: Exception) {
                    fileName
                }
            }
        }
        return fileName
    }

    /**
     * 确保文件名有正确的扩展名，否则根据 MIME 类型补充
     */
    /**
     * 确保文件名有正确的扩展名，替换无效扩展名（如 .bin, .file, .tmp 等）
     */
    private fun ensureExtension(fileName: String, mimeType: String): String {
        val invalidExtensions = listOf(".bin", ".file", ".tmp", ".part")
        var name = fileName

        // 1. 去除末尾的无效扩展名（循环去除）
        var changed = true
        while (changed) {
            changed = false
            for (invalidExt in invalidExtensions) {
                if (name.endsWith(invalidExt, ignoreCase = true)) {
                    name = name.substring(0, name.length - invalidExt.length)
                    changed = true
                    break
                }
            }
        }

        val correctExt = getExtensionFromMimeType(mimeType)

        // 2. 如果已有正确的扩展名，直接返回
        if (correctExt.isNotEmpty() && name.endsWith(correctExt, ignoreCase = true)) {
            return name
        }

        // 3. 如果 MIME 类型无法识别（correctExt 为空），则尽量保留原扩展名
        if (correctExt.isEmpty()) {
            // 提取原有扩展名（如果有且不是无效扩展名）
            val lastDot = name.lastIndexOf('.')
            if (lastDot > 0 && lastDot < name.length - 1) {
                val originalExt = name.substring(lastDot)
                // 如果原扩展名不在无效列表中，保留它
                if (!invalidExtensions.contains(originalExt.lowercase())) {
                    return name
                }
            }
            // 无有效扩展名，回退到 .bin
            return "$name.bin"
        }

        // 4. 有正确扩展名：替换或追加
        val lastDot = name.lastIndexOf('.')
        if (lastDot > 0 && lastDot < name.length - 1) {
            // 替换现有扩展名
            return name.substring(0, lastDot) + correctExt
        } else {
            // 追加扩展名
            return name + correctExt
        }
    }

    /**
     * 根据 MIME 类型返回标准的文件扩展名（包含点号）
     */
    private fun getExtensionFromMimeType(mimeType: String): String {
        return when {
            mimeType.contains("spreadsheetml") -> ".xlsx"
            mimeType == "application/vnd.ms-excel" -> ".xls"
            mimeType == "application/pdf" -> ".pdf"
            mimeType == "application/msword" -> ".doc"
            mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> ".docx"
            mimeType.startsWith("image/") -> ".${mimeType.split("/").last().replace("jpeg", "jpg")}" // 统一 .jpg
            mimeType == "application/octet-stream" -> ""  // 未知类型，不强制替换
            mimeType.isBlank() -> ""
            else -> ""  // 其他未识别的 MIME 也保留原扩展名
        }
    }

    /**
     * 根据文件扩展名获取标准的 MIME 类型
     * @return 对应的 MIME 类型，如果无法识别则返回 null
     */
    private fun getMimeTypeFromExtension(fileName: String): String? {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "bmp" -> "image/bmp"
            "webp" -> "image/webp"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "xls" -> "application/vnd.ms-excel"
            "pdf" -> "application/pdf"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "ppt" -> "application/vnd.ms-powerpoint"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "txt" -> "text/plain"
            "csv" -> "text/csv"
            "zip" -> "application/zip"
            "rar" -> "application/x-rar-compressed"
            else -> null
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        webView.destroy()
    }
}

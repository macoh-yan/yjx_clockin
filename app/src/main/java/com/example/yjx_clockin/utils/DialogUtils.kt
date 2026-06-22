package com.example.yjx_clockin.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.example.yjx_clockin.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object DialogUtils {

    fun showCustomDialog(
        context: Context,
        title: String? = null,
        message: String,
        iconRes: Int? = null,
        positiveText: String = "确定",
        negativeText: String? = null,
        onPositive: (() -> Unit)? = null,
        onNegative: (() -> Unit)? = null
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_custom_layout, null)
        val ivIcon = view.findViewById<ImageView>(R.id.iv_icon)
        val tvTitle = view.findViewById<TextView>(R.id.tv_title)
        val tvMessage = view.findViewById<TextView>(R.id.tv_message)
        val btnPositive = view.findViewById<Button>(R.id.btn_positive)
        val btnNegative = view.findViewById<Button>(R.id.btn_negative)

        // 设置图标
        if (iconRes != null) {
            ivIcon.setImageResource(iconRes)
            ivIcon.visibility = View.VISIBLE
        } else {
            ivIcon.visibility = View.GONE
        }

        // 设置标题
        if (!title.isNullOrEmpty()) {
            tvTitle.text = title
            tvTitle.visibility = View.VISIBLE
        } else {
            tvTitle.visibility = View.GONE
        }

        // 设置消息内容
        tvMessage.text = message

        // 创建对话框（必须在设置按钮监听器之前）
        val dialog = MaterialAlertDialogBuilder(context)
            .setView(view)
            .setCancelable(false)
            .create()

        // 设置对话框窗口背景透明，让自定义布局的圆角完全显示
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // 确定按钮
        btnPositive.text = positiveText
        btnPositive.setOnClickListener {
            onPositive?.invoke()
            dialog.dismiss()
        }

        // 取消/否定按钮
        if (negativeText != null) {
            btnNegative.text = negativeText
            btnNegative.visibility = View.VISIBLE
            btnNegative.setOnClickListener {
                onNegative?.invoke()
                dialog.dismiss()
            }
        } else {
            btnNegative.visibility = View.GONE
        }

        dialog.show()
    }
}
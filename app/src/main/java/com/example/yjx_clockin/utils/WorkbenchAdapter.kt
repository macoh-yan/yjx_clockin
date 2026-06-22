package com.example.yjx_clockin.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.yjx_clockin.R

class WorkbenchAdapter(
    private val onPersonalItemClick: (PersonalGridItem) -> Unit,
    private val onMenuItemClick: (MenuItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_WELCOME = 0
        private const val TYPE_PERSONAL_GRID = 1
        private const val TYPE_APPLY_HEADER = 2
        private const val TYPE_APPLY_GRID = 3
        private const val TYPE_APPROVAL_HEADER = 4
        private const val TYPE_APPROVAL_GRID = 5
    }

    data class PersonalGridItem(val name: String, val iconRes: Int, val url: String)
    data class MenuItem(val name: String, val iconRes: Int, val url: String,val iconTint: Int? = null)

    private var userName: String = ""
    private var personalItems: List<PersonalGridItem> = emptyList()
    private var applyMenus: List<MenuItem> = emptyList()
    private var approvalMenus: List<MenuItem> = emptyList()

    // 用于记录实际显示的区块列表（用于 getItemCount 和 getItemViewType）
    private data class Section(val type: Int, val data: Any? = null)

    private var sections: List<Section> = emptyList()

    fun setData(userName: String, personalItems: List<PersonalGridItem>, applyMenus: List<MenuItem>, approvalMenus: List<MenuItem>) {
        this.userName = userName
        this.personalItems = personalItems
        this.applyMenus = applyMenus
        this.approvalMenus = approvalMenus
        buildSections()
        notifyDataSetChanged()
    }

    private fun buildSections() {
        val list = mutableListOf<Section>()
        list.add(Section(TYPE_WELCOME))
        list.add(Section(TYPE_PERSONAL_GRID))
        list.add(Section(TYPE_APPLY_HEADER))
        list.add(Section(TYPE_APPLY_GRID))
        if (approvalMenus.isNotEmpty()) {
            list.add(Section(TYPE_APPROVAL_HEADER))
            list.add(Section(TYPE_APPROVAL_GRID))
        }
        sections = list
    }

    override fun getItemViewType(position: Int): Int {
        return sections[position].type
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_WELCOME -> WelcomeViewHolder(
                inflater.inflate(R.layout.item_welcome_card, parent, false)
            )
            TYPE_PERSONAL_GRID -> PersonalGridViewHolder(
                inflater.inflate(R.layout.item_personal_grid, parent, false),
                onPersonalItemClick
            )
            TYPE_APPLY_HEADER -> SectionHeaderViewHolder(
                inflater.inflate(R.layout.item_section_header, parent, false),
                "申请管理"
            )
            TYPE_APPLY_GRID -> GridMenuViewHolder(
                inflater.inflate(R.layout.item_grid_menu, parent, false),
                onMenuItemClick
            )
            TYPE_APPROVAL_HEADER -> SectionHeaderViewHolder(
                inflater.inflate(R.layout.item_section_header, parent, false),
                "审批管理"
            )
            else -> GridMenuViewHolder(
                inflater.inflate(R.layout.item_grid_menu, parent, false),
                onMenuItemClick
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is WelcomeViewHolder -> holder.bind(userName)
            is PersonalGridViewHolder -> holder.bind(personalItems)
            is SectionHeaderViewHolder -> holder.bind()
            is GridMenuViewHolder -> {
                val items = when (getItemViewType(position)) {
                    TYPE_APPLY_GRID -> applyMenus
                    TYPE_APPROVAL_GRID -> approvalMenus
                    else -> emptyList()
                }
                holder.bind(items)
            }
        }
    }

    override fun getItemCount(): Int = sections.size

    // ========== ViewHolder 实现 ==========

    class WelcomeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvUserName = itemView.findViewById<TextView>(R.id.tvUserName)
        fun bind(name: String) {
            tvUserName.text = "你好，$name"
        }
    }

    class PersonalGridViewHolder(
        itemView: View,
        private val onItemClick: (PersonalGridItem) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val gridRecycler = itemView.findViewById<RecyclerView>(R.id.personalGridRecycler)
        fun bind(items: List<PersonalGridItem>) {
            gridRecycler.layoutManager = GridLayoutManager(itemView.context, 3)
            gridRecycler.adapter = PersonalGridInnerAdapter(items, onItemClick)
        }
    }

    class SectionHeaderViewHolder(itemView: View, private val title: String) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle = itemView.findViewById<TextView>(R.id.sectionTitle)
        fun bind() {
            tvTitle.text = title
        }
    }

    class GridMenuViewHolder(
        itemView: View,
        private val onItemClick: (MenuItem) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val gridRecycler = itemView.findViewById<RecyclerView>(R.id.gridRecycler)
        fun bind(items: List<MenuItem>) {
            gridRecycler.layoutManager = GridLayoutManager(itemView.context, 3)
            gridRecycler.adapter = GridMenuInnerAdapter(items, onItemClick)
        }
    }

// 个人九宫格内部适配器（同样逻辑）
    class PersonalGridInnerAdapter(
        private val items: List<PersonalGridItem>,
        private val onItemClick: (PersonalGridItem) -> Unit
    ) : RecyclerView.Adapter<PersonalGridInnerAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_personal_grid_button, parent, false)
            return ViewHolder(view, onItemClick)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        class ViewHolder(itemView: View, private val onItemClick: (PersonalGridItem) -> Unit) :
            RecyclerView.ViewHolder(itemView) {
            private val iconView = itemView.findViewById<ImageView>(R.id.gridIcon)
            private val nameView = itemView.findViewById<TextView>(R.id.gridName)

            fun bind(item: PersonalGridItem) {
                nameView.text = item.name
                iconView.setImageResource(item.iconRes)
                // 个人菜单也给多彩背景（按类型分配）
                val bgRes = when (item.name) {
                    "请假记录" -> R.drawable.bg_circle_blue
                    "调休记录" -> R.drawable.bg_circle_orange
                    "报销记录" -> R.drawable.bg_circle_green
                    "外出记录" -> R.drawable.bg_circle_purple
                    else -> R.drawable.bg_circle_blue
                }
                iconView.setBackgroundResource(bgRes)
                itemView.setOnClickListener { onItemClick(item) }
            }
        }
    }

// 通用网格菜单内部适配器
    class GridMenuInnerAdapter(
        private val items: List<MenuItem>,
        private val onItemClick: (MenuItem) -> Unit
    ) : RecyclerView.Adapter<GridMenuInnerAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_grid_menu_button, parent, false)
            return ViewHolder(view, onItemClick)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        class ViewHolder(itemView: View, private val onItemClick: (MenuItem) -> Unit) :
            RecyclerView.ViewHolder(itemView) {
            private val iconView = itemView.findViewById<ImageView>(R.id.menuIcon)
            private val nameView = itemView.findViewById<TextView>(R.id.menuName)

            fun bind(item: MenuItem) {
                nameView.text = item.name
                iconView.setImageResource(item.iconRes)
                // 多彩背景+图标色
                when (item.iconTint) {
                    android.R.color.holo_blue_light -> {
                        iconView.setBackgroundResource(R.drawable.bg_circle_blue)
                        iconView.setColorFilter(itemView.context.getColor(android.R.color.holo_blue_light))
                    }
                    android.R.color.holo_orange_light -> {
                        iconView.setBackgroundResource(R.drawable.bg_circle_orange)
                        iconView.setColorFilter(itemView.context.getColor(android.R.color.holo_orange_light))
                    }
                    android.R.color.holo_green_light, android.R.color.holo_green_dark -> {
                        iconView.setBackgroundResource(R.drawable.bg_circle_green)
                        iconView.setColorFilter(itemView.context.getColor(android.R.color.holo_green_light))
                    }
                    android.R.color.holo_purple -> {
                        iconView.setBackgroundResource(R.drawable.bg_circle_purple)
                        iconView.setColorFilter(itemView.context.getColor(android.R.color.holo_purple))
                    }
                    android.R.color.holo_red_light -> {
                        iconView.setBackgroundResource(R.drawable.bg_circle_red)
                        iconView.setColorFilter(itemView.context.getColor(android.R.color.holo_red_light))
                    }
                    else -> {
                        iconView.setBackgroundResource(R.drawable.bg_circle_blue)
                        iconView.colorFilter = null
                    }
                }
                itemView.setOnClickListener { onItemClick(item) }
            }
        }
    }

}
package me.zhanghai.android.files.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import me.zhanghai.android.files.R
import me.zhanghai.android.files.app.AppActivity
import me.zhanghai.android.files.settings.SelectionActions.Placement
import me.zhanghai.android.files.util.createIntent

/**
 * Two tabs, "Toolbar" and "⋮ menu". Each tab lists the actions placed there and lets the user reorder
 * them (drag), move one to the other tab, or hide it. Hidden actions are listed at the bottom of the
 * "⋮ menu" tab and can be brought back from there.
 */
class SelectionActionsSettingsActivity : AppActivity() {
    private lateinit var adapter: Adapter
    private lateinit var tabs: TabLayout
    private lateinit var hint: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.selection_actions_settings)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.settings_selection_actions_title)

        tabs = findViewById(R.id.tabs)
        hint = findViewById(R.id.hint)
        tabs.addTab(tabs.newTab().setText(R.string.settings_selection_actions_toolbar))
        tabs.addTab(tabs.newTab().setText(R.string.settings_selection_actions_menu))

        val list = findViewById<RecyclerView>(R.id.list)
        list.layoutManager = LinearLayoutManager(this)
        adapter = Adapter()
        list.adapter = adapter
        val touch = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun getMovementFlags(rv: RecyclerView, holder: RecyclerView.ViewHolder): Int =
                if (adapter.isDraggable(holder.bindingAdapterPosition)) {
                    super.getMovementFlags(rv, holder)
                } else {
                    0
                }

            override fun onMove(
                rv: RecyclerView,
                from: RecyclerView.ViewHolder,
                to: RecyclerView.ViewHolder
            ): Boolean {
                val toPosition = to.bindingAdapterPosition
                if (!adapter.isDraggable(toPosition)) return false
                adapter.move(from.bindingAdapterPosition, toPosition)
                return true
            }

            override fun onSwiped(holder: RecyclerView.ViewHolder, direction: Int) = Unit

            override fun clearView(rv: RecyclerView, holder: RecyclerView.ViewHolder) {
                super.clearView(rv, holder)
                adapter.save()
            }
        })
        touch.attachToRecyclerView(list)
        adapter.itemTouchHelper = touch

        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) = showTab(tab.position)
            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
        showTab(0)
    }

    private fun showTab(position: Int) {
        val menuTab = position == 1
        hint.setText(
            if (menuTab) R.string.settings_selection_actions_hint_menu
            else R.string.settings_selection_actions_hint_toolbar
        )
        adapter.showTab(if (menuTab) Placement.MENU else Placement.TOOLBAR)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, MENU_RESET, 0, R.string.settings_selection_actions_reset)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        MENU_RESET -> {
            adapter.reset()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private sealed class Row {
        object Header : Row()
        class Item(val id: String) : Row()
    }

    private inner class Adapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val prefs = PreferenceManager.getDefaultSharedPreferences(this@SelectionActionsSettingsActivity)

        /** Every action with its placement; the order here is the display order inside each placement. */
        private var entries = SelectionActions.load(prefs).toMutableList()
        private var tab = Placement.TOOLBAR
        private var rows = listOf<Row>()
        var itemTouchHelper: ItemTouchHelper? = null

        init {
            rebuild()
        }

        fun showTab(placement: Placement) {
            tab = placement
            rebuild()
            notifyDataSetChanged()
        }

        private fun rebuild() {
            val result = mutableListOf<Row>()
            entries.filter { it.placement == tab }.forEach { result.add(Row.Item(it.id)) }
            if (tab == Placement.MENU) {
                val hidden = entries.filter { it.placement == Placement.HIDDEN }
                if (hidden.isNotEmpty()) {
                    result.add(Row.Header)
                    hidden.forEach { result.add(Row.Item(it.id)) }
                }
            }
            rows = result
        }

        fun isDraggable(position: Int): Boolean {
            val row = rows.getOrNull(position) as? Row.Item ?: return false
            return entries.first { it.id == row.id }.placement == tab
        }

        override fun getItemCount() = rows.size

        override fun getItemViewType(position: Int) = if (rows[position] is Row.Header) 0 else 1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return if (viewType == 0) {
                object : RecyclerView.ViewHolder(
                    inflater.inflate(R.layout.selection_actions_settings_header, parent, false)
                ) {}
            } else {
                ItemHolder(inflater.inflate(R.layout.selection_actions_settings_item, parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val row = rows[position]
            if (row is Row.Header) {
                (holder.itemView as TextView).setText(R.string.settings_selection_actions_hidden)
                return
            }
            holder as ItemHolder
            val id = (row as Row.Item).id
            val entry = entries.first { it.id == id }
            val hidden = entry.placement == Placement.HIDDEN
            holder.title.setText(SelectionActions.info(id)?.titleRes ?: R.string.share)
            holder.handle.visibility = if (hidden) View.INVISIBLE else View.VISIBLE
            holder.hide.visibility = if (hidden) View.GONE else View.VISIBLE
            holder.move.contentDescription = getString(
                if (hidden || tab == Placement.TOOLBAR) R.string.settings_selection_actions_move_to_menu
                else R.string.settings_selection_actions_move_to_toolbar
            )
            holder.handle.setOnTouchListener { _, event ->
                if (!hidden && event.actionMasked == android.view.MotionEvent.ACTION_DOWN) {
                    itemTouchHelper?.startDrag(holder)
                }
                true
            }
            holder.move.setOnClickListener {
                // From the toolbar tab: to the menu. From the menu tab: to the toolbar. A hidden action
                // comes back into the menu.
                val target = if (!hidden && tab == Placement.MENU) Placement.TOOLBAR else Placement.MENU
                place(id, target)
            }
            holder.hide.setOnClickListener { place(id, Placement.HIDDEN) }
        }

        /** Puts the action at the end of the target placement. */
        private fun place(id: String, placement: Placement) {
            val index = entries.indexOfFirst { it.id == id }
            if (index < 0) return
            entries.removeAt(index)
            entries.add(SelectionActions.Entry(id, placement))
            save()
            rebuild()
            notifyDataSetChanged()
        }

        /** Reorders within the current tab; positions are row positions of two draggable rows. */
        fun move(from: Int, to: Int) {
            if (from == to || from < 0 || to < 0) return
            val fromId = (rows[from] as? Row.Item)?.id ?: return
            val toId = (rows[to] as? Row.Item)?.id ?: return
            val fromIndex = entries.indexOfFirst { it.id == fromId }
            val toIndex = entries.indexOfFirst { it.id == toId }
            entries.add(toIndex, entries.removeAt(fromIndex))
            val mutable = rows.toMutableList()
            mutable.add(to, mutable.removeAt(from))
            rows = mutable
            notifyItemMoved(from, to)
        }

        fun save() = SelectionActions.save(prefs, entries)

        fun reset() {
            SelectionActions.reset(prefs)
            entries = SelectionActions.defaults().toMutableList()
            rebuild()
            notifyDataSetChanged()
        }
    }

    private class ItemHolder(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.title)
        val handle: View = v.findViewById(R.id.handle)
        val move: ImageButton = v.findViewById(R.id.move)
        val hide: ImageButton = v.findViewById(R.id.hide)
    }

    companion object {
        private const val MENU_RESET = 1
    }
}

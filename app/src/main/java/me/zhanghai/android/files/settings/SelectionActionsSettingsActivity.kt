package me.zhanghai.android.files.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.zhanghai.android.files.R
import me.zhanghai.android.files.app.AppActivity
import me.zhanghai.android.files.util.createIntent

/** Lets the user choose where each file-selection action goes (toolbar / ⋮ menu / hidden) and its order. */
class SelectionActionsSettingsActivity : AppActivity() {
    private lateinit var adapter: Adapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.editor_toolbar_settings)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.settings_selection_actions_title)

        val list = findViewById<RecyclerView>(R.id.list)
        list.layoutManager = LinearLayoutManager(this)
        adapter = Adapter()
        list.adapter = adapter
        val touch = ItemTouchHelper(object :
            ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(
                rv: RecyclerView,
                from: RecyclerView.ViewHolder,
                to: RecyclerView.ViewHolder
            ): Boolean {
                adapter.move(from.bindingAdapterPosition, to.bindingAdapterPosition)
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

    private inner class Adapter : RecyclerView.Adapter<Holder>() {
        private val prefs = PreferenceManager.getDefaultSharedPreferences(this@SelectionActionsSettingsActivity)
        private val items = SelectionActions.load(prefs).toMutableList()
        private val placementLabels = listOf(
            getString(R.string.settings_selection_actions_toolbar),
            getString(R.string.settings_selection_actions_menu),
            getString(R.string.settings_selection_actions_hidden)
        )
        var itemTouchHelper: ItemTouchHelper? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.selection_actions_settings_item, parent, false)
        ).also {
            it.placement.adapter = ArrayAdapter(
                parent.context, android.R.layout.simple_spinner_dropdown_item, placementLabels
            )
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val entry = items[position]
            holder.title.setText(SelectionActions.info(entry.id)?.titleRes ?: R.string.share)
            holder.handle.setOnTouchListener { _, event ->
                if (event.actionMasked == android.view.MotionEvent.ACTION_DOWN) {
                    itemTouchHelper?.startDrag(holder)
                }
                true
            }
            holder.placement.onItemSelectedListener = null
            holder.placement.setSelection(entry.placement.ordinal, false)
            holder.placement.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    val current = holder.bindingAdapterPosition
                    if (current == RecyclerView.NO_POSITION) return
                    val placement = SelectionActions.Placement.values()[pos]
                    if (items[current].placement != placement) {
                        items[current] = SelectionActions.Entry(items[current].id, placement)
                        save()
                    }
                }

                override fun onNothingSelected(p: AdapterView<*>?) = Unit
            }
        }

        fun move(from: Int, to: Int) {
            if (from == to || from < 0 || to < 0) return
            items.add(to, items.removeAt(from))
            notifyItemMoved(from, to)
        }

        fun save() = SelectionActions.save(prefs, items)

        fun reset() {
            SelectionActions.reset(prefs)
            items.clear()
            items.addAll(SelectionActions.defaults())
            notifyDataSetChanged()
        }
    }

    private class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.title)
        val placement: Spinner = v.findViewById(R.id.placement)
        val handle: View = v.findViewById(R.id.handle)
    }

    companion object {
        private const val MENU_RESET = 1
    }
}

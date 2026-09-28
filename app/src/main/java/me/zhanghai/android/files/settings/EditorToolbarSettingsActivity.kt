package me.zhanghai.android.files.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.zhanghai.android.files.R
import me.zhanghai.android.files.app.AppActivity
import me.zhanghai.android.files.util.createIntent

class EditorToolbarSettingsActivity : AppActivity() {
    private lateinit var adapter: Adapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.editor_toolbar_settings)
        val plainText = intent.getBooleanExtra(EXTRA_PLAIN_TEXT, false)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(if (plainText) R.string.settings_text_editor_toolbar_title else R.string.settings_code_editor_toolbar_title)

        val list = findViewById<RecyclerView>(R.id.list)
        list.layoutManager = LinearLayoutManager(this)
        adapter = Adapter(this, plainText)
        list.adapter = adapter
        val touch = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(rv: RecyclerView, from: RecyclerView.ViewHolder, to: RecyclerView.ViewHolder): Boolean {
                adapter.move(from.bindingAdapterPosition, to.bindingAdapterPosition); return true
            }
            override fun onSwiped(holder: RecyclerView.ViewHolder, direction: Int) = Unit
            override fun clearView(rv: RecyclerView, holder: RecyclerView.ViewHolder) { super.clearView(rv, holder); adapter.save() }
        })
        touch.attachToRecyclerView(list)
        adapter.itemTouchHelper = touch
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    companion object {
        const val EXTRA_PLAIN_TEXT = "plain_text"
        fun createIntent(plainText: Boolean) = EditorToolbarSettingsActivity::class.createIntent().apply { putExtra(EXTRA_PLAIN_TEXT, plainText) }
    }

    private class Adapter(private val activity: EditorToolbarSettingsActivity, private val plainText: Boolean) : RecyclerView.Adapter<Holder>() {
        private val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(activity)
        private val prefix = if (plainText) "key_text_editor_" else "key_code_editor_"
        private val defaults = listOf("undo","redo","cut","copy","paste","select_all","indent","outdent","comment","search","find_replace")
        private val labels = mapOf(
            "undo" to R.string.text_editor_undo, "redo" to R.string.text_editor_redo, "cut" to R.string.cut,
            "copy" to R.string.copy, "paste" to R.string.paste, "select_all" to R.string.select_all,
            "indent" to R.string.text_editor_indent, "outdent" to R.string.text_editor_outdent,
            "comment" to R.string.text_editor_comment, "search" to R.string.text_editor_find,
            "find_replace" to R.string.text_editor_search
        )
        private val items = getOrder().toMutableList()
        var itemTouchHelper: ItemTouchHelper? = null
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder = Holder(LayoutInflater.from(parent.context).inflate(R.layout.editor_toolbar_settings_item, parent, false))
        override fun getItemCount() = items.size
        override fun onBindViewHolder(holder: Holder, position: Int) {
            val id = items[position]
            holder.title.setText(labels[id] ?: R.string.text_editor_search)
            holder.enabled.isChecked = !id.startsWith("!")
            holder.handle.setOnTouchListener { _, event ->
                if (event.actionMasked == android.view.MotionEvent.ACTION_DOWN) itemTouchHelper?.startDrag(holder)
                true
            }
            holder.enabled.setOnCheckedChangeListener(null)
            holder.enabled.setOnCheckedChangeListener { _: CompoundButton, checked: Boolean ->
                val current = holder.bindingAdapterPosition
                if (current != RecyclerView.NO_POSITION) {
                    val currentId = items[current].removePrefix("!")
                    items[current] = if (checked) currentId else "!$currentId"
                    save()
                }
            }
        }
        fun move(from: Int, to: Int) { if (from == to) return; val x=items.removeAt(from); items.add(to,x); notifyItemMoved(from,to) }
        fun save() { prefs.edit().putString(prefix + "toolbar_actions", items.joinToString(",") { it.removePrefix("!") }).apply(); prefs.edit().putString(prefix + "toolbar_enabled", items.filter { !it.startsWith("!") }.joinToString(",")).apply() }
        private fun getOrder(): List<String> {
            val order = prefs.getString(prefix + "toolbar_actions", null)?.split(',')?.filter { it.isNotBlank() } ?: defaults
            val storedIds = prefs.getString(prefix + "toolbar_actions", null)?.split(',')?.filter { it.isNotBlank() }?.map { it.removePrefix("!") }
            // Actions added after the user last saved the toolbar layout are enabled by default.
            val newIds = defaults.filter { storedIds != null && it !in storedIds }
            val enabled = prefs.getString(prefix + "toolbar_enabled", null)?.split(',')?.filter { it.isNotBlank() }?.toSet()?.plus(newIds) ?: defaults.toSet()
            val normalized = order.map { it.removePrefix("!") }.filter { it in defaults } + defaults.filter { it !in order.map { x -> x.removePrefix("!") } }
            return normalized.map { if (it in enabled) it else "!$it" }
        }
    }
    private class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.title)
        val enabled: android.widget.Switch = v.findViewById(R.id.enabled)
        val handle: View = v.findViewById(R.id.handle)
    }
}

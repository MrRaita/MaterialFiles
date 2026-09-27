package me.zhanghai.android.files.viewer.pdf

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.chrisbanes.photoview.PhotoView
import java8.nio.file.Path
import me.zhanghai.android.files.R
import me.zhanghai.android.files.databinding.PdfViewerFragmentBinding
import me.zhanghai.android.files.file.fileProviderUri
import me.zhanghai.android.files.util.extraPath
import me.zhanghai.android.files.app.AppActivity

class PdfViewerActivity : AppActivity() {
    private lateinit var binding: PdfViewerFragmentBinding
    private lateinit var path: Path
    private lateinit var descriptor: ParcelFileDescriptor
    private lateinit var renderer: android.graphics.pdf.PdfRenderer
    private lateinit var adapter: PdfAdapter
    private var night = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = PdfViewerFragmentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        path = intent.extraPath ?: run { finish(); return }
        descriptor = contentResolver.openFileDescriptor(path.fileProviderUri, "r") ?: run { finish(); return }
        renderer = android.graphics.pdf.PdfRenderer(descriptor)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = path.fileName.toString()
        supportActionBar?.subtitle = getString(R.string.pdf_viewer_page_count, renderer.pageCount)
        adapter = PdfAdapter(renderer, night)
        binding.pages.layoutManager = LinearLayoutManager(this)
        binding.pages.adapter = adapter
        val saved = getPreferences(0).getInt("page_${path}", 0).coerceIn(0, (renderer.pageCount - 1).coerceAtLeast(0))
        binding.pages.scrollToPosition(saved)
        binding.pages.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val pos = (binding.pages.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                    if (pos >= 0) getPreferences(0).edit().putInt("page_${path}", pos).apply()
                }
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean { menuInflater.inflate(R.menu.pdf_viewer, menu); return true }
    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> { finish(); true }
        R.id.action_pdf_goto -> { showGoTo(); true }
        R.id.action_pdf_night -> { night = !night; item.isChecked = night; adapter.night = night; adapter.notifyDataSetChanged(); true }
        else -> super.onOptionsItemSelected(item)
    }

    private fun showGoTo() {
        val input = EditText(this).apply { inputType = android.text.InputType.TYPE_CLASS_NUMBER; hint = getString(R.string.pdf_viewer_page_number) }
        AlertDialog.Builder(this).setTitle(R.string.pdf_viewer_go_to_page).setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val page = input.text.toString().toIntOrNull()?.minus(1) ?: return@setPositiveButton
                binding.pages.scrollToPosition(page.coerceIn(0, (renderer.pageCount - 1).coerceAtLeast(0)))
            }.show()
    }

    override fun onDestroy() {
        if (::renderer.isInitialized) renderer.close()
        if (::descriptor.isInitialized) descriptor.close()
        super.onDestroy()
    }

    private class PdfAdapter(private val renderer: android.graphics.pdf.PdfRenderer, var night: Boolean) : RecyclerView.Adapter<PdfAdapter.Holder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder = Holder(LayoutInflater.from(parent.context).inflate(R.layout.pdf_viewer_item, parent, false) as ViewGroup)
        override fun getItemCount(): Int = renderer.pageCount
        override fun onBindViewHolder(holder: Holder, position: Int) {
            val page = renderer.openPage(position)
            val width = parentWidth(holder.itemView as ViewGroup).coerceAtLeast(600)
            val scale = width.toFloat() / page.width.coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(width, (page.height * scale).toInt().coerceIn(1, 8192), Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            holder.page.setImageBitmap(bitmap)
            holder.pageNumber.text = (position + 1).toString()
            holder.page.setColorFilter(if (night) android.graphics.ColorMatrixColorFilter(floatArrayOf(-1f,0f,0f,0f,255f, 0f,-1f,0f,0f,255f, 0f,0f,-1f,0f,255f, 0f,0f,0f,1f,0f)) else null)
        }
        private fun parentWidth(v: ViewGroup): Int = v.width.takeIf { it > 0 } ?: 1080
        class Holder(v: ViewGroup) : RecyclerView.ViewHolder(v) { val page: PhotoView = v.findViewById(R.id.page); val pageNumber = v.findViewById<android.widget.TextView>(R.id.pageNumber) }
    }
}

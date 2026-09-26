package me.zhanghai.android.files.viewer.text

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import io.github.rosemoe.sora.widget.CodeEditor

/** A subtle 88-column page-width guide for plain-text documents. */
class TextEditorPageGuideView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private var editor: CodeEditor? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = resources.displayMetrics.density
        alpha = 72
    }

    fun attachTo(editor: CodeEditor) {
        this.editor = editor
        paint.color = resolveControlColor()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val editor = editor ?: return
        val charWidth = editor.textPaint.measureText("M")
        val x = editor.paddingLeft + charWidth * 88f - editor.scrollX
        if (x > 0f && x < width) {
            canvas.drawLine(x, 0f, x, height.toFloat(), paint)
        }
    }

    private fun resolveControlColor(): Int {
        val value = android.util.TypedValue()
        context.theme.resolveAttribute(android.R.attr.textColorSecondary, value, true)
        return if (value.resourceId != 0) resources.getColor(value.resourceId, context.theme) else value.data
    }
}

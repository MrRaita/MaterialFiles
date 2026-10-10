package me.zhanghai.android.files.settings

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import me.zhanghai.android.files.util.createIntent

class SelectionActionsSettingsPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : Preference(context, attrs) {
    init {
        isPersistent = false
    }

    override fun onClick() {
        context.startActivity(SelectionActionsSettingsActivity::class.createIntent())
    }
}

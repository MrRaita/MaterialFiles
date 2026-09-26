package me.zhanghai.android.files.viewer.text

import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import org.eclipse.tm4e.core.registry.IThemeSource
import java.io.ByteArrayInputStream
import org.json.JSONArray
import org.json.JSONObject

/** Small, local TextMate theme factory so editor colors can be changed without shipping
 * a different grammar/theme package for every user customization. */
object EditorThemeFactory {
    data class Palette(
        val background: String,
        val foreground: String,
        val comment: String,
        val keyword: String,
        val string: String,
        val number: String,
        val type: String,
        val function: String,
        val variable: String,
        val constant: String,
        val operator: String,
        val tag: String,
        val attribute: String,
        val punctuation: String
    )

    private val presets = mapOf(
        "quiet_light" to Palette("#FFFFFF", "#333333", "#6A737D", "#AF00DB", "#A31515", "#098658", "#795E26", "#795E26", "#001080", "#0070C1", "#000000", "#800000", "#E50000", "#333333"),
        "github_light" to Palette("#FFFFFF", "#24292F", "#6E7781", "#CF222E", "#0A3069", "#0550AE", "#8250DF", "#8250DF", "#953800", "#0550AE", "#24292F", "#116329", "#953800", "#24292F"),
        "github_dark" to Palette("#0D1117", "#E6EDF3", "#8B949E", "#FF7B72", "#A5D6FF", "#79C0FF", "#D2A8FF", "#D2A8FF", "#FFA657", "#79C0FF", "#E6EDF3", "#7EE787", "#FFA657", "#E6EDF3"),
        "dracula" to Palette("#282A36", "#F8F8F2", "#6272A4", "#FF79C6", "#F1FA8C", "#BD93F9", "#8BE9FD", "#50FA7B", "#FF79C6", "#BD93F9", "#FF79C6", "#8BE9FD", "#50FA7B", "#F8F8F2"),
        "one_dark" to Palette("#282C34", "#ABB2BF", "#5C6370", "#C678DD", "#98C379", "#D19A66", "#E5C07B", "#61AFEF", "#E06C75", "#56B6C2", "#56B6C2", "#E06C75", "#E06C75", "#ABB2BF"),
        "nord" to Palette("#2E3440", "#D8DEE9", "#616E88", "#81A1C1", "#A3BE8C", "#B48EAD", "#8FBCBB", "#88C0D0", "#D08770", "#5E81AC", "#81A1C1", "#88C0D0", "#8FBCBB", "#D8DEE9"),
        "tokyo_night" to Palette("#1A1B26", "#A9B1D6", "#565F89", "#BB9AF7", "#9ECE6A", "#FF9E64", "#7DCFFF", "#7AA2F7", "#F7768E", "#2AC3DE", "#89DDFF", "#7DCFFF", "#73DACA", "#A9B1D6"),
        "catppuccin_mocha" to Palette("#1E1E2E", "#CDD6F4", "#6C7086", "#CBA6F7", "#A6E3A1", "#FAB387", "#89DCEB", "#89B4FA", "#F38BA8", "#F9E2AF", "#89DCEB", "#74C7EC", "#F5C2E7", "#CDD6F4"),
        "solarized_dark" to Palette("#002B36", "#839496", "#586E75", "#859900", "#2AA198", "#D33682", "#B58900", "#268BD2", "#CB4B16", "#B58900", "#839496", "#268BD2", "#B58900", "#93A1A1"),
        "darcula" to Palette("#242424", "#CCCCCC", "#707070", "#CC8242", "#6A8759", "#7A9EC2", "#7A9EC2", "#FFC66D", "#9876AA", "#CC8242", "#CCCCCC", "#E8BF6A", "#BABABA", "#CCCCCC")
    )

    fun palette(name: String): Palette = presets[name] ?: presets.getValue("github_dark")


    fun paletteWithOverrides(name: String, overrides: Map<String, String>): Palette {
        val base = palette(name)
        fun v(key: String, fallback: String) = overrides[key]?.takeIf { it.isNotBlank() } ?: fallback
        return base.copy(
            background = v("key_editor_color_background", base.background),
            foreground = v("key_editor_color_text", base.foreground),
            comment = v("key_editor_color_comment", base.comment),
            keyword = v("key_editor_color_keyword", base.keyword),
            string = v("key_editor_color_string", base.string),
            number = v("key_editor_color_number", base.number),
            type = v("key_editor_color_type", base.type),
            function = v("key_editor_color_function", base.function),
            variable = v("key_editor_color_variable", base.variable),
            constant = v("key_editor_color_constant", base.constant),
            operator = v("key_editor_color_operator", base.operator),
            tag = v("key_editor_color_tag", base.tag),
            attribute = v("key_editor_color_attribute", base.attribute),
            punctuation = v("key_editor_color_punctuation", base.punctuation)
        )
    }

    fun themeModel(name: String, palette: Palette): ThemeModel {
        val root = JSONObject()
            .put("name", "Material Files $name")
        val settings = JSONArray()
        settings.put(JSONObject().put("settings", JSONObject()
            .put("background", palette.background)
            .put("foreground", palette.foreground)
            .put("lineHighlight", palette.background)
            .put("selection", palette.keyword)
            .put("highlightedDelimitersForeground", palette.punctuation)))

        fun add(scope: String, color: String) {
            settings.put(JSONObject()
                .put("name", scope)
                .put("scope", scope)
                .put("settings", JSONObject().put("foreground", color)))
        }
        add("comment", palette.comment)
        add("keyword,storage,storage.type,storage.modifier", palette.keyword)
        add("string,string.quoted,string.template", palette.string)
        add("constant.numeric", palette.number)
        add("entity.name.type,support.type,class", palette.type)
        add("entity.name.function,support.function,meta.function-call", palette.function)
        add("variable,variable.other,variable.parameter", palette.variable)
        add("constant,constant.language", palette.constant)
        add("keyword.operator,punctuation.accessor", palette.operator)
        add("entity.name.tag", palette.tag)
        add("entity.other.attribute-name", palette.attribute)
        add("punctuation", palette.punctuation)

        root.put("settings", settings)
        val json = root.toString().toByteArray(Charsets.UTF_8)
        val source = IThemeSource.fromInputStream(ByteArrayInputStream(json), "material-files-$name.json", null)
        return ThemeModel(source, "material-files-$name")
    }
}

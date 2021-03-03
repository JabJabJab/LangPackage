package jab.spigot.language

import jab.spigot.language.`object`.LangComplex
import jab.spigot.language.`object`.LangComponent
import jab.spigot.language.processor.LangProcessor
import jab.spigot.language.processor.PercentProcessor
import jab.spigot.language.util.*
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.io.*
import java.net.URL
import java.util.*

/**
 * LangPackage is a utility that provides the ability to substitute sections of a string
 * recursively. This allows for Strings to be dynamically edited, and defined anywhere within the
 * String to be injected with EntryFields. Adding to this is the ability to select what Language to
 * choose from, falling back to English if not defined.
 *
 * TODO: Document.
 *
 * @author Jab
 *
 * @property name The String name of the LanguagePackage. This is noted in the LanguageFiles as
 *      "{{name}}_{{language_abbreviation}}.yml"
 * @property dir (Optional) The File Object for the directory where the LangFiles are stored. DEFAULT: 'lang/'
 * @throws IllegalArgumentException Thrown if the directory doesn't exist or isn't a valid directory. Thrown if
 *      the name given is empty.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class LangPackage(val name: String, val dir: File = File("lang")) {

    /** Handles processing of texts for the LanguageFile. */
    var processor: LangProcessor = PercentProcessor()

    /** The language file to default to if a raw string cannot be located with another language. */
    var defaultLang: Language = Language.ENGLISH_GENERIC

    /** The Map for LanguageFiles, assigned with their Languages. */
    private val files: EnumMap<Language, LangFile> = EnumMap(Language::class.java)

    init {
        if (!dir.exists()) {
            throw IllegalArgumentException("""The directory "$dir" doesn't exist.""")
        } else if (!dir.isDirectory) {
            throw IllegalArgumentException("""The path "$dir" is not a valid directory.""")
        }
        if (name.isEmpty()) {
            throw IllegalArgumentException("""The name "$name" is empty.""")
        }
    }

    /**
     * Reads and loads the LangPackage.
     * @param save (Optional) Set to true to try to detect & save files from the plugin to the lang folder.
     * @param force (Optional) Set to true to save resources, even if they are already present.
     *
     * @return Returns the instance. (For one-line executions)
     */
    fun load(save: Boolean = false, force: Boolean = false): LangPackage {
        append(name, save, force)
        return this
    }

    /**
     * Appends a language package.
     *
     * @param name The name of the package to append.
     * @param save (Optional) Set to true to try to detect & save files from the plugin to the lang folder.
     * @param force (Optional) Set to true to save resources, even if they are already present.
     *
     * @return Returns the instance. (For one-line executions)
     */
    fun append(name: String, save: Boolean = false, force: Boolean = false): LangPackage {

        // Save any resources detected.
        if (save) {
            val slash = File.separator
            for (lang in Language.values()) {
                val resourcePath = "${dir.path}$slash${name}_${lang.abbreviation}.yml"
                try {
                    saveResource(resourcePath, force)
                } catch (e: Exception) {
                    System.err.println("Failed to save resource: $resourcePath")
                    e.printStackTrace(System.err)
                }
            }
        }

        // Search for and load LangFiles for the package.
        for (lang in Language.values()) {

            val file = File(dir, "${name}_${lang.abbreviation}.yml")
            if (file.exists()) {

                val langFile = files[lang]
                if (langFile != null) {
                    langFile.append(file)
                } else {
                    files[lang] = LangFile(file, lang).load()
                }
            }
        }

        return this
    }

    /**
     * Sets a value for a language.
     *
     * @param lang The language to set.
     * @param field The field to set.
     * @param value The value to set.
     */
    fun set(lang: Language, field: String, value: Any?) {
        val file: LangFile = files.computeIfAbsent(lang) { LangFile(lang) }
        file.set(field, value)
    }

    /**
     * Sets a value for the language.
     *
     * @param lang The language to set.
     * @param fields The fields to set.
     */
    fun set(lang: Language, vararg fields: LangArg) {
        // Make sure that we have fields to set.
        if (fields.isEmpty()) {
            return
        }

        val file: LangFile = files.computeIfAbsent(lang) { LangFile(lang) }
        for (field in fields) {
            file.set(field.key, field.value)
        }
    }

    /**
     * TODO: Document.
     *
     * @param field
     * @param lang
     * @param args
     *
     * @return
     */
    fun getList(field: String, lang: Language = defaultLang, vararg args: LangArg): List<String>? {
        val string = getString(field, lang, *args) ?: return null
        val rawList = toAList(string)
        val processedList = ArrayList<String>()
        for (raw in rawList) {
            if (raw != null) {
                processedList.add(processor.processString(raw, this, lang, *args))
            } else {
                processedList.add("")
            }
        }
        return processedList
    }

    /**
     * TODO: Document.
     *
     * @param field
     * @param lang
     *
     * @return
     */
    fun getString(field: String, lang: Language = defaultLang, vararg args: LangArg): String? {

        val raw = getRaw(field, lang)
        return if (raw != null) {
            when (raw) {
                is LangComponent -> {
                    raw.process(this, lang, *args).toPlainText()
                }
                is LangComplex -> {
                    raw.process(this, lang, *args)
                }
                else -> {
                    processor.processString(raw.toString(), this, lang, *args)
                }
            }
        } else {
            return null
        }
    }

    /**
     * TODO: Document.
     *
     * @param field
     * @param lang
     *
     * @return
     */
    fun getRaw(field: String, lang: Language): Any? {

        // Attempt to grab the most relevant LangFile.
        var langFile = files[lang]
        if (langFile == null) {

            // Check language fallbacks if the file is not defined.
            val fallBack = lang.getFallback()
            if (fallBack != null) {
                langFile = files[fallBack]
            }
        }

        var raw: Any? = null
        if (langFile != null) {
            raw = langFile.get(field)
        }

        // Check global last.
        if (raw == null && this != global) {
            raw = global.getRaw(field, lang)
        }

        return raw
    }

    /**
     * Broadcasts a message to all online players, checking their locales and sending the corresponding dialog.
     *
     *
     * @param field The ID of the dialog to send.
     * @param args The variables to apply to the dialog sent.
     */
    fun broadcast(field: String, vararg args: LangArg) {

        val cache: EnumMap<Language, TextComponent> = EnumMap<Language, TextComponent>(Language::class.java)

        for (player in Bukkit.getOnlinePlayers()) {
            // Grab the players language, else fallback to default.
            val langPlayer = Language.getLanguage(player, defaultLang)
            var lang = langPlayer

            if (cache[lang] != null) {
                player.spigot().sendMessage(cache[lang])
                continue
            }

            var value = getRaw(field, lang)
            if (value == null) {
                lang = defaultLang
                value = getRaw(field, lang)
            }

            val component: TextComponent
            if (value != null) {
                component = when (value) {
                    is LangComponent -> {
                        value.get()
                    }
                    is LangComplex -> {
                        TextComponent(value.get())
                    }
                    is TextComponent -> {
                        value
                    }
                    else -> {
                        TextComponent(value.toString())
                    }
                }
            } else {
                component = TextComponent(field)
            }

            val result = processor.processComponent(component, this, langPlayer, *args)
            cache[lang] = result
            cache[langPlayer] = result

            player.spigot().sendMessage(result)
        }
    }

    /**
     * Messages a player with a given field and arguments. The language will be based on [Player.getLocale].
     *   If the language is not supported, [LangPackage.defaultLang] will be used.
     *
     * @param player The player to send the message.
     * @param field The field to send.
     * @param args Additional arguments to apply.
     */
    fun message(player: Player, field: String, vararg args: LangArg) {

        val langPlayer = Language.getLanguage(player, defaultLang)
        var lang = langPlayer

        var value = getRaw(field, lang)
        if (value == null) {
            lang = defaultLang
            value = getRaw(field, lang)
        }

        val component: TextComponent
        if (value != null) {
            component = when (value) {
                is LangComponent -> {
                    value.get()
                }
                is LangComplex -> {
                    TextComponent(value.get())
                }
                is TextComponent -> {
                    value
                }
                else -> {
                    TextComponent(value.toString())
                }
            }
        } else {
            component = TextComponent(field)
        }

        val result = processor.processComponent(component, this, langPlayer, *args)

//        println("\tResult: ")
//        val resultList = ComponentUtil.toPretty(result, "\t")
//        ConsoleColor.println(resultList)

        player.spigot().sendMessage(result)
    }

    /**
     * TODO: Document.
     *
     * @param lang
     * @param field
     *
     * @return
     */
    fun contains(lang: Language, field: String): Boolean {
        return files[lang]?.contains(field.toLowerCase()) ?: false
    }

    /**
     * TODO: Document.
     *
     * @param lang The language to test.
     * @param field The field to test.
     *
     * @return Returns true if the field for the language stores a [LangComplex] object.
     */
    fun isComplex(lang: Language, field: String): Boolean {
        return files[lang]?.isComplex(field) ?: false
    }

    /**
     * TODO: Document.
     *
     * @param lang The language to test.
     * @param field The field to test.
     *
     * @return Returns true if the field for the language stores a component-based value.
     */
    fun isLangComponent(lang: Language, field: String): Boolean {
        return files[lang]?.isLangComponent(field) ?: false
    }

    /**
     * TODO: Document.
     *
     * @param lang The language to test.
     * @param field The field to test.
     *
     * @return Returns true if the field for the language stores a [StringPool].
     */
    fun isStringPool(lang: Language, field: String): Boolean {
        return files[lang]?.isStringPool(field) ?: false
    }

    /**
     * TODO: Document.
     *
     * @param lang The language to test.
     * @param field The field to test.
     *
     * @return Returns true if the field for the language stores a ActionText.
     */
    fun isActionText(lang: Language, field: String): Boolean {
        return files[lang]?.isActionText(field) ?: false
    }

    companion object {

        /** TODO: Document. */
        val global: LangPackage

        /** TODO: Document. */
        val GLOBAL_DIRECTORY: File = File("lang")

        /** The standard 'line.separator' for most Java Strings. */
        const val NEW_LINE: String = "\n"

        /** TODO: Document. */
        var DEFAULT_RANDOM: Random = Random()

        init {
            // The global 'lang' directory.
            if (!GLOBAL_DIRECTORY.exists()) {
                GLOBAL_DIRECTORY.mkdirs()
            }

            // Store all global lang files present in the jar.
            for (lang in Language.values()) {
                saveResource("lang${File.separator}global_${lang.abbreviation}.yml")
            }

            global = LangPackage("global").load()
        }

        /**
         * Converts any object given to a string. Lists are compacted into one String using [NEW_LINE] as a separator.
         *
         * @param value The value to process to a String.
         *
         * @return Returns the result String.
         */
        fun toAString(value: Any): String {
            return if (value is List<*>) {
                val builder: StringBuilder = StringBuilder()
                for (next in value) {
                    val line = value.toString()
                    if (builder.isEmpty()) {
                        builder.append(line)
                    } else {
                        builder.append(NEW_LINE).append(line)
                    }
                }
                builder.toString()
            } else {
                value.toString()
            }
        }

        /**
         * Creates a component with a [ClickEvent] for firing a command.
         *
         * @param text The text to display.
         * @param command The command to execute when clicked.
         *
         * @return Returns a text component with a click event for executing the command.
         */
        fun createCommandComponent(text: String, command: String): TextComponent {
            val component = TextComponent(text)
            component.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, command)
            return component
        }

        /**
         * Creates a component with a [HoverEvent] for displaying lines of text.
         *
         * @param text The text to display.
         * @param lines The lines of text to display when the text is hovered by a mouse.
         *
         * @return TODO: Document.
         */
        fun createHoverComponent(text: String, lines: Array<String>): TextComponent {
            val component = TextComponent(text)

            var list: Array<TextComponent> = emptyArray()
            for (arg in lines) {
                list = list.plus(TextComponent(arg))
            }

            @Suppress("DEPRECATION")
            component.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, list)
            return component
        }

        /**
         * Creates a component with a [HoverEvent] for displaying lines of text.
         *
         * @param text The text to display.
         * @param lines The lines of text to display when the text is hovered by a mouse.
         *
         * @return
         */
        @Suppress("DEPRECATION")
        fun createHoverComponent(text: String, lines: List<String>): TextComponent {
            val component = TextComponent(text)

            var list: Array<TextComponent> = emptyArray()
            for (arg in lines) {
                list = list.plus(TextComponent(arg))
            }

            component.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, list)
            return component
        }

        /**
         * @param value The value to partition as a string with the [NEW_LINE] operator.
         *
         * @return Returns a List of Strings, partitioned by the [NEW_LINE] operator.
         */
        fun toAList(value: Any): List<String?> {
            val string = value.toString()
            return if (string.contains(NEW_LINE)) {
                string.split(NEW_LINE)
            } else {
                listOf(string)
            }
        }

        /**
         * Converts a List of Strings to a String Array.
         *
         * @param list The List to convert.
         *
         * @return Returns a String Array of the String Lines in the List provided.
         */
        fun toAStringArray(list: List<String>): Array<String> {
            var array: Array<String> = emptyArray()
            for (next in list) {
                array = array.plus(next)
            }
            return array
        }

        /**
         * Colors a list of strings to the Minecraft color-code specifications using an alternative color-code.
         *
         * @param strings The strings to color.
         * @param colorCode (Default: '&') The alternative color-code to process.
         *
         * @return TODO: Document.
         */
        fun color(strings: List<String>, colorCode: Char = '&'): List<String> {
            val coloredList = ArrayList<String>()
            for (string in strings) {
                coloredList.add(color(string, colorCode))
            }
            return coloredList
        }

        /**
         * Colors a string to the Minecraft color-code specifications using an alternative color-code.
         *
         * @param string The string to color.
         * @param colorCode (Default: '&') The alternative color-code to process.
         *
         * @return TODO: Document.
         */
        fun color(string: String, colorCode: Char = '&'): String {
            return ChatColor.translateAlternateColorCodes(colorCode, string)
        }

        /**
         * Message a player with multiple lines of text.
         *
         * @param sender The player to send the texts.
         * @param lines The lines of text to send.
         */
        fun message(sender: CommandSender, lines: Array<String>) {
            if (lines.isEmpty()) {
                return
            }

            sender.sendMessage(lines)
        }

        /**
         * Message a player with multiple lines of text.
         *
         * @param sender The player to send the texts.
         * @param lines The lines of text to send.
         */
        fun message(sender: CommandSender, lines: List<String>) {
            // Convert to an array to send all messages at once.
            var array = emptyArray<String>()
            for (line in lines) {
                array = array.plus(line)

            }

            sender.sendMessage(array)
        }

        /**
         * Broadcasts multiple lines of text to all players on the server.
         *
         * @param lines The lines of text to broadcast.
         */
        fun broadcast(lines: Array<String>) {
            for (line in lines) {
                Bukkit.broadcastMessage(line)
            }
        }

        /**
         * Broadcasts multiple lines of text to all players on the server.
         *
         * @param lines The lines of text to broadcast.
         */
        fun broadcast(lines: List<String>) {
            for (line in lines) {
                Bukkit.broadcastMessage(line)
            }
        }

        /**
         * Broadcasts multiple lines of text to all players on the server.
         *
         * <br/><b>NOTE:</b> If any lines of text are null, it is ignored.
         *
         * @param lines The lines of text to broadcast.
         */
        fun broadcastSafe(lines: Array<String?>) {
            for (line in lines) {
                if (line != null) {
                    Bukkit.broadcastMessage(line)
                }
            }
        }

        /**
         * Broadcasts multiple lines of text to all players on the server.
         *
         * <br/><b>NOTE:</b> If any lines of text are null, it is ignored.
         *
         * @param lines The lines of text to broadcast.
         */
        fun broadcastSafe(lines: List<String?>) {
            for (line in lines) {
                if (line != null) {
                    Bukkit.broadcastMessage(line)
                }
            }
        }

        /**
         * Modified method from [JavaPlugin] to store global lang files.
         */
        private fun saveResource(resourcePath: String, replace: Boolean = false) {
            if (resourcePath.isEmpty()) {
                throw RuntimeException("ResourcePath cannot be empty.")
            }

            var resourcePath2 = resourcePath
            resourcePath2 = resourcePath2.replace('\\', '/')
            val `in`: InputStream = getResource(resourcePath2)
                ?: return
            val outFile = File(resourcePath2)
            val lastIndex = resourcePath2.lastIndexOf('/')
            val outDir = File(resourcePath2.substring(0, if (lastIndex >= 0) lastIndex else 0))
            if (!outDir.exists()) {
                outDir.mkdirs()
            }
            try {
                if (!outFile.exists() || replace) {
                    val out: OutputStream = FileOutputStream(outFile)
                    val buf = ByteArray(1024)
                    var len: Int
                    while (`in`.read(buf).also { len = it } > 0) {
                        out.write(buf, 0, len)
                    }
                    out.close()
                    `in`.close()
                }
            } catch (ex: IOException) {
                System.err.println("Could not save ${outFile.name} to $outFile")
            }
        }

        /**
         * Modified method from [JavaPlugin] to retrieve global lang files.
         */
        private fun getResource(fileName: String): InputStream? {
            return try {
                val url: URL = this::class.java.classLoader.getResource(fileName) ?: return null
                val connection = url.openConnection()
                connection.useCaches = false
                connection.getInputStream()
            } catch (ex: IOException) {
                null
            }
        }
    }
}
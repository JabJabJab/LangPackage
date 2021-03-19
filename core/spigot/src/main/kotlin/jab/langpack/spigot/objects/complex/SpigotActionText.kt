@file:Suppress("unused")

package jab.langpack.spigot.objects.complex

import jab.langpack.core.Language
import jab.langpack.core.objects.LangArg
import jab.langpack.core.objects.complex.ActionText
import jab.langpack.core.objects.complex.CommandText
import jab.langpack.core.objects.complex.Complex
import jab.langpack.core.objects.complex.HoverText
import jab.langpack.spigot.SpigotLangPack
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import java.util.*

/**
 * TODO: Document.
 *
 * @author Jab
 */
class SpigotActionText : ActionText {

    /**
     * @see ActionText
     */
    constructor(text: String) : super(text)

    /**
     * @see ActionText
     */
    constructor(text: String, hoverText: HoverText) : super(text, hoverText)

    /**
     * @see ActionText
     */
    constructor(text: String, command: String) : super(text, command)

    /**
     * @see ActionText
     */
    constructor(text: String, command: String, hover: List<String>) : super(text, command, hover)

    /**
     * @see ActionText
     */
    constructor(text: String, commandText: CommandText, hoverText: HoverText) : super(text, commandText, hoverText)

    /**
     * @see ActionText
     */
    constructor(cfg: ConfigurationSection) : super(cfg)

    /**
     * Sends the ActionText to a given player.
     *
     * @param player The player to send.
     */
    fun message(player: Player) {
        if (!player.isOnline) return
        player.spigot().sendMessage(get())
    }

    /**
     * Sends the ActionText as a message to a player.
     *
     * @param player The player to receive the message.
     * @param pack (Optional) The package to process the text.
     * @param args (Optional) Additional arguments to provide to process the text.
     */
    fun send(player: Player, pack: SpigotLangPack? = null, vararg args: LangArg) {
        val textComponent = if (pack != null) {
            process(pack, pack.getLanguage(player), null, *args)
        } else {
            get()
        }
        player.spigot().sendMessage(textComponent)
    }

    /**
     * Broadcasts the ActionText to all online players on the server.
     */
    fun broadcast() {
        val message = get()
        for (player in Bukkit.getOnlinePlayers()) {
            player.spigot().sendMessage(message)
        }
    }

    /**
     * Broadcasts the ActionText to all players in a given world.
     *
     * @param world The world to broadcast.
     */
    fun broadcast(world: World) {
        val message = get()
        for (player in world.players) {
            player.spigot().sendMessage(message)
        }
    }

    /**
     * Broadcasts the ActionText to all online players on the server.
     *
     * @param pack The package to process the text.
     * @param args (Optional) Additional arguments to provide to process the text.
     */
    fun broadcast(pack: SpigotLangPack, vararg args: LangArg) {

        val cache = EnumMap<Language, TextComponent>(Language::class.java)

        for (player in Bukkit.getOnlinePlayers()) {

            val textComponent: TextComponent
            val lang = pack.getLanguage(player)

            if (cache[lang] != null) {
                textComponent = cache[lang]!!
            } else {
                textComponent = process(pack, pack.getLanguage(player), null, *args)
                cache[lang] = textComponent
            }

            player.spigot().sendMessage(textComponent)
        }
    }

    /**
     * Broadcasts the ActionText to all players in a given world.
     *
     * @param pack The package to process the text.
     * @param args (Optional) Additional arguments to provide to process the text.
     */
    fun broadcast(world: World, pack: SpigotLangPack, vararg args: LangArg) {

        val cache = EnumMap<Language, TextComponent>(Language::class.java)

        for (player in world.players) {

            val textComponent: TextComponent
            val lang = pack.getLanguage(player)

            if (cache[lang] != null) {
                textComponent = cache[lang]!!
            } else {
                textComponent = process(pack, lang, null, *args)
                cache[lang] = textComponent
            }

            player.spigot().sendMessage(textComponent)
        }
    }

    /**
     * TODO: Document.
     *
     * @author Jab
     */
    class Loader : Complex.Loader<SpigotActionText> {
        override fun load(cfg: ConfigurationSection): SpigotActionText = SpigotActionText(cfg)
    }
}

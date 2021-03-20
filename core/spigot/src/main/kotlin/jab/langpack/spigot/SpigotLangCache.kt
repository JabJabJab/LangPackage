@file:Suppress("unused")

package jab.langpack.spigot

import jab.langpack.core.LangCache
import jab.langpack.core.Language
import jab.langpack.core.objects.LangArg
import org.bukkit.entity.Player

/**
 * TODO: Document.
 *
 * @author Jab
 *
 * @param pack
 */
class SpigotLangCache(pack: SpigotLangPack) : LangCache<SpigotLangPack>(pack) {

    /**
     * @see SpigotLangPack.broadcast
     */
    fun broadcast(field: String, vararg args: LangArg) {
        pack.broadcast(field, *args)
    }

    /**
     * @see SpigotLangPack.message
     */
    fun message(player: Player, field: String, vararg args: LangArg) {
        pack.message(player, field, *args)
    }

    /**
     * @see SpigotLangPack.getLanguage
     */
    fun getLanguage(player: Player): Language = pack.getLanguage(player)
}
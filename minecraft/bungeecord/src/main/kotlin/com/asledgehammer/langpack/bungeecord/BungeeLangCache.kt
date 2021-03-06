@file:Suppress("unused")

package com.asledgehammer.langpack.bungeecord

import com.asledgehammer.langpack.core.LangCache
import com.asledgehammer.langpack.core.Language
import com.asledgehammer.langpack.core.objects.LangArg
import net.md_5.bungee.api.connection.ProxiedPlayer

/**
 * **BungeeLangCache** wraps the [LangCache] class to provide additional support for the BungeeCord API.
 *
 * @author Jab
 *
 * @param pack The BungeeLangPack instance.
 */
class BungeeLangCache(pack: BungeeLangPack) : LangCache<BungeeLangPack>(pack) {

    /**
     * @see BungeeLangPack.broadcast
     */
    fun broadcast(field: String, vararg args: LangArg) = pack.broadcast(field, *args)

    /**
     * @see BungeeLangPack.message
     */
    fun message(player: ProxiedPlayer, field: String, vararg args: LangArg) = pack.message(player, field, *args)

    /**
     * @see BungeeLangPack.getLanguage
     */
    fun getLanguage(player: ProxiedPlayer): Language = pack.getLanguage(player)
}

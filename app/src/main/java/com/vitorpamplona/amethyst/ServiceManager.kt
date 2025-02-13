package com.vitorpamplona.amethyst

import com.vitorpamplona.amethyst.model.Account
import com.vitorpamplona.amethyst.model.LocalCache
import com.vitorpamplona.amethyst.service.HttpClient
import com.vitorpamplona.amethyst.service.NostrAccountDataSource
import com.vitorpamplona.amethyst.service.NostrChannelDataSource
import com.vitorpamplona.amethyst.service.NostrChatroomListDataSource
import com.vitorpamplona.amethyst.service.NostrGlobalDataSource
import com.vitorpamplona.amethyst.service.NostrHomeDataSource
import com.vitorpamplona.amethyst.service.NostrSingleChannelDataSource
import com.vitorpamplona.amethyst.service.NostrSingleEventDataSource
import com.vitorpamplona.amethyst.service.NostrSingleUserDataSource
import com.vitorpamplona.amethyst.service.NostrThreadDataSource
import com.vitorpamplona.amethyst.service.NostrUserProfileDataSource
import com.vitorpamplona.amethyst.service.NostrVideoDataSource
import com.vitorpamplona.amethyst.service.relays.Client

object ServiceManager {
    private var account: Account? = null

    fun start(account: Account) {
        this.account = account
        start()
    }

    fun start() {
        val myAccount = account
        HttpClient.start(account)
        if (myAccount != null) {
            Client.connect(myAccount.activeRelays() ?: myAccount.convertLocalRelays())

            // start services
            NostrAccountDataSource.account = myAccount
            NostrHomeDataSource.account = myAccount
            NostrChatroomListDataSource.account = myAccount
            NostrVideoDataSource.account = myAccount

            // Notification Elements
            NostrHomeDataSource.start()
            NostrAccountDataSource.start()
            NostrChatroomListDataSource.start()

            // More Info Data Sources
            NostrSingleEventDataSource.start()
            NostrSingleChannelDataSource.start()
            NostrSingleUserDataSource.start()
        }
    }

    fun pause() {
        NostrAccountDataSource.stop()
        NostrHomeDataSource.stop()
        NostrChannelDataSource.stop()
        NostrChatroomListDataSource.stop()

        NostrGlobalDataSource.stop()
        NostrSingleChannelDataSource.stop()
        NostrSingleEventDataSource.stop()
        NostrSingleUserDataSource.stop()
        NostrThreadDataSource.stop()
        NostrUserProfileDataSource.stop()
        NostrVideoDataSource.stop()

        Client.disconnect()
    }

    fun cleanUp() {
        LocalCache.cleanObservers()

        account?.let {
            LocalCache.pruneOldAndHiddenMessages(it)
            LocalCache.pruneHiddenMessages(it)
            LocalCache.pruneContactLists(it)
            // LocalCache.pruneNonFollows(it)
        }
    }
}

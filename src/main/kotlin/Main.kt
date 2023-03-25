package org.example

import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession

fun main() {
    val botsApi = TelegramBotsApi(DefaultBotSession::class.java)
    botsApi.registerBot(TelegramBot())
}
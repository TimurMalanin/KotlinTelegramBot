package org.example

import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import java.io.IOException
import java.util.*

class TelegramBot internal constructor() : TelegramLongPollingBot() {
    private val accounts: MutableList<InstagramLoader> = ArrayList()
    private var currentAccount: InstagramLoader? = null
    private var programSate = States.NOT_ASSIGNED

    init {
        Locale.setDefault(Locale("en", "US"))
        val myThread = MyThread()
        myThread.start()
    }

    override fun onUpdateReceived(update: Update) {
        if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.callbackQuery)
        } else if (update.hasMessage() && update.message.hasText()) {
            val updateChatId = update.message.chatId.toString()
            when (update.message.text) {
                "/start" -> startCase(updateChatId)
                "/follow" -> followCase(updateChatId)
                "/show" -> showCase(updateChatId)
                "/unfollow" -> unfollowCase(updateChatId)
                "/timeline" -> timelineCase(updateChatId)
                else -> {
                    when (programSate) {
                        States.WAITING_USERNAME -> trackingAccount(update)
                        States.WAITING_COMMENT -> commentPost(update)
                        States.WAITING_UNFOLLOW -> unfollowAccount(update)
                        States.WAITING_TIMELINE -> sendTimeline(update)
                        else -> {}
                    }
                }
            }
        }
    }

    private fun startCase(updateChatId: String) {
        sendMessage(updateChatId, resourceBundle.getString("StartMessage"))
    }

    private fun followCase(updateChatId: String) {
        sendMessage(updateChatId, resourceBundle.getString("TrackingMessage"))
        programSate = States.WAITING_USERNAME
    }

    private fun showCase(updateChatId: String) {
        val usernames = accounts.joinToString("\n") { it.targetUsername }
        val message = usernames.ifBlank {
            resourceBundle.getString("EmptyAccounts")
        }
        sendMessage(updateChatId, message)
    }

    private fun unfollowCase(updateChatId: String) {
        sendMessage(updateChatId, resourceBundle.getString("UnfollowMessage"))
        programSate = States.WAITING_UNFOLLOW
    }

    private fun timelineCase(updateChatId: String) {
        sendMessage(updateChatId, resourceBundle.getString("TimelineMessage"))
        programSate = States.WAITING_TIMELINE
    }

    private fun sendTimeline(update: Update) {
        programSate = States.NOT_ASSIGNED
        val account = loadAccount(update)
        val timeLine = account.timeLine()
        if (timeLine != null) {
            sendPhotos(timeLine, account.chatId)
        } else {
            sendMessage(currentAccount!!.chatId, resourceBundle.getString("FailedSendMessage"))
        }
    }

    private fun sendPhotos(URLs: List<String>, chatId: String) {
        for (URL in URLs) {
            sendPhoto(chatId, URL)
        }
    }

    private fun unfollowAccount(update: Update) {
        val unfollowUsername = update.message.text
        val chatId = update.message.chatId.toString()
        programSate = States.NOT_ASSIGNED
        accounts.removeIf { it: InstagramLoader -> it.targetUsername == unfollowUsername }
        sendMessage(chatId, String.format(resourceBundle.getString("UnfollowExecute"), unfollowUsername))
    }

    private fun commentPost(update: Update) {
        programSate = States.NOT_ASSIGNED
        val comment = update.message.text
        val reply = currentAccount?.commentPost(comment)
        val message = if (reply != null) {
            resourceBundle.getString("PostCommentedMessage")
        } else {
            resourceBundle.getString("FailedCommentMessage")
        }
        sendMessage(currentAccount!!.chatId, message)
    }

    private fun loadAccount(update: Update): InstagramLoader {
        val chatId = update.message.chatId.toString()
        val targetUsername = update.message.text
        return InstagramLoader(targetUsername, chatId)
    }

    private fun checkinForNewPost(account: InstagramLoader) {
        currentAccount = account
        val url = try {
            account.getLatestPictureUrl()
        } catch (e: IOException) {
            sendMessage(account.chatId, resourceBundle.getString("UnableLoad"))
            null
        }
        url?.let { sendData(account.chatId, it) }
    }

    private fun trackingAccount(update: Update) {
        programSate = States.NOT_ASSIGNED
        val targetAccount = loadAccount(update)
        accounts.add(targetAccount)
    }

    private fun handleCallbackQuery(callbackQuery: CallbackQuery) {
        val chatId = callbackQuery.message.chatId.toString()
        when (callbackQuery.data) {
            "likeButtonPressed" -> likeButtonAction()
            "commentButtonPressed" -> commentButtonAction(chatId)
            "captionButtonPressed" -> captionButtonAction(chatId)
        }
    }

    private fun likeButtonAction() {
        programSate = States.NOT_ASSIGNED
        val reply = currentAccount?.likePost()
        val message = if (reply != null) {
            resourceBundle.getString("PostLikedMessage")
        } else {
            resourceBundle.getString("FailedLikeMessage")
        }
        sendMessage(currentAccount!!.chatId, message)
    }

    private fun commentButtonAction(chatId: String) {
        sendMessage(chatId, resourceBundle.getString("CommentButtonAction"))
        programSate = States.WAITING_COMMENT
    }

    private fun captionButtonAction(chatId: String) {
        val caption = try {
            currentAccount?.caption
        } catch (e: IOException) {
            currentAccount?.chatId?.let { sendMessage(it, resourceBundle.getString("FailedCaption")) }
            null
        }
        sendMessage(chatId, caption ?: resourceBundle.getString("FailedCaption"))
    }

    private fun sendData(chatId: String, URL: String) {
        sendMessage(
            currentAccount!!.chatId,
            String.format(resourceBundle.getString("PostMessage"), currentAccount!!.targetUsername)
        )
        sendPhoto(chatId, URL)
        sendMessage(currentAccount!!.chatId, resourceBundle.getString("ButtonText"), createKeyboardMarkup())
    }

    private fun sendPhoto(chatId: String, URL: String) {
        val photo = SendPhoto()
        photo.chatId = chatId
        photo.photo = InputFile(URL)
        try {
            execute(photo)
        } catch (e: TelegramApiException) {
            sendMessage(chatId, resourceBundle.getString("FailedPhoto"))
        }
    }

    private fun sendMessage(chatId: String, text: String?, keyboard: ReplyKeyboard? = null) {
        val message = SendMessage()
        message.chatId = chatId
        message.text = text!!
        message.replyMarkup = keyboard
        try {
            execute(message)
        } catch (e: TelegramApiException) {
            sendMessage(chatId, resourceBundle.getString("FailedMessage"))
        }
    }

    private fun createInlineKeyboardButton(text: String, callbackData: String): InlineKeyboardButton {
        val likeButton = InlineKeyboardButton()
        likeButton.text = text
        likeButton.callbackData = callbackData
        return likeButton
    }

    private fun createKeyboardMarkup(): ReplyKeyboard {
        return InlineKeyboardMarkup(
            listOf(
                listOf(
                    createInlineKeyboardButton("Like", "likeButtonPressed"),
                    createInlineKeyboardButton("Comment", "commentButtonPressed"),
                    createInlineKeyboardButton("Caption", "captionButtonPressed")
                )
            )
        )
    }

    private inner class MyThread : Thread() {
        private fun checkingAccountsForNewPosts() {
            for (account in accounts)
                checkinForNewPost(account)
        }

        override fun run() {
            while (true) {
                checkingAccountsForNewPosts()
                try {
                    val delay: Long = 30000
                    sleep(delay)
                } catch (e: InterruptedException) {
                    currentThread().interrupt()
                    System.err.println(String.format(resourceBundle.getString("ThreadInterrupted"), e.message))
                }
            }
        }
    }

    companion object {
        private val resourceBundle = ResourceBundle.getBundle("TelegramBotResources_en_US")
        private val API_TOKEN = System.getenv("API_TOKEN")
        private val BOT_USERNAME = System.getenv("BOT_NAME")
    }

    override fun getBotUsername(): String {
        return BOT_USERNAME
    }

    override fun getBotToken(): String {
        return API_TOKEN
    }
}
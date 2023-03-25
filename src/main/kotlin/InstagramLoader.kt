package org.example

import org.brunocvcunha.instagram4j.Instagram4j
import org.brunocvcunha.instagram4j.requests.InstagramLikeRequest
import org.brunocvcunha.instagram4j.requests.InstagramPostCommentRequest
import org.brunocvcunha.instagram4j.requests.InstagramSearchUsernameRequest
import org.brunocvcunha.instagram4j.requests.InstagramUserFeedRequest
import org.brunocvcunha.instagram4j.requests.payload.InstagramFeedItem


class InstagramLoader(targetUsername: String, chatId: String) {
    private val instagram: Instagram4j = Instagram4j.builder().username(USERNAME).password(PASSWORD).build()
    private var lastPostDate = 0L
    val targetUsername: String
    val chatId: String
    val caption: String = lastItem().caption.text

    init {
        this.targetUsername = targetUsername
        this.chatId = chatId
        instagram.setup()
        instagram.login()
    }

    private fun lastItem(): InstagramFeedItem {
        val usernameResult = instagram.sendRequest(InstagramSearchUsernameRequest(targetUsername))
        val userId = usernameResult.user.pk
        val timeLine = instagram.sendRequest(InstagramUserFeedRequest(userId))
        return timeLine.items[0]
    }

    fun timeLine(): List<String> {
        val userPk = instagram.sendRequest(InstagramSearchUsernameRequest(targetUsername)).user.pk
        val feedResult = instagram.sendRequest(InstagramUserFeedRequest(userPk)).items
        return feedResult.take(5.coerceAtMost(feedResult.size))
            .map { it.getImage_versions2().getCandidates()[0].getUrl() }
    }

    fun pictureUpload(): String? {
        val lastItem = lastItem()
        return if (lastPostDate < lastItem.getTaken_at()) {
            lastPostDate = lastItem.getTaken_at()
            lastItem.getImage_versions2().getCandidates()[0].getUrl()
        } else
            null
    }

    fun commentPost(text: String?) {
        instagram.sendRequest(InstagramPostCommentRequest(lastItem().getPk(), text))
    }

    fun likePost() {
        instagram.sendRequest(InstagramLikeRequest(lastItem().getPk()))
    }

    companion object {
        private val USERNAME = System.getenv("INST_LOGIN")
        private val PASSWORD = System.getenv("INST_PASSWORD")
    }
}
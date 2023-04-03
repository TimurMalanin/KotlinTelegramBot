package org.example

import org.brunocvcunha.instagram4j.Instagram4j
import org.brunocvcunha.instagram4j.requests.InstagramLikeRequest
import org.brunocvcunha.instagram4j.requests.InstagramPostCommentRequest
import org.brunocvcunha.instagram4j.requests.InstagramSearchUsernameRequest
import org.brunocvcunha.instagram4j.requests.InstagramUserFeedRequest
import org.brunocvcunha.instagram4j.requests.payload.InstagramFeedItem

class InstagramLoader(val targetUsername: String, val chatId: String) {
    private val instagram: Instagram4j = Instagram4j.builder().username(USERNAME).password(PASSWORD).build()
    private var lastPostDate = 0L
    val caption: String?

    init {
        instagram.setup()
        instagram.login()
        caption = getItemFromTimeline(targetUsername)?.caption?.text
    }

    private fun getItemFromTimeline(targetUsername: String): InstagramFeedItem? {
        return try {
            val usernameResult = instagram.sendRequest(InstagramSearchUsernameRequest(targetUsername))
            val userId = usernameResult.user.pk
            val timeLine = instagram.sendRequest(InstagramUserFeedRequest(userId))
            timeLine.items[0]
        } catch (e: Exception) {
            println("Error while getting item from timeline: ${e.message}")
            null
        }
    }

    fun timeLine(): List<String>? {
        return try {
            val userPk = instagram.sendRequest(InstagramSearchUsernameRequest(targetUsername)).user.pk
            val feedResult = instagram.sendRequest(InstagramUserFeedRequest(userPk)).items
            feedResult.take(6.coerceAtMost(feedResult.size)).mapNotNull {
                it.getImage_versions2()?.getCandidates()?.get(0)?.getUrl()
            }
        } catch (e: Exception) {
            println("Error while getting timeline: ${e.message}")
            null
        }
    }

    fun getLatestPictureUrl(): String? {
        val lastItem = getItemFromTimeline(targetUsername)
        return lastItem?.let {
            if (lastPostDate < it.getTaken_at()) {
                lastPostDate = it.getTaken_at()
                it.getImage_versions2().getCandidates()[0].getUrl()
            } else {
                null
            }
        }
    }

    fun commentPost(text: String?): String? {
        return try {
            getItemFromTimeline(targetUsername)?.let { item ->
                instagram.sendRequest(InstagramPostCommentRequest(item.getPk(), text))
                COMMENT_SUCCESSFULLY_MESSAGE
            }
        } catch (e: Exception) {
            println("Error while commenting on post: ${e.message}")
            null
        }
    }

    fun likePost(): String? {
        return try {
            getItemFromTimeline(targetUsername)?.let { item ->
                instagram.sendRequest(InstagramLikeRequest(item.pk))
                LIKED_SUCCESSFULLY_MESSAGE
            }
        } catch (e: Exception) {
            println("Error while liking post: ${e.message}")
            null
        }
    }

    companion object {
        private const val COMMENT_SUCCESSFULLY_MESSAGE = "Comment liked successfully"
        private const val LIKED_SUCCESSFULLY_MESSAGE = "Post liked successfully"
        private val USERNAME = System.getenv("INST_LOGIN")
        private val PASSWORD = System.getenv("INST_PASSWORD")
    }
}
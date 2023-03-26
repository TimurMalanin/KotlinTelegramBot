# KotlinTelegramBot

This is a Kotlin program for a Telegram bot that allows users to track Instagram accounts and receive notifications when new posts are published, as well as comment on posts, unfollow accounts, and view account timelines.

## Getting start

Set the API_TOKEN, BOT_NAME, INST_LOGIN and INST_PASSWORD environment variables in your system or IDE.

## Usage

You can use the following commands to interact with bot:

* `/start`: Displays a welcome message.
* `/follow`: Begins tracking a new Instagram account.
* `/show`: Displays a list of all tracked accounts.
* `/unfollow`: Stops tracking a previously tracked account.
* `/timeline`: Displays the latest posts from all tracked accounts.

To track a new Instagram account, use the `/follow` command and enter the Instagram username when prompted. Once the account is being tracked, the bot will send notifications whenever a new post is published.

To comment on a post, reply to the notification with your comment. To unfollow an account, use the `/unfollow` command and enter the Instagram username when prompted. To view the latest posts from certain account, use the `/timeline` command.

## Built With

* Telegram Bots API - The API used to interact with Telegram bots.
* Instagram4j - The Java library used to interact with Instagram.
* Maven - Dependency management.

## Authors

* Timur Malanin

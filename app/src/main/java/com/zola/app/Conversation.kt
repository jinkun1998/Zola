package com.zola.app

data class Conversation(
    val id: String,
    val name: String,
    val isGroup: Boolean,
    val avatar: String,
    val lastMessage: String,
    val time: Long,
    val unread: Int
)

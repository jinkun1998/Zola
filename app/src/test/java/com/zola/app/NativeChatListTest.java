package com.zola.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class NativeChatListTest {
    @Test
    public void testIsEmptyConversation() {
        org.junit.Assert.assertTrue(NativeChatList.isEmptyConversation(0, null));
        org.junit.Assert.assertTrue(NativeChatList.isEmptyConversation(0, ""));
        org.junit.Assert.assertTrue(NativeChatList.isEmptyConversation(0, "   "));
        org.junit.Assert.assertTrue(NativeChatList.isEmptyConversation(0, "Chưa có tin nhắn"));
        org.junit.Assert.assertTrue(NativeChatList.isEmptyConversation(0, "  Chưa có tin nhắn  "));
        org.junit.Assert.assertTrue(NativeChatList.isEmptyConversation(-1, ""));

        org.junit.Assert.assertFalse(NativeChatList.isEmptyConversation(1, ""));
        org.junit.Assert.assertFalse(NativeChatList.isEmptyConversation(1, null));
        org.junit.Assert.assertFalse(NativeChatList.isEmptyConversation(1, "Chưa có tin nhắn"));
        org.junit.Assert.assertFalse(NativeChatList.isEmptyConversation(0, "Hello"));
        org.junit.Assert.assertFalse(NativeChatList.isEmptyConversation(0, "[Hình ảnh]"));
        org.junit.Assert.assertFalse(NativeChatList.isEmptyConversation(0, "[Sticker]"));
    }

    @Test
    public void testIconRes() {
        assertNotEquals(0, NativeChatList.iconRes("picture"));
        assertNotEquals(0, NativeChatList.iconRes("video"));
        assertNotEquals(0, NativeChatList.iconRes("sticker"));
        assertNotEquals(0, NativeChatList.iconRes("file"));
        assertNotEquals(0, NativeChatList.iconRes("link"));
        assertNotEquals(0, NativeChatList.iconRes("voice"));
        assertNotEquals(0, NativeChatList.iconRes("Call"));
        assertEquals(0, NativeChatList.iconRes(""));
        assertEquals(0, NativeChatList.iconRes(null));
    }
}

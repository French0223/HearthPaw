package com.example.hearthpaw.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.hearthpaw.data.local.AppDatabase;
import com.example.hearthpaw.data.local.ChatMessageDao;
import com.example.hearthpaw.data.model.ChatMessage;

import java.util.List;

public class ChatRepository {
    private final ChatMessageDao chatMessageDao;
    private final LiveData<List<ChatMessage>> allMessages;

    public ChatRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        chatMessageDao = db.chatMessageDao();
        allMessages = chatMessageDao.getAllMessages();
    }

    public LiveData<List<ChatMessage>> getAllMessages() {
        return allMessages;
    }

    public List<ChatMessage> getAllMessagesNow() {
        return chatMessageDao.getAllMessagesNow();
    }

    public List<ChatMessage> getRecentMessages(int limit) {
        return chatMessageDao.getRecentMessages(limit);
    }

    public void insert(ChatMessage message) {
        AppDatabase.databaseWriteExecutor.execute(() -> chatMessageDao.insert(message));
    }

    public void insertNow(ChatMessage message) {
        chatMessageDao.insert(message);
    }

    public void updateNow(ChatMessage message) {
        chatMessageDao.update(message);
    }

    public void clearHistory() {
        AppDatabase.databaseWriteExecutor.execute(chatMessageDao::clearAll);
    }

    public void clearHistoryNow() {
        chatMessageDao.clearAll();
    }
}
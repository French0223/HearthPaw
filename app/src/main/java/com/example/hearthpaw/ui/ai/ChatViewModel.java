package com.example.hearthpaw.ui.ai;

import android.app.Application;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.hearthpaw.BuildConfig;
import com.example.hearthpaw.data.local.AppDatabase;
import com.example.hearthpaw.data.model.CareTask;
import com.example.hearthpaw.data.model.ChatMessage;
import com.example.hearthpaw.data.model.Pet;
import com.example.hearthpaw.data.repository.ChatRepository;
import com.example.hearthpaw.data.repository.PetRepository;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatViewModel extends AndroidViewModel {
    private static final String TAG = "ChatViewModel";
    private static final String MODEL_NAME = "gemini-2.5-flash";
    private static final int CONTEXT_MESSAGE_LIMIT = 12;
    private static final String OLD_WELCOME_PREFIX = "Hi! I am Bantay";
    private static final String NEW_WELCOME_TEXT = "Hi! I am BantAI. Tell me your pet's name or ask me anything about rescue care.";

    private final ChatRepository repository;
    private final PetRepository petRepository;
    private final LiveData<List<ChatMessage>> messages;
    private final MutableLiveData<Boolean> sending = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public ChatViewModel(@NonNull Application application) {
        super(application);
        repository = new ChatRepository(application);
        petRepository = new PetRepository(application);
        messages = repository.getAllMessages();
        AppDatabase.databaseWriteExecutor.execute(() -> {
            seedWelcomeMessageIfNeeded();
            normalizeStoredWelcomeMessages();
        });
    }

    public LiveData<List<ChatMessage>> getMessages() {
        return messages;
    }

    public LiveData<Boolean> isSending() {
        return sending;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void sendMessage(String userInput) {
        if (TextUtils.isEmpty(userInput) || TextUtils.isEmpty(userInput.trim())) {
            return;
        }

        if (TextUtils.isEmpty(BuildConfig.GEMINI_API_KEY)) {
            errorMessage.postValue("Add your Gemini API key in local.properties as geminiApiKey=...");
            return;
        }

        String cleanedInput = userInput.trim();
        repository.insert(new ChatMessage(ChatMessage.ROLE_USER, cleanedInput, System.currentTimeMillis()));
        sending.postValue(true);
        errorMessage.postValue(null);

        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                String prompt = buildPrompt(cleanedInput);
                Client client = Client.builder().apiKey(BuildConfig.GEMINI_API_KEY).build();
                
                Log.d(TAG, "Sending request to Gemini (" + MODEL_NAME + ")...");
                GenerateContentResponse response = client.models.generateContent(MODEL_NAME, prompt, null);
                String assistantText = response.text();

                if (TextUtils.isEmpty(assistantText)) {
                    assistantText = "I’m here, but I couldn’t generate a reply just now. Please try again.";
                }

                handleAssistantCommands(assistantText);
                String cleanAssistantText = assistantText.replaceAll("\\[\\[ADD_TASK:.*?\\]\\]", "").trim();

                repository.insert(new ChatMessage(ChatMessage.ROLE_ASSISTANT, cleanAssistantText, System.currentTimeMillis()));
            } catch (Exception exception) {
                Log.e(TAG, "Gemini Connection Error: " + exception.getMessage(), exception);
                errorMessage.postValue("BantAI could not reach Gemini: " + exception.getMessage());
                repository.insert(new ChatMessage(ChatMessage.ROLE_ASSISTANT, "I couldn’t connect to Gemini. Please check your internet or API key.", System.currentTimeMillis()));
            } finally {
                sending.postValue(false);
            }
        });
    }

    private void handleAssistantCommands(String text) {
        if (TextUtils.isEmpty(text)) return;
        Pattern pattern = Pattern.compile("\\[\\[ADD_TASK:\\s*(.*?)\\s*\\|\\s*(.*?)\\s*\\|\\s*(.*?)\\s*\\]\\]");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            executeAddTask(matcher.group(1), matcher.group(2), matcher.group(3));
        }
    }

    private void executeAddTask(String petName, String taskName, String taskTime) {
        List<Pet> pets = petRepository.getAllPetsNow();
        if (pets == null) return;
        for (Pet p : pets) {
            if (p.getName() != null && p.getName().equalsIgnoreCase(petName.trim())) {
                petRepository.insertTask(new CareTask(p.getId(), taskName.trim(), taskTime.trim()));
                break;
            }
        }
    }

    public void clearHistory() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            repository.clearHistoryNow();
            seedWelcomeMessageIfNeeded();
        });
    }

    private void seedWelcomeMessageIfNeeded() {
        List<ChatMessage> recent = repository.getRecentMessages(1);
        if (recent == null || recent.isEmpty()) {
            repository.insertNow(new ChatMessage(ChatMessage.ROLE_ASSISTANT, NEW_WELCOME_TEXT, System.currentTimeMillis()));
        }
    }

    private void normalizeStoredWelcomeMessages() {
        List<ChatMessage> allMessages = repository.getAllMessagesNow();
        if (allMessages == null || allMessages.isEmpty()) return;
        for (ChatMessage message : allMessages) {
            if (ChatMessage.ROLE_ASSISTANT.equals(message.getRole()) && message.getText() != null && message.getText().startsWith(OLD_WELCOME_PREFIX)) {
                message.setText(NEW_WELCOME_TEXT);
                repository.updateNow(message);
            }
        }
    }

    private String buildPrompt(String currentUserInput) {
        List<ChatMessage> recentMessages = repository.getRecentMessages(CONTEXT_MESSAGE_LIMIT);
        Pet inferredPet = inferReferencedPet(recentMessages, currentUserInput);
        List<Pet> allPets = petRepository.getAllPetsNow();
        
        StringBuilder builder = new StringBuilder();
        builder.append("You are BantAI, a friendly AI assistant for the HearthPaw rescue app.\n");
        builder.append("Be helpful, warm, and extremely concise. Answer questions directly.\n");
        builder.append("Never use canned phrases like 'Ready when you are' or 'I am here to help'. Just answer the user.\n\n");

        builder.append("CAPABILITY: You can add care tasks (logs/reminders) to a pet's schedule.\n");
        builder.append("If a user asks to log a feeding or task, include this at the end: [[ADD_TASK: PetName | TaskName | Time]].\n\n");

        if (allPets != null && !allPets.isEmpty()) {
            builder.append("Pets in database: ");
            for (Pet p : allPets) builder.append(p.getName()).append(", ");
            builder.append("\n\n");
        }

        if (inferredPet != null) {
            builder.append("Context Pet: ").append(inferredPet.getName()).append(" (").append(safeText(inferredPet.getSpecies())).append(")\n\n");
        }

        if (!TextUtils.isEmpty(currentUserInput)) {
            builder.append("Current user request: ").append(currentUserInput).append("\n\n");
        }

        builder.append("History:\n");
        for (int i = recentMessages.size() - 1; i >= 0; i--) {
            ChatMessage message = recentMessages.get(i);
            builder.append(message.getRole()).append(": ").append(message.getText()).append("\n");
        }
        builder.append("Assistant:");
        return builder.toString();
    }

    private Pet inferReferencedPet(List<ChatMessage> recentMessages, String currentUserInput) {
        if (recentMessages == null || recentMessages.isEmpty()) return null;
        List<Pet> pets = petRepository.getAllPetsNow();
        if (pets == null) return null;

        if (!TextUtils.isEmpty(currentUserInput)) {
            String currentText = currentUserInput.toLowerCase(Locale.ROOT);
            for (Pet p : pets) {
                if (p.getName() != null && currentText.contains(p.getName().toLowerCase(Locale.ROOT))) return p;
            }
        }

        for (ChatMessage msg : recentMessages) {
            if (ChatMessage.ROLE_USER.equals(msg.getRole()) && msg.getText() != null) {
                String text = msg.getText().toLowerCase(Locale.ROOT);
                for (Pet p : pets) {
                    if (p.getName() != null && text.contains(p.getName().toLowerCase(Locale.ROOT))) return p;
                }
            }
        }
        return null;
    }

    private String safeText(String value) {
        return TextUtils.isEmpty(value) ? "unknown" : value;
    }
}

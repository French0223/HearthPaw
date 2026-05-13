package com.example.hearthpaw.ui.ai;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.hearthpaw.BuildConfig;
import com.example.hearthpaw.R;
import com.example.hearthpaw.data.model.ChatMessage;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;

import java.util.List;

public class ChatActivity extends AppCompatActivity {
    private ChatViewModel viewModel;
    private ChatMessageAdapter adapter;
    private RecyclerView recyclerView;
    private TextInputEditText messageInput;
    private MaterialButton sendButton;
    private LinearProgressIndicator progressIndicator;
    private MaterialTextView statusText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        setContentView(R.layout.activity_chat_bantay);

        View chatRoot = findViewById(R.id.chat_root);
        if (chatRoot != null) {
            ViewCompat.setOnApplyWindowInsetsListener(chatRoot, (view, insets) -> {
                int bottomInset = Math.max(
                        insets.getInsets(WindowInsetsCompat.Type.ime()).bottom,
                        insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
                );
                view.setPadding(view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(), bottomInset);
                return insets;
            });
            ViewCompat.requestApplyInsets(chatRoot);
        }

        Toolbar toolbar = findViewById(R.id.toolbar_chat);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.rv_chat_messages);
        messageInput = findViewById(R.id.et_chat_message);
        sendButton = findViewById(R.id.btn_chat_send);
        progressIndicator = findViewById(R.id.progress_chat);
        statusText = findViewById(R.id.tv_chat_status);

        adapter = new ChatMessageAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        // Starter prompt chips
        Chip chip1 = findViewById(R.id.chip_prompt_1);
        Chip chip2 = findViewById(R.id.chip_prompt_2);
        Chip chip3 = findViewById(R.id.chip_prompt_3);
        View.OnClickListener promptClick = v -> {
            if (v instanceof Chip) {
                CharSequence text = ((Chip) v).getText();
                if (!TextUtils.isEmpty(text)) {
                    viewModel.sendMessage(text.toString());
                }
            }
        };
        if (chip1 != null) chip1.setOnClickListener(promptClick);
        if (chip2 != null) chip2.setOnClickListener(promptClick);
        if (chip3 != null) chip3.setOnClickListener(promptClick);

        viewModel.getMessages().observe(this, this::renderMessages);
        viewModel.isSending().observe(this, isSending -> {
            boolean busy = isSending != null && isSending;
            progressIndicator.setVisibility(busy ? View.VISIBLE : View.GONE);
            sendButton.setEnabled(!busy);
        });
        viewModel.getErrorMessage().observe(this, error -> {
            if (TextUtils.isEmpty(error)) {
                statusText.setVisibility(View.GONE);
            } else {
                statusText.setText(error);
                statusText.setVisibility(View.VISIBLE);
            }
        });

        if (TextUtils.isEmpty(BuildConfig.GEMINI_API_KEY)) {
            statusText.setText(R.string.bantay_missing_key);
            statusText.setVisibility(View.VISIBLE);
            sendButton.setEnabled(false);
        }

        sendButton.setOnClickListener(v -> sendCurrentMessage());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_clear_chat) {
            viewModel.clearHistory();
            Toast.makeText(this, R.string.bantay_new_chat_cleared, Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void renderMessages(List<ChatMessage> messages) {
        adapter.submitList(messages);
        recyclerView.post(() -> {
            int count = adapter.getItemCount();
            if (count > 0) {
                recyclerView.scrollToPosition(count - 1);
            }
        });
    }

    private void sendCurrentMessage() {
        String text = messageInput.getText() != null ? messageInput.getText().toString() : "";
        if (TextUtils.isEmpty(text.trim())) {
            return;
        }

        messageInput.setText("");
        viewModel.sendMessage(text);
    }
}
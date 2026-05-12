package com.example.hearthpaw.ui.ai;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hearthpaw.R;
import com.example.hearthpaw.data.model.ChatMessage;
import com.google.android.material.card.MaterialCardView;

import java.text.DateFormat;
import java.util.Date;

public class ChatMessageAdapter extends ListAdapter<ChatMessage, ChatMessageAdapter.MessageViewHolder> {
    public ChatMessageAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<ChatMessage> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ChatMessage>() {
                @Override
                public boolean areItemsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
                    return oldItem.getTimestamp() == newItem.getTimestamp()
                            && safeEquals(oldItem.getRole(), newItem.getRole())
                            && safeEquals(oldItem.getText(), newItem.getText());
                }
            };

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    private static boolean safeEquals(String first, String second) {
        if (first == null) {
            return second == null;
        }
        return first.equals(second);
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final TextView messageView;
        private final TextView timeView;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_message);
            messageView = itemView.findViewById(R.id.tv_message_body);
            timeView = itemView.findViewById(R.id.tv_message_time);
        }

        void bind(ChatMessage message) {
            boolean isUser = ChatMessage.ROLE_USER.equals(message.getRole());
            messageView.setText(message.getText());
            timeView.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(message.getTimestamp())));

            cardView.setCardBackgroundColor(itemView.getContext().getColor(
                    isUser ? R.color.hearthpaw_primary_container : R.color.hearthpaw_secondary_container));
            messageView.setTextColor(itemView.getContext().getColor(
                    isUser ? R.color.hearthpaw_on_primary_container : R.color.hearthpaw_on_secondary_container));
            timeView.setTextColor(itemView.getContext().getColor(R.color.hearthpaw_text_soft));

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) cardView.getLayoutParams();
            if (isUser) {
                params.gravity = Gravity.END;
                params.setMargins(64, 0, 0, 0);
            } else {
                params.gravity = Gravity.START;
                params.setMargins(0, 0, 64, 0);
            }
            cardView.setLayoutParams(params);
        }
    }
}
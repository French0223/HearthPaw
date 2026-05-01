package com.example.hearthpaw.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hearthpaw.R;
import com.example.hearthpaw.data.model.CareTask;

public class CareTaskAdapter extends ListAdapter<CareTask, CareTaskAdapter.TaskViewHolder> {

    public interface OnTaskActionListener {
        void onStatusChanged(CareTask task, boolean isCompleted);
        void onDeleteTask(CareTask task);
    }

    private final OnTaskActionListener listener;

    public CareTaskAdapter(OnTaskActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<CareTask> DIFF_CALLBACK = new DiffUtil.ItemCallback<CareTask>() {
        @Override
        public boolean areItemsTheSame(@NonNull CareTask oldItem, @NonNull CareTask newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull CareTask oldItem, @NonNull CareTask newItem) {
            return oldItem.isCompleted() == newItem.isCompleted() &&
                    oldItem.getTaskName().equals(newItem.getTaskName()) &&
                    oldItem.getTaskTime().equals(newItem.getTaskTime());
        }
    };

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_care_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName;
        private final TextView tvTime;
        private final CheckBox checkBox;
        private final ImageButton btnDelete;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_task_name);
            tvTime = itemView.findViewById(R.id.tv_task_time);
            checkBox = itemView.findViewById(R.id.cb_task_complete);
            btnDelete = itemView.findViewById(R.id.btn_delete_task);
        }

        void bind(CareTask task, OnTaskActionListener listener) {
            tvName.setText(task.getTaskName());
            tvTime.setText(task.getTaskTime());
            
            // Remove listener before setting checked state to avoid infinite loop
            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(task.isCompleted());
            
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                listener.onStatusChanged(task, isChecked);
            });

            btnDelete.setOnClickListener(v -> listener.onDeleteTask(task));
        }
    }
}

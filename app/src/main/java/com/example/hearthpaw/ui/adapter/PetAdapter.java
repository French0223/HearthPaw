package com.example.hearthpaw.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hearthpaw.R;
import com.example.hearthpaw.data.model.Pet;
import com.example.hearthpaw.util.PetImageUtils;

public class PetAdapter extends ListAdapter<Pet, PetAdapter.PetViewHolder> {

    public interface OnPetClickListener {
        void onPetClick(Pet pet);
    }

    private final OnPetClickListener listener;

    public PetAdapter(OnPetClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Pet> DIFF_CALLBACK = new DiffUtil.ItemCallback<Pet>() {
        @Override
        public boolean areItemsTheSame(@NonNull Pet oldItem, @NonNull Pet newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Pet oldItem, @NonNull Pet newItem) {
            return oldItem.getId() == newItem.getId()
                    && safeEquals(oldItem.getName(), newItem.getName())
                    && safeEquals(oldItem.getDescription(), newItem.getDescription())
                    && safeEquals(oldItem.getPhotoPath(), newItem.getPhotoPath())
                    && safeEquals(oldItem.getStatus(), newItem.getStatus())
                    && safeEquals(oldItem.getContactNumber(), newItem.getContactNumber())
                    && oldItem.getLatitude() == newItem.getLatitude()
                    && oldItem.getLongitude() == newItem.getLongitude()
                    && oldItem.getTimestamp() == newItem.getTimestamp();
        }

        private boolean safeEquals(String left, String right) {
            if (left == null) {
                return right == null;
            }
            return left.equals(right);
        }
    };

    @NonNull
    @Override
    public PetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pet, parent, false);
        return new PetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PetViewHolder holder, int position) {
        Pet pet = getItem(position);
        holder.bind(pet, listener);
    }

    static class PetViewHolder extends RecyclerView.ViewHolder {

        private final ImageView photoView;
        private final TextView nameView;
        private final ImageView statusIconView;
        private final TextView statusView;
        private final TextView descriptionView;

        PetViewHolder(@NonNull View itemView) {
            super(itemView);
            photoView = itemView.findViewById(R.id.item_pet_photo);
            nameView = itemView.findViewById(R.id.item_pet_name);
            statusIconView = itemView.findViewById(R.id.item_pet_status_icon);
            statusView = itemView.findViewById(R.id.item_pet_status);
            descriptionView = itemView.findViewById(R.id.item_pet_description);
        }

        void bind(Pet pet, OnPetClickListener listener) {
            nameView.setText(pet.getName());
            statusView.setText(getDisplayStatus(pet.getStatus()));
            descriptionView.setText(pet.getDescription());
            PetImageUtils.loadPhoto(pet.getPhotoPath(), photoView);

            if (isAdoptedStatus(pet.getStatus())) {
                statusIconView.setImageResource(R.drawable.ic_heart_adopted);
                nameView.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.status_adopted));
            } else {
                statusIconView.setImageResource(R.drawable.ic_heart_searching);
                nameView.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.status_searching));
            }
            
            itemView.setOnClickListener(v -> listener.onPetClick(pet));
        }

        private boolean isAdoptedStatus(String status) {
            if (status == null) {
                return false;
            }

            String normalizedStatus = status.trim().toLowerCase();
            return normalizedStatus.contains("adopt")
                    || normalizedStatus.contains("found a parent")
                    || normalizedStatus.contains("found a home");
        }

        private String getDisplayStatus(String status) {
            if (isAdoptedStatus(status)) {
                return itemView.getResources().getString(R.string.cute_status_adoptable);
            }

            return itemView.getResources().getString(R.string.cute_status_owner);
        }
    }
}
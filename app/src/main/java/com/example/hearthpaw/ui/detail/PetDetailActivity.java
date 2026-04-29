package com.example.hearthpaw.ui.detail;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.hearthpaw.R;
import com.example.hearthpaw.data.model.Pet;
import com.example.hearthpaw.ui.viewmodel.PetViewModel;
import com.example.hearthpaw.util.PetImageUtils;

public class PetDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PET_ID = "extra_pet_id";

    private ImageView petPhotoView;
    private TextView petNameView;
    private TextView petDescriptionView;
    private TextView petStatusView;
    private TextView petPhoneView;
    private Button callButton;
    private PetViewModel petViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pet_detail);

        petPhotoView = findViewById(R.id.detail_pet_photo);
        petNameView = findViewById(R.id.detail_pet_name);
        petDescriptionView = findViewById(R.id.detail_pet_description);
        petStatusView = findViewById(R.id.detail_pet_status);
        petPhoneView = findViewById(R.id.detail_pet_phone);
        callButton = findViewById(R.id.btn_call_pet);

        petViewModel = new ViewModelProvider(this).get(PetViewModel.class);

        int petId = getIntent().getIntExtra(EXTRA_PET_ID, -1);
        if (petId == -1) {
            Toast.makeText(this, "Pet not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        petViewModel.getPetById(petId).observe(this, pet -> {
            if (pet == null) {
                return;
            }
            bindPet(pet);
        });

        callButton.setOnClickListener(v -> {
            String phoneNumber = petPhoneView.getText().toString().trim();
            if (phoneNumber.isEmpty()) {
                Toast.makeText(this, "No contact number available", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneNumber));
            startActivity(dialIntent);
        });
    }

    private void bindPet(Pet pet) {
        petNameView.setText(pet.getName());
        petDescriptionView.setText(pet.getDescription());
        petStatusView.setText(pet.getStatus());
        petPhoneView.setText(pet.getContactNumber());
        PetImageUtils.loadPhoto(pet.getPhotoPath(), petPhotoView);
        setTitle(pet.getName().isEmpty() ? getString(R.string.detail_title) : pet.getName());
    }
}
package com.example.hearthpaw.ui.main;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.hearthpaw.R;
import com.example.hearthpaw.data.model.Pet;
import com.example.hearthpaw.ui.viewmodel.PetViewModel;
import com.google.android.material.textfield.TextInputEditText;

public class AddPetActivity extends AppCompatActivity {

    private TextInputEditText etName, etDescription, etPhone;
    private Button btnSave;
    private ImageView ivPhoto;
    private PetViewModel petViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_pet);

        // Initialize Views
        etName = findViewById(R.id.et_pet_name);
        etDescription = findViewById(R.id.et_pet_description);
        etPhone = findViewById(R.id.et_pet_phone);
        btnSave = findViewById(R.id.btn_save_pet);
        ivPhoto = findViewById(R.id.iv_pet_photo);

        // Initialize ViewModel
        petViewModel = new ViewModelProvider(this).get(PetViewModel.class);

        btnSave.setOnClickListener(v -> savePet());
    }

    private void savePet() {
        String name = etName.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (name.isEmpty() || description.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // placeholder lng muna for photo and loc
        Pet newPet = new Pet(
                name,
                description,
                "placeholder_path", 
                "Searching for Owner",
                0.0, // Default Latitude
                0.0, // Default Longitude
                phone
        );

        petViewModel.insert(newPet);
        Toast.makeText(this, "Pet registered successfully!", Toast.LENGTH_SHORT).show();
        finish(); // Close activity after saving
    }
}

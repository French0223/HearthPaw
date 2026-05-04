package com.example.hearthpaw.ui.main;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;

import com.example.hearthpaw.R;
import com.example.hearthpaw.data.model.Pet;
import com.example.hearthpaw.ui.viewmodel.PetViewModel;
import com.example.hearthpaw.util.PetImageUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class AddPetActivity extends AppCompatActivity {

    private TextInputEditText etName, etDescription, etPhone;
    private AutoCompleteTextView etSpecies;
    private Button btnSave;
    private MaterialButton btnTakePhoto;
    private MaterialButton btnUpdateLocation;
    private ImageView ivPhoto;
    private ChipGroup chipGroupGender, chipGroupAge, chipGroupHealth;
    private TextView tvMapCoordinates;
    
    private PetViewModel petViewModel;
    private ActivityResultLauncher<Intent> takePhotoLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    
    private String currentPhotoPath;
    private String selectedSpecies = "";
    private String selectedGender = "";
    private String selectedAge = "";
    private String selectedHealth = "";
    
    private FusedLocationProviderClient fusedLocationClient;
    private double currentLat = 0.0;
    private double currentLng = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_pet);

        // Initialize Views
        etName = findViewById(R.id.et_pet_name);
        etDescription = findViewById(R.id.et_pet_description);
        etPhone = findViewById(R.id.et_pet_phone);
        etSpecies = findViewById(R.id.et_species);
        btnSave = findViewById(R.id.btn_save_pet);
        btnTakePhoto = findViewById(R.id.btn_take_photo);
        btnUpdateLocation = findViewById(R.id.btn_update_location);
        ivPhoto = findViewById(R.id.iv_pet_photo);
        chipGroupGender = findViewById(R.id.chip_group_gender);
        chipGroupAge = findViewById(R.id.chip_group_age);
        chipGroupHealth = findViewById(R.id.chip_group_health);
        tvMapCoordinates = findViewById(R.id.tv_map_coordinates);

        // Initialize ViewModel and Location Client
        petViewModel = new ViewModelProvider(this).get(PetViewModel.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupLaunchers();
        setupSpeciesDropdown();
        setupChipListeners();

        // Click Listeners
        btnTakePhoto.setOnClickListener(v -> capturePhoto());
        btnTakePhoto.setOnLongClickListener(v -> {
            pickImageLauncher.launch("image/*");
            return true;
        });
        btnUpdateLocation.setOnClickListener(v -> getLastKnownLocation());
        btnSave.setOnClickListener(v -> requestLocationAndSave());
        
        // Request location permission early
        requestLocationPermission();
    }

    private void setupSpeciesDropdown() {
        try {
            List<String> species = new ArrayList<>();
            species.add(getString(R.string.species_dog));
            species.add(getString(R.string.species_cat));
            species.add(getString(R.string.species_rabbit));
            species.add(getString(R.string.species_bird));
            species.add(getString(R.string.species_other));

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, species);
            etSpecies.setAdapter(adapter);

            etSpecies.setOnItemClickListener((parent, view, position, id) -> {
                selectedSpecies = (String) parent.getItemAtPosition(position);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupChipListeners() {
        try {
            // Gender chips
            chipGroupGender.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.isEmpty()) {
                    selectedGender = "";
                } else {
                    int chipId = checkedIds.get(0);
                    selectedGender = getChipText(chipId);
                }
            });

            // Age chips
            chipGroupAge.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.isEmpty()) {
                    selectedAge = "";
                } else {
                    int chipId = checkedIds.get(0);
                    selectedAge = getChipText(chipId);
                }
            });

            // Health status chips
            chipGroupHealth.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.isEmpty()) {
                    selectedHealth = "";
                } else {
                    int chipId = checkedIds.get(0);
                    selectedHealth = getChipText(chipId);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getChipText(int chipId) {
        try {
            return findViewById(chipId).getContentDescription().toString();
        } catch (Exception e) {
            // Fallback for older approach
            return "";
        }
    }

    private void setupLaunchers() {
        takePhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && currentPhotoPath != null) {
                        PetImageUtils.loadPhoto(currentPhotoPath, ivPhoto);
                    } else if (currentPhotoPath != null) {
                        deleteTempFile(currentPhotoPath);
                        currentPhotoPath = null;
                    }
                }
        );

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        try {
                            currentPhotoPath = saveImageToInternalStorage(uri);
                            PetImageUtils.loadPhoto(currentPhotoPath, ivPhoto);
                        } catch (IOException e) {
                            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                    if (fineLocationGranted || coarseLocationGranted) {
                        getLastKnownLocation();
                    }
                }
        );
    }

    private void requestLocationPermission() {
        try {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getLastKnownLocation() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    currentLat = location.getLatitude();
                    currentLng = location.getLongitude();
                    updateCoordinatesDisplay();
                    Toast.makeText(this, "Location updated", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Error getting location", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void updateCoordinatesDisplay() {
        try {
            if (tvMapCoordinates != null) {
                String coordinates = String.format("Lat: %.4f, Lng: %.4f", currentLat, currentLng);
                tvMapCoordinates.setText(coordinates);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void requestLocationAndSave() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                    if (location != null) {
                        currentLat = location.getLatitude();
                        currentLng = location.getLongitude();
                    }
                    savePet();
                }).addOnFailureListener(e -> savePet());
            } else {
                savePet();
            }
        } catch (Exception e) {
            savePet();
        }
    }

    private void capturePhoto() {
        try {
            Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            File photoFile = createImageFile();
            Uri photoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            captureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            currentPhotoPath = photoFile.getAbsolutePath();
            takePhotoLauncher.launch(captureIntent);
        } catch (IOException e) {
            currentPhotoPath = null;
            Toast.makeText(this, "Error creating file", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private File createImageFile() throws IOException {
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        String imageFileName = "HearthPaw_" + System.currentTimeMillis();
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private String saveImageToInternalStorage(Uri uri) throws IOException {
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File file = new File(storageDir, "HearthPaw_Gallery_" + System.currentTimeMillis() + ".jpg");
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             OutputStream outputStream = new FileOutputStream(file)) {
            if (inputStream == null) throw new IOException("Failed to open input stream");
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        }
        return file.getAbsolutePath();
    }

    private void deleteTempFile(String path) {
        try {
            File file = new File(path);
            if (file.exists()) file.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void savePet() {
        try {
            String name = etName.getText().toString().trim();
            String description = etDescription.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();

            if (name.isEmpty() || description.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            Pet newPet = new Pet(
                    name,
                    description,
                    currentPhotoPath != null ? currentPhotoPath : "",
                    getString(R.string.cute_status_owner),
                    currentLat,
                    currentLng,
                    phone
            );

            newPet.setSpecies(selectedSpecies);
            newPet.setGender(selectedGender);
            newPet.setAge(selectedAge);
            newPet.setHealthStatus(selectedHealth);

            petViewModel.insert(newPet);
            Toast.makeText(this, "Pet registered successfully!", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Error saving pet", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}

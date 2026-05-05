package com.example.hearthpaw.ui.main;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
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
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.gms.tasks.CancellationTokenSource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AddPetActivity extends AppCompatActivity {

    private TextInputEditText etName, etDescription, etPhone;
    private AutoCompleteTextView etSpecies;
    private TextInputLayout tilName, tilDescription, tilPhone, tilSpecies;
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
    private boolean hasLocation = false;
    private String currentPlaceName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_pet);

        // Initialize Views
        tilName = findViewById(R.id.til_name);
        tilDescription = findViewById(R.id.til_description);
        tilPhone = findViewById(R.id.til_phone);
        tilSpecies = findViewById(R.id.til_species);
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
        btnUpdateLocation.setOnClickListener(v -> requestFreshLocation(true, null));
        btnSave.setOnClickListener(v -> {
            if (!validateForm()) {
                return;
            }
            requestLocationAndSave();
        });

        if (btnUpdateLocation != null) {
            btnUpdateLocation.setText(R.string.location_button_get_current);
        }
        setLocationUnavailableState();
        
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
        Chip chip = findViewById(chipId);
        return chip != null ? chip.getText().toString().trim() : "";
    }

    private boolean isPhoneValid(String phone) {
        String normalized = phone.replaceAll("[\\s\\-()]", "").trim();
        if (normalized.startsWith("+")) {
            normalized = normalized.substring(1);
        }
        return normalized.matches("\\d{7,15}");
    }

    private boolean hasAtLeastOneDetailChip() {
        return !TextUtils.isEmpty(selectedGender)
                || !TextUtils.isEmpty(selectedAge)
                || !TextUtils.isEmpty(selectedHealth);
    }

    private void clearFormErrors() {
        if (tilName != null) tilName.setError(null);
        if (tilDescription != null) tilDescription.setError(null);
        if (tilPhone != null) tilPhone.setError(null);
        if (tilSpecies != null) tilSpecies.setError(null);
    }

    private boolean validateForm() {
        clearFormErrors();

        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";
        String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
        String species = etSpecies.getText() != null ? etSpecies.getText().toString().trim() : "";

        boolean isValid = true;

        if (name.isEmpty()) {
            if (tilName != null) tilName.setError(getString(R.string.error_name_required));
            isValid = false;
        }

        if (description.isEmpty()) {
            if (tilDescription != null) tilDescription.setError(getString(R.string.error_description_required));
            isValid = false;
        }

        if (phone.isEmpty()) {
            if (tilPhone != null) tilPhone.setError(getString(R.string.error_phone_required));
            isValid = false;
        } else if (!isPhoneValid(phone)) {
            if (tilPhone != null) tilPhone.setError(getString(R.string.error_phone_invalid));
            isValid = false;
        }

        if (species.isEmpty()) {
            if (tilSpecies != null) tilSpecies.setError(getString(R.string.error_species_required));
            isValid = false;
        } else {
            selectedSpecies = species;
        }

        if (!hasAtLeastOneDetailChip()) {
            Toast.makeText(this, getString(R.string.error_chip_required), Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        return isValid;
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
                        requestFreshLocation(false, null);
                    } else if (!hasLocation) {
                        setLocationUnavailableState();
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

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void setLocatingState() {
        if (tvMapCoordinates != null) {
            tvMapCoordinates.setText(R.string.location_state_locating);
        }
        if (btnUpdateLocation != null) {
            btnUpdateLocation.setEnabled(false);
            btnUpdateLocation.setText(R.string.location_button_locating);
        }
    }

    private void setLocationUnavailableState() {
        if (tvMapCoordinates != null) {
            tvMapCoordinates.setText(R.string.location_state_unavailable);
        }
        if (btnUpdateLocation != null) {
            btnUpdateLocation.setEnabled(true);
            btnUpdateLocation.setText(R.string.location_button_get_current);
        }
    }

    private void applyLocation(Location location) {
        currentLat = location.getLatitude();
        currentLng = location.getLongitude();
        hasLocation = true;
        resolvePlaceName(location);
        if (btnUpdateLocation != null) {
            btnUpdateLocation.setEnabled(true);
            btnUpdateLocation.setText(R.string.location_button_get_current);
        }
    }

    private void resolvePlaceName(Location location) {
        currentPlaceName = "";

        if (!Geocoder.isPresent()) {
            updateCoordinatesDisplay();
            return;
        }

        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                java.util.List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);

                    if (address.getLocality() != null && !address.getLocality().trim().isEmpty()) {
                        currentPlaceName = address.getLocality().trim();
                    } else if (address.getSubAdminArea() != null && !address.getSubAdminArea().trim().isEmpty()) {
                        currentPlaceName = address.getSubAdminArea().trim();
                    } else if (address.getAdminArea() != null && !address.getAdminArea().trim().isEmpty()) {
                        currentPlaceName = address.getAdminArea().trim();
                    } else if (address.getCountryName() != null && !address.getCountryName().trim().isEmpty()) {
                        currentPlaceName = address.getCountryName().trim();
                    }
                }
            } catch (Exception ignored) {
                currentPlaceName = "";
            }

            runOnUiThread(this::updateCoordinatesDisplay);
        }).start();
    }

    @android.annotation.SuppressLint("MissingPermission")
    private void requestFreshLocation(boolean showUserFeedback, Runnable onComplete) {
        try {
            if (!hasLocationPermission()) {
                if (showUserFeedback) {
                    requestLocationPermission();
                } else if (!hasLocation) {
                    setLocationUnavailableState();
                }
                if (onComplete != null) {
                    onComplete.run();
                }
                return;
            }

            setLocatingState();
            CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
            fusedLocationClient
                    .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.getToken())
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            applyLocation(location);
                            if (showUserFeedback) {
                                Toast.makeText(this, getString(R.string.location_updated), Toast.LENGTH_SHORT).show();
                            }
                            if (onComplete != null) {
                                onComplete.run();
                            }
                        } else {
                            fallbackToLastKnownLocation(showUserFeedback, onComplete);
                        }
                    })
                    .addOnFailureListener(this, e -> fallbackToLastKnownLocation(showUserFeedback, onComplete));
        } catch (Exception e) {
            fallbackToLastKnownLocation(showUserFeedback, onComplete);
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    private void fallbackToLastKnownLocation(boolean showUserFeedback, Runnable onComplete) {
        try {
            if (!hasLocationPermission()) {
                if (!hasLocation) {
                    setLocationUnavailableState();
                }
                if (onComplete != null) {
                    onComplete.run();
                }
                return;
            }

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            applyLocation(location);
                            if (showUserFeedback) {
                                Toast.makeText(this, getString(R.string.location_updated), Toast.LENGTH_SHORT).show();
                            }
                        } else if (!hasLocation) {
                            setLocationUnavailableState();
                            if (showUserFeedback) {
                                Toast.makeText(this, getString(R.string.location_unavailable_toast), Toast.LENGTH_SHORT).show();
                            }
                        }

                        if (onComplete != null) {
                            onComplete.run();
                        }
                    })
                    .addOnFailureListener(this, e -> {
                        if (!hasLocation) {
                            setLocationUnavailableState();
                            if (showUserFeedback) {
                                Toast.makeText(this, getString(R.string.location_unavailable_toast), Toast.LENGTH_SHORT).show();
                            }
                        }

                        if (onComplete != null) {
                            onComplete.run();
                        }
                    });
        } catch (Exception e) {
            if (!hasLocation) {
                setLocationUnavailableState();
            }
            if (onComplete != null) {
                onComplete.run();
            }
        }
    }

    private void updateCoordinatesDisplay() {
        try {
            if (tvMapCoordinates != null) {
                if (!currentPlaceName.isEmpty()) {
                    tvMapCoordinates.setText(getString(R.string.location_place_name, currentPlaceName));
                } else {
                    String coordinates = String.format(Locale.US, "Lat: %.4f, Lng: %.4f", currentLat, currentLng);
                    tvMapCoordinates.setText(coordinates);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void requestLocationAndSave() {
        requestFreshLocation(false, () -> savePet(!hasLocation));
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

    private void savePet(boolean savedWithoutLocation) {
        try {
            String name = etName.getText().toString().trim();
            String description = etDescription.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();

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
            if (savedWithoutLocation) {
                Toast.makeText(this, getString(R.string.location_saved_without), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.pet_registered_success), Toast.LENGTH_SHORT).show();
            }
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Error saving pet", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}

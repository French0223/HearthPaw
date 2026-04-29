package com.example.hearthpaw.ui.main;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;

import com.example.hearthpaw.R;
import com.example.hearthpaw.data.model.Pet;
import com.example.hearthpaw.util.PetImageUtils;
import com.example.hearthpaw.ui.viewmodel.PetViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AddPetActivity extends AppCompatActivity {

    private TextInputEditText etName, etDescription, etPhone;
    private Button btnSave;
    private MaterialButton btnTakePhoto;
    private ImageView ivPhoto;
    private PetViewModel petViewModel;
    private ActivityResultLauncher<Intent> takePhotoLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;
    private String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_pet);

        // Initialize Views
        etName = findViewById(R.id.et_pet_name);
        etDescription = findViewById(R.id.et_pet_description);
        etPhone = findViewById(R.id.et_pet_phone);
        btnSave = findViewById(R.id.btn_save_pet);
        btnTakePhoto = findViewById(R.id.btn_take_photo);
        ivPhoto = findViewById(R.id.iv_pet_photo);

        // Initialize ViewModel
        petViewModel = new ViewModelProvider(this).get(PetViewModel.class);

        // Launcher for Camera
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

        // Launcher for Gallery
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        try {
                            currentPhotoPath = saveImageToInternalStorage(uri);
                            PetImageUtils.loadPhoto(currentPhotoPath, ivPhoto);
                        } catch (IOException e) {
                            Toast.makeText(this, "Failed to load image from gallery", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        // NORMAL CLICK for Camera, LONG CLICK for Gallery
        btnTakePhoto.setOnClickListener(v -> capturePhoto());
        btnTakePhoto.setOnLongClickListener(v -> {
            pickImageLauncher.launch("image/*");
            return true;
        });

        btnSave.setOnClickListener(v -> savePet());
        
        // Let the user know about the gallery option
        Toast.makeText(this, "Tip: Long-press 'CAPTURE PHOTO' to pick from gallery", Toast.LENGTH_LONG).show();
    }

    private void capturePhoto() {
        Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            File photoFile = createImageFile();
            Uri photoUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    photoFile
            );
            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            captureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            currentPhotoPath = photoFile.getAbsolutePath();
            takePhotoLauncher.launch(captureIntent);
        } catch (IOException e) {
            currentPhotoPath = null;
            Toast.makeText(this, "Unable to create photo file", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        String imageFileName = "HearthPaw_" + System.currentTimeMillis();
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private String saveImageToInternalStorage(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        if (inputStream == null) throw new IOException("InputStream is null");
        
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File file = new File(storageDir, "HearthPaw_Gallery_" + System.currentTimeMillis() + ".jpg");
        
        try (InputStream input = inputStream; OutputStream outputStream = new FileOutputStream(file)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = input.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        }
        return file.getAbsolutePath();
    }

    private void deleteTempFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
    }

    private void savePet() {
        String name = etName.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (name.isEmpty() || description.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Pet newPet = new Pet(
                name,
                description,
                currentPhotoPath != null ? currentPhotoPath : "",
                getString(R.string.cute_status_owner),
                0.0,
                0.0,
                phone
        );

        petViewModel.insert(newPet);
        Toast.makeText(this, "Pet registered successfully!", Toast.LENGTH_SHORT).show();
        finish();
    }
}

package com.example.hearthpaw.ui.detail;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hearthpaw.R;
import com.example.hearthpaw.data.model.CareTask;
import com.example.hearthpaw.data.model.Pet;
import com.example.hearthpaw.ui.adapter.CareTaskAdapter;
import com.example.hearthpaw.ui.viewmodel.PetViewModel;
import com.example.hearthpaw.util.CareNotificationHelper;
import com.example.hearthpaw.util.PetImageUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class PetDetailActivity extends AppCompatActivity implements CareTaskAdapter.OnTaskActionListener {

    public static final String EXTRA_PET_ID = "extra_pet_id";

    private ImageView petPhotoView;
    private TextView petNameView, petDescriptionView, petPhoneView, tvEmptyCareLog;
    private MaterialButton btnCall, btnStatusToggle, btnAddTask;
    private PetViewModel petViewModel;
    private RecyclerView rvCareTasks;
    private CareTaskAdapter careTaskAdapter;
    private Pet currentPet;
    
    private int selectedHour = -1;
    private int selectedMinute = -1;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pet_detail);

        CareNotificationHelper.createNotificationChannel(this);
        checkNotificationPermission();

        Toolbar toolbar = findViewById(R.id.toolbar_detail);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        petPhotoView = findViewById(R.id.detail_pet_photo);
        petNameView = findViewById(R.id.detail_pet_name);
        petDescriptionView = findViewById(R.id.detail_pet_description);
        petPhoneView = findViewById(R.id.detail_pet_phone);
        btnCall = findViewById(R.id.btn_call_pet);
        btnStatusToggle = findViewById(R.id.btn_status_toggle);
        btnAddTask = findViewById(R.id.btn_add_task);
        rvCareTasks = findViewById(R.id.rv_care_tasks);
        tvEmptyCareLog = findViewById(R.id.tv_empty_care_log);

        setupRecyclerView();

        petViewModel = new ViewModelProvider(this).get(PetViewModel.class);

        int petId = getIntent().getIntExtra(EXTRA_PET_ID, -1);
        if (petId == -1) {
            Toast.makeText(this, "Pet not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        petViewModel.getPetById(petId).observe(this, pet -> {
            if (pet != null) {
                currentPet = pet;
                bindPet(pet);
            }
        });

        petViewModel.getTasksForPet(petId).observe(this, tasks -> {
            if (tasks == null || tasks.isEmpty()) {
                tvEmptyCareLog.setVisibility(View.VISIBLE);
                rvCareTasks.setVisibility(View.GONE);
            } else {
                tvEmptyCareLog.setVisibility(View.GONE);
                rvCareTasks.setVisibility(View.VISIBLE);
                careTaskAdapter.submitList(tasks);
            }
        });

        btnCall.setOnClickListener(v -> makeCall());
        btnStatusToggle.setOnClickListener(v -> toggleStatus());
        btnAddTask.setOnClickListener(v -> showAddTaskDialog());
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void setupRecyclerView() {
        careTaskAdapter = new CareTaskAdapter(this);
        rvCareTasks.setLayoutManager(new LinearLayoutManager(this));
        rvCareTasks.setAdapter(careTaskAdapter);
    }

    private void bindPet(Pet pet) {
        petNameView.setText(pet.getName());
        petDescriptionView.setText(pet.getDescription());
        petPhoneView.setText(pet.getContactNumber());
        btnStatusToggle.setText(pet.getStatus());
        PetImageUtils.loadPhoto(pet.getPhotoPath(), petPhotoView);
    }

    private void makeCall() {
        if (currentPet != null && !currentPet.getContactNumber().isEmpty()) {
            Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + currentPet.getContactNumber()));
            startActivity(dialIntent);
        } else {
            Toast.makeText(this, "No contact number available", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleStatus() {
        if (currentPet == null) return;

        List<String> statuses = Arrays.asList("Found", "Searching for Owner", "Adoptable");
        int currentIndex = statuses.indexOf(currentPet.getStatus());
        int nextIndex = (currentIndex + 1) % statuses.size();
        
        currentPet.setStatus(statuses.get(nextIndex));
        petViewModel.update(currentPet);
        Toast.makeText(this, "Status updated to: " + currentPet.getStatus(), Toast.LENGTH_SHORT).show();
    }

    private void showAddTaskDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null);
        TextInputEditText etTaskName = dialogView.findViewById(R.id.et_task_name);
        TextInputEditText etTaskTime = dialogView.findViewById(R.id.et_task_time);

        selectedHour = -1;
        selectedMinute = -1;

        etTaskTime.setOnClickListener(v -> {
            MaterialTimePicker picker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_12H)
                    .setHour(12)
                    .setMinute(0)
                    .setTitleText("Select Task Time")
                    .build();

            picker.addOnPositiveButtonClickListener(view -> {
                selectedHour = picker.getHour();
                selectedMinute = picker.getMinute();
                String time = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);
                etTaskTime.setText(time);
            });

            picker.show(getSupportFragmentManager(), "TIME_PICKER");
        });

        new AlertDialog.Builder(this)
                .setTitle("New Care Task")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = etTaskName.getText().toString().trim();
                    if (!name.isEmpty() && selectedHour != -1) {
                        String timeStr = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);
                        petViewModel.insertTask(new CareTask(currentPet.getId(), name, timeStr));
                        
                        // Schedule notification
                        CareNotificationHelper.scheduleNotification(this, currentPet.getName(), name, selectedHour, selectedMinute);
                        Toast.makeText(this, "Task added and reminder set!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onStatusChanged(CareTask task, boolean isCompleted) {
        task.setCompleted(isCompleted);
        petViewModel.updateTask(task);
    }

    @Override
    public void onDeleteTask(CareTask task) {
        petViewModel.deleteTask(task);
    }
}

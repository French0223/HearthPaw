package com.example.hearthpaw;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hearthpaw.ui.main.AddPetActivity;
import com.example.hearthpaw.ui.viewmodel.PetViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    private PetViewModel petViewModel;
    private RecyclerView recyclerView;
    private TextView tvEmptyMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Views
        recyclerView = findViewById(R.id.rv_pets);
        tvEmptyMessage = findViewById(R.id.tv_empty_message);
        FloatingActionButton fab = findViewById(R.id.fab_add_pet);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        // Initialize ViewModel
        petViewModel = new ViewModelProvider(this).get(PetViewModel.class);

        // Observe the LiveData from ViewModel
        petViewModel.getAllPets().observe(this, pets -> {
            if (pets == null || pets.isEmpty()) {
                tvEmptyMessage.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                tvEmptyMessage.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                // TODO: Update RecyclerView Adapter here in the next step
            }
        });

        // Set FAB click listener
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddPetActivity.class);
            startActivity(intent);
        });
    }
}

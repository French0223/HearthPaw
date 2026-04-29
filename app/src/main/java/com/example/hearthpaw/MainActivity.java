package com.example.hearthpaw;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hearthpaw.ui.main.AddPetActivity;
import com.example.hearthpaw.ui.adapter.PetAdapter;
import com.example.hearthpaw.ui.detail.PetDetailActivity;
import com.example.hearthpaw.ui.viewmodel.PetViewModel;

public class MainActivity extends AppCompatActivity {

    private PetViewModel petViewModel;
    private RecyclerView recyclerView;
    private LinearLayout llEmptyState;
    private PetAdapter petAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Views
        recyclerView = findViewById(R.id.rv_pets);
        llEmptyState = findViewById(R.id.ll_empty_state);
        ImageButton fab = findViewById(R.id.fab_add_pet);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        petAdapter = new PetAdapter(pet -> {
            Intent intent = new Intent(MainActivity.this, PetDetailActivity.class);
            intent.putExtra(PetDetailActivity.EXTRA_PET_ID, pet.getId());
            startActivity(intent);
        });
        recyclerView.setAdapter(petAdapter);

        // Initialize ViewModel
        petViewModel = new ViewModelProvider(this).get(PetViewModel.class);

        // Observe the LiveData from ViewModel
        petViewModel.getAllPets().observe(this, pets -> {
            if (pets == null || pets.isEmpty()) {
                llEmptyState.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                llEmptyState.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                petAdapter.submitList(pets);
            }
        });

        // Set FAB click listener
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddPetActivity.class);
            startActivity(intent);
        });
    }
}

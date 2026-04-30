package com.example.hearthpaw;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hearthpaw.data.model.Pet;
import com.example.hearthpaw.ui.main.AddPetActivity;
import com.example.hearthpaw.ui.adapter.PetAdapter;
import com.example.hearthpaw.ui.detail.PetDetailActivity;
import com.example.hearthpaw.ui.viewmodel.PetViewModel;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private PetViewModel petViewModel;
    private RecyclerView recyclerView;
    private View mapContainer;
    private LinearLayout llEmptyState;
    private PetAdapter petAdapter;
    private GoogleMap googleMap;
    private boolean isMapView = false;
    private ImageButton btnToggleView;

    private final ActivityResultLauncher<String[]> locationPermissionRequest =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                if ((fineLocationGranted != null && fineLocationGranted) || 
                    (coarseLocationGranted != null && coarseLocationGranted)) {
                    enableMyLocation();
                } else {
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Views
        recyclerView = findViewById(R.id.rv_pets);
        mapContainer = findViewById(R.id.map_container);
        llEmptyState = findViewById(R.id.ll_empty_state);
        btnToggleView = findViewById(R.id.btn_toggle_view);
        ImageButton fab = findViewById(R.id.fab_add_pet);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        petAdapter = new PetAdapter(pet -> {
            Intent intent = new Intent(MainActivity.this, PetDetailActivity.class);
            intent.putExtra(PetDetailActivity.EXTRA_PET_ID, pet.getId());
            startActivity(intent);
        });
        recyclerView.setAdapter(petAdapter);

        // Initialize Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_container);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Initialize ViewModel
        petViewModel = new ViewModelProvider(this).get(PetViewModel.class);

        // Observe the LiveData from ViewModel
        petViewModel.getAllPets().observe(this, this::updateUI);

        // Toggle View Listener
        btnToggleView.setOnClickListener(v -> toggleView());

        // Set FAB click listener
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddPetActivity.class);
            startActivity(intent);
        });
    }

    private void updateUI(List<Pet> pets) {
        // Handle Empty State visibility
        if (pets == null || pets.isEmpty()) {
            // Show empty state only if we are NOT in Map View
            llEmptyState.setVisibility(isMapView ? View.GONE : View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            llEmptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(isMapView ? View.GONE : View.VISIBLE);
            petAdapter.submitList(pets);
        }

        // Always handle map visibility based on toggle
        mapContainer.setVisibility(isMapView ? View.VISIBLE : View.GONE);
        
        if (pets != null) {
            updateMapMarkers(pets);
        }
    }

    private void toggleView() {
        isMapView = !isMapView;
        btnToggleView.setImageResource(isMapView ? android.R.drawable.ic_menu_sort_by_size : android.R.drawable.ic_dialog_map);
        updateUI(petViewModel.getAllPets().getValue());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;
        enableMyLocation();
        
        // 1. Handle Info Window Clicks (leads to details)
        googleMap.setOnInfoWindowClickListener(marker -> {
            Pet pet = (Pet) marker.getTag();
            if (pet != null) {
                Intent intent = new Intent(MainActivity.this, PetDetailActivity.class);
                intent.putExtra(PetDetailActivity.EXTRA_PET_ID, pet.getId());
                startActivity(intent);
            }
        });

        updateMapMarkers(petViewModel.getAllPets().getValue());
        
        // Default camera position (e.g., center of PH)
        LatLng manila = new LatLng(14.5995, 120.9842);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(manila, 10));
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (googleMap != null) {
                googleMap.setMyLocationEnabled(true);
            }
        } else {
            locationPermissionRequest.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void updateMapMarkers(List<Pet> pets) {
        if (googleMap == null || pets == null) return;

        googleMap.clear();
        if (pets.isEmpty()) return;

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        boolean hasMarkers = false;

        for (Pet pet : pets) {
            if (pet.getLatitude() != 0 || pet.getLongitude() != 0) {
                LatLng position = new LatLng(pet.getLatitude(), pet.getLongitude());
                Marker marker = googleMap.addMarker(new MarkerOptions()
                        .position(position)
                        .title(pet.getName())
                        .snippet("Status: " + pet.getStatus() + "\nTap to view details"));
                
                if (marker != null) {
                    marker.setTag(pet);
                }
                builder.include(position);
                hasMarkers = true;
            }
        }

        // 3. Auto-zoom to fit all markers
        if (hasMarkers && isMapView) {
            try {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
            } catch (IllegalStateException e) {
                // If map layout isn't ready, it will use default zoom
            }
        }
    }
}

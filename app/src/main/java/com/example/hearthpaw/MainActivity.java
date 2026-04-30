package com.example.hearthpaw;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hearthpaw.data.model.Pet;
import com.example.hearthpaw.ui.main.AddPetActivity;
import com.example.hearthpaw.ui.adapter.PetAdapter;
import com.example.hearthpaw.ui.detail.PetDetailActivity;
import com.example.hearthpaw.ui.viewmodel.PetViewModel;
import com.example.hearthpaw.util.BitmapUtils;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private PetViewModel petViewModel;
    private RecyclerView recyclerView;
    private View mapContainer;
    private LinearLayout llEmptyState;
    private PetAdapter petAdapter;
    private GoogleMap googleMap;
    private boolean isMapView = false;
    private ImageButton btnToggleView;
    private TextView tvTotalRescues, tvActiveFound;

    private List<Pet> allPetsList = new ArrayList<>();
    private String currentSearchQuery = "";

    private final ActivityResultLauncher<String[]> locationPermissionRequest =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                if ((fineLocationGranted != null && fineLocationGranted) || 
                    (coarseLocationGranted != null && coarseLocationGranted)) {
                    checkLocationSettings();
                } else {
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<IntentSenderRequest> locationSettingsLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    enableMyLocation();
                } else {
                    Toast.makeText(this, "Location settings not enabled", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize Views
        recyclerView = findViewById(R.id.rv_pets);
        mapContainer = findViewById(R.id.map_container);
        llEmptyState = findViewById(R.id.ll_empty_state);
        btnToggleView = findViewById(R.id.btn_toggle_view);
        tvTotalRescues = findViewById(R.id.tv_total_rescues);
        tvActiveFound = findViewById(R.id.tv_active_searches);
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
        petViewModel.getAllPets().observe(this, pets -> {
            this.allPetsList = pets != null ? pets : new ArrayList<>();
            updateDashboard(allPetsList);
            filterPets(currentSearchQuery);
        });

        // Toggle View Listener
        btnToggleView.setOnClickListener(v -> toggleView());

        // Set FAB click listener
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddPetActivity.class);
            startActivity(intent);
        });
    }

    private void updateDashboard(List<Pet> pets) {
        if (tvTotalRescues == null || tvActiveFound == null) return;
        
        int total = pets.size();
        long activeFound = pets.stream()
                .filter(pet -> pet.getStatus() != null && pet.getStatus().equalsIgnoreCase("Found"))
                .count();

        tvTotalRescues.setText(String.valueOf(total));
        tvActiveFound.setText(String.valueOf(activeFound));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        
        // Ensure SearchView updates local state correctly
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentSearchQuery = newText;
                filterPets(newText);
                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem mapTypeItem = menu.findItem(R.id.action_map_type);
        if (mapTypeItem != null) {
            mapTypeItem.setVisible(isMapView);
            if (googleMap != null) {
                if (googleMap.getMapType() == GoogleMap.MAP_TYPE_SATELLITE) {
                    mapTypeItem.setTitle("Standard View");
                } else {
                    mapTypeItem.setTitle("Satellite View");
                }
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_map_type) {
            toggleMapType();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleMapType() {
        if (googleMap == null) return;
        
        if (googleMap.getMapType() == GoogleMap.MAP_TYPE_NORMAL) {
            googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
            Toast.makeText(this, "Switched to Satellite View", Toast.LENGTH_SHORT).show();
        } else {
            googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            Toast.makeText(this, "Switched to Standard View", Toast.LENGTH_SHORT).show();
        }
        invalidateOptionsMenu();
    }

    private void filterPets(String query) {
        List<Pet> filteredList;
        if (query == null || query.isEmpty()) {
            filteredList = allPetsList;
        } else {
            String lowerCaseQuery = query.toLowerCase().trim();
            filteredList = allPetsList.stream()
                    .filter(pet -> (pet.getName() != null && pet.getName().toLowerCase().contains(lowerCaseQuery)) ||
                                   (pet.getDescription() != null && pet.getDescription().toLowerCase().contains(lowerCaseQuery)) ||
                                   (pet.getStatus() != null && pet.getStatus().toLowerCase().contains(lowerCaseQuery)))
                    .collect(Collectors.toList());
        }
        updateUI(filteredList);
    }

    private void updateUI(List<Pet> pets) {
        if (llEmptyState == null || recyclerView == null || mapContainer == null) return;

        boolean hasResults = pets != null && !pets.isEmpty();

        // Handle visibility logic to ensure no flickering
        if (isMapView) {
            llEmptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);
            mapContainer.setVisibility(View.VISIBLE);
        } else {
            llEmptyState.setVisibility(hasResults ? View.GONE : View.VISIBLE);
            recyclerView.setVisibility(hasResults ? View.VISIBLE : View.GONE);
            mapContainer.setVisibility(View.GONE);
        }

        petAdapter.submitList(pets);
        
        if (googleMap != null) {
            updateMapMarkers(pets);
        }
    }

    private void toggleView() {
        isMapView = !isMapView;
        btnToggleView.setImageResource(isMapView ? android.R.drawable.ic_menu_sort_by_size : android.R.drawable.ic_dialog_map);
        filterPets(currentSearchQuery);
        invalidateOptionsMenu(); // Critical for map type toggle visibility
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;
        checkLocationSettings();
        
        googleMap.setOnInfoWindowClickListener(marker -> {
            Pet pet = (Pet) marker.getTag();
            if (pet != null) {
                Intent intent = new Intent(MainActivity.this, PetDetailActivity.class);
                intent.putExtra(PetDetailActivity.EXTRA_PET_ID, pet.getId());
                startActivity(intent);
            }
        });

        filterPets(currentSearchQuery);
        
        LatLng manila = new LatLng(14.5995, 120.9842);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(manila, 10));
    }

    private void checkLocationSettings() {
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, locationSettingsResponse -> enableMyLocation());

        task.addOnFailureListener(this, e -> {
            if (e instanceof ResolvableApiException) {
                try {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    locationSettingsLauncher.launch(new IntentSenderRequest.Builder(resolvable.getResolution()).build());
                } catch (Exception sendEx) {
                }
            }
        });
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
        if (googleMap == null) return;

        googleMap.clear();
        if (pets == null || pets.isEmpty()) return;

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        boolean hasMarkers = false;

        BitmapDescriptor pawIcon = BitmapUtils.bitmapDescriptorFromVector(this, R.drawable.ic_paw);

        for (Pet pet : pets) {
            if (pet.getLatitude() != 0 || pet.getLongitude() != 0) {
                LatLng position = new LatLng(pet.getLatitude(), pet.getLongitude());
                MarkerOptions options = new MarkerOptions()
                        .position(position)
                        .title(pet.getName())
                        .snippet("Status: " + pet.getStatus() + "\nTap to view details");
                
                if (pawIcon != null) {
                    options.icon(pawIcon);
                }

                Marker marker = googleMap.addMarker(options);
                
                if (marker != null) {
                    marker.setTag(pet);
                }
                builder.include(position);
                hasMarkers = true;
            }
        }

        if (hasMarkers && isMapView && currentSearchQuery.isEmpty()) {
            try {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150));
            } catch (Exception e) {
            }
        }
    }
}

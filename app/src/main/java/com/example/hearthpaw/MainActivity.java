package com.example.hearthpaw;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hearthpaw.data.model.Pet;
import com.example.hearthpaw.ui.ai.ChatActivity;
import com.example.hearthpaw.ui.main.AddPetActivity;
import com.example.hearthpaw.ui.adapter.PetAdapter;
import com.example.hearthpaw.ui.detail.PetDetailActivity;
import com.example.hearthpaw.ui.viewmodel.PetViewModel;
import com.example.hearthpaw.util.BitmapUtils;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, NavigationView.OnNavigationItemSelectedListener {

    private PetViewModel petViewModel;
    private RecyclerView recyclerView;
    private View mapContainerWrapper; // This is now the CardView cv_map
    private LinearLayout llEmptyState;
    private PetAdapter petAdapter;
    private GoogleMap googleMap;
    private boolean isMapView = false;
    private TextView tvTotalRescues, tvActiveFound;
    private DrawerLayout drawerLayout;
    private SearchView searchView;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize Sidebar (Drawer)
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.app_name, R.string.app_name);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Initialize Views
        recyclerView = findViewById(R.id.rv_pets);
        mapContainerWrapper = findViewById(R.id.cv_map); // Now finding the CardView
        llEmptyState = findViewById(R.id.ll_empty_state);
        tvTotalRescues = findViewById(R.id.tv_total_rescues);
        tvActiveFound = findViewById(R.id.tv_active_searches);
        ImageButton fab = findViewById(R.id.fab_add_pet);
        searchView = findViewById(R.id.search_view_main);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        petAdapter = new PetAdapter(pet -> {
            Intent intent = new Intent(MainActivity.this, PetDetailActivity.class);
            intent.putExtra(PetDetailActivity.EXTRA_PET_ID, pet.getId());
            startActivity(intent);
        });
        recyclerView.setAdapter(petAdapter);

        // Setup Search
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

        // Initialize Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_container);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Initialize Custom Map Controls
        View btnZoomIn = findViewById(R.id.btn_zoom_in);
        View btnZoomOut = findViewById(R.id.btn_zoom_out);
        View btnMyLocation = findViewById(R.id.btn_my_location);

        if (btnZoomIn != null) {
            btnZoomIn.setOnClickListener(v -> {
                if (googleMap != null) googleMap.animateCamera(CameraUpdateFactory.zoomIn());
            });
        }
        if (btnZoomOut != null) {
            btnZoomOut.setOnClickListener(v -> {
                if (googleMap != null) googleMap.animateCamera(CameraUpdateFactory.zoomOut());
            });
        }
        if (btnMyLocation != null) {
            btnMyLocation.setOnClickListener(v -> centerMapOnMyLocation());
        }

        // Initialize ViewModel
        petViewModel = new ViewModelProvider(this).get(PetViewModel.class);

        // Observe the LiveData from ViewModel
        petViewModel.getAllPets().observe(this, pets -> {
            this.allPetsList = pets != null ? pets : new ArrayList<>();
            updateDashboard(allPetsList);
            filterPets(currentSearchQuery);
        });

        // Set FAB click listener
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddPetActivity.class);
            startActivity(intent);
        });

        // Set initial state
        navigationView.setCheckedItem(R.id.nav_home);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Sync the navigation drawer state when returning to the activity
        NavigationView navigationView = findViewById(R.id.nav_view);
        if (navigationView != null) {
            navigationView.setCheckedItem(isMapView ? R.id.nav_map : R.id.nav_home);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        boolean isHandled = false;

        if (id == R.id.nav_home) {
            isMapView = false;
            filterPets(currentSearchQuery);
            isHandled = true;
        } else if (id == R.id.nav_bantay) {
            startActivity(new Intent(MainActivity.this, ChatActivity.class));
            // Immediately restore the checked item back to the current view (Home or Map)
            // so the drawer doesn't stay highlighted on BantAI after returning.
            NavigationView navView = findViewById(R.id.nav_view);
            if (navView != null) {
                navView.setCheckedItem(isMapView ? R.id.nav_map : R.id.nav_home);
            }
            // Don't mark as handled so the activity state remains unchanged
        } else if (id == R.id.nav_map) {
            isMapView = true;
            filterPets(currentSearchQuery);
            isHandled = true;
        } else if (id == R.id.nav_about) {
            showAboutDialog();
            // Dialog doesn't change activity state
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return isHandled;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
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
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem mapTypeItem = menu.findItem(R.id.action_map_type);
        if (mapTypeItem != null) {
            mapTypeItem.setVisible(isMapView);
            if (googleMap != null) {
                mapTypeItem.setTitle(googleMap.getMapType() == GoogleMap.MAP_TYPE_SATELLITE ? "Standard View" : "Satellite View");
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
        } else {
            googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
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
                    .filter(pet -> matchesQuery(pet, lowerCaseQuery))
                    .collect(Collectors.toList());
        }
        updateUI(filteredList);
    }

    private boolean matchesQuery(Pet pet, String query) {
        if (pet == null || query == null || query.isEmpty()) {
            return false;
        }

        return containsIgnoreCase(pet.getName(), query)
                || containsIgnoreCase(pet.getDescription(), query)
                || containsIgnoreCase(pet.getStatus(), query)
                || containsIgnoreCase(pet.getSpecies(), query)
                || containsIgnoreCase(pet.getGender(), query)
                || containsIgnoreCase(pet.getAge(), query)
                || containsIgnoreCase(pet.getHealthStatus(), query)
                || containsIgnoreCase(pet.getContactNumber(), query);
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private void updateUI(List<Pet> pets) {
        if (llEmptyState == null || recyclerView == null || mapContainerWrapper == null) return;

        boolean hasResults = pets != null && !pets.isEmpty();

        if (isMapView) {
            llEmptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);
            mapContainerWrapper.setVisibility(View.VISIBLE); // Shows the CardView
        } else {
            llEmptyState.setVisibility(hasResults ? View.GONE : View.VISIBLE);
            recyclerView.setVisibility(hasResults ? View.VISIBLE : View.GONE);
            mapContainerWrapper.setVisibility(View.GONE);
        }

        petAdapter.submitList(pets);
        
        if (googleMap != null) {
            updateMapMarkers(pets);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;
        googleMap.getUiSettings().setZoomControlsEnabled(false);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
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
                    // locationSettingsLauncher skipped to avoid complexity in this specific change
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

    private void centerMapOnMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            locationPermissionRequest.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
            return;
        }

        if (googleMap == null) return;

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
            } else {
                Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAboutDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_about, null);
        
        TextView tvHotline = dialogView.findViewById(R.id.tv_hotline_dialog);
        tvHotline.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:911"));
            startActivity(intent);
        });

        new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .show();
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
                        .snippet("Status: " + pet.getStatus());
                
                if (pawIcon != null) {
                    options.icon(pawIcon);
                }

                Marker marker = googleMap.addMarker(options);
                if (marker != null) marker.setTag(pet);
                builder.include(position);
                hasMarkers = true;
            }
        }

        if (hasMarkers) {
            focusMapOnPets(pets, builder);
        }
    }

    private void focusMapOnPets(List<Pet> pets, LatLngBounds.Builder builder) {
        if (googleMap == null || pets == null || pets.isEmpty()) {
            return;
        }

        int matchCount = 0;
        LatLng singlePosition = null;
        for (Pet pet : pets) {
            if (pet.getLatitude() != 0 || pet.getLongitude() != 0) {
                matchCount++;
                if (singlePosition == null) {
                    singlePosition = new LatLng(pet.getLatitude(), pet.getLongitude());
                }
            }
        }

        if (matchCount == 0) {
            return;
        }

        if (matchCount == 1 && singlePosition != null) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(singlePosition, 15));
            return;
        }

        try {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 120));
        } catch (IllegalStateException ignored) {
            if (singlePosition != null) {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(singlePosition, 12));
            }
        }
    }
}

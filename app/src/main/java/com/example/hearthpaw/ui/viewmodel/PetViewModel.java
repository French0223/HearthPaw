package com.example.hearthpaw.ui.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.example.hearthpaw.data.model.Pet;
import com.example.hearthpaw.data.repository.PetRepository;
import java.util.List;

/** ViewModel class to provide data to the UI and survive configuration changes.*/
public class PetViewModel extends AndroidViewModel {
    private PetRepository repository;
    private LiveData<List<Pet>> allPets;

    public PetViewModel(@NonNull Application application) {
        super(application);
        repository = new PetRepository(application);
        allPets = repository.getAllPets();
    }

    public LiveData<List<Pet>> getAllPets() {
        return allPets;
    }

    public LiveData<Pet> getPetById(int petId) {
        return repository.getPetById(petId);
    }

    public void insert(Pet pet) {
        repository.insert(pet);
    }

    public void update(Pet pet) {
        repository.update(pet);
    }

    public void delete(Pet pet) {
        repository.delete(pet);
    }
}

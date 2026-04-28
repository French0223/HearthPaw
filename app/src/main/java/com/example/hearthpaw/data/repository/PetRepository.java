package com.example.hearthpaw.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.example.hearthpaw.data.local.AppDatabase;
import com.example.hearthpaw.data.local.PetDao;
import com.example.hearthpaw.data.model.Pet;
import java.util.List;

/*** Repository class to abstract access to multiple data sources.*/
public class PetRepository {
    private PetDao petDao;
    private LiveData<List<Pet>> allPets;

    public PetRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        petDao = db.petDao();
        allPets = petDao.getAllPets();
    }

    public LiveData<List<Pet>> getAllPets() {
        return allPets;
    }

    public void insert(Pet pet) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            petDao.insert(pet);
        });
    }

    public void update(Pet pet) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            petDao.update(pet);
        });
    }

    public void delete(Pet pet) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            petDao.delete(pet);
        });
    }
}

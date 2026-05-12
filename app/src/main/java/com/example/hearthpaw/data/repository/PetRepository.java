package com.example.hearthpaw.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.example.hearthpaw.data.local.AppDatabase;
import com.example.hearthpaw.data.local.CareTaskDao;
import com.example.hearthpaw.data.local.PetDao;
import com.example.hearthpaw.data.model.CareTask;
import com.example.hearthpaw.data.model.Pet;
import java.util.List;

/*** Repository class to abstract access to multiple data sources.*/
public class PetRepository {
    private PetDao petDao;
    private CareTaskDao careTaskDao;
    private LiveData<List<Pet>> allPets;

    public PetRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        petDao = db.petDao();
        careTaskDao = db.careTaskDao();
        allPets = petDao.getAllPets();
    }

    public LiveData<List<Pet>> getAllPets() {
        return allPets;
    }

    public List<Pet> getAllPetsNow() {
        return petDao.getAllPetsNow();
    }

    public LiveData<Pet> getPetById(int id) {
        return petDao.getPetById(id);
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

    // Care Task Operations
    public LiveData<List<CareTask>> getTasksForPet(int petId) {
        return careTaskDao.getTasksForPet(petId);
    }

    public void insertTask(CareTask task) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            careTaskDao.insert(task);
        });
    }

    public void updateTask(CareTask task) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            careTaskDao.update(task);
        });
    }

    public void deleteTask(CareTask task) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            careTaskDao.delete(task);
        });
    }
}

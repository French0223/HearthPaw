package com.example.hearthpaw.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.hearthpaw.data.model.Pet;

import java.util.List;

@Dao
public interface PetDao {
    @Insert
    void insert(Pet pet);

    @Update
    void update(Pet pet);

    @Delete
    void delete(Pet pet);

    @Query("SELECT * FROM pets ORDER BY timestamp DESC")
    LiveData<List<Pet>> getAllPets();

    @Query("SELECT * FROM pets WHERE id = :id")
    LiveData<Pet> getPetById(int id);
}

package com.example.hearthpaw.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.hearthpaw.data.model.CareTask;

import java.util.List;

@Dao
public interface CareTaskDao {
    @Insert
    void insert(CareTask task);

    @Update
    void update(CareTask task);

    @Delete
    void delete(CareTask task);

    @Query("SELECT * FROM care_tasks WHERE petId = :petId ORDER BY taskTime ASC")
    LiveData<List<CareTask>> getTasksForPet(int petId);
}

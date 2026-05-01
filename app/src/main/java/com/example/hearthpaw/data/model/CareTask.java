package com.example.hearthpaw.data.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "care_tasks",
        foreignKeys = @ForeignKey(entity = Pet.class,
                parentColumns = "id",
                childColumns = "petId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("petId")})
public class CareTask {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int petId;
    private String taskName;
    private String taskTime;
    private boolean isCompleted;

    public CareTask(int petId, String taskName, String taskTime) {
        this.petId = petId;
        this.taskName = taskName;
        this.taskTime = taskTime;
        this.isCompleted = false;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPetId() { return petId; }
    public void setPetId(int petId) { this.petId = petId; }

    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }

    public String getTaskTime() { return taskTime; }
    public void setTaskTime(String taskTime) { this.taskTime = taskTime; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }
}

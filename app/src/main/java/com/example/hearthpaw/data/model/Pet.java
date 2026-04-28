package com.example.hearthpaw.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/*** Data model for a Rescued Pet, annotated for Room Database.*/
@Entity(tableName = "pets")
public class Pet {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private String name;
    private String description;
    private String photoPath;
    private String status; // "Found", "Searching for Owner", "Adoptable"
    private double latitude;
    private double longitude;
    private String contactNumber;
    private long timestamp;

    // Constructor
    public Pet(String name, String description, String photoPath, String status, 
               double latitude, double longitude, String contactNumber) {
        this.name = name;
        this.description = description;
        this.photoPath = photoPath;
        this.status = status;
        this.latitude = latitude;
        this.longitude = longitude;
        this.contactNumber = contactNumber;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}

package com.example.chemsolve2;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
@Entity(tableName = "scans")
public class ScanItem
{
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long createdAt;      // System.currentTimeMillis()
    public String resultText;   // server response
    public byte[] imagePng;     // thumbnail image bytes (small)
    public boolean saved;       // user saved or not
}

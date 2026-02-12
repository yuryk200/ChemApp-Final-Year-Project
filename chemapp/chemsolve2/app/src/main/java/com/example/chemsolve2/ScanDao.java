package com.example.chemsolve2;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface ScanDao
{
    @Insert
    long insert(ScanItem item);

    @Query("UPDATE scans SET saved = 1 WHERE id = :id")
    void markSaved(long id);

    @Query("SELECT * FROM scans WHERE saved = 0 ORDER BY createdAt DESC LIMIT 50")
    List<ScanItem> getRecent();

    @Query("SELECT * FROM scans WHERE saved = 1 ORDER BY createdAt DESC")
    List<ScanItem> getSaved();
}

package com.example.chemsolve2;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {ScanItem.class}, version = 1)
public abstract class AppDb extends RoomDatabase
{
    public abstract ScanDao scanDao();

    private static volatile AppDb INSTANCE;

    public static AppDb get(Context ctx)
    {
        if (INSTANCE == null)
        {
            synchronized (AppDb.class)
            {
                if (INSTANCE == null)
                {
                    INSTANCE = Room.databaseBuilder(ctx, AppDb.class, "chemsolve2.db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}

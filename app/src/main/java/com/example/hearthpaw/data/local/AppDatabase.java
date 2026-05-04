package com.example.hearthpaw.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.hearthpaw.data.model.CareTask;
import com.example.hearthpaw.data.model.Pet;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Pet.class, CareTask.class}, version = 4, exportSchema = true)
public abstract class AppDatabase extends RoomDatabase {

    public abstract PetDao petDao();
    public abstract CareTaskDao careTaskDao();

    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `care_tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `petId` INTEGER NOT NULL, `taskName` TEXT, `taskTime` TEXT, `isCompleted` INTEGER NOT NULL, FOREIGN KEY(`petId`) REFERENCES `pets`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_care_tasks_petId` ON `care_tasks` (`petId`)");
        }
    };

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE pets ADD COLUMN species TEXT");
            database.execSQL("ALTER TABLE pets ADD COLUMN gender TEXT");
            database.execSQL("ALTER TABLE pets ADD COLUMN age TEXT");
            database.execSQL("ALTER TABLE pets ADD COLUMN healthStatus TEXT");
        }
    };

    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE pets ADD COLUMN statusUpdatedAt INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE pets ADD COLUMN nextReminderDate INTEGER NOT NULL DEFAULT 0");
        }
    };

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "hearthpaw_database")
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}

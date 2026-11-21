package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TripEntity::class, AccelerationRunEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class SpeedProDatabase : RoomDatabase() {
    abstract fun speedProDao(): SpeedProDao

    companion object {
        @Volatile
        private var INSTANCE: SpeedProDatabase? = null

        fun getInstance(context: Context): SpeedProDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SpeedProDatabase::class.java,
                    "speed_pro.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

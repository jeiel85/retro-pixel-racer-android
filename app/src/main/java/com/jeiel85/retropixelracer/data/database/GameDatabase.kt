package com.jeiel85.retropixelracer.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jeiel85.retropixelracer.data.dao.GameDao
import com.jeiel85.retropixelracer.data.entity.UserProfile
import com.jeiel85.retropixelracer.data.entity.DailyMission
import com.jeiel85.retropixelracer.data.entity.TrackState
import com.jeiel85.retropixelracer.data.entity.LeaderboardEntry

@Database(
    entities = [
        UserProfile::class,
        DailyMission::class,
        TrackState::class,
        LeaderboardEntry::class
    ],
    version = 1,
    exportSchema = false
)
abstract class GameDatabase : RoomDatabase() {

    abstract fun gameDao(): GameDao

    companion object {
        @Volatile
        private var INSTANCE: GameDatabase? = null

        fun getDatabase(context: Context): GameDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GameDatabase::class.java,
                    "retro_racing_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

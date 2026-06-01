package com.jeiel85.retropixelracer.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jeiel85.retropixelracer.data.entity.UserProfile
import com.jeiel85.retropixelracer.data.entity.DailyMission
import com.jeiel85.retropixelracer.data.entity.TrackState
import com.jeiel85.retropixelracer.data.entity.LeaderboardEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {

    // --- User Profile ---
    @Query("SELECT * FROM user_profile WHERE id = 'singleton' LIMIT 1")
    fun getUserProfileFlow(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 'singleton' LIMIT 1")
    suspend fun getUserProfile(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfile)

    @Update
    suspend fun updateUserProfile(profile: UserProfile)


    // --- Daily Missions ---
    @Query("SELECT * FROM daily_missions")
    fun getDailyMissionsFlow(): Flow<List<DailyMission>>

    @Query("SELECT * FROM daily_missions WHERE id = :id LIMIT 1")
    suspend fun getDailyMission(id: String): DailyMission?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyMissions(missions: List<DailyMission>)

    @Update
    suspend fun updateDailyMission(mission: DailyMission)


    // --- Tracks state ---
    @Query("SELECT * FROM track_states")
    fun getTrackStatesFlow(): Flow<List<TrackState>>

    @Query("SELECT * FROM track_states WHERE trackId = :trackId LIMIT 1")
    suspend fun getTrackState(trackId: String): TrackState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackStates(tracks: List<TrackState>)

    @Update
    suspend fun updateTrackState(track: TrackState)


    // --- Leaderboards ---
    @Query("SELECT * FROM leaderboards WHERE trackId = :trackId ORDER BY timeMillis ASC")
    fun getLeaderboardEntriesFlow(trackId: String): Flow<List<LeaderboardEntry>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLeaderboardEntry(entry: LeaderboardEntry)

    @Query("DELETE FROM leaderboards WHERE isUser = 1 AND trackId = :trackId")
    suspend fun deleteUserEntriesForTrack(trackId: String)
}

package com.jeiel85.retropixelracer.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jeiel85.retropixelracer.data.database.GameDatabase
import com.jeiel85.retropixelracer.data.entity.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GameRepositoryTest {

    private lateinit var db: GameDatabase
    private lateinit var repository: GameRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GameDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = GameRepository(db.gameDao())
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testInitializeDefaultDataSeedsNewExpertTracks() = runBlocking {
        repository.initializeDefaultData()
        val tracks = repository.trackStates.first()
        
        // Assert that default + new expert tracks are present (6 total)
        assertEquals(6, tracks.size)
        
        val volcanoTrack = tracks.firstOrNull { it.trackId == "track_volcano" }
        val spaceTrack = tracks.firstOrNull { it.trackId == "track_space" }
        
        assertTrue(volcanoTrack != null)
        assertEquals("Magma Inferno", volcanoTrack?.name)
        assertEquals("DESERT", volcanoTrack?.theme)
        assertEquals("EXPERT", volcanoTrack?.difficulty)
        assertEquals(4000, volcanoTrack?.lengthMeters)
        assertFalse(volcanoTrack?.unlocked ?: true)
        
        assertTrue(spaceTrack != null)
        assertEquals("Cosmic Speedway", spaceTrack?.name)
        assertEquals("NEON", spaceTrack?.theme)
        assertEquals("EXPERT", spaceTrack?.difficulty)
        assertEquals(5000, spaceTrack?.lengthMeters)
        assertFalse(spaceTrack?.unlocked ?: true)
    }

    @Test
    fun testBuyUpgradeScaleUpToLevel10() = runBlocking {
        // Initialize profile and give it lots of coins
        repository.initializeDefaultData()
        val profile = repository.getActiveUserProfile()!!
        
        // Purchase engine level up to 10
        // Level 1->2 cost: 250
        // Level 2->3 cost: 500
        // Level 3->4 cost: 750
        // Level 4->5 cost: 1000
        // Level 5->6 cost: 1250
        // Level 6->7 cost: 1500
        // Level 7->8 cost: 1750
        // Level 8->9 cost: 2000
        // Level 9->10 cost: 2250
        // Total cost: 11250.
        repository.saveUserProfile(profile.copy(coins = 20000, engineLevel = 1))
        
        for (i in 1..9) {
            val success = repository.buyUpgrade("ENGINE")
            assertTrue("Should succeed to upgrade from Level $i", success)
        }
        
        val maxedProfile = repository.getActiveUserProfile()!!
        assertEquals(10, maxedProfile.engineLevel)
        
        // Try one more upgrade, should fail as it is capped at level 10
        val cappedSuccess = repository.buyUpgrade("ENGINE")
        assertFalse("Upgrade should fail when already at Level 10", cappedSuccess)
    }
}

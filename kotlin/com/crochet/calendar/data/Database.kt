package com.crochet.calendar.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE year=:year AND month=:month AND day=:day ORDER BY time ASC")
    fun getEventsForDay(year: Int, month: Int, day: Int): Flow<List<Event>>

    @Query("SELECT * FROM events ORDER BY year DESC, month DESC, day ASC, time ASC")
    fun getAllEvents(): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE (year > :year) OR (year = :year AND month > :month) OR (year = :year AND month = :month AND day >= :day) ORDER BY year ASC, month ASC, day ASC, time ASC")
    fun getAllUpcomingEvents(year: Int, month: Int, day: Int): Flow<List<Event>>

    @Query("SELECT DISTINCT day FROM events WHERE year=:year AND month=:month")
    fun getDaysWithEvents(year: Int, month: Int): Flow<List<Int>>

    @Query("SELECT DISTINCT month, day FROM events WHERE year = :year")
    fun getDaysWithEventsForYear(year: Int): Flow<List<MonthDay>>

    @Query("SELECT DISTINCT day FROM events WHERE year=:year AND month=:month AND reminder")
    fun getDaysWithReminders(year: Int, month: Int): Flow<List<Int>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: Event): Long

    @Update
    suspend fun updateEvent(event: Event)

    @Delete
    suspend fun deleteEvent(event: Event)
}

@Dao
interface ProjectDao {
    @Transaction
    @Query("SELECT * FROM projects ORDER BY name ASC")
    fun getAllProjects(): Flow<List<Project>>

    @Transaction
    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: Int): Project?

    @Query("SELECT * FROM components WHERE patternId = :patternID ORDER BY id ASC")
    suspend fun getComponents(patternID: Int): List<Component>


    suspend fun rankComp(patternID: Int,comp: Component): Int{
        val comps: List<Component> = getComponents(patternID)
        return comps.indexOfFirst {it.id == comp.id}
    }

    @Transaction
    suspend fun changeComp(projectId: Int,comp: Component){
        val project= getProjectById(projectId) ?: return
        project.curComp = rankComp(project.patternId, comp)
    }
    @Update
    suspend fun updateProject(project: Project)
    @Transaction
    suspend fun setSteps(projectId: Int, stepIndex: Int, num: Int) {
        val project = getProjectById(projectId) ?: return
        val components = getComponents(project.patternId)
        val currentSteps = project.compSteps.toMutableList()
        while (currentSteps.size < components.size) {
            currentSteps.add(0)
        }
        if (stepIndex in currentSteps.indices) {
            currentSteps[stepIndex] = num.coerceAtLeast(0)
            updateProject(project.copy(compSteps = currentSteps))
        }
    }

    @Transaction
    suspend fun incrementStep(projectId: Int, stepIndex: Int) {
        val pro = getProjectById(projectId) ?: return
        val currentVal = pro.compSteps.getOrElse(stepIndex) { 0 }
        setSteps(pro.id, stepIndex, currentVal + 1)
    }

    @Transaction
    suspend fun decrementStep(projectId: Int, stepIndex: Int) {
        val pro = getProjectById(projectId) ?: return
        val currentVal = pro.compSteps.getOrElse(stepIndex) { 0 }
        setSteps(pro.id, stepIndex, (currentVal - 1).coerceAtLeast(0))
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project): Long

    @Delete
    suspend fun deleteProject(project: Project)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: Int)
}

@Dao
interface PatternDao {
    @Transaction
    @Query("SELECT * FROM patterns ORDER BY name ASC")
    fun getAllPatterns(): Flow<List<Pattern>>

    @Transaction
    @Query("SELECT * FROM patterns WHERE id = :id")
    fun getPatternById(id: Int): Flow<Pattern?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPattern(pattern: Pattern): Long

    @Update
    suspend fun updatePattern(pattern: Pattern)

    @Delete
    suspend fun deletePattern(pattern: Pattern)

    @Query("DELETE FROM patterns WHERE id = :id")
    suspend fun deletePatternById(id: Int)
}

@Dao
interface ComponentDao {
    @Transaction
    @Query("SELECT * FROM components WHERE patternId = :patternId ORDER BY name ASC")
    fun getComponentsForProject(patternId: Int): Flow<List<Component>>

    @Query("SELECT * FROM components")
    fun getAllComponents(): Flow<List<Component>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComponent(component: Component): Long

    @Update
    suspend fun updateComponent(component: Component)

    @Delete
    suspend fun deleteComponent(component: Component)
}

@Database(
    entities     = [Event::class, Project::class, Component::class, Pattern::class],
    version      = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CalendarDatabase : RoomDatabase() {
    abstract fun eventDao():     EventDao
    abstract fun projectDao():   ProjectDao
    abstract fun patternDao(): PatternDao
    abstract fun componentDao():      ComponentDao
}

data class MonthDay(
    val month: Int,
    val day: Int
)

package com.crochet.calendar.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromStringList(list: List<String>): String =
        list.joinToString("||")

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isBlank()) emptyList() else value.split("||")

    @TypeConverter
    fun fromIntList(list: List<Int>): String =
        list.joinToString(",")

    @TypeConverter
    fun toIntList(value: String): List<Int> =
        if (value.isBlank()) emptyList() else value.split(",").mapNotNull { it.toIntOrNull() }
}

@Entity(
    tableName = "events",
    foreignKeys = [ForeignKey(
        entity        = Project::class,
        parentColumns = ["id"],
        childColumns  = ["projectId"],
        onDelete      = ForeignKey.SET_NULL
    )],
    indices = [Index("projectId")])
data class Event(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val year: Int,
    val month: Int,
    val day: Int,
    val time: String? = "", //optional
    val reminder: Boolean = false,
    val projectId: Int? = null   // optional
)

@Entity(
    tableName = "projects",
    foreignKeys = [ForeignKey(
        entity        = Pattern::class,
        parentColumns = ["id"],
        childColumns  = ["patternId"],
        onDelete      = ForeignKey.CASCADE
    )],
    indices = [Index("patternId")]
)
data class Project(
    @PrimaryKey(autoGenerate = true)
    val id:        Int           = 0,
    val patternId: Int,
    val name: String,
    var curComp: Int,
    var compSteps: List<Int> = emptyList()
)


//patterns will be used for storing patterns and also in projects
@Entity(tableName = "patterns")
data class Pattern(
    @PrimaryKey(autoGenerate = true)
    val id:   Int = 0,
    val name: String = "",
    val notes: String = "",
    val colorTag: String = "#526447"
)

@Entity(
    tableName   = "components",
    foreignKeys = [ForeignKey(
        entity        = Pattern::class,
        parentColumns = ["id"],
        childColumns  = ["patternId"],
        onDelete      = ForeignKey.CASCADE
    )],
    indices = [Index("patternId")]
)
data class Component(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val patternId: Int,
    val name:      String = "",
    val num:        Int    = 0,
    val steps: List<String> = emptyList()
) {
    fun getLength(): Int = steps.size

    fun addStep(name: String): Component =
        copy(steps = steps + name.trim())

    fun addStep(name: String, i: Int): Component {
        val newSteps = steps.toMutableList().also { it.add(i.coerceIn(0, steps.size), name.trim()) }
        return copy(steps = newSteps)
    }

    fun duplicateStep(i: Int): Component = //made it because I was lazy(pls don't judge)
        addStep(steps.get(i),i)

    fun moveStep(i: Int, Dir: Boolean): Component { // archaeic method
        if (Dir&&i>=steps.size-1 || !Dir&&i<=0) return this
        val newSteps = steps.toMutableList()
        val targetIndex = if (Dir) i + 1 else i - 1

        val temp = newSteps[i]
        newSteps[i] = newSteps[targetIndex]
        newSteps[targetIndex] = temp

        return copy(steps = newSteps)
    }
    fun shiftStep(start: Int, end: Int): Component {
        if (steps.isEmpty()) return this
        val safeStart = start.coerceIn(steps.indices)
        val safeEnd = end.coerceIn(steps.indices)
        if (safeStart == safeEnd) return this
        val newSteps = steps.toMutableList()
        val item = newSteps.removeAt(safeStart)
        newSteps.add(safeEnd, item)
        return copy(steps = newSteps)
    }

    /*
        fun shiftStepjavaver(start: Int, end:Int): Component {
        if (start>steps.size-1 && start<0) return this
        val newSteps = steps.toMutableList()
        if(start<end){
            for(int i = Math.min(end,steps.size-1; i>start; i--) {
                moveStep(i,false)
            }
        } if(start>end) {
            for(int i = Math.max(end,0); i<start; i++) {
                moveStep(i,true)
            }
        }
        return copy(steps = newSteps)
        }
     */

    fun removeStep(): Component =
        if (steps.isEmpty()) this
        else copy(steps = steps.dropLast(1))

    fun removeStep(i: Int): Component =
        if (i !in steps.indices) this
        else copy(steps = steps.toMutableList().also { it.removeAt(i) })
}

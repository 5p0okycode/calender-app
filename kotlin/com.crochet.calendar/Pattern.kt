package com.crochet.calendar

 //to store list string
class Converters {
    @TypeConverter
    fun fromStringList(list: List<String>): String =
        list.joinToString("||")

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isBlank()) emptyList() else value.split("||")
}

@Entity(
    tableName = "events"
            foreignKeys = [ForeignKey(
        entity        = Project::class,
        parentColumns = ["id"],
        childColumns  = ["projectId"],
        onDelete      = ForeignKey.CASCADE
    )],
    indices = [Index("projectId")])
data class Event(
    @PrimaryKey(autoGenerate = true)
    val id:        Int           = 0,
    val name:      String,
    val year:      Int,
    val month:     Int,
    val day:       Int,
    val time:      String?        = "", //optional
    val reminder:  Boolean       = false,
    val projectId: Int?          = null   // optional
)

@entity(
    tableName = "projects"
            foreignKeys = [ForeignKey(
        entity        = Pattern::class,
        parentColumns = ["id"],
        childColumns  = ["patternId"],
        onDelete      = ForeignKey.CASCADE
    )]
            indices = [index("patternId")]
)
data class Project(
    @PrimaryKey(autoGenerate = true)
    val id:        Int           = 0,
    val patternId: Int,
    val name: String,
    val compSteps: int
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

    fun removeStep(): Component =
        if (steps.isEmpty()) this
        else copy(steps = steps.dropLast(1))

    fun removeStep(i: Int): Component =
        if (i !in steps.indices) this
        else copy(steps = steps.toMutableList().also { it.removeAt(i) })
}





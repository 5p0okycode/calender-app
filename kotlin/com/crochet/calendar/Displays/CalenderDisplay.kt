package com.crochet.calendar

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crochet.calendar.AppColors
import com.crochet.calendar.PlusJakartaSans

private val DOW = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

// Colour constants — replace with your actual theme imports if you have them
private val Primary            = Color(0xFF526447)
private val PrimaryContainer   = Color(0xFFD4E9C4)
private val OnPrimary          = Color(0xFFECFFDD)
private val OnPrimaryContainer = Color(0xFF45573B)
private val OnSurface          = Color(0xFF3A3216)
private val OnSurfaceVariant   = Color(0xFF685F3E)
private val Tertiary           = Color(0xFF7E572E)

@Composable
fun CalendarGrid(
    daysInMonth:    Int,
    firstDayOfWeek: Int,
    actualDay:      Int,
    curDay:         Int,
    daysWithEvents: Set<Int>,
    slideDir:       Int,           // -1 = prev | 0 = none | 1 = next
    onDayClick:     (Int) -> Unit,
    modifier:       Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // Day-of-week header row
        Row(Modifier.fillMaxWidth()) {
            DOW.forEach { label ->
                Text(
                    text          = label.uppercase(),
                    modifier      = Modifier.weight(1f),
                    textAlign     = TextAlign.Center,
                    fontFamily    = PlusJakartaSans,
                    fontWeight    = FontWeight.Bold,
                    fontSize      = 10.sp,
                    color         = AppColors.OnSurfaceVariant.copy(alpha = 0.5f),
                    letterSpacing = 0.8.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Slide animation direction
        val enterAnim = if (slideDir > 0)
            slideInHorizontally { it } + fadeIn()
        else
            slideInHorizontally { -it } + fadeIn()

        val exitAnim = if (slideDir > 0)
            slideOutHorizontally { -it } + fadeOut()
        else
            slideOutHorizontally { it } + fadeOut()

        // AnimatedContent keys on both days+offset so it triggers on navigation
        AnimatedContent(
            targetState      = daysInMonth to firstDayOfWeek,
            transitionSpec   = { enterAnim togetherWith exitAnim },
            contentAlignment = Alignment.TopStart,
            label            = "calendar_grid"
        ) { (days, offset) ->
            DayGrid(
                daysInMonth    = days,
                firstDayOfWeek = offset,
                todayDay       = actualDay,
                selectedDay    = curDay,
                daysWithEvents = daysWithEvents,
                onDayClick     = onDayClick
            )
        }
    }
}

@Composable
private fun DayGrid(
    daysInMonth:    Int,
    firstDayOfWeek: Int,
    todayDay:       Int,
    selectedDay:    Int,
    daysWithEvents: Set<Int>,
    onDayClick:     (Int) -> Unit,
) {
    val neededCells = firstDayOfWeek + daysInMonth   // renamed NeededCells → neededCells

    Column {
        var cell = 0
        while (cell < neededCells) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val dayNum = cell - firstDayOfWeek + 1
                    Box(
                        modifier         = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (cell >= firstDayOfWeek && dayNum <= daysInMonth) {
                            DayCell(
                                day        = dayNum,
                                isToday    = dayNum == todayDay,
                                isSelected = dayNum == selectedDay && dayNum != todayDay,
                                hasEvents  = dayNum in daysWithEvents,
                                onDayClick = onDayClick
                            )
                        }
                    }
                    cell++
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun DayCell(
    day:        Int,
    isToday:    Boolean,
    isSelected: Boolean,
    hasEvents:  Boolean,
    onDayClick: (Int) -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                when {
                    isToday    -> AppColors.Primary
                    isSelected -> AppColors.Primary.copy(alpha = 0.1f)
                    else       -> Color.Transparent
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null
            ) { onDayClick(day) }
    ) {
        Text(
            text       = day.toString(),
            fontFamily = PlusJakartaSans,
            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize   = 15.sp,
            color      = when {
                isToday    -> Color.White
                isSelected -> AppColors.Primary
                else       -> AppColors.OnSurface
            }
        )
    }

    // Event dot below the number
    if (hasEvents) {
        Box(
            modifier = Modifier
                .offset(y = 12.dp)
                .size(4.dp)
                .clip(CircleShape)
                .background(if (isToday) Color.White else AppColors.Secondary)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventMenuDialog(
    event: Event,
    projects: List<Project>,
    onDismiss: () -> Unit,
    onSave: (Event) -> Unit
) {
    var name      by remember { mutableStateOf(event.name) }
    var time      by remember { mutableStateOf(event.time ?: "") }
    var reminder  by remember { mutableStateOf(event.reminder) }
    var projectId by remember { mutableStateOf(event.projectId) }
    var expanded  by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text("Edit Event", fontWeight = FontWeight.Bold) },
        text    = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Name") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value         = time,
                    onValueChange = { time = it },
                    label         = { Text("Time (optional)") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )

                // Project Selection
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = projects.find { it.id == projectId }?.name ?: "None",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Link Project") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None") },
                            onClick = {
                                projectId = null
                                expanded = false
                            }
                        )
                        projects.forEach { project ->
                            DropdownMenuItem(
                                text = { Text(project.name) },
                                onClick = {
                                    projectId = project.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = reminder, onCheckedChange = { reminder = it })
                    Text("Reminder")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    onSave(event.copy(name = name.trim(), time = time, reminder = reminder, projectId = projectId))
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventDialog(
    projects:  List<Project>,
    onDismiss: () -> Unit,
    onSave:    (name: String, time: String, reminder: Boolean, projectId: Int?) -> Unit
) {
    var name      by remember { mutableStateOf("") }
    var time      by remember { mutableStateOf("") }
    var reminder  by remember { mutableStateOf(false) }
    var projectId by remember { mutableStateOf<Int?>(null) }
    var expanded  by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text("New Event", fontWeight = FontWeight.Bold) },
        text    = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Name") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value         = time,
                    onValueChange = { time = it },
                    label         = { Text("Time (optional)") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )

                // Project Selection
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = projects.find { it.id == projectId }?.name ?: "None",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Link Project") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None") },
                            onClick = {
                                projectId = null
                                expanded = false
                            }
                        )
                        projects.forEach { project ->
                            DropdownMenuItem(
                                text = { Text(project.name) },
                                onClick = {
                                    projectId = project.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = reminder, onCheckedChange = { reminder = it })
                    Text("Reminder")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onSave(name, time, reminder, projectId) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

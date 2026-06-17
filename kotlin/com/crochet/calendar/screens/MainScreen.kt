package com.crochet.calendar.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crochet.calendar.R
import com.crochet.calendar.ui.AppColors
import com.crochet.calendar.data.Event
import com.crochet.calendar.ui.PlusJakartaSans
import com.crochet.calendar.data.Project
import com.crochet.calendar.ui.GrannySquare
import com.crochet.calendar.ViewState
import kotlin.Int
import kotlin.math.ceil

private val DOW = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
private val MonthLen: IntArray = intArrayOf(
    31, 28, 31, 30, 31, 30,
    31, 31, 30, 31, 30, 31
)

@Composable
fun CalendarGrid(
    daysInMonth: Int,
    firstDayOfWeek: Int,
    firstDayNumOfWeek: Int,
    actualDay: Int,
    actualMonth: Int,
    actualYear: Int,
    curDay: Int,
    month: Int,
    year: Int,
    daysWithEvents: Set<Int>,
    slideDir: Int,           // -1 = prev | 0 = none | 1 = next
    currentViewState: ViewState,
    events: List<Event>,
    onDayClick: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // Day-of-week header row
        Row(Modifier.fillMaxWidth()) {
            DOW.forEach { label ->
                Text(
                    text = label.uppercase(),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = AppColors.OnSurfaceVariant.copy(alpha = 0.5f),
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

        when (currentViewState) {
            ViewState.DAILY -> {
                AnimatedContent(
                    targetState = Triple(curDay, month, year),
                    transitionSpec = { enterAnim togetherWith exitAnim },
                    contentAlignment = Alignment.TopStart,
                    label = "daily_view"
                ) { (d, m, y) ->
                    DailyView(
                        day = d,
                        isToday = d == actualDay && m == actualMonth && y == actualYear,
                        month = m,
                        events = events,
                        onDayClick = onDayClick
                    )
                }
            }

            ViewState.WEEKLY -> {
                AnimatedContent(
                    targetState = Triple(firstDayNumOfWeek, month, year),
                    transitionSpec = { enterAnim togetherWith exitAnim },
                    contentAlignment = Alignment.TopStart,
                    label = "weekly_view"
                ) { (f, m, y) ->
                    WeeklyView(
                        daysInMonth = daysInMonth,
                        todayDay = actualDay,
                        selectedDay = curDay,
                        daysWithEvents = daysWithEvents,
                        month = m,
                        year = y,
                        onDayClick = onDayClick,
                        firstDayNumOfWeek = f,
                        events = events
                    )
                }
            }

            ViewState.YEARLY -> {
                AnimatedContent(
                    targetState = year,
                    transitionSpec = { enterAnim togetherWith exitAnim },
                    contentAlignment = Alignment.TopStart,
                    label = "yearly_view"
                ) { y ->
                    yearView(
                        todayDay = actualDay,
                        todayMonth = actualMonth,
                        todayYear = actualYear,
                        curDay = curDay,
                        curMonth = month,
                        curYear = y,
                        daysWithEvents = daysWithEvents,
                        onDayClick = onDayClick
                    )
                }
            }

            else -> {// AnimatedContent keys on both days+offset so it triggers on navigation
                AnimatedContent(
                    targetState = Triple(month, year, daysInMonth to firstDayOfWeek),
                    transitionSpec = { enterAnim togetherWith exitAnim },
                    contentAlignment = Alignment.TopStart,
                    label = "monthly_view"
                ) { (m, y, meta) ->
                    monthView(
                        daysInMonth = meta.first,
                        firstDayOfWeek = meta.second,
                        firstDayNumOfWeek = firstDayNumOfWeek,
                        todayDay = actualDay,
                        selectedDay = curDay,
                        month = m,
                        year = y,
                        daysWithEvents = daysWithEvents,
                        onDayClick = onDayClick
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyView(
    day:        Int,
    isToday:    Boolean,
    month: Int,
    events: List<Event>,
    onDayClick: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Large Day Cell
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(100.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null
                ) { onDayClick(day, month) }
        ) {
            val monthColors = AppColors.MonthColors[month % 12]
            val (color1, color2) = monthColors
            GrannySquare(
                color1 = if (isToday) AppColors.Primary else color1,
                color2 = if (isToday) AppColors.PrimaryContainer else color2,
                modifier = Modifier.size(100.dp)
            )

            Text(
                text       = day.toString(),
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize   = 40.sp,
                color      = if (isToday) Color.White else AppColors.OnSurface
            )
        }
        Spacer(Modifier.height(48.dp))

        Timeline(events = events)

        Spacer(Modifier.height(48.dp))

        Text(
            text = "Today's Thread",
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color.White
        )
        events.filter { !it.time.isNullOrBlank() }.forEach { event ->
            Text(
                text = "${event.time} - ${event.name}",
                fontFamily = PlusJakartaSans,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}
@Composable
public fun WeeklyView(
    daysInMonth: Int,
    todayDay: Int,
    selectedDay: Int,
    daysWithEvents: Set<Int>,
    month: Int,
    year: Int,
    onDayClick: (Int,Int) -> Unit,
    firstDayNumOfWeek: Int,
    events: List<Event>,
    modifier: Modifier = Modifier,
){
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        weekView(
            daysInMonth = daysInMonth,
            todayDay = todayDay,
            selectedDay = selectedDay,
            daysWithEvents = daysWithEvents,
            month = month,
            year = year,
            onDayClick = onDayClick,
            firstDayNumOfWeek = firstDayNumOfWeek
        )
        Spacer(Modifier.height(48.dp))

        Timeline(events = events)

        Spacer(Modifier.height(48.dp))
    }
}
@Composable
fun Timeline(events: List<Event>, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val width = maxWidth
        val hourWidth = width / 24f

        Column {
            // Top labels: 0, 2, ..., 24
            Box(Modifier.fillMaxWidth().height(24.dp)) {
                for (h in 0..24 step 2) {
                    Text(
                        text = h.toString(),
                        color = Color.White,
                        fontSize = 10.sp,
                        modifier = Modifier.offset(x = hourWidth * h.toFloat() - 6.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(vertical = 8.dp)
            ) { //hour lines
                Canvas(Modifier.fillMaxSize()) {
                    val step = size.width / 24f
                    for (i in 0..24) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.3f),
                            start = Offset(i * step, 0f),
                            end = Offset(i * step, size.height),
                            strokeWidth = 1f
                        )
                    }

                    // Draw events as lines
                    events.forEach { event ->
                        event.time?.let { timeStr ->
                            val parts = timeStr.split(":")
                            if (parts.size >= 2) {
                                val h = parts[0].toFloatOrNull() ?: 0f
                                val m = parts[1].toFloatOrNull() ?: 0f
                                val totalHours = h + (m / 60f)
                                if (totalHours in 0f..24f) {
                                    val x = totalHours * step
                                    drawLine(
                                        color = Color.White,
                                        start = Offset(x, 0f),
                                        end = Offset(x, size.height),
                                        strokeWidth = 4f
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Box(Modifier.fillMaxWidth().height(24.dp)) {
                for (h in 1..23 step 2) {
                    Text(
                        text = h.toString(),
                        color = Color.White,
                        fontSize = 10.sp,
                        modifier = Modifier.offset(x = hourWidth * h.toFloat() - 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun weekView(
    daysInMonth: Int,
    todayDay: Int,
    selectedDay: Int,
    daysWithEvents: Set<Int>,
    month: Int,
    year: Int,
    onDayClick: (Int,Int) -> Unit,
    firstDayNumOfWeek: Int,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth()) {
        for (i in 0..6) {
            val dayNum = i + firstDayNumOfWeek
            val isCurrentMonth = dayNum in 1..daysInMonth
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                DayCell(
                    day = if (dayNum > daysInMonth) dayNum - daysInMonth else dayNum,
                    isToday = isCurrentMonth && dayNum == todayDay,
                    isSelected = dayNum == selectedDay && dayNum != todayDay,
                    hasEvents = dayNum in daysWithEvents,
                    month = if (dayNum > daysInMonth) month + 1 else month,
                    onDayClick = onDayClick
                )
            }
        }
    }
}

@Composable
private fun monthView(
    daysInMonth:    Int,
    firstDayOfWeek: Int,
    firstDayNumOfWeek: Int,
    todayDay: Int,
    selectedDay: Int,
    daysWithEvents: Set<Int>,
    month: Int,
    year: Int,
    onDayClick: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val prevMonthLen = remember(month, year) {
        val prevMonth = (month - 1 + 12) % 12
        val prevYear  = if (month == 0) year - 1 else year
        val base = intArrayOf(31,28,31,30,31,30,31,31,30,31,30,31)[prevMonth]
        if (prevMonth == 1 && (prevYear % 4 == 0 && (prevYear % 100 != 0 || prevYear % 400 == 0))) 29 else base
    }
    val neededWeeks = ceil((daysInMonth + firstDayOfWeek) / 7.0).toInt()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (i in 0 until neededWeeks) {
            weekView(
                firstDayNumOfWeek = if(firstDayOfWeek!=0){
                    if(i>0) firstDayNumOfWeek-prevMonthLen+(7*i)+1 else firstDayNumOfWeek+1
                } else
                    1 + (7*i),
                daysInMonth    = if(i==0 && firstDayOfWeek!=0) prevMonthLen else daysInMonth,
                todayDay       = todayDay,
                selectedDay    = selectedDay,
                daysWithEvents = daysWithEvents,
                month          = if(i==0 && firstDayOfWeek!=0) ((month+11)%12) else month,
                year = year,
                onDayClick     = onDayClick,
            )
        }
    }
}

@Composable
private fun yearView(
    todayDay: Int,
    todayMonth: Int, // 0-based
    todayYear: Int,
    curDay: Int,
    curMonth: Int,
    curYear: Int,
    daysWithEvents: Set<Int>,
    onDayClick: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        for(m in 0 until 12) {
            val monthName = java.text.DateFormatSymbols().months.getOrElse(m) { "" }
            Text(
                text = monthName,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color.White,
                modifier = Modifier.padding(16.dp)
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DayCell(
    day:        Int,
    isToday:    Boolean,
    isSelected: Boolean,
    hasEvents:  Boolean,
    month: Int,
    onDayClick: (Int,Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val monthColors = AppColors.MonthColors[(month+12) % 12]
    val (color1, color2) = monthColors
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(40.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null
            ) { onDayClick(day,month) }
    ) {
        GrannySquare(
            color1 = if (isToday) AppColors.Primary else color1,
            color2 = if (isToday) AppColors.PrimaryContainer else color2,
            modifier = Modifier.alpha(if (isSelected) 0.8f else 1f).size(40.dp)
        )

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

        // Stitch marker icon below the number
        if (hasEvents) {
            Icon(
                painter = painterResource(R.drawable.stitch_marker),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(y = (7).dp)
                    .size(12.dp),
                tint = if (isToday) Color.Black else AppColors.StitchGreen// I want to get the complement of the middle of color1 and color2
            )
        }
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
@Composable
fun addBirthdayDialog(
    onDismiss: () -> Unit,
    onSave:    (name: String, month: Int, day: Int, mine: Boolean) -> Unit,
    initialMonth: Int = 1,
    initialDay: Int = 1
) {
    var name     by remember { mutableStateOf("") }
    var monthStr by remember { mutableStateOf(initialMonth.toString()) }
    var dayStr   by remember { mutableStateOf(initialDay.toString()) }
    var mine     by remember { mutableStateOf(false) }

    // Parse to Int for validation and saving
    val month = monthStr.toIntOrNull() ?: 0
    val day   = dayStr.toIntOrNull() ?: 0

    // Validate inputs
    val isFormValid = name.isNotBlank() && month in 1..12 && day in 1..31

    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text("Add Birthday", fontWeight = FontWeight.Bold) },
        text    = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Name") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value         = monthStr,
                        onValueChange = { if (it.length <= 2) monthStr = it.filter { c -> c.isDigit() } },
                        label         = { Text("month") },
                        placeholder   = { Text("MM") },
                        singleLine    = true,
                        modifier      = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value         = dayStr,
                        onValueChange = { if (it.length <= 2) dayStr = it.filter { c -> c.isDigit() } },
                        label         = { Text("day") },
                        placeholder   = { Text("DD") },
                        singleLine    = true,
                        modifier      = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null
                        ) { mine = !mine }
                ) {
                    Checkbox(checked = mine, onCheckedChange = { mine = it })
                    Text("Your birthday?")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), month, day, mine) },
                enabled = isFormValid
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun addHolidayDialog(
    onDismiss: () -> Unit,
    onSave:    (name: String, month: Int, day: Int, prefix: String) -> Unit
){
    var name     by remember { mutableStateOf("") }
    var monthStr by remember { mutableStateOf("") }
    var dayStr   by remember { mutableStateOf("") }
    var prefix     by remember { mutableStateOf("") }

    // Parse to Int for validation and saving
    val month = monthStr.toIntOrNull() ?: 0
    val day   = dayStr.toIntOrNull() ?: 0

    // Validate inputs
    val isFormValid = name.isNotBlank() && month in 1..12 && day in 1..31

    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text("Add Holiday", fontWeight = FontWeight.Bold) },
        text    = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value         = prefix,
                        onValueChange = { prefix = it },
                        label         = { Text("Prefix") },
                        singleLine    = true,
                        modifier      = Modifier.weight(0.4f)
                    )
                    OutlinedTextField(
                        value         = name,
                        onValueChange = { name = it },
                        label         = { Text("Name") },
                        singleLine    = true,
                        modifier      = Modifier.weight(0.6f)
                    )
                }

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value         = monthStr,
                        onValueChange = { if (it.length <= 2) monthStr = it.filter { c -> c.isDigit() } },
                        label         = { Text("month") },
                        placeholder   = { Text("MM") },
                        singleLine    = true,
                        modifier      = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value         = dayStr,
                        onValueChange = { if (it.length <= 2) dayStr = it.filter { c -> c.isDigit() } },
                        label         = { Text("day") },
                        placeholder   = { Text("DD") },
                        singleLine    = true,
                        modifier      = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), month, day, prefix.trim()) },
                enabled = isFormValid
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

package com.crochet.calendar

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calander.ui.theme.*

private val DOW = listOf("Sun","Mon","Tue","Wed","Thu","Fri","Sat")

@Composable
fun CalendarGrid(
    daysInMonth:    Int,
    firstDayOfWeek: Int,
    ActualDay:       Int,          // -1 if today not in this month
    curDay:    Int,
    daysWithEvents: Set<Int>,
    slideDir:       Int,          // -1 | 0 | 1
    onDayClick:     (Int) -> Unit,
    modifier:       Modifier = Modifier,
) {
    Column(modifier = modifier) {

        // ── Day-of-week header row ─────────────────────────────────────────
        Row(Modifier.fillMaxWidth()) {
            DOW.forEach { label ->
                Text(
                    text      = label.uppercase(),
                    modifier  = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style     = MaterialTheme.typography.labelSmall,
                    color     = OnSurfaceVariant.copy(alpha = 0.7f),
                    letterSpacing = 0.8.sp
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Animated day grid ─────────────────────────────────────────────
        val enterAnim = if (slideDir > 0)
            slideInHorizontally { it } + fadeIn()
        else
            slideInHorizontally { -it } + fadeIn()

        val exitAnim = if (slideDir > 0)
            slideOutHorizontally { -it } + fadeOut()
        else
            slideOutHorizontally { it } + fadeOut()

        // Key on month+year so AnimatedContent triggers on navigation
        AnimatedContent(
            targetState      = daysInMonth to firstDayOfWeek,
            transitionSpec   = { enterAnim togetherWith exitAnim },
            contentAlignment = Alignment.TopStart,
            label            = "calendar_grid"
        ) { (days, offset) ->
            DayGrid(
                daysInMonth    = days,
                firstDayOfWeek = offset,
                todayDay       = todayDay,
                selectedDay    = selectedDay,
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
    val NeededCells = (firstDayOfWeek + daysInMonth)

    Column {
        var cell = 0
        while (cell < NeededCells) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val dayNum = cell - firstDayOfWeek + 1
                    Box(
                        modifier          = Modifier.weight(1f),
                        contentAlignment  = Alignment.Center
                    ) {
                        if (cell >= firstDayOfWeek && dayNum <= daysInMonth) {
                            DayCell(
                                day            = dayNum,
                                isToday        = dayNum == ActualDay,
                                isSelected     = dayNum == curDay && dayNum != ActualDay,
                                hasEvents      = dayNum in daysWithEvents,
                                onDayClick     = onDayClick
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
            .then(
                if (isToday) Modifier.shadow(
                    elevation       = 6.dp,
                    shape           = CircleShape,
                    ambientColor    = Primary.copy(alpha = 0.3f),
                    spotColor       = Primary.copy(alpha = 0.4f)
                ) else Modifier
            )
            .clip(CircleShape)
            .background(
                when {
                    isToday    -> Primary
                    isSelected -> PrimaryContainer
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
            fontWeight = when {
                isToday    -> FontWeight.Bold
                isSelected -> FontWeight.SemiBold
                else       -> FontWeight.Medium
            },
            fontSize   = 17.sp,
            color      = when {
                isToday    -> OnPrimary
                isSelected -> OnPrimaryContainer
                else       -> OnSurface
            }
        )
    }

    // Event dot below the day number
    if (hasEvents) {
        Box(
            modifier = Modifier
                .offset(y = 18.dp)
                .size(5.dp)
                .clip(CircleShape)
                .background(if (isToday) OnPrimary.copy(alpha = 0.7f) else Tertiary)
        )
    }
}
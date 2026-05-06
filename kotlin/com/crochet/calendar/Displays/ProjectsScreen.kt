package com.crochet.calendar

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import com.crochet.calendar.ui.DashedDivider
import com.crochet.calendar.ui.GrainOverlay
import com.crochet.calendar.ui.StitchedCard
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// Projects Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(viewModel: MainViewModel) {

    val projects        by viewModel.projects.collectAsState()
    val patterns        by viewModel.patterns.collectAsState()
    val allComponents   by viewModel.allComponents.collectAsState()
    var showAddSheet    by remember { mutableStateOf(false) }
    var selectedProject by remember { mutableStateOf<Project?>(null) }

    if (selectedProject != null) {
        ProjectDetailScreen(
            pwpInitial = selectedProject!!,
            viewModel  = viewModel,
            onBack     = { selectedProject = null }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        GrainOverlay()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppColors.Background.copy(alpha = 0.85f))
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text       = "My Projects",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 22.sp,
                            color      = Color(0xFF2D5016)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(AppColors.PrimaryContainer)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "${projects.size}",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize   = 12.sp,
                                color      = AppColors.OnPrimaryContainer
                            )
                        }
                    }
                    DashedDivider()
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick        = { showAddSheet = true },
                    containerColor = AppColors.Primary,
                    contentColor   = AppColors.OnPrimary,
                    shape          = CircleShape,
                    modifier       = Modifier
                        .size(60.dp)
                        .shadow(12.dp, CircleShape,
                            ambientColor = AppColors.Primary.copy(0.3f),
                            spotColor    = AppColors.Primary.copy(0.5f))
                ) {
                    Icon(Icons.Outlined.Add, "Add project", Modifier.size(28.dp))
                }
            }
        ) { padding ->
            if (projects.isEmpty()) {
                ProjectEmptyState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            } else {
                LazyColumn(
                    modifier            = Modifier.fillMaxSize().padding(padding),
                    contentPadding      = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(projects, key = { it.id }) { pwp ->
                        val pattern = patterns.find { it.id == pwp.patternId }
                        val projectComponents = allComponents.filter { it.patternId == pwp.patternId }
                        ProjectCard(
                            pwp        = pwp,
                            pattern    = pattern,
                            components = projectComponents,
                            onClick    = { selectedProject = pwp },
                            onDelete   = { viewModel.deleteProject(pwp) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showAddSheet) {
        AddProjectSheet(
            patterns  = patterns,
            onDismiss = { showAddSheet = false },
            onSave    = { name, patternId ->
                viewModel.addProject(name, patternId)
                showAddSheet = false
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Project Card — with animated progress bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ProjectCard(
    pwp:        Project,
    pattern:    Pattern?,
    components: List<Component>,
    onClick:    () -> Unit,
    onDelete:   () -> Unit
) {
    // We don't have components here, so show compSteps / estimated total
    val colorHex = pattern?.colorTag ?: "#526447"
    val accent   = try { Color(android.graphics.Color.parseColor(colorHex)) }
                   catch (e: Exception) { AppColors.Primary }

    StitchedCard(
        modifier = Modifier.fillMaxWidth(),
        stitchColor = AppColors.StitchGreen,
        cornerRadius = 16.dp,
        inset = 5.dp,
        dashLength = 10f,
        gapLength = 6f,
        strokeWidth = 2.dp,
        surfaceColor = AppColors.SurfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Colour dot — ports .wooden-dots concept
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(accent)
                    )
                    Text(
                        text = pwp.name,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = AppColors.OnSurface
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Outlined.Close,
                            "Delete",
                            tint = AppColors.OutlineVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Progress bar
            val totalSteps = components.sumOf { it.steps.size * it.num }
            val doneSteps = pwp.compSteps.sum()
            val progress = if (totalSteps > 0) doneSteps.toFloat() / totalSteps else 0f

            val animatedProgress by animateFloatAsState(
                targetValue = progress,
                animationSpec = tween(600, easing = EaseOutCubic),
                label = "progress"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(AppColors.OutlineVariant.copy(alpha = 0.25f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(accent)
                )
            }

            Spacer(Modifier.height(6.dp))
            Text(
                text = "${pwp.compSteps.sum()} steps completed",
                fontFamily = BeVietnamPro,
                fontSize = 11.sp,
                color = AppColors.OnSurfaceVariant
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Project Detail Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    pwpInitial: Project,
    viewModel:  MainViewModel,
    onBack:     () -> Unit
) {
    val projects    by viewModel.projects.collectAsState()
    val pwp         = projects.find { it.id == pwpInitial.id } ?: pwpInitial

    val patterns    by viewModel.patterns.collectAsState()
    val patternData = patterns.firstOrNull { it.id == pwp.patternId }
    val components  by viewModel.componentDao.getComponentsForProject(pwp.patternId).collectAsState(initial = emptyList())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        GrainOverlay()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppColors.Background.copy(alpha = 0.85f))
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                                "Back",
                                tint = AppColors.OnSurfaceVariant
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text       = pwp.name,
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize   = 18.sp,
                                color      = Color(0xFF2D5016)
                            )
                            patternData?.let {
                                Text(
                                    text       = "Based on: ${it.name}",
                                    fontFamily = BeVietnamPro,
                                    fontSize   = 11.sp,
                                    color      = AppColors.OnSurfaceVariant
                                )
                            }
                        }
                    }
                    DashedDivider()
                }
            }
        ) { padding ->
            val totalSteps = components.sumOf { it.steps.size * it.num }
            val doneSteps  = pwp.compSteps.sum()
            val progress   = if (totalSteps > 0) doneSteps.toFloat() / totalSteps else 0f

            LazyColumn(
                modifier            = Modifier.fillMaxSize().padding(padding),
                contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Overall progress card
                item {
                    StitchedCard(
                        modifier = Modifier.fillMaxWidth(),
                        stitchColor = AppColors.StitchGreen,
                        cornerRadius = 16.dp,
                        surfaceColor = AppColors.SurfaceContainer
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Overall Progress",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = AppColors.OnSurfaceVariant
                                )
                                Text(
                                    "${(progress * 100).toInt()}%",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = AppColors.Primary
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            val animProg by animateFloatAsState(
                                targetValue = progress,
                                animationSpec = tween(700, easing = EaseOutCubic),
                                label = "overall"
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(AppColors.OutlineVariant.copy(alpha = 0.25f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(animProg)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(AppColors.Primary)
                                )
                            }
                        }
                    }
                }

                item { SectionHeader("Components", Modifier.padding(top = 4.dp)) }

                if (components.isEmpty()) {
                    item { EmptyStateCard("This pattern has no components yet") }
                } else {
                    items(components, key = { it.id }) { component ->
                        val compIndex  = components.indexOf(component)
                        val stepsDone  = pwp.compSteps.getOrElse(compIndex) { 0 }
                        val stepsTotal = component.steps.size * component.num
                        val isCurrent  = pwp.curComp == compIndex

                        ProjectComponentCard(
                            component = component,
                            stepsDone = stepsDone,
                            stepsTotal = stepsTotal,
                            isCurrent = isCurrent,
                            onIncrement = {
                                viewModel.incrementProjectStep(pwp, compIndex)
                            },
                            onDecrement = {
                                viewModel.decrementProjectStep(pwp, compIndex)
                            },
                            onSetSteps = { newVal ->
                                viewModel.setProjectStep(pwp, compIndex, newVal)
                            }
                        )
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Project Component Card — shows per-component progress with +1 button
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ProjectComponentCard(
    component:   Component,
    stepsDone:   Int,
    stepsTotal:  Int,
    isCurrent:   Boolean,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onSetSteps:  (Int) -> Unit
) {
    val progress    = if (stepsTotal > 0) stepsDone.toFloat() / stepsTotal else 0f
    val isComplete  = stepsDone >= stepsTotal && stepsTotal > 0
    var isEditing   by remember { mutableStateOf(false) }
    var editValue   by remember(stepsDone) { mutableStateOf(stepsDone.toString()) }

    StitchedCard(
        modifier = Modifier.fillMaxWidth().then(
            if (isCurrent) Modifier.border(
                2.dp,
                AppColors.Primary.copy(alpha = 0.5f),
                RoundedCornerShape(12.dp)
            )
            else Modifier
        ),
        stitchColor = if (isComplete) AppColors.StitchGreen else AppColors.StitchBrown,
        cornerRadius = 12.dp,
        inset = 5.dp,
        dashLength = 10f,
        gapLength = 6f,
        strokeWidth = 1.5.dp,
        surfaceColor = if (isCurrent) AppColors.SurfaceContainer else AppColors.SurfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isComplete) AppColors.Primary.copy(0.15f)
                                else AppColors.Tertiary.copy(0.12f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isComplete) Icons.Outlined.CheckCircle else Icons.Outlined.Build,
                            null,
                            tint = if (isComplete) AppColors.Primary else AppColors.Tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = component.name,
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = AppColors.OnSurface
                            )
                            if (component.num > 1) {
                                Text(
                                    text = " (×${component.num})",
                                    fontFamily = BeVietnamPro,
                                    fontSize = 11.sp,
                                    color = AppColors.Primary,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }

                        if (isEditing) {
                            OutlinedTextField(
                                value = editValue,
                                onValueChange = { if (it.all { c -> c.isDigit() }) editValue = it },
                                modifier = Modifier.width(100.dp),
                                textStyle = TextStyle(fontSize = 12.sp, fontFamily = BeVietnamPro),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = {
                                    val newVal = editValue.toIntOrNull() ?: stepsDone
                                    onSetSteps(newVal)
                                    isEditing = false
                                }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedBorderColor = AppColors.Primary,
                                    unfocusedBorderColor = AppColors.OutlineVariant
                                )
                            )
                        } else {
                            if(component.num>1){
                                Text(
                                    text = "${stepsDone%component.steps.size} / ${component.steps.size} steps  (${stepsDone / component.steps.size} done)",
                                    fontFamily = BeVietnamPro,
                                    fontSize = 11.sp,
                                    color = AppColors.OnSurfaceVariant,
                                    modifier = Modifier.clickable { isEditing = true }
                                )
                            }else {
                                Text(
                                    text = "$stepsDone / $stepsTotal steps",
                                    fontFamily = BeVietnamPro,
                                    fontSize = 11.sp,
                                    color = AppColors.OnSurfaceVariant,
                                    modifier = Modifier.clickable { isEditing = true }
                                )
                            }
                        }
                    }
                }

                if (isComplete) {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        "Complete",
                        tint = AppColors.Primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            val currentStepName = if (stepsTotal > 0 && !isComplete) {
                val stepIdx = stepsDone % (component.steps.size.coerceAtLeast(1))
                component.steps.getOrNull(stepIdx) ?: ""
            } else if (isComplete) {
                "Complete!"
            } else {
                ""
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp), // Align with bar
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentStepName,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = AppColors.Primary
                )
            }

            Spacer(Modifier.height(4.dp))

            // Progress bar with buttons on sides
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Decrement Button
                if (stepsDone > 0) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(AppColors.Primary)
                            .clickable { onDecrement() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "-1",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = AppColors.OnPrimary
                        )
                    }
                } else {
                    Spacer(Modifier.size(32.dp))
                }

                // Progress bar
                val animProg by animateFloatAsState(
                    targetValue = progress,
                    animationSpec = tween(500, easing = EaseOutCubic),
                    label = "comp_progress"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(AppColors.OutlineVariant.copy(alpha = 0.25f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animProg)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (isComplete) AppColors.Primary else AppColors.Tertiary)
                    )
                }

                // Increment Button
                if (!isComplete) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(AppColors.Primary)
                            .clickable { onIncrement() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "+1",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = AppColors.OnPrimary
                        )
                    }
                } else {
                    Spacer(Modifier.size(32.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Add Project Sheet
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProjectSheet(
    patterns:  List<Pattern>,
    onDismiss: () -> Unit,
    onSave:    (name: String, patternId: Int) -> Unit
) {
    var name     by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(patterns.firstOrNull()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = AppColors.Background,
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                "New Project",
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize   = 18.sp,
                color      = AppColors.OnSurface
            )
            Spacer(Modifier.height(20.dp))

            SheetLabel("Project Name")
            Spacer(Modifier.height(6.dp))
            StitchedTextField(
                value         = name,
                onValueChange = { name = it },
                placeholder   = "e.g. Mum's Birthday Cardigan"
            )

            if (patterns.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                SheetLabel("Based on Pattern")
                Spacer(Modifier.height(4.dp))
                patterns.forEach { pattern ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (selected?.id == pattern.id) AppColors.PrimaryContainer
                                else Color.Transparent
                            )
                            .clickable { selected = pattern }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RadioButton(
                            selected = selected?.id == pattern.id,
                            onClick  = { selected = pattern },
                            colors   = RadioButtonDefaults.colors(
                                selectedColor   = AppColors.Primary,
                                unselectedColor = AppColors.OutlineVariant
                            )
                        )
                        Text(
                            text       = pattern.name,
                            fontFamily = BeVietnamPro,
                            fontWeight = FontWeight.Medium,
                            fontSize   = 14.sp,
                            color      = AppColors.OnSurface
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(12.dp))
                Text(
                    "No patterns yet — add a pattern first",
                    fontFamily = BeVietnamPro,
                    fontSize   = 13.sp,
                    color      = AppColors.OnSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            Spacer(Modifier.height(28.dp))
            WoodenButton(
                text    = "Create Project",
                onClick = { if (name.isNotBlank() && selected != null) onSave(name, selected!!.id) },
                enabled = name.isNotBlank() && selected != null
            )
        }
    }
}

@Composable
fun ProjectEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier             = modifier,
        horizontalAlignment  = Alignment.CenterHorizontally,
        verticalArrangement  = Arrangement.Center
    ) {
        Text("🧶", fontSize = 52.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "No projects yet",
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize   = 18.sp,
            color      = AppColors.OnSurface.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Tap + to start tracking your first project",
            fontFamily = BeVietnamPro,
            fontSize   = 13.sp,
            color      = AppColors.OnSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun AddProjectDialog(
    patterns:  List<Pattern>,
    onDismiss: () -> Unit,
    onSave:    (name: String, patternId: Int) -> Unit
) {
    var name     by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(patterns.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text("New Project", fontWeight = FontWeight.Bold) },
        text    = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Project Name") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                Text("Pattern:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                patterns.forEach { pattern ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = pattern.id == selected.id,
                            onClick  = { selected = pattern }
                        )
                        Text(pattern.name)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onSave(name, selected.id) }) {
                Text("Create")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}


// ViewModel helper for incrementing project step
fun MainViewModel.incrementProjectStep(project: Project, stepIndex: Int) {
    viewModelScope.launch { projectDao.incrementStep(project.id, stepIndex) }
}

fun MainViewModel.decrementProjectStep(project: Project, stepIndex: Int) {
    viewModelScope.launch { projectDao.decrementStep(project.id, stepIndex) }
}
fun MainViewModel.setProjectStep(project: Project, stepIndex: Int, num: Int) {
    viewModelScope.launch { projectDao.setSteps(project.id, stepIndex, num) }
}

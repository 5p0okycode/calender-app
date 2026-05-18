package com.crochet.calendar

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.viewModelScope
import com.crochet.calendar.ui.DashedDivider
import com.crochet.calendar.ui.GrainOverlay
import com.crochet.calendar.ui.StitchedCard
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// Patterns Screen
// Layout based on the HTML design: stitch-bordered cards, warm earthy palette,
// FAB for adding, event-card style rows for each pattern.
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatternsScreen(viewModel: MainViewModel) {

    val patterns        by viewModel.patterns.collectAsState()
    val allComponents   by viewModel.allComponents.collectAsState()
    var showAddSheet    by remember { mutableStateOf(false) }
    var selectedPattern by remember { mutableStateOf<Pattern?>(null) }

    // If a pattern is selected, show its detail screen
    if (selectedPattern != null) {
        PatternDetailScreen(
            pwc       = selectedPattern!!,
            viewModel = viewModel,
            onBack    = { selectedPattern = null }
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
                            text       = "My Patterns",
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
                                text       = "${patterns.size}",
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
                        .shadow(
                            elevation    = 12.dp,
                            shape        = CircleShape,
                            ambientColor = AppColors.Primary.copy(alpha = 0.3f),
                            spotColor    = AppColors.Primary.copy(alpha = 0.5f)
                        )
                ) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = "Add pattern",
                        modifier           = Modifier.size(28.dp)
                    )
                }
            }
        ) { padding ->
            if (patterns.isEmpty()) {
                PatternEmptyState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            } else {
                LazyColumn(
                    modifier            = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(patterns, key = { it.id }) { pwc ->
                        val compCount = allComponents.count { it.patternId == pwc.id }
                        PatternCard(
                            pwc            = pwc,
                            componentCount = compCount,
                            onClick        = { selectedPattern = pwc },
                            onDelete       = { viewModel.deletePattern(pwc) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showAddSheet) {
        AddPatternSheet(
            onDismiss = { showAddSheet = false },
            onSave    = { name, notes ->
                viewModel.addPattern(name, notes)
                showAddSheet = false
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Pattern Card — ports the .stitch-wrap event card from HTML
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PatternCard(
    pwc:            Pattern,
    componentCount: Int,
    onClick:        () -> Unit,
    onDelete:       () -> Unit
) {
    StitchedCard(
        modifier = Modifier.fillMaxWidth(),
        stitchColor = AppColors.StitchBrown,
        cornerRadius = 12.dp,
        inset = 5.dp,
        dashLength = 12f,
        gapLength = 6f,
        strokeWidth = 2.dp,
        surfaceColor = AppColors.SurfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AppColors.Secondary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.GridView,
                    contentDescription = null,
                    tint = AppColors.Secondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pwc.name,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = AppColors.OnSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "$componentCount component${if (componentCount != 1) "s" else ""}",
                    fontFamily = BeVietnamPro,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = AppColors.OnSurfaceVariant
                )
                if (pwc.notes.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = pwc.notes,
                        fontFamily = BeVietnamPro,
                        fontSize = 11.sp,
                        color = AppColors.OnSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Delete pattern",
                    tint = AppColors.OutlineVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Pattern Detail Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatternDetailScreen(
    pwc:       Pattern,
    viewModel: MainViewModel,
    onBack:    () -> Unit
) {
    var showAddComponent by remember { mutableStateOf(false) }
    var editingComponent by remember { mutableStateOf<Component?>(null) }
    val components by viewModel.componentDao.getComponentsForProject(pwc.id).collectAsState(initial = emptyList<Component>())

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
                                Icons.Outlined.ChevronLeft,
                                contentDescription = "Back",
                                tint               = AppColors.OnSurfaceVariant
                            )
                        }
                        Text(
                            text       = pwc.name,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 18.sp,
                            color      = Color(0xFF2D5016),
                            modifier   = Modifier.weight(1f)
                        )
                    }
                    DashedDivider()
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick        = { showAddComponent = true },
                    containerColor = AppColors.Primary,
                    contentColor   = AppColors.OnPrimary,
                    shape          = CircleShape,
                    modifier       = Modifier.size(60.dp).shadow(12.dp, CircleShape,
                        ambientColor = AppColors.Primary.copy(0.3f),
                        spotColor    = AppColors.Primary.copy(0.5f))
                ) {
                    Icon(Icons.Outlined.Add, "Add component", Modifier.size(28.dp))
                }
            }
        ) { padding ->
            LazyColumn(
                modifier            = Modifier.fillMaxSize().padding(padding),
                contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (pwc.notes.isNotBlank()) {
                    item {
                        StitchedCard(
                            modifier = Modifier.fillMaxWidth(),
                            stitchColor = AppColors.StitchTan,
                            cornerRadius = 12.dp,
                            inset = 5.dp,
                            dashLength = 8f,
                            gapLength = 5f,
                            strokeWidth = 1.5.dp,
                            surfaceColor = AppColors.SurfaceContainer
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Outlined.Notes,
                                    null,
                                    tint = AppColors.OutlineVariant,
                                    modifier = Modifier.size(18.dp).padding(top = 2.dp)
                                )
                                Text(
                                    text = pwc.notes,
                                    fontFamily = BeVietnamPro,
                                    fontSize = 13.sp,
                                    color = AppColors.OnSurfaceVariant
                                )
                            }
                        }
                    }
                }

                item {
                    SectionHeader(
                        text     = "Components",
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (components.isEmpty()) {
                    item {
                        EmptyStateCard(message = "No components yet — tap + to add one")
                    }
                } else {
                    items(components, key = { it.id }) { component ->
                        ComponentCard(
                            component = component,
                            onEdit    = { editingComponent = component },
                            onDelete  = { viewModel.removeComponent(component) }
                        )
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showAddComponent) {
        AddComponentSheet(
            onDismiss = { showAddComponent = false },
            onSave    = { name, num, steps ->
                viewModel.addComponent(pwc.id, name, num, steps)
                showAddComponent = false
            }
        )
    }

    if (editingComponent != null) {
        ComponentMenuDialog(
            component = editingComponent!!,
            onDismiss = { editingComponent = null },
            onSave    = { updated ->
                viewModel.updateComponent(updated)
                editingComponent = null
            },
            onDelete  = { comp ->
                viewModel.removeComponent(comp)
                editingComponent = null
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Component Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ComponentCard(
    component: Component,
    onEdit:    () -> Unit,
    onDelete:  () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    StitchedCard(
        modifier = Modifier.fillMaxWidth(),
        stitchColor = AppColors.StitchGreen,
        cornerRadius = 12.dp,
        inset = 5.dp,
        dashLength = 10f,
        gapLength = 6f,
        strokeWidth = 1.5.dp,
        surfaceColor = AppColors.SurfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(14.dp)
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
                            .background(AppColors.Tertiary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Layers,
                            null,
                            tint = AppColors.Tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = component.name,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = AppColors.OnSurface
                        )
                        if(component.num >1){
                        Text(
                            text = "${component.steps.size} Steps ·  (Make ${component.num})",
                            fontFamily = BeVietnamPro,
                            fontSize = 11.sp,
                            color = AppColors.OnSurfaceVariant
                        )
                        } else {
                        Text(
                            text = "${component.steps.size} Steps",
                            fontFamily = BeVietnamPro,
                            fontSize = 11.sp,
                            color = AppColors.OnSurfaceVariant
                        )
                        }

                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        null,
                        tint = AppColors.OutlineVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))

                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Outlined.MoreVert, "More",
                                tint = AppColors.OutlineVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Component") },
                                onClick = {
                                    showMenu = false
                                    onEdit()
                                },
                                leadingIcon = { Icon(Icons.Outlined.Edit, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = Color.Red) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        null,
                                        tint = Color.Red
                                    )
                                }
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    component.steps.forEachIndexed { index, step ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.PrimaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = AppColors.OnPrimaryContainer
                                )
                            }
                            Text(
                                text = step,
                                fontFamily = BeVietnamPro,
                                fontSize = 13.sp,
                                color = AppColors.OnSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PatternEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier             = modifier,
        horizontalAlignment  = Alignment.CenterHorizontally,
        verticalArrangement  = Arrangement.Center
    ) {
        Text("📐", fontSize = 52.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text       = "No patterns yet",
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize   = 18.sp,
            color      = AppColors.OnSurface.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text       = "Tap + to save your first crochet pattern",
            fontFamily = BeVietnamPro,
            fontSize   = 13.sp,
            color      = AppColors.OnSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sheets
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPatternSheet(
    onDismiss: () -> Unit,
    onSave:    (name: String, notes: String) -> Unit
) {
    var name  by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

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
                "New Pattern",
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize   = 18.sp,
                color      = AppColors.OnSurface
            )
            Spacer(Modifier.height(20.dp))

            SheetLabel("Pattern Name")
            Spacer(Modifier.height(6.dp))
            StitchedTextField(
                value         = name,
                onValueChange = { name = it },
                placeholder   = "e.g. Granny Square Blanket"
            )

            Spacer(Modifier.height(14.dp))
            SheetLabel("Notes (optional)")
            Spacer(Modifier.height(6.dp))
            StitchedTextField(
                value         = notes,
                onValueChange = { notes = it },
                placeholder   = "Yarn weight, needle size…",
                singleLine    = false
            )

            Spacer(Modifier.height(28.dp))
            WoodenButton(
                text    = "Save Pattern",
                onClick = { if (name.isNotBlank()) onSave(name, notes) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddComponentSheet(
    onDismiss: () -> Unit,
    onSave:    (name: String, num: Int, steps: List<String>) -> Unit
) {
    var name      by remember { mutableStateOf("") }
    var numInput  by remember { mutableStateOf("1") }
    var stepInput by remember { mutableStateOf("") }
    var steps     by remember { mutableStateOf(listOf<String>()) }
    val keyboard  = LocalSoftwareKeyboardController.current

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
                "New Component",
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize   = 18.sp,
                color      = AppColors.OnSurface
            )
            Spacer(Modifier.height(20.dp))

            SheetLabel("Component Name")
            Spacer(Modifier.height(6.dp))
            StitchedTextField(
                value         = name,
                onValueChange = { name = it },
                placeholder   = "e.g. Left Sleeve"
            )

            Spacer(Modifier.height(14.dp))
            SheetLabel("Quantity (How many needed?)")
            Spacer(Modifier.height(6.dp))
            StitchedTextField(
                value         = numInput,
                onValueChange = { if (it.all { c -> c.isDigit() }) numInput = it },
                placeholder   = "e.g. 2",
                singleLine    = true
            )

            Spacer(Modifier.height(14.dp))
            SheetLabel("Add Steps")
            Spacer(Modifier.height(6.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                StitchedTextField(
                    value         = stepInput,
                    onValueChange = { stepInput = it },
                    placeholder   = "e.g. Cast on 20 stitches",
                    modifier      = Modifier.weight(1f),
                    onDone        = {
                        if (stepInput.isNotBlank()) {
                            steps = steps + stepInput.trim()
                            stepInput = ""
                        }
                    }
                )
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AppColors.Primary)
                        .clickable {
                            if (stepInput.isNotBlank()) {
                                steps = steps + stepInput.trim()
                                stepInput = ""
                                keyboard?.hide()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Add, "Add step", tint = AppColors.OnPrimary, modifier = Modifier.size(22.dp))
                }
            }

            if (steps.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                steps.forEachIndexed { i, step ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(AppColors.PrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${i + 1}",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize   = 10.sp,
                                color      = AppColors.OnPrimaryContainer
                            )
                        }
                        Text(
                            text       = step,
                            fontFamily = BeVietnamPro,
                            fontSize   = 13.sp,
                            color      = AppColors.OnSurfaceVariant,
                            modifier   = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick  = { steps = steps.toMutableList().also { it.removeAt(i) } },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Outlined.Close, "Remove", tint = AppColors.OutlineVariant, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            WoodenButton(
                text    = "Add Component",
                onClick = { 
                    val quantity = numInput.toIntOrNull() ?: 1
                    if (name.isNotBlank() && steps.isNotEmpty()) onSave(name, quantity, steps) 
                },
                enabled = name.isNotBlank() && steps.isNotEmpty()
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared small UI components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier              = modifier
    ) {
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(1.dp)
                .background(AppColors.OutlineVariant)
        )
        Text(
            text       = text,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize   = 14.sp,
            color      = AppColors.OnSurface.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun EmptyStateCard(message: String, modifier: Modifier = Modifier) {
    StitchedCard(
        modifier = modifier.fillMaxWidth(),
        stitchColor = AppColors.StitchTan,
        cornerRadius = 12.dp,
        inset = 5.dp,
        dashLength = 8f,
        gapLength = 5f,
        strokeWidth = 1.5.dp,
        surfaceColor = AppColors.SurfaceContainerLow.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Outlined.GridView,
                null,
                tint = AppColors.OutlineVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = message,
                fontFamily = BeVietnamPro,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = AppColors.OnSurfaceVariant
            )
        }
    }
}

@Composable
fun SheetLabel(text: String) {
    Text(
        text          = text.uppercase(),
        fontFamily    = PlusJakartaSans,
        fontWeight    = FontWeight.Bold,
        fontSize      = 10.sp,
        color         = AppColors.OnSurfaceVariant,
        letterSpacing = 0.8.sp
    )
}

@Composable
fun StitchedTextField(
    value:         String,
    onValueChange: (String) -> Unit,
    placeholder:   String,
    modifier:      Modifier  = Modifier.fillMaxWidth(),
    singleLine:    Boolean   = true,
    onDone:        (() -> Unit)? = null
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        placeholder   = {
            Text(
                placeholder,
                fontFamily = BeVietnamPro,
                fontSize   = 14.sp,
                color      = AppColors.OnSurfaceVariant.copy(alpha = 0.45f)
            )
        },
        singleLine    = singleLine,
        keyboardOptions = KeyboardOptions(imeAction = if (singleLine) ImeAction.Done else ImeAction.Default),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
        shape         = RoundedCornerShape(10.dp),
        colors        = OutlinedTextFieldDefaults.colors(
            focusedBorderColor      = AppColors.Primary.copy(alpha = 0.5f),
            unfocusedBorderColor    = AppColors.OutlineVariant.copy(alpha = 0.5f),
            focusedContainerColor   = AppColors.SurfaceContainer,
            unfocusedContainerColor = AppColors.SurfaceContainer,
            cursorColor             = AppColors.Primary,
            focusedTextColor        = AppColors.OnSurface,
            unfocusedTextColor      = AppColors.OnSurface
        ),
        textStyle     = androidx.compose.ui.text.TextStyle(
            fontFamily = BeVietnamPro,
            fontSize   = 14.sp
        ),
        modifier      = modifier
    )
}

@Composable
fun WoodenButton(
    text:    String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Button(
        onClick   = onClick,
        enabled   = enabled,
        modifier  = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(
                elevation    = if (enabled) 8.dp else 0.dp,
                shape        = RoundedCornerShape(12.dp),
                ambientColor = AppColors.Primary.copy(alpha = 0.3f),
                spotColor    = AppColors.Primary.copy(alpha = 0.4f)
            ),
        shape     = RoundedCornerShape(12.dp),
        colors    = ButtonDefaults.buttonColors(
            containerColor         = AppColors.Primary,
            contentColor           = AppColors.OnPrimary,
            disabledContainerColor = AppColors.Primary.copy(alpha = 0.4f),
            disabledContentColor   = AppColors.OnPrimary.copy(alpha = 0.5f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation  = 6.dp,
            pressedElevation  = 2.dp
        )
    ) {
        Text(
            text       = text,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize   = 15.sp
        )
    }
}

@Composable
fun AddPatternDialog(
    onDismiss: () -> Unit,
    onSave:    (name: String, notes: String) -> Unit
) {
    var name  by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text("New Pattern", fontWeight = FontWeight.Bold) },
        text    = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Pattern Name") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value         = notes,
                    onValueChange = { notes = it },
                    label         = { Text("Notes (optional)") },
                    modifier      = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onSave(name, notes) }) {
                Text("Create")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentMenuDialog(
    component: Component,
    onDismiss: () -> Unit,
    onSave:    (Component) -> Unit,
    onDelete:  (Component) -> Unit
) {
    var name      by remember { mutableStateOf(component.name) }
    var numInput  by remember { mutableStateOf(component.num.toString()) }
    var currentComponent by remember { mutableStateOf(component) }
    var stepInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = AppColors.Background,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    "Edit Component",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 20.sp,
                    color      = AppColors.OnSurface
                )
                IconButton(onClick = { onDelete(component) }) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Delete component",
                        tint               = Color(0xFFB3261E)
                    )
                }
            }
        },
        text = {
            Column(
                modifier            = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    SheetLabel("Component Name")
                    Spacer(Modifier.height(6.dp))
                    StitchedTextField(
                        value         = name,
                        onValueChange = { name = it },
                        placeholder   = "e.g. Left Sleeve"
                    )
                }

                Column {
                    SheetLabel("Quantity")
                    Spacer(Modifier.height(6.dp))
                    StitchedTextField(
                        value         = numInput,
                        onValueChange = { if (it.all { c -> c.isDigit() }) numInput = it },
                        placeholder   = "e.g. 2"
                    )
                }

                Column {
                    SheetLabel("Steps")
                    Spacer(Modifier.height(8.dp))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        currentComponent.steps.forEachIndexed { i, step ->
                            Row(
                                modifier              = Modifier
                                    .fillMaxWidth()
                                    .background(AppColors.Surface, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Drag Handle / Order Indicator
                                Icon(
                                    Icons.Outlined.DragIndicator,
                                    contentDescription = null,
                                    tint = AppColors.OutlineVariant,
                                    modifier = Modifier.size(20.dp)
                                )

                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(AppColors.PrimaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "${i + 1}",
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize   = 12.sp,
                                        color      = AppColors.OnPrimaryContainer
                                    )
                                }
                                
                                Text(
                                    text       = step,
                                    modifier   = Modifier.weight(1f),
                                    fontFamily = BeVietnamPro,
                                    fontSize   = 14.sp,
                                    color      = AppColors.OnSurface
                                )
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Move Up
                                    IconButton(
                                        onClick  = { currentComponent = currentComponent.moveStep(i, false) },
                                        enabled  = i > 0,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.KeyboardArrowUp, 
                                            null, 
                                            modifier = Modifier.size(18.dp), 
                                            tint = if(i > 0) AppColors.Primary else AppColors.OutlineVariant.copy(alpha = 0.3f)
                                        )
                                    }

                                    // Move Down
                                    IconButton(
                                        onClick  = { currentComponent = currentComponent.moveStep(i, true) },
                                        enabled  = i < currentComponent.steps.size - 1,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.KeyboardArrowDown, 
                                            null, 
                                            modifier = Modifier.size(18.dp), 
                                            tint = if(i < currentComponent.steps.size - 1) AppColors.Primary else AppColors.OutlineVariant.copy(alpha = 0.3f)
                                        )
                                    }

                                    IconButton(
                                        onClick  = { currentComponent = currentComponent.removeStep(i) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Outlined.DeleteOutline, null, modifier = Modifier.size(18.dp), tint = Color(0xFFB3261E).copy(alpha = 0.8f))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        StitchedTextField(
                            value         = stepInput,
                            onValueChange = { stepInput = it },
                            placeholder   = "Add a step...",
                            modifier      = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AppColors.Primary)
                                .clickable {
                                    if (stepInput.isNotBlank()) {
                                        currentComponent = currentComponent.addStep(stepInput.trim())
                                        stepInput = ""
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Add, null, tint = AppColors.OnPrimary, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(currentComponent.copy(
                            name  = name.trim(),
                            num   = numInput.toIntOrNull() ?: 1
                        ))
                    }
                }
            ) {
                Text("Save Changes", color = AppColors.Primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = AppColors.OnSurfaceVariant)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatternsTab(viewModel: MainViewModel) {
    val patterns by viewModel.patterns.collectAsState()
    var showAdd  by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Patterns", fontWeight = FontWeight.Bold) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }, shape = CircleShape) {
                Icon(Icons.Outlined.Add, "Add pattern")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier       = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp)
        ) {
            if (patterns.isEmpty()) {
                item { Text("No patterns yet — tap + to add one", color = Color.Gray, fontSize = 13.sp) }
            } else {
                items(patterns, key = { it.id }) { pattern ->
                    PatternRow(
                        pattern = pattern,
                        onDelete = { viewModel.deletePattern(pattern) },
                        onCompMenu = { /* No-op or handle appropriately */ }
                    )
                }
            }
        }
    }

    if (showAdd) {
        AddPatternDialog(
            onDismiss = { showAdd = false },
            onSave    = { name, notes ->
                viewModel.addPattern(name, notes)
                showAdd = false
            }
        )
    }
}



// ─────────────────────────────────────────────────────────────────────────────
// ViewModel Helpers
// ─────────────────────────────────────────────────────────────────────────────

fun MainViewModel.addComponent(patternId: Int, name: String, num: Int, steps: List<String>) {
    if (name.isBlank() || steps.isEmpty()) return
    viewModelScope.launch {
        componentDao.insertComponent(
            Component(patternId = patternId, name = name.trim(), num = num, steps = steps)
        )
    }
}

fun MainViewModel.updateComponent(component: Component) {
    viewModelScope.launch { componentDao.updateComponent(component) }
}

fun MainViewModel.removeComponent(component: Component) {
    viewModelScope.launch { componentDao.deleteComponent(component) }
}

package com.crochet.calendar

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Yard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.crochet.calendar.ui.DashedDivider
import com.crochet.calendar.ui.GrainOverlay
import com.crochet.calendar.ui.stitchBorder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────────────────

data class CalendarUiState(
    val monthLabel:     String   = "",
    val daysInMonth:    Int      = 30,
    val firstDayOfWeek: Int      = 0,
    val actualDay:      Int      = -1,   // today's day number, -1 if not in viewed month
    val curDay:         Int      = 1,    // selected day
    val viewYear:       Int      = 0,
    val viewMonth:      Int      = 0,
    val daysWithEvents: Set<Int> = emptySet(),
    val slideDir:       Int      = 0    // -1 prev | 0 none | 1 next
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {

    internal val db    = Room.databaseBuilder(app, CalendarDatabase::class.java, "crochet_db").build()
    private val logic = CalendarLogic()

    val eventDao   = db.eventDao()
    val patternDao   = db.patternDao()
    val projectDao   = db.projectDao()
    val componentDao = db.componentDao()

    private val _uiState = MutableStateFlow(buildUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    // Events for the currently selected day
    val selectedDayEvents: StateFlow<List<Event>> = _uiState
        .flatMapLatest { s -> eventDao.getEventsForDay(s.viewYear, s.viewMonth, s.curDay) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // All patterns
    val patterns: StateFlow<List<Pattern>> = patternDao
        .getAllPatterns()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // All projects
    val projects: StateFlow<List<Project>> = projectDao
        .getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // All components
    val allComponents: StateFlow<List<Component>> = componentDao
        .getAllComponents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // Keep event dots up to date when the viewed month changes
        viewModelScope.launch {
            _uiState
                .map { it.viewYear to it.viewMonth }
                .distinctUntilChanged()
                .collectLatest { (year, month) ->
                    eventDao.getDaysWithEvents(year, month)
                        .collect { days -> _uiState.update { it.copy(daysWithEvents = days.toSet()) } }
                }
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    fun nextMonth() {
        logic.nextMonth()
        _uiState.value = buildUiState(slideDir = 1)
    }

    fun prevMonth() {
        logic.prevMonth()
        _uiState.value = buildUiState(slideDir = -1)
    }

    fun selectDay(day: Int) {
        logic.curDay = day
        _uiState.update { it.copy(curDay = day) }
    }

    // ── Event CRUD ────────────────────────────────────────────────────────────

    fun addEvent(name: String, time: String, reminder: Boolean, projectId: Int? = null) {
        if (name.isBlank()) return
        val s = _uiState.value
        viewModelScope.launch {
            eventDao.insertEvent(
                Event(
                    name      = name.trim(),
                    year      = s.viewYear,
                    month     = s.viewMonth,
                    day       = s.curDay,
                    time      = time,
                    reminder  = reminder,
                    projectId = projectId
                )
            )
        }
    }

    fun deleteEvent(event: Event) {
        viewModelScope.launch { eventDao.deleteEvent(event) }
    }

    fun updateEvent(event: Event) {
        viewModelScope.launch { eventDao.insertEvent(event) }
    }

    // ── Pattern CRUD ──────────────────────────────────────────────────────────

    fun addPattern(name: String, notes: String = "") {
        if (name.isBlank()) return
        viewModelScope.launch { patternDao.insertPattern(Pattern(name = name.trim(), notes = notes)) }
    }

    fun deletePattern(pattern: Pattern) {
        viewModelScope.launch { patternDao.deletePattern(pattern) }
    }

    // ── Project CRUD ──────────────────────────────────────────────────────────

    fun addProject(name: String, patternId: Int) {
        if (name.isBlank()) return
        viewModelScope.launch { projectDao.insertProject(Project(name = name.trim(), patternId = patternId, curComp = 0)) }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch { projectDao.deleteProject(project) }
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private fun buildUiState(slideDir: Int = 0) = CalendarUiState(
        monthLabel     = logic.monthLabel,
        daysInMonth    = logic.daysInMonth,
        firstDayOfWeek = logic.firstDayOfWeek - 1, // JC is 1-based, grid needs 0-based
        actualDay      = if (logic.curYear  == logic.todayYear &&
                             logic.curMonth == logic.todayMonth) logic.todayDay else -1,
        curDay         = logic.curDay,
        viewYear       = logic.curYear,
        viewMonth      = logic.curMonth,
        slideDir       = slideDir
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Activity
// ─────────────────────────────────────────────────────────────────────────────

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                AppRoot(viewModel)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Navigation tabs
// ─────────────────────────────────────────────────────────────────────────────

private enum class Tab { CALENDAR, PROJECTS, PATTERNS }

@Composable
fun AppRoot(viewModel: MainViewModel) {
    var currentTab by remember { mutableStateOf(Tab.CALENDAR) }

    Scaffold(
        bottomBar = {
            Box(contentAlignment = Alignment.TopCenter){
            NavigationBar(
                modifier = Modifier
                    .padding(top = 8.dp),
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = currentTab == Tab.CALENDAR,
                    onClick  = { currentTab = Tab.CALENDAR },
                    icon     = { Icon(Icons.Outlined.CalendarMonth, "Calendar") },
                    label    = { Text("Calendar", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }

                )
                NavigationBarItem(
                    selected = currentTab == Tab.PROJECTS,
                    onClick  = { currentTab = Tab.PROJECTS },
                    icon     = { Icon(Icons.Outlined.Yard, "Projects") },
                    label    = { Text("Projects", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
                )
                NavigationBarItem(
                    selected = currentTab == Tab.PATTERNS,
                    onClick  = { currentTab = Tab.PATTERNS },
                    icon     = { Icon(Icons.Outlined.GridView, "Patterns") },
                    label    = { Text("Patterns", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
                )
            }
                DashedDivider(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-1).dp)
                        .graphicsLayer(scaleY = -1f)
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentTab) {
                Tab.CALENDAR -> CalendarTab(viewModel)
                Tab.PROJECTS -> ProjectsScreen(viewModel)
                Tab.PATTERNS -> PatternsScreen(viewModel)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Calendar Tab
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarTab(viewModel: MainViewModel) {
    val uiState  by viewModel.uiState.collectAsState()
    val events   by viewModel.selectedDayEvents.collectAsState()
    val projects by viewModel.projects.collectAsState()

    var showAdd  by remember { mutableStateOf(false) }
    var showOptions by remember { mutableStateOf(false) }

    var editingEvent by remember { mutableStateOf<Event?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        GrainOverlay()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.Background.copy(alpha = 0.85f)) // backdrop-blur effect
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left Section: Settings Button
                        IconButton(
                            onClick = { showOptions = true },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.Transparent, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Options",
                                tint = AppColors.OnSurfaceVariant,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Right Section: Stitched Profile Placeholder
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(AppColors.SurfaceContainer)
                                .stitchBorder(
                                    color = AppColors.StitchGreen,
                                    cornerRadiusDp = 999f,
                                    insetDp = 3f,
                                    dashLength = 6f,
                                    gapLength = 4f,
                                    strokeWidthDp = 1.5f
                                )
                        )
                    }

                    // Absolute Center Title
                    Text(
                        text = uiState.monthLabel,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = AppColors.Primary,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    // Hand-drawn bottom divider
                    DashedDivider(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 6.dp)
                    )
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAdd = true },
                    containerColor = AppColors.Primary,
                    contentColor = AppColors.OnPrimary,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(56.dp)
                        .shadow(
                            12.dp, CircleShape,
                            ambientColor = AppColors.Primary.copy(0.3f),
                            spotColor = AppColors.Primary.copy(0.5f)
                        )
                ) {
                    Icon(Icons.Default.Add, "Add event")
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    // Navigation arrows for month
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.prevMonth() }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Prev", tint = AppColors.Tertiary)
                        }
                        
                        Button(
                            onClick = { /* Change Background action */ },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppColors.StitchBrown,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Change Background", fontSize = 11.sp, fontFamily = BeVietnamPro)
                        }

                        IconButton(onClick = { viewModel.nextMonth() }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next", tint = AppColors.Tertiary)
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                16.dp, RoundedCornerShape(24.dp),
                                ambientColor = Color.Black.copy(0.05f),
                                spotColor = Color.Black.copy(0.1f)
                            ),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        CalendarGrid(
                            daysInMonth    = uiState.daysInMonth,
                            firstDayOfWeek = uiState.firstDayOfWeek,
                            actualDay      = uiState.actualDay,
                            curDay         = uiState.curDay,
                            daysWithEvents = uiState.daysWithEvents,
                            slideDir       = uiState.slideDir,
                            onDayClick     = { viewModel.selectDay(it) },
                            modifier       = Modifier.padding(16.dp)
                        )
                    }
                    
                    Spacer(Modifier.height(32.dp))
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier
                            .width(20.dp)
                            .height(1.dp)
                            .background(AppColors.OutlineVariant))
                        Text(
                            if (uiState.curDay > uiState.actualDay) " Future Thread " else if (uiState.curDay < uiState.actualDay) "Past Thread" else " Today's Thread ",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = AppColors.OnSurfaceVariant
                        )
                        Box(modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(AppColors.OutlineVariant))
                    }
                    Spacer(Modifier.height(12.dp))
                }

                if (events.isEmpty()) {
                    item {
                        Text(
                            "No plans for this day",
                            fontFamily = BeVietnamPro,
                            color = AppColors.OnSurfaceVariant.copy(alpha = 0.5f),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                } else {
                    items(events, key = { it.id }) { event ->
                        val linkedProject = projects.find { it.id == event.projectId }
                        EventRow(
                            event = event,
                            projectName = linkedProject?.name,
                            onEventMenu = { editingEvent = event },
                            onDelete = { viewModel.deleteEvent(event) }
                        )
                    }
                }
                
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showAdd) {
        AddEventDialog(
            projects  = projects,
            onDismiss = { showAdd = false },
            onSave    = { name, time, reminder, projectId ->
                viewModel.addEvent(name, time, reminder, projectId)
                showAdd = false
            }
        )
    }

    if (showOptions) {
        OptionDialog(
            onDismiss = { showOptions = false }
        )
    }

    if (editingEvent != null) {
        EventMenuDialog(
            event = editingEvent!!,
            projects = projects,
            onDismiss = { editingEvent = null },
            onSave = { updatedEvent: Event ->
                viewModel.updateEvent(updatedEvent)
                editingEvent = null
            }
        )
    }
    
}

@Composable
private fun EventRow(
    event: Event,
    projectName: String? = null,
    onEventMenu: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceContainerLow.copy(alpha = 0.8f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppColors.TertiaryContainer.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "yarn",
                    fontSize = 10.sp,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Tertiary
                )
            }
            
            Spacer(Modifier.width(12.dp))
            
            Column(Modifier.weight(1f)) {
                Text(
                    text = event.name,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold, 
                    fontSize = 14.sp,
                    color = AppColors.OnSurface
                )
                Text(
                    text = if (projectName != null) "${event.time ?: "All day"} — $projectName" else event.time ?: "All day",
                    fontFamily = BeVietnamPro,
                    color = AppColors.OnSurfaceVariant,
                    fontSize = 12.sp
                )
            }
            
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, "More", tint = AppColors.OutlineVariant)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit Event") },
                        onClick = {
                            showMenu = false
                            onEventMenu()
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, null) }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Delete", color = Color.Red) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                    )
                }
            }
        }
    }
}



// ─────────────────────────────────────────────────────────────────────────────
// Projects Tab
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsTab(viewModel: MainViewModel) {
    val projects by viewModel.projects.collectAsState()
    var showAdd  by remember { mutableStateOf(false) }
    val patterns by viewModel.patterns.collectAsState()

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Projects", fontWeight = FontWeight.Bold) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }, shape = CircleShape) {
                Icon(Icons.Outlined.Add, "Add project")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier       = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp)
        ) {
            if (projects.isEmpty()) {
                item { Text("No projects yet — tap + to create one", color = Color.Gray, fontSize = 13.sp) }
            } else {
                items(projects, key = { it.id }) { project ->
                    val pattern = patterns.find { it.id == project.patternId }
                    ProjectRow(project = project, patternName = pattern?.name, onDelete = { viewModel.deleteProject(project) })
                }
            }
        }
    }

    if (showAdd && patterns.isNotEmpty()) {
        AddProjectDialog(
            patterns  = patterns,
            onDismiss = { showAdd = false },
            onSave    = { name, patternId ->
                viewModel.addProject(name, patternId)
                showAdd = false
            }
        )
    }
}

@Composable
private fun ProjectRow(project: Project, patternName: String?, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(project.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            patternName?.let { Text("Pattern: $it", color = Color.Gray, fontSize = 12.sp) }
        }
        TextButton(onClick = onDelete) { Text("✕", color = Color.Gray) }
    }
    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
}


// ─────────────────────────────────────────────────────────────────────────────
// Patterns Tab
// ─────────────────────────────────────────────────────────────────────────────



@Composable
fun PatternRow(pattern: Pattern, onCompMenu: ()-> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(pattern.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
        TextButton(onClick = onCompMenu) { Text(":", color = Color.Gray) }
    }
    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
}
@Composable
private fun OptionDialog(
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text("Options", fontWeight = FontWeight.Bold) },
        text    = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ListItem(
                    headlineContent = { Text("Schedule", fontFamily = PlusJakartaSans) },
                    leadingContent = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = AppColors.Primary) },
                    modifier = Modifier.clickable { /* Handle Schedule */ }
                )
                ListItem(
                    headlineContent = { Text("Reminders", fontFamily = PlusJakartaSans) },
                    leadingContent = { Icon(Icons.Default.Notifications, contentDescription = null, tint = AppColors.Primary) },
                    modifier = Modifier.clickable { /* Handle Reminders */ }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

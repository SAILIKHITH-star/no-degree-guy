package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.Course
import com.example.data.CourseSubject
import com.example.data.Doubt
import com.example.data.DoubtStatus
import com.example.data.FeeCycle
import com.example.data.InstituteConfig
import com.example.data.MaterialSubject
import com.example.data.MaterialType
import com.example.data.StudyMaterial
import com.example.data.UiState
import com.example.ui.components.AccentPill
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorBanner
import com.example.ui.components.SectionHeader
import com.example.ui.components.formatTimestampShort
import com.example.ui.components.materialAccentColor
import com.example.ui.components.subjectAccentColor
import com.example.ui.theme.LuminousTeal
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusVerified
import com.example.ui.theme.WhatsAppGreen
import com.example.util.ContactHelper
import com.example.viewmodel.StudyCircleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoursesScreen(
    viewModel: StudyCircleViewModel,
    initialSubTab: Int = 0
) {
    val context = LocalContext.current
    var selectedSubTab by remember { mutableIntStateOf(initialSubTab) }
    LaunchedEffect(initialSubTab) {
        selectedSubTab = initialSubTab
    }

    val configState by viewModel.instituteConfig.collectAsStateWithLifecycle()
    val coursesState by viewModel.courses.collectAsStateWithLifecycle()
    val enrollmentsState by viewModel.enrollments.collectAsStateWithLifecycle()
    val materialsState by viewModel.studyMaterials.collectAsStateWithLifecycle()
    val doubtsState by viewModel.doubts.collectAsStateWithLifecycle()

    val config = (configState as? UiState.Success)?.data ?: InstituteConfig()
    val courses = (coursesState as? UiState.Success)?.data.orEmpty()
    val enrolledCourseIds = remember(enrollmentsState) {
        (enrollmentsState as? UiState.Success)?.data?.map { it.courseId }?.toSet().orEmpty()
    }
    val materials = (materialsState as? UiState.Success)?.data.orEmpty()
    val doubts = (doubtsState as? UiState.Success)?.data.orEmpty()

    var selectedSubjectFilter by remember { mutableStateOf<CourseSubject?>(null) }
    var selectedMaterialSubject by remember { mutableStateOf<MaterialSubject?>(null) }

    var showCreateCourseDialog by remember { mutableStateOf(false) }
    var showCreateMaterialDialog by remember { mutableStateOf(false) }
    var showAskDoubtDialog by remember { mutableStateOf(false) }
    var doubtToAnswer by remember { mutableStateOf<Doubt?>(null) }

    val tabs = listOf("Batches", "DPPs & Notes", "Doubt Desk")

    Column(modifier = Modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            tabs.forEachIndexed { index, label ->
                SegmentedButton(
                    selected = selectedSubTab == index,
                    onClick = { selectedSubTab = index },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
                    modifier = Modifier.testTag("courses_subtab_$index")
                ) {
                    Text(label, maxLines = 1)
                }
            }
        }

        when (selectedSubTab) {
            0 -> BatchesTabContent(
                courses = courses,
                coursesState = coursesState,
                enrolledCourseIds = enrolledCourseIds,
                selectedFilter = selectedSubjectFilter,
                onFilterSelected = { selectedSubjectFilter = it },
                isAdmin = viewModel.isAdmin,
                onAddCourse = { showCreateCourseDialog = true },
                onEnroll = { viewModel.enrollInCourse(it) },
                onDrop = { viewModel.dropEnrollment(it.id) },
                onWhatsAppEnquire = { course ->
                    val cycle = FeeCycle.fromWire(course.feeCycle)
                    val msg = "Hello Rupa IIT / NEET Study Circle, I want to enquire about ${course.title} (${ContactHelper.formatInr(course.feeAmountInr)} ${cycle.shortLabel})."
                    if (!ContactHelper.openWhatsAppChat(context, config.mainPhone, msg)) {
                        viewModel.emitMessage("Could not open WhatsApp for ${config.mainPhone}.")
                    }
                },
                onDeleteCourse = { viewModel.deleteCourse(it.id) }
            )

            1 -> StudyMaterialsTabContent(
                materials = materials,
                materialsState = materialsState,
                selectedSubject = selectedMaterialSubject,
                onSelectSubject = { selectedMaterialSubject = it },
                isAdmin = viewModel.isAdmin,
                onAddMaterial = { showCreateMaterialDialog = true },
                onDeleteMaterial = { viewModel.deleteStudyMaterial(it.id) },
                onOpenUrlFailed = { viewModel.emitMessage("No browser app found to open this link.") }
            )

            else -> DoubtDeskTabContent(
                doubts = doubts,
                doubtsState = doubtsState,
                isAdmin = viewModel.isAdmin,
                onAskDoubt = { showAskDoubtDialog = true },
                onAnswerDoubt = { doubtToAnswer = it },
                onDeleteDoubt = { viewModel.deleteDoubt(it.id) }
            )
        }
    }

    if (showCreateCourseDialog) {
        CreateCourseDialog(
            onDismiss = { showCreateCourseDialog = false },
            onSubmit = { title, subject, exam, instructor, schedule, fee, cycle, room, desc ->
                viewModel.createCourse(title, subject, exam, instructor, schedule, fee, cycle, room, desc)
                showCreateCourseDialog = false
            }
        )
    }

    if (showCreateMaterialDialog) {
        CreateMaterialDialog(
            onDismiss = { showCreateMaterialDialog = false },
            onSubmit = { title, subject, chapter, type, summary, url ->
                viewModel.createStudyMaterial(title, subject, chapter, type, summary, url)
                showCreateMaterialDialog = false
            }
        )
    }

    if (showAskDoubtDialog) {
        AskDoubtDialog(
            onDismiss = { showAskDoubtDialog = false },
            onSubmit = { subject, question ->
                viewModel.submitDoubt(subject, question)
                showAskDoubtDialog = false
            }
        )
    }

    doubtToAnswer?.let { doubt ->
        AnswerDoubtDialog(
            doubt = doubt,
            onDismiss = { doubtToAnswer = null },
            onSubmit = { answer ->
                viewModel.answerDoubt(doubt.id, answer)
                doubtToAnswer = null
            }
        )
    }
}

@Composable
private fun BatchesTabContent(
    courses: List<Course>,
    coursesState: UiState<List<Course>>,
    enrolledCourseIds: Set<String>,
    selectedFilter: CourseSubject?,
    onFilterSelected: (CourseSubject?) -> Unit,
    isAdmin: Boolean,
    onAddCourse: () -> Unit,
    onEnroll: (Course) -> Unit,
    onDrop: (Course) -> Unit,
    onWhatsAppEnquire: (Course) -> Unit,
    onDeleteCourse: (Course) -> Unit
) {
    val filtered = remember(courses, selectedFilter) {
        if (selectedFilter == null) courses
        else courses.filter { it.subject == selectedFilter.wireValue }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "IIT-JEE, NEET & Foundation Batches",
                subtitle = "1st Batch: 6:00–7:30 AM • 2nd Batch: 6:00–8:30 PM • ₹2,500/month",
                actionLabel = if (isAdmin) "+ Add Batch" else null,
                actionTestTag = "add_course_button",
                onActionClick = if (isAdmin) onAddCourse else null
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { onFilterSelected(null) },
                        label = { Text("All (${courses.size})") }
                    )
                }
                items(CourseSubject.entries) { subject ->
                    FilterChip(
                        selected = selectedFilter == subject,
                        onClick = { onFilterSelected(subject) },
                        label = { Text(subject.label) }
                    )
                }
            }
        }

        if (coursesState is UiState.Error) {
            item { ErrorBanner(coursesState.message) }
        } else if (filtered.isEmpty() && coursesState is UiState.Success) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.School,
                    title = "No Batches Found",
                    subtitle = if (isAdmin) "Tap + Add Batch to publish a new IIT-JEE, NEET, Intermediate or Foundation batch." else "Try selecting another course filter."
                )
            }
        } else {
            items(filtered, key = { it.id.ifBlank { it.title } }) { course ->
                CourseDetailCard(
                    course = course,
                    isEnrolled = enrolledCourseIds.contains(course.id),
                    isAdmin = isAdmin,
                    onEnroll = { onEnroll(course) },
                    onDrop = { onDrop(course) },
                    onWhatsAppEnquire = { onWhatsAppEnquire(course) },
                    onDelete = { onDeleteCourse(course) }
                )
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun CourseDetailCard(
    course: Course,
    isEnrolled: Boolean,
    isAdmin: Boolean,
    onEnroll: () -> Unit,
    onDrop: () -> Unit,
    onWhatsAppEnquire: () -> Unit,
    onDelete: () -> Unit
) {
    val subject = CourseSubject.fromWire(course.subject)
    val cycle = FeeCycle.fromWire(course.feeCycle)
    val accent = subjectAccentColor(course.subject)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AccentPill(text = subject.label, accent = accent)
                    if (isEnrolled) {
                        AccentPill(text = "Enrolled", accent = StatusVerified)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = ContactHelper.formatInr(course.feeAmountInr),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = cycle.shortLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isAdmin && course.id.isNotBlank()) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.testTag("delete_course_${course.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Delete batch",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = course.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = course.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CourseMetaRow(Icons.Filled.Person, "Faculty: ${course.instructor}")
                CourseMetaRow(Icons.Filled.Schedule, "Timings: ${course.schedule}")
                CourseMetaRow(Icons.Filled.MeetingRoom, "${course.roomName} • ${course.targetExam}")
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isEnrolled) {
                    OutlinedButton(
                        onClick = onDrop,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("drop_course_${course.id}"),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = StatusVerified,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Enrolled")
                    }
                } else {
                    Button(
                        onClick = onEnroll,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("enroll_course_${course.id}"),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Join Batch")
                    }
                }

                Button(
                    onClick = onWhatsAppEnquire,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("whatsapp_enquire_${course.id}"),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WhatsAppGreen,
                        contentColor = Color.White
                    )
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_whatsapp),
                        contentDescription = stringResource(id = R.string.cd_whatsapp_logo),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("WhatsApp", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun CourseMetaRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StudyMaterialsTabContent(
    materials: List<StudyMaterial>,
    materialsState: UiState<List<StudyMaterial>>,
    selectedSubject: MaterialSubject?,
    onSelectSubject: (MaterialSubject?) -> Unit,
    isAdmin: Boolean,
    onAddMaterial: () -> Unit,
    onDeleteMaterial: (StudyMaterial) -> Unit,
    onOpenUrlFailed: () -> Unit
) {
    val context = LocalContext.current
    val filtered = remember(materials, selectedSubject) {
        if (selectedSubject == null) materials
        else materials.filter { it.subject == selectedSubject.wireValue }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "DPPs, Notes & Formula Sheets",
                subtitle = "Physics, Chemistry, Mathematics & Biology practice material",
                actionLabel = if (isAdmin) "+ Publish" else null,
                actionTestTag = "add_material_button",
                onActionClick = if (isAdmin) onAddMaterial else null
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedSubject == null,
                        onClick = { onSelectSubject(null) },
                        label = { Text("All (${materials.size})") }
                    )
                }
                items(MaterialSubject.entries) { subj ->
                    FilterChip(
                        selected = selectedSubject == subj,
                        onClick = { onSelectSubject(subj) },
                        label = { Text(subj.label) }
                    )
                }
            }
        }

        if (materialsState is UiState.Error) {
            item { ErrorBanner(materialsState.message) }
        } else if (filtered.isEmpty() && materialsState is UiState.Success) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.MenuBook,
                    title = "No Study Materials Yet",
                    subtitle = if (isAdmin) "Tap + Publish to share DPPs, lecture notes, or PDF links." else "New DPPs and formula sheets will appear here."
                )
            }
        } else {
            items(filtered, key = { it.id.ifBlank { it.title } }) { item ->
                var expanded by remember(item.id) { mutableStateOf(false) }
                val subj = MaterialSubject.fromWire(item.subject)
                val type = MaterialType.fromWire(item.materialType)
                val accent = materialAccentColor(item.subject)

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded },
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                AccentPill(text = subj.label, accent = accent)
                                AccentPill(text = type.label, accent = MaterialTheme.colorScheme.primary)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isAdmin && item.id.isNotBlank()) {
                                    IconButton(onClick = { onDeleteMaterial(item) }) {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteOutline,
                                            contentDescription = "Delete material",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = if (expanded) "Collapse" else "Expand",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Chapter: ${item.chapter}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        AnimatedVisibility(visible = expanded) {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = item.summary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (!item.resourceUrl.isNullOrBlank() && ContactHelper.isSafeExternalUrl(item.resourceUrl)) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedButton(
                                        onClick = {
                                            if (!ContactHelper.openSafeUrl(context, item.resourceUrl)) {
                                                onOpenUrlFailed()
                                            }
                                        },
                                        shape = MaterialTheme.shapes.medium
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Open Attached PDF / Resource")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun DoubtDeskTabContent(
    doubts: List<Doubt>,
    doubtsState: UiState<List<Doubt>>,
    isAdmin: Boolean,
    onAskDoubt: () -> Unit,
    onAnswerDoubt: (Doubt) -> Unit,
    onDeleteDoubt: (Doubt) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = if (isAdmin) "Student Doubt Queue" else "My Doubt Desk",
                subtitle = if (isAdmin) "Answer student questions directly" else "Ask Physics, Chemistry, Maths & Biology questions",
                actionLabel = "+ Ask Doubt",
                actionTestTag = "ask_doubt_button",
                onActionClick = onAskDoubt
            )
        }

        if (doubtsState is UiState.Error) {
            item { ErrorBanner(doubtsState.message) }
        } else if (doubts.isEmpty() && doubtsState is UiState.Success) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.QuestionAnswer,
                    title = "No Doubts Yet",
                    subtitle = "Tap + Ask Doubt to submit any Physics, Chemistry, Mathematics, or Biology question to Rupa faculty."
                )
            }
        } else {
            items(doubts, key = { it.id.ifBlank { it.question } }) { doubt ->
                val subject = MaterialSubject.fromWire(doubt.subject)
                val status = DoubtStatus.fromWire(doubt.status)
                val accent = materialAccentColor(doubt.subject)

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                AccentPill(text = subject.label, accent = accent)
                                AccentPill(
                                    text = status.label,
                                    accent = if (status == DoubtStatus.ANSWERED) StatusVerified else StatusPending
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = formatTimestampShort(doubt.createdAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                IconButton(onClick = { onDeleteDoubt(doubt) }) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = "Delete doubt",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Asked by ${doubt.studentName}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = doubt.question,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (!doubt.answer.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = LuminousTeal.copy(alpha = 0.10f),
                                border = BorderStroke(1.dp, LuminousTeal.copy(alpha = 0.25f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Faculty Reply • ${doubt.answeredBy ?: "Shankar Royal. G"}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = LuminousTeal,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = doubt.answer,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        if (isAdmin) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { onAnswerDoubt(doubt) },
                                modifier = Modifier.testTag("answer_doubt_${doubt.id}"),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Icon(Icons.Filled.Reply, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (doubt.answer.isNullOrBlank()) "Write Faculty Solution" else "Update Solution")
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun CreateCourseDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, CourseSubject, String, String, String, Long, FeeCycle, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf(CourseSubject.IIT_JEE) }
    var targetExam by remember { mutableStateOf("IIT-JEE / NEET") }
    var instructor by remember { mutableStateOf("Shankar Royal. G") }
    var schedule by remember { mutableStateOf("1st Batch: 6:00 AM – 7:30 AM • 2nd Batch: 6:00 PM – 8:30 PM") }
    var feeText by remember { mutableStateOf("2500") }
    var cycle by remember { mutableStateOf(FeeCycle.MONTHLY) }
    var room by remember { mutableStateOf("NGO Colony Campus, Kadapa") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Study Circle Batch") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(120) },
                    label = { Text("Batch Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("course_title_input")
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(CourseSubject.entries) { subj ->
                        FilterChip(
                            selected = subject == subj,
                            onClick = { subject = subj },
                            label = { Text(subj.label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = targetExam,
                    onValueChange = { targetExam = it.take(60) },
                    label = { Text("Target Exam / Board") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = instructor,
                    onValueChange = { instructor = it.take(80) },
                    label = { Text("Faculty") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = schedule,
                    onValueChange = { schedule = it.take(120) },
                    label = { Text("Batch Timings") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = feeText,
                        onValueChange = { feeText = it.filter(Char::isDigit).take(6) },
                        label = { Text("Fee (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = room,
                        onValueChange = { room = it.take(60) },
                        label = { Text("Hall / Campus") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(FeeCycle.entries) { item ->
                        FilterChip(
                            selected = cycle == item,
                            onClick = { cycle = item },
                            label = { Text(item.name.replace("_", " ")) }
                        )
                    }
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(600) },
                    label = { Text("Syllabus & Batch Highlights") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val fee = feeText.toLongOrNull() ?: 2500L
                    onSubmit(title, subject, targetExam, instructor, schedule, fee, cycle, room, description)
                },
                enabled = title.trim().length >= 2 && description.trim().length >= 2,
                modifier = Modifier.testTag("submit_course_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Publish Batch")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CreateMaterialDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, MaterialSubject, String, MaterialType, String, String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf(MaterialSubject.PHYSICS) }
    var chapter by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(MaterialType.DPP) }
    var summary by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Publish Study Material") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(140) },
                    label = { Text("Title (e.g., Electrostatics DPP #5)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("material_title_input")
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(MaterialSubject.entries) { subj ->
                        FilterChip(
                            selected = subject == subj,
                            onClick = { subject = subj },
                            label = { Text(subj.label) }
                        )
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(MaterialType.entries) { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t.label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = chapter,
                    onValueChange = { chapter = it.take(100) },
                    label = { Text("Chapter") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it.take(1200) },
                    label = { Text("Key Formulas / Questions / Summary") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it.take(500) },
                    label = { Text("Optional PDF / Drive Link (https://...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(title, subject, chapter, type, summary, url.ifBlank { null }) },
                enabled = title.trim().length >= 2 && chapter.trim().isNotEmpty() && summary.trim().length >= 2,
                modifier = Modifier.testTag("submit_material_button")
            ) {
                Text("Publish")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AskDoubtDialog(
    onDismiss: () -> Unit,
    onSubmit: (MaterialSubject, String) -> Unit
) {
    var subject by remember { mutableStateOf(MaterialSubject.PHYSICS) }
    var question by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ask Rupa IIT / NEET Faculty") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(MaterialSubject.entries) { subj ->
                        FilterChip(
                            selected = subject == subj,
                            onClick = { subject = subj },
                            label = { Text(subj.label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it.take(1000) },
                    label = { Text("Describe your concept doubt or problem") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth().testTag("doubt_question_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(subject, question) },
                enabled = question.trim().length >= 5,
                modifier = Modifier.testTag("submit_doubt_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Send Doubt")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AnswerDoubtDialog(
    doubt: Doubt,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var answer by remember { mutableStateOf(doubt.answer.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Faculty Solution") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = doubt.question,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it.take(1500) },
                    label = { Text("Step-by-step explanation") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth().testTag("doubt_answer_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(answer) },
                enabled = answer.trim().length >= 2,
                modifier = Modifier.testTag("submit_doubt_answer_button")
            ) {
                Text("Publish Reply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

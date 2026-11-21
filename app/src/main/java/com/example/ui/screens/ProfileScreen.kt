package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AttendanceRecord
import com.example.data.Enrollment
import com.example.data.MockTestScore
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.ui.components.AccentPill
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.LuminousTeal
import com.example.ui.theme.RoyalIndigo
import com.example.ui.theme.SaffronGold
import com.example.ui.theme.StatusVerified
import com.example.ui.theme.SubjectChemistry
import com.example.ui.theme.SubjectMath
import com.example.ui.theme.SubjectPhysics
import com.example.util.ContactHelper
import com.example.util.LocationHelper
import com.example.viewmodel.StudyCircleViewModel

@Composable
fun ProfileScreen(
    viewModel: StudyCircleViewModel,
    onSignOut: () -> Unit
) {
    val profileState by viewModel.userProfile.collectAsStateWithLifecycle()
    val enrollmentsState by viewModel.enrollments.collectAsStateWithLifecycle()
    val attendanceState by viewModel.attendance.collectAsStateWithLifecycle()
    val testScoresState by viewModel.testScores.collectAsStateWithLifecycle()

    val profile = (profileState as? UiState.Success)?.data
    val enrollments = (enrollmentsState as? UiState.Success)?.data.orEmpty()
    val attendance = (attendanceState as? UiState.Success)?.data.orEmpty()
    val scores = (testScoresState as? UiState.Success)?.data.orEmpty()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showLogTestDialog by remember { mutableStateOf(false) }

    val displayName = profile?.displayName?.ifBlank { viewModel.userDisplayName }
        ?: viewModel.userDisplayName
    val initials = displayName.split(" ")
        .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
        .take(2)
        .joinToString("")
        .ifBlank { "R" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. User Profile Header Card
        item(key = "profile_card") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(RoyalIndigo),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                style = MaterialTheme.typography.headlineSmall,
                                color = SaffronGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = viewModel.userEmail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                AccentPill(
                                    text = if (viewModel.isAdmin) "Faculty / Admin" else "Student",
                                    accent = if (viewModel.isAdmin) SaffronGold else LuminousTeal
                                )
                                if (!profile?.targetYear.isNullOrBlank()) {
                                    AccentPill(
                                        text = profile!!.targetYear!!,
                                        accent = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = { showEditProfileDialog = true },
                            modifier = Modifier.testTag("edit_profile_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit profile",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (!profile?.phone.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Phone: ${profile!!.phone}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ProfileStatBadge("Batches", "${enrollments.size}")
                        ProfileStatBadge("Check-Ins", "${attendance.size}")
                        ProfileStatBadge("Mock Tests", "${scores.size}")
                    }
                }
            }
        }

        // 2. Mock Test Score Tracker
        item(key = "scores_header") {
            SectionHeader(
                title = "Mock Test Analytics",
                subtitle = "Track Physics, Chemistry & Maths / Biology progress",
                actionLabel = "+ Log Score",
                actionTestTag = "log_mock_test_button",
                onActionClick = { showLogTestDialog = true }
            )
        }

        if (scores.isEmpty()) {
            item(key = "scores_empty") {
                EmptyStateCard(
                    icon = Icons.Filled.Analytics,
                    title = "No Mock Test Scores Yet",
                    subtitle = "Tap + Log Score after your weekly Rupa IIT / NEET mock test to visualize subject-wise accuracy."
                )
            }
        } else {
            items(scores, key = { it.id.ifBlank { it.testName } }) { score ->
                MockTestScoreCard(
                    score = score,
                    onDelete = { viewModel.deleteMockTestScore(score.id) }
                )
            }
        }

        // 3. Enrolled Batches
        item(key = "enrollments_header") {
            SectionHeader(
                title = "My Enrolled Batches",
                subtitle = "Active classroom registrations"
            )
        }

        if (enrollments.isEmpty()) {
            item(key = "enrollments_empty") {
                EmptyStateCard(
                    icon = Icons.Filled.School,
                    title = "Not Enrolled Yet",
                    subtitle = "Explore the Batches tab to join an IIT-JEE, NEET, Intermediate, or 6th–10th Foundation batch."
                )
            }
        } else {
            items(enrollments, key = { it.id.ifBlank { it.courseId } }) { enrollment ->
                EnrollmentRowCard(enrollment = enrollment)
            }
        }

        // 4. Recent Attendance History
        item(key = "attendance_header") {
            SectionHeader(
                title = "Attendance Log",
                subtitle = "Campus check-ins & study sessions"
            )
        }

        if (attendance.isEmpty()) {
            item(key = "attendance_empty") {
                EmptyStateCard(
                    icon = Icons.Filled.EventAvailable,
                    title = "No Attendance Recorded",
                    subtitle = "Check in from the Location tab when you arrive at Rupa IIT / NEET Study Circle."
                )
            }
        } else {
            items(attendance.take(6), key = { it.id.ifBlank { it.dateLabel } }) { record ->
                AttendanceRowCard(record = record)
            }
        }

        // 5. Sign Out Button
        item(key = "sign_out") {
            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("sign_out_button"),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out", fontWeight = FontWeight.SemiBold)
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            profile = profile,
            fallbackName = displayName,
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, phone, target ->
                viewModel.updateProfile(name, phone, target)
                showEditProfileDialog = false
            }
        )
    }

    if (showLogTestDialog) {
        LogMockTestDialog(
            onDismiss = { showLogTestDialog = false },
            onSave = { name, phy, chem, math, max ->
                viewModel.logMockTestScore(name, phy, chem, math, max)
                showLogTestDialog = false
            }
        )
    }
}

@Composable
private fun ProfileStatBadge(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MockTestScoreCard(
    score: MockTestScore,
    onDelete: () -> Unit
) {
    val ratio = (score.totalScore.toFloat() / score.maxScore.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
    val percentage = (ratio * 100f).toInt()

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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = score.testName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = score.dateLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AccentPill(
                        text = "${score.totalScore}/${score.maxScore} ($percentage%)",
                        accent = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Delete score",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = LuminousTeal,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SubjectScorePill("Physics", score.physicsScore, SubjectPhysics)
                SubjectScorePill("Chemistry", score.chemistryScore, SubjectChemistry)
                SubjectScorePill("Maths/Bio", score.mathScore, SubjectMath)
            }
        }
    }
}

@Composable
private fun SubjectScorePill(label: String, marks: Long, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = "$label: $marks",
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun EnrollmentRowCard(enrollment: Enrollment) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Enrolled",
                tint = StatusVerified,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = enrollment.courseTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = enrollment.schedule,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${ContactHelper.formatInr(enrollment.feeAmountInr)}/mo",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun AttendanceRowCard(record: AttendanceRecord) {
    val isPresent = record.status == "PRESENT"
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.batchName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${record.dateLabel} • ${LocationHelper.formatDistance(record.distanceMeters)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AccentPill(
                text = if (isPresent) "Present" else "Remote Study",
                accent = if (isPresent) StatusVerified else LuminousTeal
            )
        }
    }
}

@Composable
private fun EditProfileDialog(
    profile: UserProfile?,
    fallbackName: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(profile?.displayName?.ifBlank { fallbackName } ?: fallbackName) }
    var phone by remember { mutableStateOf(profile?.phone.orEmpty()) }
    var targetYear by remember { mutableStateOf(profile?.targetYear.orEmpty().ifBlank { "IIT-JEE / NEET" }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(100) },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.take(25) },
                    label = { Text("Phone Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetYear,
                    onValueChange = { targetYear = it.take(20) },
                    label = { Text("Target Exam (e.g., IIT-JEE / NEET)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, phone, targetYear) },
                enabled = name.trim().isNotEmpty(),
                modifier = Modifier.testTag("save_profile_button")
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
private fun LogMockTestDialog(
    onDismiss: () -> Unit,
    onSave: (String, Long, Long, Long, Long) -> Unit
) {
    var testName by remember { mutableStateOf("") }
    var phyText by remember { mutableStateOf("") }
    var chemText by remember { mutableStateOf("") }
    var mathText by remember { mutableStateOf("") }
    var maxText by remember { mutableStateOf("300") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Mock Test Score") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = testName,
                    onValueChange = { testName = it.take(120) },
                    label = { Text("Test Name (e.g., Rupa Weekly Test #9)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("mock_test_name_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = phyText,
                        onValueChange = { phyText = it.filter { c -> c.isDigit() || c == '-' }.take(4) },
                        label = { Text("Physics") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = chemText,
                        onValueChange = { chemText = it.filter { c -> c.isDigit() || c == '-' }.take(4) },
                        label = { Text("Chem") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = mathText,
                        onValueChange = { mathText = it.filter { c -> c.isDigit() || c == '-' }.take(4) },
                        label = { Text("Maths/Bio") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = maxText,
                    onValueChange = { maxText = it.filter(Char::isDigit).take(3) },
                    label = { Text("Total Maximum Marks") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val phy = phyText.toLongOrNull()
            val chem = chemText.toLongOrNull()
            val math = mathText.toLongOrNull()
            val max = maxText.toLongOrNull()
            Button(
                onClick = {
                    if (phy != null && chem != null && math != null && max != null) {
                        onSave(testName, phy, chem, math, max)
                    }
                },
                enabled = testName.trim().length >= 2 && phy != null && chem != null && math != null && max != null && max > 0,
                modifier = Modifier.testTag("submit_mock_test_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Score")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

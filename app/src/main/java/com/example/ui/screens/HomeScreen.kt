package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.Announcement
import com.example.data.AnnouncementCategory
import com.example.data.Course
import com.example.data.CourseSubject
import com.example.data.FeeCycle
import com.example.data.InstituteConfig
import com.example.data.UiState
import com.example.ui.components.AccentPill
import com.example.ui.components.ContactNumbersCard
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorBanner
import com.example.ui.components.OfficialInstituteBannerCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.announcementAccentColor
import com.example.ui.components.formatTimestampShort
import com.example.ui.components.subjectAccentColor
import com.example.ui.theme.LuminousTeal
import com.example.ui.theme.RoyalIndigo
import com.example.ui.theme.SaffronGold
import com.example.ui.theme.SubjectFullJee
import com.example.util.ContactHelper
import com.example.util.LocationHelper
import com.example.viewmodel.StudyCircleViewModel

@Composable
fun HomeScreen(
    viewModel: StudyCircleViewModel,
    onNavigateToCourses: () -> Unit,
    onNavigateToCampus: () -> Unit,
    onNavigateToDoubts: () -> Unit
) {
    val context = LocalContext.current
    val configState by viewModel.instituteConfig.collectAsStateWithLifecycle()
    val coursesState by viewModel.courses.collectAsStateWithLifecycle()
    val announcementsState by viewModel.announcements.collectAsStateWithLifecycle()
    val enrollmentsState by viewModel.enrollments.collectAsStateWithLifecycle()
    val materialsState by viewModel.studyMaterials.collectAsStateWithLifecycle()
    val distanceMeters by viewModel.distanceMeters.collectAsStateWithLifecycle()

    val config = (configState as? UiState.Success)?.data ?: InstituteConfig()
    val courses = (coursesState as? UiState.Success)?.data.orEmpty()
    val announcements = (announcementsState as? UiState.Success)?.data.orEmpty()
    val enrollments = (enrollmentsState as? UiState.Success)?.data.orEmpty()
    val materialsCount = (materialsState as? UiState.Success)?.data?.size ?: 0

    var showAddNoticeDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Welcome Header Row
        item(key = "welcome_bar") {
            WelcomeHeaderStrip(
                userName = viewModel.userDisplayName.substringBefore(" "),
                isAdmin = viewModel.isAdmin,
                city = config.city
            )
        }

        // 2. Official Rupa IIT / NEET Study Circle Banner + Founder Credentials
        item(key = "official_banner") {
            OfficialInstituteBannerCard(config = config)
        }

        // 3. Monthly Fee & Batch Timings Info Strip
        item(key = "fee_timings_strip") {
            FeeAndTimingsCard(
                monthlyFeeInr = config.monthlyFeeInr,
                batchTimings = config.batchTimings
            )
        }

        // 4. Campus Distance & 1-Tap Google Maps Directions Card
        item(key = "campus_distance_card") {
            HomeDistanceAndDirectionsCard(
                config = config,
                distanceMeters = distanceMeters,
                onOpenDirectToCampus = {
                    if (!LocationHelper.openCampusDirections(context, config)) {
                        viewModel.emitMessage("Could not open Google Maps directions.")
                    }
                },
                onOpenCampusTab = onNavigateToCampus
            )
        }

        // 5. Direct Call & WhatsApp Desk (Main + 3 Alternative Numbers)
        item(key = "contact_numbers_desk") {
            ContactNumbersCard(
                config = config,
                onStatusMessage = { viewModel.emitMessage(it) }
            )
        }

        // 6. Quick Explore Tiles (Batches + Doubt Desk)
        item(key = "quick_actions") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionTile(
                    title = "All Batches",
                    subtitle = "${enrollments.size} enrolled • ${courses.size} live",
                    icon = Icons.Filled.School,
                    accent = RoyalIndigo,
                    onClick = onNavigateToCourses,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_quick_courses")
                )
                QuickActionTile(
                    title = "DPPs & Doubts",
                    subtitle = "$materialsCount DPPs & Notes",
                    icon = Icons.Filled.QuestionAnswer,
                    accent = SubjectFullJee,
                    onClick = onNavigateToDoubts,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_quick_doubts")
                )
            }
        }

        // 7. Notice Board / Study Circle Updates
        item(key = "notice_header") {
            SectionHeader(
                title = "Study Circle Updates",
                subtitle = "Admissions, batch timings & exam announcements",
                actionLabel = if (viewModel.isAdmin) "+ Post Update" else null,
                actionTestTag = "post_notice_button",
                onActionClick = if (viewModel.isAdmin) ({ showAddNoticeDialog = true }) else null
            )
        }

        if (announcementsState is UiState.Error) {
            item(key = "notice_error") {
                ErrorBanner((announcementsState as UiState.Error).message)
            }
        } else if (announcements.isEmpty() && announcementsState is UiState.Success) {
            item(key = "notice_empty") {
                EmptyStateCard(
                    icon = Icons.Filled.Campaign,
                    title = "No Updates Yet",
                    subtitle = if (viewModel.isAdmin) "Tap + Post Update to publish batch schedules or announcements." else "Updates from Rupa IIT / NEET Study Circle will appear here."
                )
            }
        } else {
            items(announcements.take(4), key = { it.id.ifBlank { it.title } }) { notice ->
                AnnouncementCard(
                    announcement = notice,
                    isAdmin = viewModel.isAdmin,
                    onDelete = { viewModel.deleteAnnouncement(notice.id) }
                )
            }
        }

        // 8. Live Batches Horizontal Strip
        item(key = "batches_header") {
            SectionHeader(
                title = "IIT-JEE, NEET & Foundation Batches",
                subtitle = "Monthly fee ${ContactHelper.formatInr(config.monthlyFeeInr)} / month across programs",
                actionLabel = "See All",
                actionTestTag = "home_see_all_courses",
                onActionClick = onNavigateToCourses
            )
        }

        item(key = "batches_strip") {
            if (courses.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.Filled.MenuBook,
                    title = "Batches Loading",
                    subtitle = "Explore the Batches tab to view all IIT-JEE, NEET, Intermediate & Foundation programs."
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(courses, key = { it.id.ifBlank { it.title } }) { course ->
                        HomeBatchPreviewCard(
                            course = course,
                            onClick = onNavigateToCourses
                        )
                    }
                }
            }
        }

        // 9. Mentoring Spotlight Banner
        item(key = "mentor_spotlight") {
            MentoringSpotlightCard(
                directorName = config.directorName,
                onAskDoubt = onNavigateToDoubts
            )
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }

    if (showAddNoticeDialog) {
        PostAnnouncementDialog(
            onDismiss = { showAddNoticeDialog = false },
            onSubmit = { title, body, category, pinned ->
                viewModel.createAnnouncement(title, body, category, pinned)
                showAddNoticeDialog = false
            }
        )
    }
}

@Composable
private fun WelcomeHeaderStrip(
    userName: String,
    isAdmin: Boolean,
    city: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Namaste, $userName",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isAdmin) SaffronGold else LuminousTeal.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = if (isAdmin) "ADMIN" else "STUDENT",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isAdmin) RoyalIndigo else LuminousTeal,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = city,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Surface(
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            border = BorderStroke(1.5.dp, SaffronGold)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_rupa_icon_1795253176025),
                contentDescription = stringResource(id = R.string.cd_app_emblem),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun FeeAndTimingsCard(
    monthlyFeeInr: Long,
    batchTimings: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, SaffronGold.copy(alpha = 0.45f)),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SaffronGold.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CurrencyRupee,
                            contentDescription = "Monthly fee",
                            tint = SaffronGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Class Fee",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${ContactHelper.formatInr(monthlyFeeInr)} / month",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                AccentPill(text = "6th–10th & Inter / IIT / NEET", accent = LuminousTeal)
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = "Batch timings",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = batchTimings,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeDistanceAndDirectionsCard(
    config: InstituteConfig,
    distanceMeters: Long?,
    onOpenDirectToCampus: () -> Unit,
    onOpenCampusTab: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, LuminousTeal.copy(alpha = 0.45f)),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(LuminousTeal.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = "Campus location",
                            tint = LuminousTeal,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Campus Location & Distance",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${config.address}, ${config.city}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                TextButton(
                    onClick = onOpenCampusTab,
                    modifier = Modifier.testTag("home_open_campus_tab")
                ) {
                    Text("Details")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Tapping Distance goes directly to campus in Google Maps
                OutlinedButton(
                    onClick = onOpenDirectToCampus,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("home_distance_button"),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, LuminousTeal)
                ) {
                    Icon(
                        imageVector = Icons.Filled.NearMe,
                        contentDescription = null,
                        tint = LuminousTeal,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = distanceMeters?.let { LocationHelper.formatDistance(it) } ?: "Distance",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = LuminousTeal,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Tapping Directions goes directly to campus in Google Maps
                Button(
                    onClick = onOpenDirectToCampus,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("home_directions_button"),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LuminousTeal,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Directions,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Directions", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AnnouncementCard(
    announcement: Announcement,
    isAdmin: Boolean,
    onDelete: () -> Unit
) {
    val category = AnnouncementCategory.fromWire(announcement.category)
    val accent = announcementAccentColor(announcement.category)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (announcement.pinned) 1.5.dp else 1.dp,
            color = if (announcement.pinned) accent.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AccentPill(text = category.label, accent = accent)
                    if (announcement.pinned) {
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = "Pinned update",
                            tint = accent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatTimestampShort(announcement.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isAdmin && announcement.id.isNotBlank()) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.testTag("delete_notice_${announcement.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Delete update",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = announcement.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = announcement.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HomeBatchPreviewCard(
    course: Course,
    onClick: () -> Unit
) {
    val subject = CourseSubject.fromWire(course.subject)
    val cycle = FeeCycle.fromWire(course.feeCycle)
    val accent = subjectAccentColor(course.subject)

    Surface(
        modifier = Modifier
            .width(268.dp)
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
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
                AccentPill(text = subject.label, accent = accent)
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
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = course.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = course.instructor,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = course.schedule,
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MentoringSpotlightCard(
    directorName: String,
    onAskDoubt: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            Image(
                painter = painterResource(id = R.drawable.img_study_mentor_1795253183595),
                contentDescription = stringResource(id = R.string.cd_study_mentor),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp)
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Direct Mentorship by $directorName",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Need help with an IIT-JEE, NEET, Intermediate, or 6th–10th Foundation concept? Post your doubt directly to Rupa faculty for clear step-by-step guidance.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onAskDoubt,
                    modifier = Modifier.testTag("home_ask_doubt_button"),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        imageVector = Icons.Filled.QuestionAnswer,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Doubt Desk")
                }
            }
        }
    }
}

@Composable
private fun PostAnnouncementDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String, AnnouncementCategory, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(AnnouncementCategory.GENERAL) }
    var pinned by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Post Study Circle Update") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(120) },
                    label = { Text("Headline") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("notice_title_input")
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it.take(800) },
                    label = { Text("Details") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("notice_body_input")
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(AnnouncementCategory.entries) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.label) }
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { pinned = !pinned }
                ) {
                    Checkbox(checked = pinned, onCheckedChange = { pinned = it })
                    Text("Pin to top of updates", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(title, body, selectedCategory, pinned) },
                enabled = title.trim().length >= 2 && body.trim().length >= 2,
                modifier = Modifier.testTag("submit_notice_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Publish")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

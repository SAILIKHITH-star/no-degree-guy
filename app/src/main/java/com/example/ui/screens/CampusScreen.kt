package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.InstituteConfig
import com.example.data.UiState
import com.example.ui.components.AccentPill
import com.example.ui.components.ContactNumbersCard
import com.example.ui.components.OfficialInstituteBannerCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.LuminousTeal
import com.example.ui.theme.RoyalIndigo
import com.example.ui.theme.SaffronGold
import com.example.ui.theme.StatusVerified
import com.example.util.ContactHelper
import com.example.util.LocationHelper
import com.example.viewmodel.StudyCircleViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun CampusScreen(
    viewModel: StudyCircleViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val configState by viewModel.instituteConfig.collectAsStateWithLifecycle()
    val enrollmentsState by viewModel.enrollments.collectAsStateWithLifecycle()
    val distanceMeters by viewModel.distanceMeters.collectAsStateWithLifecycle()
    val deviceCoords by viewModel.deviceLocation.collectAsStateWithLifecycle()
    val isBusy by viewModel.isActionBusy.collectAsStateWithLifecycle()

    val config = (configState as? UiState.Success)?.data ?: InstituteConfig()
    val primaryBatchName = (enrollmentsState as? UiState.Success)?.data?.firstOrNull()?.courseTitle
        ?: "Rupa IIT / NEET Study Circle"

    var isLocating by remember { mutableStateOf(false) }
    var showEditCampusDialog by remember { mutableStateOf(false) }

    fun openCampusDirectlyInMaps() {
        if (!LocationHelper.openCampusDirections(context, config)) {
            if (!LocationHelper.openCampusInMaps(context, config)) {
                viewModel.emitMessage("Could not open Google Maps.")
            }
        }
    }

    fun fetchLocationNow(openMapsAfter: Boolean = false) {
        scope.launch {
            isLocating = true
            val result = LocationHelper.fetchCurrentLocation(context)
            isLocating = false
            result.onSuccess { coords ->
                viewModel.updateDeviceLocation(coords, config)
                val dist = LocationHelper.distanceMeters(
                    coords.latitude, coords.longitude, config.latitude, config.longitude
                )
                viewModel.emitMessage("Distance to campus: ${LocationHelper.formatDistance(dist)}")
            }.onFailure { err ->
                if (!openMapsAfter) {
                    viewModel.emitMessage(err.localizedMessage ?: "Unable to fetch GPS location")
                }
            }
            if (openMapsAfter) {
                openCampusDirectlyInMaps()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            fetchLocationNow(openMapsAfter = false)
        } else {
            viewModel.emitMessage("Location permission denied. Tap Distance or Directions to navigate in Google Maps.")
        }
    }

    fun requestOrFetchGpsOnly() {
        if (LocationHelper.hasCoarseLocationPermission(context)) {
            fetchLocationNow(openMapsAfter = false)
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Campus Hero Card with Tappable Distance Pill
        item(key = "campus_hero") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                shadowElevation = 4.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_campus_hero_1795253177860),
                        contentDescription = stringResource(id = R.string.cd_campus_hero),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        RoyalIndigo.copy(alpha = 0.88f)
                                    )
                                )
                            )
                    )
                    if (viewModel.isAdmin) {
                        Surface(
                            onClick = { showEditCampusDialog = true },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .testTag("edit_campus_button"),
                            shape = RoundedCornerShape(12.dp),
                            color = RoyalIndigo.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, SaffronGold.copy(alpha = 0.7f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.EditLocationAlt,
                                    contentDescription = "Edit campus details",
                                    tint = SaffronGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Edit Campus",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(18.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = LuminousTeal
                            ) {
                                Text(
                                    text = "KADAPA MAIN CAMPUS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Surface(
                                onClick = { openCampusDirectlyInMaps() },
                                shape = RoundedCornerShape(8.dp),
                                color = SaffronGold,
                                modifier = Modifier.testTag("hero_distance_pill")
                            ) {
                                Text(
                                    text = distanceMeters?.let {
                                        "${LocationHelper.formatDistance(it)} • Tap to Go"
                                    } ?: "Check Distance & Go",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RoyalIndigo,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = config.centerName,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )
                        Text(
                            text = "${config.address}, ${config.city}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.90f)
                        )
                    }
                }
            }
        }

        // 2. Interactive Tappable Campus Map, Distance & Directions Card
        item(key = "campus_map_card") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column {
                    // Tapping the map preview takes user directly to campus in Google Maps
                    CampusVectorMapPreview(
                        centerName = config.centerName,
                        latitude = config.latitude,
                        longitude = config.longitude,
                        distanceLabel = distanceMeters?.let { LocationHelper.formatDistance(it) },
                        onTapMap = { openCampusDirectlyInMaps() }
                    )

                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = "Address",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = config.address,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                if (config.landmark.isNotBlank()) {
                                    Text(
                                        text = "Landmark: ${config.landmark}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = config.city,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = "Batch timings",
                                tint = LuminousTeal,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = config.batchTimings,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.CurrencyRupee,
                                contentDescription = "Monthly fee",
                                tint = SaffronGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Class Fee: ${ContactHelper.formatInr(config.monthlyFeeInr)} per month",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Primary Distance & Directions Row — tapping EITHER navigates directly to campus in Google Maps
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { openCampusDirectlyInMaps() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("campus_directions_button"),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Icon(Icons.Filled.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Directions", maxLines = 1)
                            }

                            Button(
                                onClick = {
                                    // Directly go to campus in Google Maps to see exact route & distance,
                                    // and also refresh local GPS distance if permission is already granted.
                                    if (LocationHelper.hasCoarseLocationPermission(context)) {
                                        fetchLocationNow(openMapsAfter = true)
                                    } else {
                                        openCampusDirectlyInMaps()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("campus_distance_button"),
                                shape = MaterialTheme.shapes.medium,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LuminousTeal,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Filled.NearMe, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = distanceMeters?.let { LocationHelper.formatDistance(it) } ?: "Distance",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Secondary Row: Live GPS Refresh + Share Campus Details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { requestOrFetchGpsOnly() },
                                enabled = !isLocating,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("campus_locate_me_button"),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                if (isLocating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Locating...")
                                } else {
                                    Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Measure GPS", maxLines = 1)
                                }
                            }
                            OutlinedButton(
                                onClick = { LocationHelper.shareCampusLocation(context, config) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("campus_share_button"),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share Location", maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        // 3. Direct Call & WhatsApp Desk (Main + 3 Alternative Numbers)
        item(key = "campus_contact_desk") {
            ContactNumbersCard(
                config = config,
                onStatusMessage = { viewModel.emitMessage(it) },
                title = "Phone & WhatsApp Numbers",
                subtitle = "1-tap WhatsApp logo or Phone logo on main or any alternative number"
            )
        }

        // 4. Official Banner & Founder Profile
        item(key = "campus_official_banner") {
            OfficialInstituteBannerCard(config = config)
        }

        // 5. Campus Attendance Check-In Card
        item(key = "attendance_checkin") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, LuminousTeal.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "GPS Attendance Check-In",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Batch: $primaryBatchName",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        AccentPill(
                            text = if (distanceMeters != null && distanceMeters!! <= 2500L) "On Campus" else "Study Log",
                            accent = if (distanceMeters != null && distanceMeters!! <= 2500L) StatusVerified else LuminousTeal
                        )
                    }

                    Text(
                        text = if (deviceCoords != null && distanceMeters != null) {
                            "GPS verified (${LocationHelper.formatDistance(distanceMeters!!)}). Tap below to record today's attendance."
                        } else {
                            "Tap Measure GPS above to attach your live campus distance, or log your study session directly."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            viewModel.checkInAttendance(
                                batchName = primaryBatchName,
                                distanceMeters = distanceMeters ?: 0L
                            )
                        },
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LuminousTeal,
                            contentColor = Color.White
                        ),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("campus_checkin_button")
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mark Today's Attendance")
                    }
                }
            }
        }

        // 6. Campus Facilities Grid
        item(key = "facilities_header") {
            SectionHeader(
                title = "Study Circle Programs & Facilities",
                subtitle = "IIT-JEE • NEET • BITSAT • EAMCET • Intermediate • 6th–10th Foundation"
            )
        }

        item(key = "facilities_grid") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FacilityTile(
                        icon = Icons.Filled.LocalLibrary,
                        title = "All Boards Covered",
                        subtitle = "Dedicated 6th–10th Foundation for SSC, CBSE, ICSE, ISC & IG students",
                        modifier = Modifier.weight(1f)
                    )
                    FacilityTile(
                        icon = Icons.Filled.Computer,
                        title = "Weekly Mock Tests",
                        subtitle = "JEE Main/Advanced, NEET, BITSAT & EAMCET pattern testing",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FacilityTile(
                        icon = Icons.Filled.QuestionAnswer,
                        title = "1-on-1 Doubt Clinic",
                        subtitle = "Direct mentoring by Senior IIT Faculty Shankar Royal. G",
                        modifier = Modifier.weight(1f)
                    )
                    FacilityTile(
                        icon = Icons.Filled.Weekend,
                        title = "Morning & Evening Batches",
                        subtitle = "1st Batch 6:00–7:30 AM & 2nd Batch 6:00–8:30 PM at NGO Colony",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    if (showEditCampusDialog) {
        EditCampusConfigDialog(
            config = config,
            onDismiss = { showEditCampusDialog = false },
            onRequestCurrentGps = { onCoords ->
                scope.launch {
                    val res = LocationHelper.fetchCurrentLocation(context)
                    res.onSuccess { onCoords(it.latitude, it.longitude) }
                        .onFailure {
                            viewModel.emitMessage("Grant Location permission via Measure GPS first to auto-fill coordinates.")
                        }
                }
            },
            onSave = { updated ->
                viewModel.saveInstituteConfig(updated)
                showEditCampusDialog = false
            }
        )
    }
}

@Composable
private fun CampusVectorMapPreview(
    centerName: String,
    latitude: Double,
    longitude: Double,
    distanceLabel: String?,
    onTapMap: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(182.dp)
            .background(RoyalIndigo)
            .clickable(onClick = onTapMap)
            .testTag("campus_map_preview")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w * 0.52f
            val cy = h * 0.48f

            val step = 36f
            var x = 0f
            while (x < w) {
                drawLine(
                    color = Color.White.copy(alpha = 0.06f),
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 1f
                )
                x += step
            }
            var y = 0f
            while (y < h) {
                drawLine(
                    color = Color.White.copy(alpha = 0.06f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1f
                )
                y += step
            }

            drawRoundRect(
                color = LuminousTeal.copy(alpha = 0.16f),
                topLeft = Offset(w * 0.08f, h * 0.15f),
                size = Size(w * 0.24f, h * 0.34f),
                cornerRadius = CornerRadius(14f, 14f)
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.07f),
                topLeft = Offset(w * 0.68f, h * 0.56f),
                size = Size(w * 0.22f, h * 0.28f),
                cornerRadius = CornerRadius(12f, 12f)
            )

            drawLine(
                color = Color.White.copy(alpha = 0.24f),
                start = Offset(0f, h * 0.56f),
                end = Offset(w, h * 0.36f),
                strokeWidth = 10f
            )
            drawLine(
                color = Color.White.copy(alpha = 0.16f),
                start = Offset(w * 0.38f, 0f),
                end = Offset(w * 0.62f, h),
                strokeWidth = 7f
            )

            drawCircle(
                color = SaffronGold.copy(alpha = 0.14f),
                radius = 74f,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = SaffronGold.copy(alpha = 0.45f),
                radius = 44f,
                center = Offset(cx, cy),
                style = Stroke(
                    width = 2.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                )
            )
            drawCircle(
                color = SaffronGold,
                radius = 14f,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = RoyalIndigo,
                radius = 5.5f,
                center = Offset(cx, cy)
            )
        }

        // Top-left GPS coordinates pill
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            shape = RoundedCornerShape(10.dp),
            color = RoyalIndigo.copy(alpha = 0.85f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.GpsFixed,
                    contentDescription = "GPS coordinates",
                    tint = LuminousTeal,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = String.format(Locale.US, "%.4f° N, %.4f° E", latitude, longitude),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }

        // Top-right Tap to Navigate badge
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            shape = RoundedCornerShape(10.dp),
            color = SaffronGold
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Directions,
                    contentDescription = "Open Google Maps",
                    tint = RoyalIndigo,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Tap for Maps",
                    style = MaterialTheme.typography.labelSmall,
                    color = RoyalIndigo,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Bottom label pill
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
            shape = RoundedCornerShape(10.dp),
            color = Color.White.copy(alpha = 0.94f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(SaffronGold)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (distanceLabel != null) "$centerName • $distanceLabel" else "$centerName • NGO Colony, Kadapa",
                    style = MaterialTheme.typography.labelMedium,
                    color = RoyalIndigo,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun FacilityTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EditCampusConfigDialog(
    config: InstituteConfig,
    onDismiss: () -> Unit,
    onRequestCurrentGps: ((Double, Double) -> Unit) -> Unit,
    onSave: (InstituteConfig) -> Unit
) {
    var centerName by remember { mutableStateOf(config.centerName) }
    var directorName by remember { mutableStateOf(config.directorName) }
    var directorBio by remember { mutableStateOf(config.directorBio) }
    var address by remember { mutableStateOf(config.address) }
    var landmark by remember { mutableStateOf(config.landmark) }
    var city by remember { mutableStateOf(config.city) }
    var latText by remember { mutableStateOf(config.latitude.toString()) }
    var lngText by remember { mutableStateOf(config.longitude.toString()) }
    var mainPhone by remember { mutableStateOf(config.mainPhone) }
    var altPhone1 by remember { mutableStateOf(config.altPhone1) }
    var altPhone2 by remember { mutableStateOf(config.altPhone2) }
    var altPhone3 by remember { mutableStateOf(config.altPhone3) }
    var batchTimings by remember { mutableStateOf(config.batchTimings) }
    var feeText by remember { mutableStateOf(config.monthlyFeeInr.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Campus Location & Contacts") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = centerName,
                    onValueChange = { centerName = it.take(100) },
                    label = { Text("Center Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = directorName,
                    onValueChange = { directorName = it.take(100) },
                    label = { Text("Founder & Director") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = directorBio,
                    onValueChange = { directorBio = it.take(300) },
                    label = { Text("Director Bio / Experience") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it.take(300) },
                    label = { Text("Street Address") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("campus_address_input")
                )
                OutlinedTextField(
                    value = landmark,
                    onValueChange = { landmark = it.take(150) },
                    label = { Text("Nearby Landmark") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it.take(80) },
                    label = { Text("City & PIN") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latText,
                        onValueChange = { latText = it.take(14) },
                        label = { Text("Latitude") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = lngText,
                        onValueChange = { lngText = it.take(14) },
                        label = { Text("Longitude") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedButton(
                    onClick = {
                        onRequestCurrentGps { lat, lng ->
                            latText = String.format(Locale.US, "%.5f", lat)
                            lngText = String.format(Locale.US, "%.5f", lng)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Fill From My Current GPS")
                }
                OutlinedTextField(
                    value = mainPhone,
                    onValueChange = { mainPhone = it.take(25) },
                    label = { Text("Main Phone & WhatsApp") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = altPhone1,
                    onValueChange = { altPhone1 = it.take(25) },
                    label = { Text("Alternative Number 1") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = altPhone2,
                    onValueChange = { altPhone2 = it.take(25) },
                    label = { Text("Alternative Number 2") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = altPhone3,
                    onValueChange = { altPhone3 = it.take(25) },
                    label = { Text("Alternative Number 3") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = batchTimings,
                    onValueChange = { batchTimings = it.take(160) },
                    label = { Text("Batch Timings") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = feeText,
                    onValueChange = { feeText = it.filter(Char::isDigit).take(6) },
                    label = { Text("Monthly Fee (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val lat = latText.toDoubleOrNull()
            val lng = lngText.toDoubleOrNull()
            val fee = feeText.toLongOrNull() ?: config.monthlyFeeInr
            Button(
                onClick = {
                    if (lat != null && lng != null) {
                        onSave(
                            config.copy(
                                centerName = centerName,
                                directorName = directorName,
                                directorBio = directorBio,
                                address = address,
                                landmark = landmark,
                                city = city,
                                latitude = lat,
                                longitude = lng,
                                mainPhone = mainPhone,
                                altPhone1 = altPhone1,
                                altPhone2 = altPhone2,
                                altPhone3 = altPhone3,
                                batchTimings = batchTimings,
                                monthlyFeeInr = fee
                            )
                        )
                    }
                },
                enabled = lat != null && lng != null && address.trim().length >= 5 && mainPhone.trim().length >= 10,
                modifier = Modifier.testTag("save_campus_config_button")
            ) {
                Text("Save Campus")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

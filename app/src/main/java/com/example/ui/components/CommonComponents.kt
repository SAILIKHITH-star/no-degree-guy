package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.R
import com.example.data.AnnouncementCategory
import com.example.data.CourseSubject
import com.example.data.InstituteConfig
import com.example.data.MaterialSubject
import com.example.ui.theme.LuminousTeal
import com.example.ui.theme.RoyalIndigo
import com.example.ui.theme.SaffronGold
import com.example.ui.theme.SubjectBiology
import com.example.ui.theme.SubjectChemistry
import com.example.ui.theme.SubjectFoundation
import com.example.ui.theme.SubjectFullJee
import com.example.ui.theme.SubjectIntermediate
import com.example.ui.theme.SubjectMath
import com.example.ui.theme.SubjectPhysics
import com.example.ui.theme.WhatsAppGreen
import com.example.util.ContactHelper
import com.example.util.ContactNumberEntry
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

fun formatTimestampShort(timestamp: Timestamp?): String {
    val date = timestamp?.toDate() ?: return "Just now"
    return SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(date)
}

fun subjectAccentColor(subjectWire: String): Color = when (subjectWire) {
    CourseSubject.IIT_JEE.wireValue -> SubjectFullJee
    CourseSubject.NEET.wireValue -> SubjectBiology
    CourseSubject.BITSAT.wireValue -> SubjectPhysics
    CourseSubject.EAMCET.wireValue -> SubjectChemistry
    CourseSubject.INTERMEDIATE.wireValue -> SubjectIntermediate
    CourseSubject.FOUNDATION.wireValue -> SubjectFoundation
    else -> LuminousTeal
}

fun materialAccentColor(subjectWire: String): Color = when (subjectWire) {
    MaterialSubject.PHYSICS.wireValue -> SubjectPhysics
    MaterialSubject.CHEMISTRY.wireValue -> SubjectChemistry
    MaterialSubject.MATHEMATICS.wireValue -> SubjectMath
    MaterialSubject.BIOLOGY.wireValue -> SubjectBiology
    else -> SaffronGold
}

fun announcementAccentColor(categoryWire: String): Color = when (categoryWire) {
    AnnouncementCategory.ADMISSION.wireValue -> SaffronGold
    AnnouncementCategory.EXAM.wireValue -> SubjectFullJee
    AnnouncementCategory.SCHEDULE.wireValue -> LuminousTeal
    else -> SubjectPhysics
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    actionTestTag: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (actionLabel != null && onActionClick != null) {
            TextButton(
                onClick = onActionClick,
                modifier = if (actionTestTag != null) Modifier.testTag(actionTestTag) else Modifier
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun AccentPill(
    text: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = accent.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun OfficialInstituteBannerCard(
    config: InstituteConfig,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 3.dp
    ) {
        Column {
            Image(
                painter = painterResource(id = R.drawable.img_rupa_official_banner),
                contentDescription = stringResource(id = R.string.cd_official_banner),
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10.6f)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RoyalIndigo)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.WorkspacePremium,
                        contentDescription = null,
                        tint = SaffronGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Founder & Director: ${config.directorName}",
                        style = MaterialTheme.typography.titleSmall,
                        color = SaffronGold,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = config.directorBio,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.90f)
                )
            }
        }
    }
}

@Composable
fun ContactNumbersCard(
    config: InstituteConfig,
    onStatusMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Direct Call & WhatsApp Desk",
    subtitle: String = "Tap WhatsApp or Phone logo on any number to connect instantly"
) {
    val context = LocalContext.current
    val entries = config.allContactEntries()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            entries.forEachIndexed { index, entry ->
                if (index == 1) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        text = "ALTERNATIVE NUMBERS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }

                PhoneContactRow(
                    entry = entry,
                    onWhatsAppClick = {
                        val opened = ContactHelper.openWhatsAppChat(context, entry.number)
                        if (!opened) {
                            onStatusMessage("Could not open WhatsApp for ${entry.number}.")
                        }
                    },
                    onCallClick = {
                        val opened = ContactHelper.dialPhone(context, entry.number)
                        if (!opened) {
                            onStatusMessage("Could not open phone dialer for ${entry.number}.")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PhoneContactRow(
    entry: ContactNumberEntry,
    onWhatsAppClick: () -> Unit,
    onCallClick: () -> Unit
) {
    val containerColor = if (entry.isMain) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    }
    val borderColor = if (entry.isMain) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        border = BorderStroke(if (entry.isMain) 1.5.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (entry.isMain) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SaffronGold
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = RoyalIndigo,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "MAIN",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RoyalIndigo,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Text(
                        text = entry.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (entry.isMain) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = ContactHelper.formatPhoneDisplay(entry.number),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1-Tap WhatsApp Logo Button (48dp touch target)
                Surface(
                    onClick = onWhatsAppClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("whatsapp_button_${entry.number}"),
                    shape = CircleShape,
                    color = WhatsAppGreen.copy(alpha = 0.14f),
                    border = BorderStroke(1.2.dp, WhatsAppGreen.copy(alpha = 0.55f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_whatsapp),
                            contentDescription = "${stringResource(id = R.string.cd_whatsapp_logo)} ${entry.number}",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // 1-Tap Phone Logo Button (48dp touch target)
                Surface(
                    onClick = onCallClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("call_button_${entry.number}"),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Call,
                            contentDescription = "${stringResource(id = R.string.cd_phone_logo)} ${entry.number}",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ErrorBanner(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

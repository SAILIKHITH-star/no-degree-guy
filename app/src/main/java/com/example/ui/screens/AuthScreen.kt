package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.GoogleAuthHelper
import com.example.ui.components.ErrorBanner
import com.example.ui.theme.LuminousTeal
import com.example.ui.theme.RoyalIndigo
import com.example.ui.theme.SaffronGold
import com.example.ui.theme.WhatsAppGreen

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var isCheckingSession by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        GoogleAuthHelper.attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isCheckingSession = false
                onAuthSuccess()
            },
            onUnauthenticated = {
                isCheckingSession = false
            },
            scope = scope
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero Art Card with overlapping emblem
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(196.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    shadowElevation = 6.dp
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_jee_banner_1795253346795),
                            contentDescription = stringResource(id = R.string.cd_jee_banner),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            RoyalIndigo.copy(alpha = 0.25f),
                                            RoyalIndigo.copy(alpha = 0.85f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 20.dp, end = 20.dp, bottom = 36.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SaffronGold.copy(alpha = 0.22f),
                                border = BorderStroke(1.dp, SaffronGold.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = "IIT-JEE • NEET • BITSAT • EAMCET • FOUNDATION",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SaffronGold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Rupa IIT / NEET Study Circle",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                            Text(
                                text = "NGO Colony, Chinna Chowk, Kadapa (A.P.) - 500601",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.88f)
                            )
                        }
                    }
                }

                // Overlapping Crest Emblem
                Surface(
                    modifier = Modifier
                        .size(72.dp)
                        .align(Alignment.BottomEnd)
                        .padding(end = 18.dp),
                    shape = CircleShape,
                    color = RoyalIndigo,
                    border = BorderStroke(3.dp, MaterialTheme.colorScheme.background),
                    shadowElevation = 6.dp
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_rupa_icon_1795253176025),
                        contentDescription = stringResource(id = R.string.cd_app_emblem),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Founder & Director: Shankar Royal. G",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Senior IIT Faculty with 25 Years Experience in Hyderabad (Formerly IIT Ramaiah & IIT Krishna Murthy College). Monthly Fee: ₹2,500/month.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AuthFeatureRow(
                    icon = Icons.Filled.School,
                    accent = RoyalIndigo,
                    title = "Batches, DPPs & Doubt Desk",
                    subtitle = "1st Batch 6:00–7:30 AM • 2nd Batch 6:00–8:30 PM • IIT-JEE, NEET, Intermediate & 6th–10th Foundation (₹2,500/mo)."
                )
                AuthFeatureRow(
                    icon = Icons.Filled.Call,
                    accent = WhatsAppGreen,
                    title = "1-Tap Direct WhatsApp & Phone",
                    subtitle = "Instant WhatsApp chat or call on main number 9849460305 and all 3 alternative campus numbers."
                )
                AuthFeatureRow(
                    icon = Icons.Filled.LocationOn,
                    accent = LuminousTeal,
                    title = "Live Campus Distance & Directions",
                    subtitle = "Near IOB Bank, NGO Colony, Chinna Chowk, Kadapa (500601) — tap distance or directions to navigate directly."
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!errorMessage.isNullOrBlank()) {
                ErrorBanner(message = errorMessage!!)
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                onClick = {
                    errorMessage = null
                    isLoading = true
                    GoogleAuthHelper.onGoogleSignInClicked(
                        context = context,
                        credentialManager = credentialManager,
                        onAuthSuccess = {
                            isLoading = false
                            onAuthSuccess()
                        },
                        onAuthError = { msg ->
                            isLoading = false
                            errorMessage = msg
                        },
                        scope = scope,
                        onAuthCancelled = {
                            isLoading = false
                        }
                    )
                },
                enabled = !isLoading && !isCheckingSession,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("google_sign_in_button")
            ) {
                if (isLoading || isCheckingSession) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isCheckingSession) "Checking session..." else "Signing in...",
                        style = MaterialTheme.typography.labelLarge
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.VerifiedUser,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(id = R.string.sign_in_with_google),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Protected with Google Sign-In & cloud sync across devices.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AuthFeatureRow(
    icon: ImageVector,
    accent: Color,
    title: String,
    subtitle: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
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
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

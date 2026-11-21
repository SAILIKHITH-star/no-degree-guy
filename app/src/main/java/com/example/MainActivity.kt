package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.GoogleAuthHelper
import com.example.data.StudyCircleRepository
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CampusScreen
import com.example.ui.screens.CoursesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.RupaIitTheme
import com.example.viewmodel.StudyCircleViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RupaIitTheme {
                RupaStudyCircleRoot()
            }
        }
    }
}

private enum class StudyCircleTab(
    val routeId: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("home", R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
    COURSES("courses", R.string.nav_courses, Icons.Filled.School, Icons.Outlined.School),
    CAMPUS("campus", R.string.nav_campus, Icons.Filled.LocationOn, Icons.Outlined.LocationOn),
    PROFILE("profile", R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person)
}

@Composable
private fun RupaStudyCircleRoot() {
    var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        val uid = user.uid
        key(uid) {
            AuthenticatedStudyCircleApp(
                currentUserId = uid,
                userEmail = user.email.orEmpty(),
                userDisplayName = user.displayName.orEmpty().ifBlank { "Student" },
                isAdmin = GoogleAuthHelper.isBootstrapAdmin(user)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedStudyCircleApp(
    currentUserId: String,
    userEmail: String,
    userDisplayName: String,
    isAdmin: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: StudyCircleViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                StudyCircleViewModel(
                    repository = StudyCircleRepository(db),
                    currentUserId = currentUserId,
                    userEmail = userEmail,
                    userDisplayName = userDisplayName,
                    isAdmin = isAdmin
                )
            }
        }
    )

    var currentTab by remember { mutableStateOf(StudyCircleTab.HOME) }
    var coursesInitialSubTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.snackbarMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // BackHandler returns from secondary tabs to Home cleanly
    BackHandler(enabled = currentTab != StudyCircleTab.HOME) {
        currentTab = StudyCircleTab.HOME
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useNavigationRail) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    StudyCircleTab.entries.forEach { tab ->
                        val selected = currentTab == tab
                        val label = stringResource(id = tab.labelRes)
                        NavigationRailItem(
                            selected = selected,
                            onClick = {
                                if (tab == StudyCircleTab.COURSES) coursesInitialSubTab = 0
                                currentTab = tab
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = label
                                )
                            },
                            label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            modifier = Modifier.testTag("nav_rail_${tab.routeId}")
                        )
                    }
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                contentWindowInsets = WindowInsets.safeDrawing,
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = when (currentTab) {
                                    StudyCircleTab.HOME -> stringResource(id = R.string.app_name)
                                    StudyCircleTab.COURSES -> "Batches & Study Hub"
                                    StudyCircleTab.CAMPUS -> "Location & Contact"
                                    StudyCircleTab.PROFILE -> "My Profile & Progress"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                },
                bottomBar = {
                    if (!useNavigationRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 3.dp
                        ) {
                            StudyCircleTab.entries.forEach { tab ->
                                val selected = currentTab == tab
                                val label = stringResource(id = tab.labelRes)
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        if (tab == StudyCircleTab.COURSES) coursesInitialSubTab = 0
                                        currentTab = tab
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    modifier = Modifier.testTag("nav_bar_${tab.routeId}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 760.dp)
                    ) {
                        when (currentTab) {
                            StudyCircleTab.HOME -> HomeScreen(
                                viewModel = viewModel,
                                onNavigateToCourses = {
                                    coursesInitialSubTab = 0
                                    currentTab = StudyCircleTab.COURSES
                                },
                                onNavigateToCampus = { currentTab = StudyCircleTab.CAMPUS },
                                onNavigateToDoubts = {
                                    coursesInitialSubTab = 2
                                    currentTab = StudyCircleTab.COURSES
                                }
                            )

                            StudyCircleTab.COURSES -> CoursesScreen(
                                viewModel = viewModel,
                                initialSubTab = coursesInitialSubTab
                            )

                            StudyCircleTab.CAMPUS -> CampusScreen(viewModel = viewModel)

                            StudyCircleTab.PROFILE -> ProfileScreen(
                                viewModel = viewModel,
                                onSignOut = {
                                    GoogleAuthHelper.signOut(
                                        credentialManager = credentialManager,
                                        scope = scope
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

package com.kosd.eventa.ui.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kosd.eventa.ui.auth.LoginScreen
import com.kosd.eventa.ui.auth.RegisterScreen
import com.kosd.eventa.ui.event.EventListScreen
import com.kosd.eventa.ui.event.EventCreateScreen
import com.kosd.eventa.ui.event.EventDetailScreen
import com.kosd.eventa.ui.event.EventReportScreen
import com.kosd.eventa.ui.event.EventStaffScreen
import com.kosd.eventa.ui.event.KioskModeScreen
import com.kosd.eventa.ui.profile.ProfileScreen
import com.kosd.eventa.ui.theme.pressScale
import com.kosd.eventa.EventaApp
import com.kosd.eventa.viewmodel.AuthViewModel
import com.kosd.eventa.viewmodel.EventViewModel
import com.kosd.eventa.viewmodel.OrganizationViewModel
import com.kosd.eventa.models.Permission
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Login    : Screen("login")
    object Register : Screen("register")
    object EmailConfirmation : Screen("email_confirmation")
    object Main     : Screen("main")
    object EventList   : Screen("event_list")
    object EventCreate : Screen("event_create")
    object EventDetail : Screen("event_detail/{eventId}")
    object EventReport : Screen("event_report/{eventId}")
    object Kiosk       : Screen("kiosk/{eventId}")
}

// ── Drawer menu items ─────────────────────────────────────────────────────────
sealed class DrawerItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val ownerOnly: Boolean = false
) {
    object Profile      : DrawerItem("profile",       "Profile",       Icons.Default.Person)
    object EventStaff   : DrawerItem("event_staff",   "Event Staff",   Icons.Default.Badge, ownerOnly = true)
}

// Routes that are full-screen overlays (no top bar)
private val fullScreenRoutes = setOf(
    "events_splash", "profile", "event_staff",
    "event_create", "event_detail", "event_report", "kiosk"
)

@Composable
fun EventaNavHost() {
    val navController = rememberNavController()

    val authViewModel: AuthViewModel            = viewModel(factory = AuthViewModel.Factory())
    val orgViewModel: OrganizationViewModel      = viewModel(factory = OrganizationViewModel.Factory())
    val eventViewModel: EventViewModel           = viewModel(factory = EventViewModel.Factory())

    // Restore session on launch
    LaunchedEffect(Unit) {
        authViewModel.loadCurrentUser(orgViewModel)
    }

    // Navigate to EmailConfirmation screen when requiresEmailConfirmation is set
    LaunchedEffect(authViewModel.requiresEmailConfirmation) {
        if (authViewModel.requiresEmailConfirmation) {
            navController.navigate(Screen.EmailConfirmation.route) {
                popUpTo(Screen.Register.route) { inclusive = true }
            }
        }
    }

    // When email is confirmed via Resend (emailConfirmed = true), navigate to
    // Login so the user can sign in. pendingRegistrationCompletion stays true
    // so completeRegistrationAfterConfirmation runs after sign-in.
    LaunchedEffect(authViewModel.emailConfirmed) {
        if (authViewModel.emailConfirmed) {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // Observe Supabase session status — when a session appears via deep link
    // callback while waiting for email confirmation, complete the registration.
    // In the Resend flow, the user signs in manually after email confirmation,
    // so this watches pendingRegistrationCompletion instead.
    LaunchedEffect(authViewModel.pendingRegistrationCompletion) {
        if (authViewModel.pendingRegistrationCompletion) {
            while (authViewModel.pendingRegistrationCompletion) {
                if (authViewModel.repository.currentUserId() != null) {
                    authViewModel.completeRegistrationAfterConfirmation(orgViewModel)
                    break
                }
                kotlinx.coroutines.delay(500)
            }
        }
    }

    // Global auth state observer
    LaunchedEffect(authViewModel.isAuthenticated) {
        val current = navController.currentBackStackEntry?.destination?.route
        if (authViewModel.isAuthenticated) {
            if (current == Screen.Login.route || current == Screen.Register.route ||
                current == Screen.EmailConfirmation.route) {
                navController.navigate(Screen.Main.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        } else {
            orgViewModel.resetState()
        }
    }

    NavHost(
        navController    = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel            = authViewModel,
                orgViewModel         = orgViewModel,
                onNavigateToRegister = { navController.navigate(Screen.Register.route) }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                viewModel       = authViewModel,
                orgViewModel    = orgViewModel,
                onNavigateBack  = { navController.popBackStack() }
            )
        }

        composable(Screen.EmailConfirmation.route) {
            EmailConfirmationScreen(
                viewModel   = authViewModel,
                onBackToLogin = {
                    authViewModel.requiresEmailConfirmation = false
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Main.route) {
            MainScreen(
                authViewModel       = authViewModel,
                orgViewModel        = orgViewModel,
                eventViewModel      = eventViewModel,
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}

// ── Transient Events Splash Card ─────────────────────────────────────────────
// Full-screen card showing the Events workspace identity for 3 seconds,
// then auto-navigates to the events list.

@Composable
fun EventsSplashCard(onTimeout: () -> Unit) {
    // Auto-navigate after 3 seconds
    LaunchedEffect(Unit) {
        delay(3000)
        onTimeout()
    }

    // Subtle fade-in animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600),
        label = "splash_fade"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .heightIn(min = 220.dp)
                .pressScale(scaleDown = 0.98f)
                .semantics {
                    role = Role.Button
                    contentDescription = "Eventa — Event check-ins"
                },
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.tertiary,
                                MaterialTheme.colorScheme.tertiary.copy(0.76f)
                            )
                        )
                    )
                    .padding(28.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.onTertiary.copy(0.16f)) {
                        Icon(
                            Icons.Default.ConfirmationNumber,
                            null,
                            Modifier.padding(10.dp).size(32.dp),
                            tint = MaterialTheme.colorScheme.onTertiary
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    "EVENTS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiary.copy(0.78f)
                )
                Text(
                    "Event check-ins",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiary
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "Guest registration, QR check-in, venue access and event reports.",
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiary.copy(0.82f)
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        null,
                        tint = MaterialTheme.colorScheme.onTertiary,
                        modifier = Modifier.padding(start = 12.dp).size(24.dp)
                    )
                }
            }
        }
    }
}

// ── Main Screen (Events-only) ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    authViewModel: AuthViewModel,
    orgViewModel: OrganizationViewModel,
    eventViewModel: EventViewModel,
    onLogout: () -> Unit
) {
    val innerNav: NavHostController = rememberNavController()
    val isAuthenticated: Boolean = authViewModel.isAuthenticated
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var modeContextReady by remember { mutableStateOf(orgViewModel.activeOrg != null && orgViewModel.activeMembership != null) }

    val backStackEntry by innerNav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Refresh orgs on entry (but don't overwrite if already loaded)
    LaunchedEffect(Unit) {
        if (orgViewModel.activeOrg == null || orgViewModel.activeMembership == null) {
            orgViewModel.loadOrganizationsAwait()
        }
        modeContextReady = true
    }

    // Watch for logout
    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated == false) onLogout()
    }

    val isOwner = orgViewModel.isOwnerInActiveOrg

    val drawerItems by remember {
        derivedStateOf {
            buildList {
                add(DrawerItem.Profile)
                if (isOwner) add(DrawerItem.EventStaff)
            }
        }
    }

    val showTopBar = currentRoute != "events_splash" && currentRoute !in fullScreenRoutes
    val showBackButton = currentRoute in setOf("event_create", "event_detail", "event_report", "kiosk", "event_staff", "profile")

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(280.dp)
            ) {
                // Drawer header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = authViewModel.currentUser?.initials ?: "?",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = authViewModel.currentUser?.fullName ?: "User",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = authViewModel.currentUser?.email ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (orgViewModel.activeOrg != null) {
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = orgViewModel.activeOrg?.name ?: "",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                HorizontalDivider()

                // Drawer items
                drawerItems.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentRoute == item.route,
                        onClick = {
                            scope.launch { drawerState.close() }
                            innerNav.navigate(item.route) {
                                popUpTo(innerNav.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Logout at bottom
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Logout, contentDescription = "Logout") },
                    label = { Text("Logout") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        authViewModel.logout()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (showTopBar) {
                    TopAppBar(
                        title = { Text(titleForRoute(currentRoute)) },
                        navigationIcon = {
                            if (showBackButton) {
                                IconButton(onClick = { innerNav.popBackStack() }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                }
                            } else {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            NavHost(
                navController = innerNav,
                startDestination = "events_splash",
                modifier = Modifier.padding(paddingValues)
            ) {
                // ── Transient Events Splash ───────────────────────────────────
                composable("events_splash") {
                    EventsSplashCard(
                        onTimeout = {
                            innerNav.navigate("events") {
                                popUpTo("events_splash") { inclusive = true }
                            }
                        }
                    )
                }

                // ── Drawer screens (full-screen) ──────────────────────────────
                composable(DrawerItem.Profile.route) {
                    ProfileScreen(authViewModel, orgViewModel)
                }
                composable(DrawerItem.EventStaff.route) {
                    EventStaffScreen(orgViewModel)
                }

                // ── EVENTS mode ───────────────────────────────────────────────
                composable("events") {
                    EventListScreen(navController = innerNav, orgViewModel = orgViewModel, eventViewModel = eventViewModel)
                }
                composable("event_create") {
                    EventCreateScreen(navController = innerNav, orgViewModel = orgViewModel, eventViewModel = eventViewModel)
                }
                composable("event_detail/{eventId}") { backStackEntry ->
                    val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
                    val event = eventViewModel.events.find { it.id == eventId } ?: eventViewModel.selectedEvent
                    if (event != null) {
                        EventDetailScreen(
                            navController = innerNav,
                            event = event,
                            eventViewModel = eventViewModel,
                            isAdmin = orgViewModel.can(Permission.MANAGE_EVENTS),
                            isEventStaff = orgViewModel.isEventStaffInActiveOrg
                        )
                    }
                }
                composable("event_report/{eventId}") { backStackEntry ->
                    val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
                    val event = eventViewModel.events.find { it.id == eventId } ?: eventViewModel.selectedEvent
                    if (event != null) {
                        EventReportScreen(navController = innerNav, event = event, eventViewModel = eventViewModel)
                    }
                }
                composable("kiosk/{eventId}") { backStackEntry ->
                    val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
                    val event = eventViewModel.events.find { it.id == eventId } ?: eventViewModel.selectedEvent
                    if (event != null) {
                        KioskModeScreen(navController = innerNav, event = event, eventViewModel = eventViewModel)
                    }
                }
            }
        }
    }
}

private fun titleForRoute(route: String?): String {
    return when (route) {
        DrawerItem.Profile.route    -> "Profile"
        DrawerItem.EventStaff.route -> "Event Staff Management"
        "events"                    -> "Events"
        "event_create"              -> "Create Event"
        "event_detail"              -> "Event Details"
        "event_report"              -> "Event Report"
        "kiosk"                     -> "Kiosk Check-In"
        else                        -> "Eventa"
    }
}

// ── Email Confirmation Screen ────────────────────────────────────────────────

@Composable
fun EmailConfirmationScreen(
    viewModel: AuthViewModel,
    onBackToLogin: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Email,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Check Your Email",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "We've sent a confirmation link to:\n${viewModel.pendingEmail ?: "your email"}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Click the link in the email to verify your account and complete registration.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        if (viewModel.isLoading) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Verifying...", style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onBackToLogin) {
            Text("Back to Login")
        }
    }
}

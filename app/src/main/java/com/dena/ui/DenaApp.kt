package com.dena.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dena.core.PaletteRegistry
import com.dena.data.DebtRepository
import com.dena.data.DenaDatabase
import com.dena.data.debt.Debt
import com.dena.ui.theme.DenaThemeMode
import com.dena.ui.components.DebtList
import com.dena.ui.components.EmptyState
import com.dena.ui.components.ScreenContainer
import com.dena.ui.screens.DebtDetailScreen
import com.dena.ui.screens.DebtFormScreen
import com.dena.ui.screens.IOweScreen
import com.dena.ui.screens.OnboardingScreen
import com.dena.ui.screens.OwedToMeScreen
import com.dena.ui.screens.SettingsScreen
import kotlinx.coroutines.flow.map

private data class DenaDestination(
    val label: String,
    val icon: ImageVector,
)

private fun destinationsFor(mode: String): List<DenaDestination> {
    val labels = com.dena.core.Terminology.labels(mode)
    return listOf(
        DenaDestination(labels.tabBorrowed, Icons.AutoMirrored.Filled.List),
        DenaDestination(labels.tabLent, Icons.Filled.Person),
        DenaDestination("Settings", Icons.Filled.Settings),
    )
}

@Composable
fun DenaApp(database: DenaDatabase) {
    val context = LocalContext.current
    val repository = remember { DebtRepository(database.debtDao(), database.transactionDao(), database) }
    val viewModelFactory = remember { DebtViewModelFactory(repository) }
    val viewModel: DebtViewModel = viewModel(factory = viewModelFactory)

    // Collect summary values from ViewModel
    val summaryOwedToMe by viewModel.summaryOwedToMe.collectAsStateWithLifecycle()
    val summaryIOwe by viewModel.summaryIOwe.collectAsStateWithLifecycle()
    val countOwedToMe by viewModel.countOwedToMe.collectAsStateWithLifecycle()
    val countIOwe by viewModel.countIOwe.collectAsStateWithLifecycle()

    val prefs = remember(context) { com.dena.core.DenaPreferences(context) }
    val destinations = destinationsFor(prefs.getTerminologyMode())
    val scope = rememberCoroutineScope()
    var pendingUpdate by remember { mutableStateOf<com.dena.core.ReleaseInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var settingsTabReselected by rememberSaveable { mutableStateOf(false) }
    var showDebtForm by rememberSaveable { mutableStateOf(false) }
    var selectedDebtId by rememberSaveable { mutableStateOf<Long?>(null) }
    var themeState by remember {
        mutableStateOf(
            ThemeState(
                themeMode = prefs.getThemeMode(),
                followSystemTheme = prefs.isFollowSystemTheme(),
                dynamicColorsEnabled = prefs.isDynamicColorsEnabled(),
                manualDark = prefs.getDarkMode(),
                paletteId = prefs.getPaletteId(),
                paletteEnabled = prefs.isPaletteEnabled(),
                dynamicScheme = prefs.getDynamicScheme(),
                fontScale = prefs.getFontScale(),
                displayScale = prefs.getDisplayScale(),
                fontKey = prefs.getAppFont(),
            )
        )
    }

    val listStateOwedToMe = rememberLazyListState()
    val listStateIOwe = rememberLazyListState()
    val activeListState = if (selectedTab == 1) listStateOwedToMe else listStateIOwe
    val showFabAnimated by remember {
        derivedStateOf {
            val layoutInfo = activeListState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) return@derivedStateOf true
            activeListState.firstVisibleItemIndex == 0 || activeListState.firstVisibleItemScrollOffset == 0
        }
    }
    // Background backfill: legacy debts without phones matched against device contacts (idempotent, no-op without READ_CONTACTS)
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try { com.dena.core.ContactPhoneMigrator.migrateIfNeeded(context, database.debtDao()) } catch (_: Exception) {}
            try {
                val prefsInner = com.dena.core.DenaPreferences(context)
                if (!prefsInner.isTemplatesSeeded() && database.templateDao().count() == 0) {
                    com.dena.core.TemplateEngine.defaultTemplates.forEach { (name, body) ->
                        database.templateDao().insert(com.dena.data.template.MessageTemplate(name = name, body = body))
                    }
                    prefsInner.setTemplatesSeeded(true)
                }
            } catch (_: Exception) {}
        }
    }

    val pagerState = rememberPagerState(
        initialPage = selectedTab.coerceAtMost(1),
        pageCount = { 2 },
    )
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress && selectedTab != 2) selectedTab = pagerState.currentPage
    }
    LaunchedEffect(selectedTab) {
        if (selectedTab < 2 && pagerState.currentPage != selectedTab) pagerState.animateScrollToPage(selectedTab, animationSpec = tween(260))
    }

    // Auto-check for updates on app open (throttled 12h) — notification + in-app dialog
    LaunchedEffect(Unit) {
        val now = System.currentTimeMillis()
        val last = prefs.getLastUpdateCheck()
        if (now - last < 12L * 60 * 60 * 1000) return@LaunchedEffect
        prefs.setLastUpdateCheck(now)
        val result = com.dena.core.UpdateChecker.fetchLatest()
        val info = result.getOrNull() ?: return@LaunchedEffect
        val current = com.dena.BuildConfig.VERSION_NAME
        if (!com.dena.core.UpdateChecker.isNewer(info.tagName, current)) return@LaunchedEffect
        if (info.tagName == prefs.getDismissedUpdateTag()) return@LaunchedEffect
        pendingUpdate = info
        showUpdateDialog = true
        // System notification (best-effort; silently no-ops if POST_NOTIFICATIONS denied)
        if (info.tagName != prefs.getLastNotifiedTag()) {
            com.dena.core.UpdateNotifier.notifyUpdateAvailable(context, info)
            prefs.setLastNotifiedTag(info.tagName)
        }
    }

    // Floating action button only on main tabs (not settings)
    val showFab = selectedTab < 2

    var lastBackPressTime by rememberSaveable { mutableLongStateOf(0L) }

    BackHandler(enabled = showDebtForm) { showDebtForm = false }
    BackHandler(enabled = selectedDebtId != null) { selectedDebtId = null }
    BackHandler(enabled = !showDebtForm && selectedDebtId == null) {
        val now = System.currentTimeMillis()
        if (now - lastBackPressTime < 2000L) {
            (context as? android.app.Activity)?.finish()
        } else {
            lastBackPressTime = now
            android.widget.Toast.makeText(context, "Press back again to exit", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // — Route depth for horizontal screen motion (slide-from-right) —
    // List = 0, Detail = 1, Form = 2, Settings = 3. Deeper = slide in from right; popping = slide to right.
    val targetRoute = when {
        showDebtForm -> 2
        selectedDebtId != null -> 1
        selectedTab == 2 -> 3
        else -> 0
    }

    com.dena.ui.theme.DenaTheme(
        themeMode = themeState.themeMode,
        palette = if (!themeState.dynamicColorsEnabled && themeState.paletteEnabled) PaletteRegistry.find(themeState.paletteId) else null,
        fontScale = themeState.fontScale,
        displayScale = themeState.displayScale,
        dynamicScheme = themeState.dynamicScheme,
        followSystemTheme = themeState.followSystemTheme,
        dynamicColorsEnabled = themeState.dynamicColorsEnabled,
        manualDark = themeState.manualDark,
        fontKey = themeState.fontKey,
    ) {
        var showOnboarding by remember { mutableStateOf(!prefs.isOnboardingDone()) }
        if (showOnboarding) {
            OnboardingScreen(onDone = { prefs.setOnboardingDone(true); showOnboarding = false })
            return@DenaTheme
        }
        Scaffold(
            bottomBar = {
                if (selectedDebtId == null && !showDebtForm) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ) {
                        destinations.forEachIndexed { index, destination ->
                            NavigationBarItem(
                                selected = selectedTab == index,
                                onClick = {
                                    if (selectedTab == index && index == 2) {
                                        settingsTabReselected = true
                                    }
                                    selectedTab = index
                                },
                                icon = {
                                    Icon(
                                        destination.icon,
                                        contentDescription = destination.label,
                                    )
                                },
                                label = {
                                    Text(
                                        destination.label,
                                        fontWeight = if (selectedTab == index) {
                                            androidx.compose.ui.text.font.FontWeight.SemiBold
                                        } else {
                                            androidx.compose.ui.text.font.FontWeight.Normal
                                        },
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = Color.Transparent,
                                ),
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = showFab && selectedDebtId == null && !showDebtForm && showFabAnimated,
                    enter = scaleIn(animationSpec = tween(220)) + fadeIn(tween(180)) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(260)),
                    exit = scaleOut(animationSpec = tween(180)) + fadeOut(tween(150)) + slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(200)),
                ) {
                    FloatingActionButton(
                        onClick = { showDebtForm = true },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Add debt",
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            },
        ) { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                color = MaterialTheme.colorScheme.background,
            ) {
                // Update dialog sits above the animated content so it doesn't remount on route change
                if (showUpdateDialog && pendingUpdate != null) {
                    com.dena.ui.components.UpdateAvailableDialog(
                        info = pendingUpdate!!,
                        onDismiss = { showUpdateDialog = false },
                        onDismissVersion = {
                            prefs.setDismissedUpdateTag(pendingUpdate!!.tagName)
                            showUpdateDialog = false
                        },
                    )
                }
                AnimatedContent(
                    targetState = targetRoute,
                    transitionSpec = {
                        val isForward = targetState > initialState
                        val slide = if (isForward) {
                            slideInHorizontally(animationSpec = tween(260), initialOffsetX = { it / 3 }) + fadeIn(tween(220)) togetherWith
                                slideOutHorizontally(animationSpec = tween(260), targetOffsetX = { -it / 3 }) + fadeOut(tween(220))
                        } else {
                            slideInHorizontally(animationSpec = tween(260), initialOffsetX = { -it / 3 }) + fadeIn(tween(220)) togetherWith
                                slideOutHorizontally(animationSpec = tween(260), targetOffsetX = { it / 3 }) + fadeOut(tween(220))
                        }
                        slide
                    },
                    label = "DenaRoute",
                ) { route ->
                    when (route) {
                        2 -> DebtFormScreen(
                            viewModel = viewModel,
                            onBack = { showDebtForm = false },
                            initialIsOwedToMe = selectedTab == 1
                        )
                        1 -> DebtDetailScreen(
                            debtId = selectedDebtId ?: -1L,
                            viewModel = viewModel,
                            onBack = { selectedDebtId = null },
                        )
                        3 -> SettingsScreen(
                            themeMode = themeState.themeMode,
                            onThemeChange = { newMode ->
                                themeState = themeState.copy(themeMode = newMode)
                                prefs.setThemeMode(newMode)
                            },
                            followSystemTheme = themeState.followSystemTheme,
                            onFollowSystemThemeChange = { v ->
                                themeState = themeState.copy(
                                    followSystemTheme = v,
                                    themeMode = if (v) DenaThemeMode.SYSTEM else DenaThemeMode.LIGHT
                                )
                                prefs.setFollowSystemTheme(v)
                                prefs.setThemeMode(themeState.themeMode)
                            },
                            manualDark = themeState.manualDark,
                            onDarkModeChange = { v ->
                                themeState = themeState.copy(manualDark = v)
                                prefs.setDarkMode(v)
                            },
                            dynamicColorsEnabled = themeState.dynamicColorsEnabled,
                            onDynamicColorsChange = { v ->
                                themeState = themeState.copy(
                                    dynamicColorsEnabled = v,
                                    paletteEnabled = if (v) false else themeState.paletteEnabled,
                                    themeMode = if (v) DenaThemeMode.DYNAMIC else DenaThemeMode.SYSTEM
                                )
                                prefs.setDynamicColorsEnabled(v)
                                prefs.setThemeMode(themeState.themeMode)
                                if (v) prefs.setPaletteEnabled(false)
                            },
                            paletteId = themeState.paletteId,
                            onPaletteChange = { newId ->
                                themeState = themeState.copy(paletteId = newId)
                                prefs.setPaletteId(newId)
                            },
                            paletteEnabled = themeState.paletteEnabled,
                            onPaletteEnabledChange = { enabled ->
                                themeState = themeState.copy(paletteEnabled = enabled)
                                prefs.setPaletteEnabled(enabled)
                            },
                            dynamicScheme = themeState.dynamicScheme,
                            onDynamicSchemeChange = { newScheme ->
                                themeState = themeState.copy(dynamicScheme = newScheme)
                                prefs.setDynamicScheme(newScheme)
                            },
                            fontScale = themeState.fontScale,
                            onFontScaleChange = { v ->
                                themeState = themeState.copy(fontScale = v)
                                prefs.setFontScale(v)
                            },
                            displayScale = themeState.displayScale,
                            onDisplayScaleChange = { v ->
                                themeState = themeState.copy(displayScale = v)
                                prefs.setDisplayScale(v)
                            },
                            fontKey = themeState.fontKey,
                            onFontKeyChange = { newKey ->
                                themeState = themeState.copy(fontKey = newKey)
                                prefs.setAppFont(newKey)
                            },
                            database = database,
                        )
                        else -> HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            userScrollEnabled = true,
                        ) { page ->
                            when (page) {
                                0 -> IOweScreen(
                                    viewModel = viewModel,
                                    summaryTotal = summaryIOwe,
                                    summaryCount = countIOwe,
                                    onDebtClick = { debt -> selectedDebtId = debt.id },
                                    listState = listStateIOwe,
                                )
                                1 -> OwedToMeScreen(
                                    viewModel = viewModel,
                                    summaryTotal = summaryOwedToMe,
                                    summaryCount = countOwedToMe,
                                    onDebtClick = { debt -> selectedDebtId = debt.id },
                                    listState = listStateOwedToMe,
                                )
                                else -> Box(modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                }
            }
        }
    }
}

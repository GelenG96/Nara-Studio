package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.model.ThemeMode
import com.example.data.model.WallpaperPageTarget
import com.example.data.model.WallpaperSourceType
import com.example.ui.screens.AiStudioScreen
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.NovelDetailScreen
import com.example.ui.screens.RecycleBinScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SettingsSection
import com.example.ui.theme.NaraTheme
import com.example.viewmodel.EditorViewModel
import com.example.viewmodel.MainAppViewModel
import com.example.viewmodel.NaraChatViewModel

object NaraRoutes {
    const val HOME = "home"
    const val LIBRARY = "library"
    const val AI_STUDIO = "ai_studio"
    const val SETTINGS = "settings?section={section}"
    const val NOVEL_DETAIL = "novel_detail/{novelId}"
    const val EDITOR = "editor/{novelId}/{chapterId}"
    const val RECYCLE_BIN = "recycle_bin"

    fun settingsRoute(section: SettingsSection = SettingsSection.MENU) = "settings?section=${section.name}"
    fun novelDetailRoute(novelId: Long) = "novel_detail/$novelId"
    fun editorRoute(novelId: Long, chapterId: Long) = "editor/$novelId/$chapterId"
}

private data class BottomNavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appContainer = (application as NaraApplication).container

        setContent {
            val mainVm: MainAppViewModel = viewModel(factory = MainAppViewModel.factory(appContainer))
            val prefs by mainVm.preferences.collectAsStateWithLifecycle()

            LaunchedEffect(prefs.privacyHidePreviewInRecents) {
                if (prefs.privacyHidePreviewInRecents) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            val darkTheme = when (prefs.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            val globalWp = prefs.resolveWallpaperFor(WallpaperPageTarget.GLOBAL)
            NaraTheme(
                darkTheme = darkTheme,
                panelOpacity = globalWp.panelOpacity,
                hasActiveWallpaper = globalWp.sourceType != WallpaperSourceType.NONE
            ) {
                NaraAppRoot(container = appContainer, mainVm = mainVm)
            }
        }
    }
}

@Composable
private fun NaraAppRoot(
    container: AppContainer,
    mainVm: MainAppViewModel
) {
    val navController = rememberNavController()
    val naraChatVm: NaraChatViewModel = viewModel(factory = NaraChatViewModel.factory(container))

    val prefs by mainVm.preferences.collectAsStateWithLifecycle()
    val apiKeyState by mainVm.apiKeyState.collectAsStateWithLifecycle()
    val folders by mainVm.activeFolders.collectAsStateWithLifecycle()
    val deletedFolders by mainVm.deletedFolders.collectAsStateWithLifecycle()
    val novels by mainVm.activeNovels.collectAsStateWithLifecycle()
    val deletedNovels by mainVm.deletedNovels.collectAsStateWithLifecycle()
    val chapters by mainVm.allActiveChapters.collectAsStateWithLifecycle()
    val deletedChapters by mainVm.deletedChapters.collectAsStateWithLifecycle()
    val recentStats by mainVm.recentStats.collectAsStateWithLifecycle()
    val revisionCount by mainVm.revisionCount.collectAsStateWithLifecycle()
    val storageSummary by mainVm.storageSummary.collectAsStateWithLifecycle()
    val connStatus by mainVm.connectionTestStatus.collectAsStateWithLifecycle()
    val connMessage by mainVm.connectionTestMessage.collectAsStateWithLifecycle()
    val ttsState by mainVm.ttsPlayer.state.collectAsStateWithLifecycle()
    val toastMsg by mainVm.toastMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(toastMsg) {
        toastMsg?.let {
            snackbarHostState.showSnackbar(it)
            mainVm.clearToast()
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val topTabs = remember {
        listOf(
            BottomNavTab(NaraRoutes.HOME, "Beranda", Icons.Rounded.Home, Icons.Outlined.Home, "nav_home"),
            BottomNavTab(NaraRoutes.LIBRARY, "Pustaka", Icons.Rounded.Book, Icons.Outlined.Book, "nav_library"),
            BottomNavTab(NaraRoutes.AI_STUDIO, "NARA", Icons.Rounded.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_ai"),
            BottomNavTab(NaraRoutes.SETTINGS, "Setelan", Icons.Rounded.Settings, Icons.Outlined.Settings, "nav_settings")
        )
    }

    val showTopLevelNav = currentRoute in listOf(
        NaraRoutes.HOME,
        NaraRoutes.LIBRARY,
        NaraRoutes.AI_STUDIO,
        NaraRoutes.SETTINGS
    )

    val navigateTopLevel: (String) -> Unit = { targetRoute ->
        val dest = if (targetRoute == NaraRoutes.SETTINGS) NaraRoutes.settingsRoute() else targetRoute
        navController.navigate(dest) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (showTopLevelNav && !useRail) {
                    val panelAlpha = prefs.resolveWallpaperFor(WallpaperPageTarget.GLOBAL).panelOpacity
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = panelAlpha),
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        topTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showTopLevelNav && useRail) {
                    val panelAlpha = prefs.resolveWallpaperFor(WallpaperPageTarget.GLOBAL).panelOpacity
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = panelAlpha)
                    ) {
                        topTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = NaraRoutes.HOME
                    ) {
                        composable(NaraRoutes.HOME) {
                            HomeScreen(
                                preferences = prefs,
                                novels = novels,
                                chapters = chapters,
                                folders = folders,
                                recentStats = recentStats,
                                onOpenNovel = { novelId ->
                                    mainVm.setActiveContext(novelId)
                                    navController.navigate(NaraRoutes.novelDetailRoute(novelId))
                                },
                                onOpenEditor = { novelId, chapterId ->
                                    mainVm.setActiveContext(novelId, chapterId)
                                    navController.navigate(NaraRoutes.editorRoute(novelId, chapterId))
                                },
                                onOpenAiStudio = { novelId, chapterId ->
                                    mainVm.setActiveContext(novelId, chapterId)
                                    naraChatVm.syncFromGlobalContext(novelId, chapterId, "")
                                    navigateTopLevel(NaraRoutes.AI_STUDIO)
                                },
                                onCreateNovel = { title, author, synopsis, genre, status, folderId, coverUri, target ->
                                    mainVm.createNovel(title, author, synopsis, genre, status, folderId, coverUri, target) { id ->
                                        navController.navigate(NaraRoutes.novelDetailRoute(id))
                                    }
                                },
                                onRestoreAutoBackup = { mainVm.restoreFromAutoBackup() }
                            )
                        }

                        composable(NaraRoutes.LIBRARY) {
                            LibraryScreen(
                                preferences = prefs,
                                folders = folders,
                                novels = novels,
                                chapters = chapters,
                                onOpenNovel = { novelId ->
                                    mainVm.setActiveContext(novelId)
                                    navController.navigate(NaraRoutes.novelDetailRoute(novelId))
                                },
                                onOpenEditor = { novelId, chapterId ->
                                    mainVm.setActiveContext(novelId, chapterId)
                                    navController.navigate(NaraRoutes.editorRoute(novelId, chapterId))
                                },
                                onOpenRecycleBin = { navController.navigate(NaraRoutes.RECYCLE_BIN) },
                                onCreateFolder = { name, parentId -> mainVm.createFolder(name, parentId) },
                                onRenameFolder = { f, newName -> mainVm.renameFolder(f, newName) },
                                onDeleteFolder = { id -> mainVm.softDeleteFolder(id) },
                                onCreateNovel = { title, author, synopsis, genre, status, folderId, coverUri, target ->
                                    mainVm.createNovel(title, author, synopsis, genre, status, folderId, coverUri, target) { id ->
                                        navController.navigate(NaraRoutes.novelDetailRoute(id))
                                    }
                                },
                                onToggleFavorite = { mainVm.toggleFavorite(it) },
                                onToggleArchive = { mainVm.toggleArchive(it) },
                                onMoveNovelToFolder = { nId, fId -> mainVm.moveNovelToFolder(nId, fId) },
                                onSoftDeleteNovel = { mainVm.softDeleteNovel(it) }
                            )
                        }

                        composable(NaraRoutes.AI_STUDIO) {
                            val activeNId by mainVm.activeContextNovelId.collectAsStateWithLifecycle()
                            val activeCId by mainVm.activeContextChapterId.collectAsStateWithLifecycle()
                            val snippet by mainVm.selectedSnippetForAi.collectAsStateWithLifecycle()

                            LaunchedEffect(activeNId, activeCId, snippet, novels.size) {
                                naraChatVm.syncFromGlobalContext(activeNId, activeCId, snippet)
                            }

                            val studioState by naraChatVm.studioState.collectAsStateWithLifecycle()
                            val studioNovels by naraChatVm.novels.collectAsStateWithLifecycle()
                            val studioChapters by naraChatVm.chaptersForSelectedNovel.collectAsStateWithLifecycle()
                            val studioBible by naraChatVm.storyBibleForSelectedNovel.collectAsStateWithLifecycle()
                            val studioMessages by naraChatVm.messages.collectAsStateWithLifecycle()

                            AiStudioScreen(
                                preferences = prefs,
                                apiKeyState = apiKeyState,
                                studioState = studioState,
                                novels = studioNovels,
                                chapters = studioChapters,
                                storyBible = studioBible,
                                messages = studioMessages,
                                ttsState = ttsState,
                                onSelectNovel = { naraChatVm.selectNovel(it) },
                                onSelectChapter = { naraChatVm.selectChapter(it) },
                                onToggleMultiChapter = { naraChatVm.toggleMultiChapter(it) },
                                onSetContextScope = { naraChatVm.setContextScope(it) },
                                onSetEditingIntensity = { naraChatVm.setEditingIntensity(it) },
                                onSetSnippet = { naraChatVm.setSelectedTextSnippet(it) },
                                onSendMessage = { msg, tool -> naraChatVm.sendUserMessage(msg, tool) },
                                onSendReplyToChapter = { text, replace -> naraChatVm.sendReplyToActiveChapter(text, replace) },
                                onClearChat = { naraChatVm.clearCurrentConversation() },
                                onSpeakText = { txt, lbl ->
                                    mainVm.ttsPlayer.speak(txt, lbl, prefs.aiConfig.autoFallbackEnabled)
                                },
                                onStopTts = { mainVm.ttsPlayer.stop() },
                                onDismissTtsError = { mainVm.ttsPlayer.clearError() },
                                onOpenAiSettings = {
                                    navController.navigate(NaraRoutes.settingsRoute(SettingsSection.AI_CONFIG))
                                },
                                onClearBanner = { naraChatVm.clearBanner() }
                            )
                        }

                        composable(
                            route = NaraRoutes.SETTINGS,
                            arguments = listOf(
                                navArgument("section") {
                                    type = NavType.StringType
                                    defaultValue = SettingsSection.MENU.name
                                }
                            )
                        ) { backStackEntry ->
                            val secName = backStackEntry.arguments?.getString("section")
                            val initialSec = runCatching {
                                SettingsSection.valueOf(secName ?: SettingsSection.MENU.name)
                            }.getOrDefault(SettingsSection.MENU)

                            SettingsScreen(
                                preferences = prefs,
                                apiKeyState = apiKeyState,
                                connectionTestStatus = connStatus,
                                connectionTestMessage = connMessage,
                                storageSummary = storageSummary,
                                revisionCount = revisionCount,
                                initialSection = initialSec,
                                onUpdateProfile = { name, bio, goal, lang -> mainVm.updateProfile(name, bio, goal, lang) },
                                onUpdateThemeMode = { mainVm.updateThemeMode(it) },
                                onUpdateEditorSettings = { mainVm.updateEditorSettings(it) },
                                onUpdateUseSeparateWallpapers = { mainVm.updateUseSeparateWallpapers(it) },
                                onUpdateWallpaper = { target, cfg -> mainVm.updateWallpaper(target, cfg) },
                                onPickGalleryWallpaper = { target, uri, cfg -> mainVm.setWallpaperFromGallery(target, uri, cfg) },
                                onSaveApiKey = { mainVm.saveApiKey(it) },
                                onDeleteApiKey = { mainVm.deleteApiKey() },
                                onTestConnection = { mainVm.testAiConnection(it) },
                                onUpdateAiConfig = { mode, fb, tts -> mainVm.updateAiEngineConfig(mode, fb, tts) },
                                onRunManualBackup = { mainVm.runManualAutoBackupNow() },
                                onRestoreAutoBackup = { mainVm.restoreFromAutoBackup() },
                                onExportFullBackup = { mainVm.exportFullBackupToUri(it) },
                                onImportFullBackup = { mainVm.importFullBackupFromUri(it) },
                                onOpenRecycleBin = { navController.navigate(NaraRoutes.RECYCLE_BIN) },
                                onClearAllChats = { mainVm.clearAllNaraChats() },
                                onUpdatePrivacyHide = { mainVm.updatePrivacyHidePreview(it) },
                                onRefreshStorage = { mainVm.refreshStorageSummary() }
                            )
                        }

                        composable(
                            route = NaraRoutes.NOVEL_DETAIL,
                            arguments = listOf(navArgument("novelId") { type = NavType.LongType })
                        ) { entry ->
                            val novelId = entry.arguments?.getLong("novelId") ?: 0L
                            val novel by remember(novelId) { mainVm.observeNovel(novelId) }
                                .collectAsStateWithLifecycle(initialValue = null)
                            val novelChapters by remember(novelId) { mainVm.observeChapters(novelId) }
                                .collectAsStateWithLifecycle(initialValue = emptyList())
                            val novelBible by remember(novelId) { mainVm.observeStoryBible(novelId) }
                                .collectAsStateWithLifecycle(initialValue = emptyList())

                            NovelDetailScreen(
                                preferences = prefs,
                                novel = novel,
                                chapters = novelChapters,
                                storyBible = novelBible,
                                folders = folders,
                                onBack = { navController.popBackStack() },
                                onOpenEditor = { chId ->
                                    mainVm.setActiveContext(novelId, chId)
                                    navController.navigate(NaraRoutes.editorRoute(novelId, chId))
                                },
                                onOpenAiStudio = { chId ->
                                    mainVm.setActiveContext(novelId, chId)
                                    naraChatVm.syncFromGlobalContext(novelId, chId, "")
                                    navigateTopLevel(NaraRoutes.AI_STUDIO)
                                },
                                onUpdateNovel = { updated, uri -> mainVm.updateNovelWithOptionalCover(updated, uri) },
                                onCreateChapter = { title, parentId ->
                                    mainVm.createChapter(novelId, title, parentId)
                                },
                                onReorderChapters = { orderedIds -> mainVm.reorderChapters(orderedIds) },
                                onSoftDeleteChapter = { chId -> mainVm.softDeleteChapter(chId) },
                                onSaveBibleEntry = { id, cat, title, sub, content, tags ->
                                    mainVm.saveStoryBibleEntry(id, novelId, cat, title, sub, content, tags)
                                },
                                onDeleteBibleEntry = { id -> mainVm.deleteStoryBibleEntry(id) },
                                onExportNovelMarkdown = { uri -> mainVm.exportNovelMarkdownToUri(novelId, uri) },
                                onImportDocumentAsChapter = { name, uri -> mainVm.importDocumentAsChapter(novelId, name, uri) }
                            )
                        }

                        composable(
                            route = NaraRoutes.EDITOR,
                            arguments = listOf(
                                navArgument("novelId") { type = NavType.LongType },
                                navArgument("chapterId") { type = NavType.LongType }
                            )
                        ) { entry ->
                            val novelId = entry.arguments?.getLong("novelId") ?: 0L
                            val chapterId = entry.arguments?.getLong("chapterId") ?: 0L
                            val editorVm: EditorViewModel = viewModel(
                                key = "editor_${novelId}_$chapterId",
                                factory = EditorViewModel.factory(container, novelId, chapterId)
                            )
                            val editorState by editorVm.uiState.collectAsStateWithLifecycle()

                            EditorScreen(
                                state = editorState,
                                preferences = prefs,
                                ttsState = ttsState,
                                onBack = { navController.popBackStack() },
                                onOpenAiStudio = { snippet ->
                                    mainVm.setActiveContext(novelId, editorState.chapter?.id ?: chapterId, snippet)
                                    naraChatVm.syncFromGlobalContext(novelId, editorState.chapter?.id ?: chapterId, snippet)
                                    navigateTopLevel(NaraRoutes.AI_STUDIO)
                                },
                                onSpeakText = { txt, lbl ->
                                    mainVm.ttsPlayer.speak(txt, lbl, prefs.aiConfig.autoFallbackEnabled)
                                },
                                onStopTts = { mainVm.ttsPlayer.stop() },
                                onDismissTtsError = { mainVm.ttsPlayer.clearError() },
                                onTitleChange = { editorVm.onTitleChange(it) },
                                onTextChange = { editorVm.onTextFieldChange(it) },
                                onToggleFormat = { editorVm.toggleFormatting(it) },
                                onAlignmentChange = { editorVm.onAlignmentChange(it) },
                                onNotesChange = { editorVm.onNotesChange(it) },
                                onUndo = { editorVm.undo() },
                                onRedo = { editorVm.redo() },
                                onPersistNow = { editorVm.persistNow() },
                                onCreateSnapshot = { editorVm.createManualSnapshot() },
                                onRestoreRevision = { editorVm.restoreRevision(it) },
                                onSwitchChapter = { chId ->
                                    mainVm.setActiveContext(novelId, chId)
                                    editorVm.loadChapter(chId)
                                },
                                onToggleFocusMode = { editorVm.toggleFocusMode() },
                                onToggleFindReplace = { editorVm.toggleFindReplace() },
                                onUpdateFindQuery = { editorVm.updateFindQuery(it) },
                                onUpdateReplaceQuery = { editorVm.updateReplaceQuery(it) },
                                onToggleCaseSensitive = { editorVm.toggleCaseSensitiveSearch() },
                                onFindNext = { editorVm.findNext() },
                                onFindPrev = { editorVm.findPrev() },
                                onReplaceCurrent = { editorVm.replaceCurrent() },
                                onReplaceAll = { editorVm.replaceAll() },
                                onUpdateEditorSettings = { mainVm.updateEditorSettings(it) },
                                onSetAiIntensity = { editorVm.setAiIntensity(it) },
                                onRunAiTool = { tool, extra -> editorVm.runInlineAiTool(tool, extra) },
                                onApplyAiResult = { replace -> editorVm.applyAiResultToEditor(replace) },
                                onExportChapter = { uri, fmt -> editorVm.exportChapterToUri(uri, fmt) },
                                onImportIntoChapter = { editorVm.importTextIntoChapter(it) },
                                onClearBanner = { editorVm.clearBanner() }
                            )
                        }

                        composable(NaraRoutes.RECYCLE_BIN) {
                            RecycleBinScreen(
                                preferences = prefs,
                                deletedNovels = deletedNovels,
                                deletedChapters = deletedChapters,
                                deletedFolders = deletedFolders,
                                onBack = { navController.popBackStack() },
                                onRestoreNovel = { mainVm.restoreNovel(it) },
                                onDeleteNovelPermanently = { mainVm.deleteNovelPermanently(it) },
                                onRestoreChapter = { mainVm.restoreChapter(it) },
                                onDeleteChapterPermanently = { mainVm.deleteChapterPermanently(it) },
                                onRestoreFolder = { mainVm.restoreFolder(it) },
                                onDeleteFolderPermanently = { mainVm.deleteFolderPermanently(it) },
                                onPurgeAll = { mainVm.purgeAllRecycleBin() }
                            )
                        }
                    }
                }
            }
        }
    }
}

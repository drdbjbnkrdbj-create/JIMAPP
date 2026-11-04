package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.JimBottomNav
import com.example.ui.courses.CourseWorkspaceScreen
import com.example.ui.courses.CoursesScreen
import com.example.ui.dashboard.HomeScreen
import com.example.ui.library.DocumentReaderScreen
import com.example.ui.library.LibraryScreen
import com.example.ui.more.MoreScreen
import com.example.ui.notes.NotesScreen
import com.example.ui.onboarding.WelcomeScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.progress.GpaProgressScreen
import com.example.ui.schedule.ScheduleImportScreen
import com.example.ui.schedule.ScheduleScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.JimTheme

@Composable
fun JimApp(
    viewModel: JimViewModel = viewModel()
) {
    val profile by viewModel.profile.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val subScreen by viewModel.subScreen.collectAsState()

    // RTL layout provider for natural Arabic interface
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        JimTheme(darkTheme = profile.isDarkMode) {
            val showBottomNav = subScreen is SubScreen.None

            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                bottomBar = {
                    if (showBottomNav) {
                        JimBottomNav(
                            selectedTab = selectedTab,
                            onTabSelected = { viewModel.selectTab(it) },
                            modifier = Modifier.navigationBarsPadding()
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (val currentSub = subScreen) {
                        is SubScreen.CourseWorkspace -> {
                            CourseWorkspaceScreen(
                                courseId = currentSub.courseId,
                                viewModel = viewModel,
                                onBack = { viewModel.navigateBack() },
                                onOpenFile = { docId -> viewModel.openDocumentReader(docId) }
                            )
                        }
                        is SubScreen.DocumentReader -> {
                            DocumentReaderScreen(
                                docId = currentSub.docId,
                                viewModel = viewModel,
                                onBack = { viewModel.navigateBack() }
                            )
                        }
                        is SubScreen.ScheduleImport -> {
                            ScheduleImportScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateBack() }
                            )
                        }
                        is SubScreen.GpaCalculator -> {
                            GpaProgressScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateBack() }
                            )
                        }
                        is SubScreen.QuickNotes -> {
                            NotesScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateBack() }
                            )
                        }
                        is SubScreen.StudentProfile -> {
                            ProfileScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateBack() }
                            )
                        }
                        is SubScreen.Settings -> {
                            SettingsScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateBack() }
                            )
                        }
                        is SubScreen.Onboarding -> {
                            WelcomeScreen(
                                viewModel = viewModel,
                                onStart = { viewModel.finishOnboarding() }
                            )
                        }
                        SubScreen.None -> {
                            when (selectedTab) {
                                MainTab.HOME -> HomeScreen(
                                    viewModel = viewModel,
                                    onNavigateToTab = { viewModel.selectTab(it) },
                                    onOpenCourse = { viewModel.openCourseWorkspace(it) }
                                )
                                MainTab.COURSES -> CoursesScreen(
                                    viewModel = viewModel,
                                    onOpenCourse = { viewModel.openCourseWorkspace(it) }
                                )
                                MainTab.SCHEDULE -> ScheduleScreen(
                                    viewModel = viewModel,
                                    onOpenImport = { viewModel.openScheduleImport() },
                                    onOpenCourse = { viewModel.openCourseWorkspace(it) }
                                )
                                MainTab.LIBRARY -> LibraryScreen(
                                    viewModel = viewModel,
                                    onOpenDoc = { viewModel.openDocumentReader(it) }
                                )
                                MainTab.MORE -> MoreScreen(
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

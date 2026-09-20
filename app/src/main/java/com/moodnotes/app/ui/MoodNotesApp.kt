package com.moodnotes.app.ui

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.moodnotes.app.R
import com.moodnotes.app.ui.calendar.CalendarScreen
import com.moodnotes.app.ui.components.EaseInOutQuint
import com.moodnotes.app.ui.components.EaseOutQuint
import com.moodnotes.app.ui.components.springy
import com.moodnotes.app.ui.diary.DiaryEditScreen
import com.moodnotes.app.ui.diary.DiaryViewScreen
import com.moodnotes.app.ui.diary.DiaryScreen
import com.moodnotes.app.ui.settings.SettingsScreen
import com.moodnotes.app.ui.stats.StatsScreen
import com.moodnotes.app.ui.theme.LocalReduceMotion
import com.moodnotes.app.ui.today.TodayScreen
import com.moodnotes.app.util.Dates

import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

private enum class TopLevel(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    TODAY("today", R.string.nav_today, Icons.Rounded.Mood),
    CALENDAR("calendar", R.string.nav_calendar, Icons.Rounded.CalendarMonth),
    DIARY("diary", R.string.nav_diary, Icons.AutoMirrored.Rounded.MenuBook),
    STATS("stats", R.string.nav_stats, Icons.Rounded.BarChart),
}

@Composable
fun MoodNotesApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = TopLevel.entries.any { it.route == currentRoute }
    val reduceMotion = LocalReduceMotion.current

    val enterMs = if (reduceMotion) 1 else 420
    val exitMs = if (reduceMotion) 1 else 220

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            // 底栏收展：进入设置/编辑器时滑出，回到底层页时滑入
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(tween(enterMs, easing = EaseOutQuint)) { it } +
                    fadeIn(tween(enterMs, easing = EaseOutQuint)),
                exit = slideOutVertically(tween(exitMs, easing = EaseInOutQuint)) { it } +
                    fadeOut(tween(exitMs, easing = EaseInOutQuint)),
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 0.dp,
                ) {
                    TopLevel.entries.forEach { destination ->
                        val selected = currentRoute == destination.route
                        val iconScale by animateFloatAsState(
                            if (selected) 1.15f else 1f,
                            springy(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                        )
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(destination.route) {
                                        popUpTo(TopLevel.TODAY.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = stringResource(destination.labelRes),
                                    modifier = Modifier.graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                    },
                                )
                            },
                            label = { Text(stringResource(destination.labelRes)) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = TopLevel.TODAY.route,
                enterTransition = {
                    fadeIn(tween(enterMs, easing = EaseOutQuint)) +
                        scaleIn(initialScale = 0.985f, animationSpec = tween(enterMs, easing = EaseOutQuint))
                },
                exitTransition = {
                    fadeOut(tween(exitMs, easing = EaseInOutQuint)) +
                        scaleOut(targetScale = 0.99f, animationSpec = tween(exitMs, easing = EaseInOutQuint))
                },
                popEnterTransition = {
                    fadeIn(tween(enterMs, easing = EaseOutQuint)) +
                        scaleIn(initialScale = 0.985f, animationSpec = tween(enterMs, easing = EaseOutQuint))
                },
                popExitTransition = {
                    fadeOut(tween(exitMs, easing = EaseInOutQuint)) +
                        scaleOut(targetScale = 0.99f, animationSpec = tween(exitMs, easing = EaseInOutQuint))
                },
            ) {
                composable(TopLevel.TODAY.route) {
                    TodayScreen(
                        onWriteDiary = {
                            navController.navigate("edit?entryId=0&date=${Dates.todayEpochDay()}")
                        },
                        onOpenEntry = { id ->
                            navController.navigate("view?entryId=$id")
                        },
                        onOpenCalendar = {
                            navController.navigate(TopLevel.CALENDAR.route) {
                                popUpTo(TopLevel.TODAY.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onOpenSettings = {
                            navController.navigate("settings")
                        },
                        onOpenDiaryTab = {
                            navController.navigate(TopLevel.DIARY.route) {
                                popUpTo(TopLevel.TODAY.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
                composable(TopLevel.CALENDAR.route) {
                    CalendarScreen(
                        onWriteDiary = { dateEpoch ->
                            navController.navigate("edit?entryId=0&date=$dateEpoch")
                        },
                        onOpenEntry = { id ->
                            navController.navigate("view?entryId=$id")
                        },
                    )
                }
                composable(TopLevel.DIARY.route) {
                    DiaryScreen(
                        onOpenEntry = { id -> navController.navigate("view?entryId=$id") },
                        onCreate = {
                            navController.navigate("edit?entryId=0&date=${Dates.todayEpochDay()}")
                        },
                    )
                }
                composable(TopLevel.STATS.route) {
                    StatsScreen()
                }
                composable(
                    route = "view?entryId={entryId}",
                    arguments = listOf(
                        navArgument("entryId") {
                            type = NavType.LongType
                            defaultValue = 0L
                        },
                    ),
                    enterTransition = {
                        fadeIn(tween(enterMs, easing = EaseOutQuint)) +
                            scaleIn(initialScale = 0.985f, animationSpec = tween(enterMs, easing = EaseOutQuint))
                    },
                    exitTransition = {
                        fadeOut(tween(exitMs, easing = EaseInOutQuint)) +
                            scaleOut(targetScale = 0.99f, animationSpec = tween(exitMs, easing = EaseInOutQuint))
                    },
                    popEnterTransition = {
                        fadeIn(tween(enterMs, easing = EaseOutQuint))
                    },
                    popExitTransition = {
                        scaleOut(targetScale = 0.99f, animationSpec = tween(exitMs, easing = EaseInOutQuint)) +
                            fadeOut(tween(exitMs, easing = EaseInOutQuint))
                    },
                ) {
                    DiaryViewScreen(
                        onBack = { navController.popBackStack() },
                        onEdit = { id, date ->
                            navController.navigate("edit?entryId=$id&date=$date")
                        },
                    )
                }
                composable("settings") {
                    SettingsScreen(onDone = { navController.popBackStack() })
                }
                composable(
                    route = "edit?entryId={entryId}&date={date}",
                    arguments = listOf(
                        navArgument("entryId") {
                            type = NavType.LongType
                            defaultValue = 0L
                        },
                        navArgument("date") {
                            type = NavType.LongType
                            defaultValue = 0L
                        },
                    ),
                    enterTransition = {
                        slideInVertically(tween(enterMs, easing = EaseOutQuint)) { it / 5 } +
                            fadeIn(tween(enterMs, easing = EaseOutQuint))
                    },
                    exitTransition = {
                        slideOutVertically(tween(exitMs, easing = EaseInOutQuint)) { it / 7 } +
                            fadeOut(tween(exitMs))
                    },
                    popEnterTransition = {
                        fadeIn(tween(enterMs, easing = EaseOutQuint))
                    },
                    popExitTransition = {
                        slideOutVertically(tween(exitMs, easing = EaseOutQuint)) { it / 6 } +
                            fadeOut(tween(exitMs))
                    },
                ) {
                    DiaryEditScreen(onDone = { navController.popBackStack() })
                }
            }
        }
    }
}

package com.example

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

/** The screens that live in the bottom navigation bar. */
val BottomTabs: List<Screen> = listOf(Screen.Dashboard, Screen.Donate, Screen.Store, Screen.Smiles)

/**
 * Opens a bottom-bar tab. Also used for shortcuts to a tab, such as the dashboard's Smile Wall card.
 *
 * If the tab is already in the back stack (for example the Dashboard underneath the Smiles or
 * Impact Tiers screen) we simply go back to it. Restoring saved state there would put the user
 * straight back on the sub-screen they were trying to leave, so the tab would seem not to work.
 */
fun NavController.goToTab(route: String) {
    if (currentDestination?.route == route) return
    if (popBackStack(route, inclusive = false)) return

    // Leave a sub-screen (like Impact Tiers) first, so a tab is never saved with it on top.
    if (BottomTabs.none { it.route == currentDestination?.route }) popBackStack()

    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * The bottom tab to highlight for a screen. Sub-screens opened from the Dashboard (Impact Tiers
 * and the profile) keep the Dashboard tab lit, so the bar never looks empty.
 */
fun highlightedTabRoute(route: String?): String? = when (route) {
    Screen.Tiers.route, Screen.Profile.route -> Screen.Dashboard.route
    else -> BottomTabs.firstOrNull { it.route == route }?.route
}

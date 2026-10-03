package com.example

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

/** The screens that live in the donor's bottom navigation bar. */
val BottomTabs: List<Screen> = listOf(Screen.Dashboard, Screen.Donate, Screen.Store, Screen.Smiles)

/** The receiver's bottom navigation bar. */
val NgoTabs: List<Screen> = listOf(Screen.NgoDashboard, Screen.NgoOffers, Screen.NgoStock, Screen.NgoSmiles)

/** Which bar a screen belongs to, or null for screens without one (welcome and role selection). */
fun tabsForRoute(route: String?): List<Screen>? = when {
    route == null -> null
    NgoTabs.any { it.route == route } || route == Screen.SendSmile.route -> NgoTabs
    BottomTabs.any { it.route == route } || route == Screen.Tiers.route || route == Screen.Profile.route -> BottomTabs
    else -> null
}

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
    val onTab = (BottomTabs + NgoTabs).any { it.route == currentDestination?.route }
    if (!onTab) popBackStack()

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
    Screen.SendSmile.route -> Screen.NgoSmiles.route
    else -> (BottomTabs + NgoTabs).firstOrNull { it.route == route }?.route
}

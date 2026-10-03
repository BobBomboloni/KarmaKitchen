package com.example

import org.junit.Assert.assertEquals
import org.junit.Test

class TabDirectionTest {
    private val home = Screen.Dashboard.route
    private val donate = Screen.Donate.route
    private val store = Screen.Store.route
    private val smiles = Screen.Smiles.route

    @Test fun movingToATabOnTheRightGivesPositive() {
        assertEquals(1, tabMoveDirection(home, donate))
        assertEquals(1, tabMoveDirection(donate, store))
        assertEquals(1, tabMoveDirection(home, smiles))
    }

    @Test fun movingToATabOnTheLeftGivesNegative() {
        assertEquals(-1, tabMoveDirection(donate, home))
        assertEquals(-1, tabMoveDirection(smiles, store))
        assertEquals(-1, tabMoveDirection(smiles, home))
    }

    @Test fun receiverTabsFollowTheirOwnOrder() {
        assertEquals(1, tabMoveDirection(Screen.NgoDashboard.route, Screen.NgoOffers.route))
        assertEquals(-1, tabMoveDirection(Screen.NgoSmiles.route, Screen.NgoStock.route))
    }

    @Test fun nonTabMovesHaveNoTabDirection() {
        assertEquals(0, tabMoveDirection(home, home))
        assertEquals(0, tabMoveDirection(home, Screen.Tiers.route))
        assertEquals(0, tabMoveDirection(Screen.RoleSelection.route, home))
        assertEquals(0, tabMoveDirection(null, home))
        // The donor and receiver bars are separate, so there is no left or right between them.
        assertEquals(0, tabMoveDirection(home, Screen.NgoDashboard.route))
    }

    @Test fun tabMovesSlideTheSameWayWhetherOrNotTheyArePops() {
        // goToTab sometimes pops back to a tab that is still in the back stack and sometimes
        // navigates to it again; the slide must not depend on which one happened.
        assertEquals(1, slideDirection(home, donate, isPop = false))
        assertEquals(1, slideDirection(home, donate, isPop = true))
        assertEquals(-1, slideDirection(donate, home, isPop = false))
        assertEquals(-1, slideDirection(donate, home, isPop = true))
    }

    @Test fun subScreensSlideForwardWhenOpenedAndBackWhenClosed() {
        assertEquals(1, slideDirection(home, Screen.Tiers.route, isPop = false))
        assertEquals(-1, slideDirection(Screen.Tiers.route, home, isPop = true))
        assertEquals(1, slideDirection(Screen.RoleSelection.route, home, isPop = false))
        assertEquals(-1, slideDirection(Screen.NgoDashboard.route, Screen.RoleSelection.route, isPop = true))
    }
}

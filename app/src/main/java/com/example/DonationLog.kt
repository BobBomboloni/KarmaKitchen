package com.example

import androidx.compose.runtime.mutableStateListOf

/**
 * Donations made from the Donate screen during this session, newest first. The home screen shows
 * them above the sample history. (A real release would keep these on a server.)
 */
object DonationLog {
    val submitted = mutableStateListOf<DonationItem>()

    fun add(item: DonationItem) {
        submitted.add(0, item)
    }
}

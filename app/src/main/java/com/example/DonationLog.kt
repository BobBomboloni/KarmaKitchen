package com.example

import androidx.compose.runtime.mutableStateListOf
import com.example.cloud.Cloud
import com.example.cloud.CloudSync

/**
 * Donations made from the Donate screen during this session, newest first. The home screen shows
 * them above the sample history, and the receiver side updates them as it accepts and receives
 * them. (A real release would keep these on a server.)
 */
object DonationLog {
    val submitted = mutableStateListOf<DonationItem>()

    /** Coins the NGO has confirmed but the donor's wallet has not picked up yet. */
    val pendingCredits = mutableStateListOf<Int>()

    /** Replaces everything with the signed-in donor's donations from the cloud, newest first. */
    fun replaceAll(items: List<DonationItem>) {
        submitted.clear()
        submitted.addAll(items)
    }

    fun add(item: DonationItem) {
        submitted.add(0, item)
    }

    fun update(id: String, change: (DonationItem) -> DonationItem) {
        val index = submitted.indexOfFirst { it.id == id }
        if (index >= 0) submitted[index] = change(submitted[index])
    }

    /** The donor has seen that donation [id] was delivered, so its card can leave the home screen. */
    fun acknowledge(id: String) {
        update(id) { it.copy(acknowledged = true) }
        if (Cloud.enabled) CloudSync.acknowledge(id)
    }

    /** Queue the coins for donation [id]; the app adds them to the donor's balance. */
    fun credit(id: String) {
        submitted.firstOrNull { it.id == id }?.let { pendingCredits.add(it.points) }
    }

    fun clear() {
        submitted.clear()
        pendingCredits.clear()
    }
}

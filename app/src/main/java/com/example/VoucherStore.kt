package com.example

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import com.example.cloud.Cloud
import org.json.JSONArray
import org.json.JSONObject

/** A reward the donor has bought. [code] is empty for give-back rewards. */
data class Voucher(
    val id: String,
    val rewardId: String,
    val brand: String,
    val title: String,
    val code: String,
    val boughtAt: Long,
    val paid: Int,
    /** 0 means it does not expire. */
    val validDays: Int
)

/**
 * What the donor bought. Without an account it is kept in SharedPreferences so it is still there
 * after a restart; with one it comes from the account in the cloud ([CloudSync] keeps it in step).
 */
object VoucherStore {
    private const val PREFS = "karmakitchen_vouchers"
    private const val KEY = "vouchers"
    private var loaded = false

    /** Newest first. Compose reads of this list update automatically. */
    val vouchers = mutableStateListOf<Voucher>()

    fun load(context: Context) {
        if (loaded) return
        loaded = true
        if (Cloud.enabled) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)?.let { raw ->
            vouchers.addAll(vouchersFromJson(raw))
        }
    }

    /** Swaps in the signed-in donor's rewards from the cloud, newest first. */
    fun replaceAll(fromCloud: List<Voucher>) {
        vouchers.clear()
        vouchers.addAll(fromCloud)
    }

    fun add(context: Context, bought: List<Voucher>) {
        vouchers.addAll(0, bought)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY, vouchersToJson(vouchers.toList()))
            .apply()
    }
}

internal fun vouchersToJson(list: List<Voucher>): String {
    val array = JSONArray()
    list.forEach { v ->
        array.put(
            JSONObject()
                .put("id", v.id)
                .put("rewardId", v.rewardId)
                .put("brand", v.brand)
                .put("title", v.title)
                .put("code", v.code)
                .put("boughtAt", v.boughtAt)
                .put("paid", v.paid)
                .put("validDays", v.validDays)
        )
    }
    return array.toString()
}

internal fun vouchersFromJson(raw: String): List<Voucher> = try {
    val array = JSONArray(raw)
    (0 until array.length()).map { i ->
        val o = array.getJSONObject(i)
        Voucher(
            id = o.getString("id"),
            rewardId = o.getString("rewardId"),
            brand = o.getString("brand"),
            title = o.getString("title"),
            code = o.getString("code"),
            boughtAt = o.getLong("boughtAt"),
            paid = o.getInt("paid"),
            validDays = o.getInt("validDays")
        )
    }
} catch (e: Exception) {
    Log.w("VoucherStore", "Could not read saved vouchers", e)
    emptyList()
}

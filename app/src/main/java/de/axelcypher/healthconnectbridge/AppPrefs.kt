package de.axelcypher.healthconnectbridge

import android.content.Context
import android.content.SharedPreferences
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class AppStatus(
    val lastReceivedWeightKg: Double?,
    val lastReceivedAt: Instant?,
    val lastWrittenAt: Instant?,
    val lastError: String?,
    val logs: List<String>,
)

class AppPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    fun snapshot(): AppStatus = AppStatus(
        lastReceivedWeightKg =
            prefs.readNullableDouble(KEY_LAST_RECEIVED_WEIGHT),
        lastReceivedAt =
            prefs.getString(KEY_LAST_RECEIVED_AT, null)?.toInstantOrNull(),
        lastWrittenAt =
            prefs.getString(KEY_LAST_WRITTEN_TIMESTAMP, null)?.toInstantOrNull(),
        lastError =
            prefs.getString(KEY_LAST_ERROR, null),
        logs = readLogs(),
    )

    fun recordReceived(
        weightKg: Double,
        instant: Instant,
        source: String?,
    ) {
        prefs.edit()
            .putLong(KEY_LAST_RECEIVED_WEIGHT, weightKg.toBits())
            .putString(KEY_LAST_RECEIVED_AT, instant.toString())
            .remove(KEY_LAST_ERROR)
            .commit()

        addLog(
            "Received ${formatWeight(weightKg)} kg from ${
                source ?: "broadcast"
            }"
        )
    }

    fun recordWritten(
        weightKg: Double,
        instant: Instant,
        hash: String,
    ) {
        prefs.edit()
            .putLong(KEY_LAST_WRITTEN_WEIGHT, weightKg.toBits())
            .putString(KEY_LAST_WRITTEN_TIMESTAMP, instant.toString())
            .putString(KEY_LAST_WRITTEN_HASH, hash)
            .remove(KEY_LAST_ERROR)
            .commit()

        addLog("Written weight to Health Connect")
    }

    fun recordNutritionReceived(
        count: Int,
    ) {
        prefs.edit()
            .putString(
                KEY_LAST_RECEIVED_AT,
                Instant.now().toString(),
            )
            .remove(KEY_LAST_ERROR)
            .commit()

        addLog("Received $count nutrition record(s)")
    }

    fun recordNutritionWritten(
        name: String,
        instant: Instant,
        hash: String,
    ) {
        prefs.edit()
            .putString(
                KEY_LAST_WRITTEN_TIMESTAMP,
                instant.toString(),
            )
            .putString(
                KEY_LAST_WRITTEN_HASH,
                hash,
            )
            .remove(KEY_LAST_ERROR)
            .commit()

        addLog("Written nutrition to Health Connect: $name")
    }

    fun recordError(message: String) {
        prefs.edit()
            .putString(KEY_LAST_ERROR, message)
            .commit()

        addLog(message)
    }

    fun addLog(message: String) {
        val entry =
            "[${LOG_TIME_FORMATTER.format(Instant.now())}] ${
                message.sanitizeLog()
            }"

        val updated =
            (listOf(entry) + readLogs())
                .take(MAX_LOG_ENTRIES)

        prefs.edit()
            .putString(
                KEY_LOGS,
                updated.joinToString(LOG_SEPARATOR),
            )
            .commit()
    }

    fun lastWrittenHash(): String? =
        prefs.getString(KEY_LAST_WRITTEN_HASH, null)

    fun clearDuplicateState() {
        prefs.edit()
            .remove(KEY_LAST_WRITTEN_WEIGHT)
            .remove(KEY_LAST_WRITTEN_TIMESTAMP)
            .remove(KEY_LAST_WRITTEN_HASH)
            .commit()

        addLog("Duplicate state cleared")
    }

    fun registerListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener,
    ) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener,
    ) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    private fun readLogs(): List<String> =
        prefs.getString(KEY_LOGS, null)
            ?.split(LOG_SEPARATOR)
            ?.filter(String::isNotBlank)
            .orEmpty()

    private fun SharedPreferences.readNullableDouble(
        key: String,
    ): Double? =
        if (contains(key)) {
            Double.fromBits(getLong(key, 0L))
        } else {
            null
        }

    private fun String.toInstantOrNull(): Instant? =
        runCatching {
            Instant.parse(this)
        }.getOrNull()

    private fun String.sanitizeLog(): String =
        replace(LOG_SEPARATOR, " ")

    companion object {
        private const val PREFS_NAME =
            "health_connect_bridge"

        private const val KEY_LAST_RECEIVED_WEIGHT =
            "last_received_weight_kg"

        private const val KEY_LAST_RECEIVED_AT =
            "last_received_at"

        private const val KEY_LAST_WRITTEN_WEIGHT =
            "last_written_weight_kg"

        private const val KEY_LAST_WRITTEN_TIMESTAMP =
            "last_written_timestamp"

        private const val KEY_LAST_WRITTEN_HASH =
            "last_written_hash"

        private const val KEY_LAST_ERROR =
            "last_error"

        private const val KEY_LOGS =
            "logs"

        private const val LOG_SEPARATOR =
            "\u001E"

        private const val MAX_LOG_ENTRIES =
            30

        private val LOG_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm")
                .withZone(ZoneId.systemDefault())

        fun formatWeight(weightKg: Double): String =
            "%.1f".format(
                java.util.Locale.ROOT,
                weightKg,
            )
    }
}

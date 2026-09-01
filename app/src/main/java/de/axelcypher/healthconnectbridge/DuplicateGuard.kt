package de.axelcypher.healthconnectbridge

import java.math.BigDecimal
import java.time.Instant

class DuplicateGuard(
    private val prefs: AppPrefs,
) {
    fun hash(
        weightKg: Double,
        instant: Instant,
    ): String {
        val normalizedWeight =
            BigDecimal.valueOf(weightKg)
                .stripTrailingZeros()
                .toPlainString()

        return "$instant|$normalizedWeight"
    }

    fun nutritionHash(
        name: String,
        kcal: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
        instant: Instant,
    ): String {
        return listOf(
            instant.toString(),
            name.trim(),
            normalize(kcal),
            normalize(protein),
            normalize(carbs),
            normalize(fat),
        ).joinToString("|")
    }

    fun isDuplicate(hash: String): Boolean =
        prefs.lastWrittenHash() == hash

    fun clear() =
        prefs.clearDuplicateState()

    private fun normalize(value: Double): String =
        BigDecimal.valueOf(value)
            .stripTrailingZeros()
            .toPlainString()
}

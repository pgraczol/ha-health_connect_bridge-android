package de.axelcypher.healthconnectbridge

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class NutritionSyncRepository(context: Context) {

    private val prefs = AppPrefs(context)
    private val duplicateGuard = DuplicateGuard(prefs)
    private val healthConnect = HealthConnectManager(context)

    suspend fun process(
        received: List<ReceivedNutrition>,
    ) = writeMutex.withLock {

        prefs.recordNutritionReceived(received.size)

        when (healthConnect.availability()) {
            HealthConnectAvailability.UNAVAILABLE -> {
                prefs.recordError(
                    "Health Connect is not available on this device."
                )
                return@withLock
            }

            HealthConnectAvailability.UPDATE_REQUIRED -> {
                prefs.recordError(
                    "Health Connect must be updated."
                )
                return@withLock
            }

            HealthConnectAvailability.AVAILABLE -> Unit
        }

        if (!healthConnect.hasWritePermission()) {
            prefs.recordError(
                "Nutrition received but Health Connect WRITE_NUTRITION permission is missing."
            )
            return@withLock
        }

        for (nutrition in received) {
            val hash = duplicateGuard.nutritionHash(
                name = nutrition.name,
                kcal = nutrition.kcal,
                protein = nutrition.protein,
                carbs = nutrition.carbs,
                fat = nutrition.fat,
                instant = nutrition.instant,
            )

            if (duplicateGuard.isDuplicate(hash)) {
                prefs.addLog(
                    "Duplicate nutrition ignored: ${nutrition.name}"
                )
                continue
            }

            try {
                healthConnect.writeNutrition(
                    name = nutrition.name,
                    kcal = nutrition.kcal,
                    protein = nutrition.protein,
                    carbs = nutrition.carbs,
                    fat = nutrition.fat,
                    mealType = nutrition.mealType,
                    instant = nutrition.instant,
                )

                prefs.recordNutritionWritten(
                    name = nutrition.name,
                    instant = nutrition.instant,
                    hash = hash,
                )
            } catch (exception: Exception) {
                prefs.recordError(
                    "Health Connect nutrition write failed: ${
                        exception.message
                            ?: exception.javaClass.simpleName
                    }"
                )
            }
        }
    }

    companion object {
        private val writeMutex = Mutex()
    }
}

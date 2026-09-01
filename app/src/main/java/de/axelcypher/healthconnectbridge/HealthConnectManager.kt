package de.axelcypher.healthconnectbridge

import android.content.Context
import android.content.Intent
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.grams
import androidx.health.connect.client.units.kilocalories
import java.time.Instant
import java.time.ZoneOffset

enum class HealthConnectAvailability {
    AVAILABLE,
    UPDATE_REQUIRED,
    UNAVAILABLE,
}

class HealthConnectManager(context: Context) {
    private val appContext = context.applicationContext

    fun availability(): HealthConnectAvailability =
        when (HealthConnectClient.getSdkStatus(appContext)) {
            HealthConnectClient.SDK_AVAILABLE ->
                HealthConnectAvailability.AVAILABLE

            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                HealthConnectAvailability.UPDATE_REQUIRED

            else ->
                HealthConnectAvailability.UNAVAILABLE
        }

    suspend fun hasWritePermission(): Boolean {
        if (availability() != HealthConnectAvailability.AVAILABLE) return false

        return client()
            .permissionController
            .getGrantedPermissions()
            .contains(WRITE_NUTRITION_PERMISSION)
    }

    suspend fun writeNutrition(
        name: String,
        kcal: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
        mealType: Int,
        instant: Instant,
    ) {
        val zoneOffset: ZoneOffset =
            ZoneOffset.systemDefault().rules.getOffset(instant)

        val record = NutritionRecord(
            startTime = instant,
            startZoneOffset = zoneOffset,
            endTime = instant.plusSeconds(1),
            endZoneOffset = zoneOffset,
            energy = kcal.kilocalories,
            protein = protein.grams,
            totalCarbohydrate = carbs.grams,
            totalFat = fat.grams,
            name = name,
            mealType = mealType,
            metadata = Metadata.manualEntry(),
        )

        client().insertRecords(listOf(record))
    }

    fun settingsIntent(): Intent =
        Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun client(): HealthConnectClient =
        HealthConnectClient.getOrCreate(appContext)

    companion object {
        val WRITE_NUTRITION_PERMISSION: String =
            HealthPermission.getWritePermission(NutritionRecord::class)

        val REQUIRED_PERMISSIONS: Set<String> =
            setOf(WRITE_NUTRITION_PERMISSION)
    }
}

package de.axelcypher.healthconnectbridge

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.lifecycleScope
import de.axelcypher.healthconnectbridge.ui.MainScreen
import de.axelcypher.healthconnectbridge.ui.MainUiState
import kotlinx.coroutines.launch
import java.time.Instant

class MainActivity : ComponentActivity() {
    private lateinit var appPrefs: AppPrefs
    private lateinit var healthConnect: HealthConnectManager
    private var uiState = MainUiState()

    @Suppress("UNCHECKED_CAST")
    private val permissionLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
            as ActivityResultContract<Set<String>, Set<String>>,
    ) {
        lifecycleScope.launch { refreshStatus() }
    }

    private val prefsListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            runOnUiThread { render() }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appPrefs = AppPrefs(this)
        healthConnect = HealthConnectManager(this)

        appPrefs.registerListener(prefsListener)

        render()
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { refreshStatus() }
    }

    override fun onDestroy() {
        appPrefs.unregisterListener(prefsListener)
        super.onDestroy()
    }

    private fun render() {
        uiState = uiState.copy(
            status = appPrefs.snapshot()
        )

        setContent {
            MainScreen(
                state = uiState,

                onGrantPermission = {
                    if (
                        healthConnect.availability() ==
                        HealthConnectAvailability.AVAILABLE
                    ) {
                        permissionLauncher.launch(
                            HealthConnectManager.REQUIRED_PERMISSIONS
                        )
                    } else {
                        appPrefs.recordError(
                            "Health Connect is not available for permission request."
                        )
                    }
                },

                onOpenSettings = {
                    runCatching {
                        startActivity(
                            healthConnect.settingsIntent()
                        )
                    }.onFailure {
                        appPrefs.recordError(
                            "Could not open Health Connect settings."
                        )
                    }
                },

                onSendTestNutrition = ::sendTestNutrition,

                onClearDuplicateState = {
                    DuplicateGuard(appPrefs).clear()
                },
            )
        }
    }

    private suspend fun refreshStatus() {
        val availability = healthConnect.availability()

        val permissionGranted = runCatching {
            availability == HealthConnectAvailability.AVAILABLE &&
                healthConnect.hasWritePermission()
        }.getOrDefault(false)

        uiState = uiState.copy(
            availability = availability,
            permissionGranted = permissionGranted,
            status = appPrefs.snapshot(),
        )

        render()
    }

    private fun sendTestNutrition() {
        val payload = """
            [
              {
                "date": "2026-08-30",
                "time": "08:53",
                "meal": "Reggeli",
                "name": "Teszt zabkasa",
                "kcal": 415.43,
                "protein": 14.07,
                "carbs": 37.02,
                "fat": 24.7
              }
            ]
        """.trimIndent()

        val intent = Intent(
            NutritionIntentParser.ACTION_WRITE_NUTRITION
        )
            .setPackage(packageName)
            .putExtra("payload", payload)

        sendBroadcast(intent)
    }
}

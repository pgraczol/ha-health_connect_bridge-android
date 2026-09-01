package de.axelcypher.healthconnectbridge.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.axelcypher.healthconnectbridge.AppPrefs
import de.axelcypher.healthconnectbridge.AppStatus
import de.axelcypher.healthconnectbridge.HealthConnectAvailability
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class MainUiState(
    val availability: HealthConnectAvailability =
        HealthConnectAvailability.UNAVAILABLE,
    val permissionGranted: Boolean = false,
    val status: AppStatus =
        AppStatus(null, null, null, null, emptyList()),
)

@Composable
fun MainScreen(
    state: MainUiState,
    onGrantPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onSendTestNutrition: () -> Unit,
    onClearDuplicateState: () -> Unit,
) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "HealthConnectBridge",
                    style = MaterialTheme.typography.headlineMedium,
                )

                StatusRow(
                    "Health Connect available",
                    when (state.availability) {
                        HealthConnectAvailability.AVAILABLE -> "yes"
                        HealthConnectAvailability.UPDATE_REQUIRED ->
                            "update required"
                        HealthConnectAvailability.UNAVAILABLE -> "no"
                    },
                )

                StatusRow(
                    "WRITE_NUTRITION permission",
                    if (state.permissionGranted) "granted" else "missing",
                )

                StatusRow(
                    "Last received",
                    state.status.lastReceivedAt.formatForDisplay(),
                )

                StatusRow(
                    "Last written to Health Connect",
                    state.status.lastWrittenAt.formatForDisplay(),
                )

                StatusRow(
                    "Last error",
                    state.status.lastError ?: "-",
                )

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onGrantPermission,
                    enabled =
                        state.availability ==
                            HealthConnectAvailability.AVAILABLE,
                ) {
                    Text("Grant Health Connect permission")
                }

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenSettings,
                    enabled =
                        state.availability !=
                            HealthConnectAvailability.UNAVAILABLE,
                ) {
                    Text("Open Health Connect settings")
                }

                if (BuildConfig.DEBUG) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onSendTestNutrition,
                    ) {
                        Text("Send test nutrition locally")
                    }
                }

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onClearDuplicateState,
                ) {
                    Text("Clear duplicate state")
                }

                HorizontalDivider(
                    modifier = Modifier.padding(top = 8.dp)
                )

                Text(
                    "Local log",
                    style = MaterialTheme.typography.titleMedium,
                )

                if (state.status.logs.isEmpty()) {
                    Text("No entries yet.")
                } else {
                    state.status.logs.forEach {
                        Text(it)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
        )

        Text(
            value,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun Instant?.formatForDisplay(): String =
    this?.let(DISPLAY_FORMATTER::format) ?: "-"

private val DISPLAY_FORMATTER =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())

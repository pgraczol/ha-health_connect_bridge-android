package de.axelcypher.healthconnectbridge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NutritionBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (
            intent.action !=
            NutritionIntentParser.ACTION_WRITE_NUTRITION
        ) {
            return
        }

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(
            SupervisorJob() + Dispatchers.IO
        ).launch {
            try {
                when (
                    val parsed =
                        NutritionIntentParser.parse(intent)
                ) {
                    is NutritionParseResult.Success -> {
                        NutritionSyncRepository(appContext)
                            .process(parsed.values)
                    }

                    is NutritionParseResult.Error -> {
                        AppPrefs(appContext)
                            .recordError(parsed.message)
                    }
                }
            } catch (exception: Exception) {
                AppPrefs(appContext).recordError(
                    "Nutrition receiver failed: ${
                        exception.message
                            ?: exception.javaClass.simpleName
                    }"
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}

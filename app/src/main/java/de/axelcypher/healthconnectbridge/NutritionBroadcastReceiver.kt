package de.axelcypher.healthconnectbridge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NutritionBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != NutritionIntentParser.ACTION_WRITE_NUTRITION) {
            return
        }

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (val result = NutritionIntentParser.parse(intent)) {

                    is NutritionParseResult.Success -> {
                        val manager = HealthConnectManager(context)

                        if (!manager.hasWritePermission()) {
                            Log.e(TAG, "Health Connect nutrition permission missing.")
                            return@launch
                        }

                        result.values.forEach { nutrition ->
                            manager.writeNutrition(
                                name = nutrition.name,
                                kcal = nutrition.kcal,
                                protein = nutrition.protein,
                                carbs = nutrition.carbs,
                                fat = nutrition.fat,
                                mealType = nutrition.mealType,
                                instant = nutrition.instant,
                            )
                        }

                        Log.i(
                            TAG,
                            "Nutrition sync successful: ${result.values.size} records."
                        )
                    }

                    is NutritionParseResult.Error -> {
                        Log.e(TAG, result.message)
                    }
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Nutrition sync failed.", exception)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "NutritionBridge"
    }
}

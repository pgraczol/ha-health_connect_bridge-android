package de.axelcypher.healthconnectbridge

import android.content.Intent
import org.json.JSONArray
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

data class ReceivedNutrition(
    val name: String,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val mealType: Int,
    val instant: Instant,
)

sealed interface NutritionParseResult {
    data class Success(val values: List<ReceivedNutrition>) : NutritionParseResult
    data class Error(val message: String) : NutritionParseResult
}

object NutritionIntentParser {

    fun parse(intent: Intent): NutritionParseResult {
        val payload = intent.getStringExtra(EXTRA_PAYLOAD)
            ?: return NutritionParseResult.Error(
                "Broadcast ignored: missing payload."
            )

        return parsePayload(payload)
    }

    internal fun parsePayload(payload: String): NutritionParseResult {
        return try {
            val array = JSONArray(payload)

            if (array.length() == 0) {
                return NutritionParseResult.Error(
                    "Broadcast ignored: payload contains no nutrition records."
                )
            }

            val records = mutableListOf<ReceivedNutrition>()

            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)

                val date = item.getString("date")
                val time = item.getString("time")

                val localDateTime = LocalDateTime.parse(
                    "$date" + "T" + time
                )

                val instant = localDateTime
                    .atZone(ZoneId.systemDefault())
                    .toInstant()

                val meal = item.optString("meal", "")
                    .trim()
                    .lowercase()

                val mealType = when (meal) {
                    "reggeli" -> MEAL_BREAKFAST
                    "ebéd" -> MEAL_LUNCH
                    "vacsora" -> MEAL_DINNER
                    "tízórai",
                    "uzsonna",
                    "nasi",
                    "snack" -> MEAL_SNACK
                    else -> MEAL_UNKNOWN
                }

                val name = item.getString("name").trim()

                if (name.isEmpty()) {
                    return NutritionParseResult.Error(
                        "Record $i has an empty food name."
                    )
                }

                records += ReceivedNutrition(
                    name = name,
                    kcal = item.getDouble("kcal"),
                    protein = item.getDouble("protein"),
                    carbs = item.getDouble("carbs"),
                    fat = item.getDouble("fat"),
                    mealType = mealType,
                    instant = instant,
                )
            }

            NutritionParseResult.Success(records)

        } catch (exception: Exception) {
            NutritionParseResult.Error(
                "Invalid nutrition payload: ${
                    exception.message ?: exception.javaClass.simpleName
                }"
            )
        }
    }

    const val ACTION_WRITE_NUTRITION =
        "de.axelcypher.healthconnectbridge.WRITE_NUTRITION"

    private const val EXTRA_PAYLOAD = "payload"

    const val MEAL_UNKNOWN = 0
    const val MEAL_BREAKFAST = 1
    const val MEAL_LUNCH = 2
    const val MEAL_DINNER = 3
    const val MEAL_SNACK = 4
}

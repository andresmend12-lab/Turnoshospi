package com.example.turnoshospi.data.local.converter

import androidx.room.TypeConverter
import com.example.turnoshospi.RegisteredUser
import com.example.turnoshospi.ShiftTime
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * TypeConverters para Room.
 * Convierte tipos complejos (Maps) a JSON para almacenamiento.
 */
class RoomTypeConverters {

    private val gson = Gson()

    // --- Map<String, ShiftTime> ---

    @TypeConverter
    fun shiftTimesMapToJson(map: Map<String, ShiftTime>?): String {
        return gson.toJson(map ?: emptyMap<String, ShiftTime>())
    }

    @TypeConverter
    fun jsonToShiftTimesMap(json: String?): Map<String, ShiftTime> {
        if (json.isNullOrBlank()) return emptyMap()
        val type = object : TypeToken<Map<String, ShiftTime>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    // --- Map<String, Int> ---

    @TypeConverter
    fun staffRequirementsMapToJson(map: Map<String, Int>?): String {
        return gson.toJson(map ?: emptyMap<String, Int>())
    }

    @TypeConverter
    fun jsonToStaffRequirementsMap(json: String?): Map<String, Int> {
        if (json.isNullOrBlank()) return emptyMap()
        val type = object : TypeToken<Map<String, Int>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    // --- Map<String, RegisteredUser> ---

    @TypeConverter
    fun registeredUsersMapToJson(map: Map<String, RegisteredUser>?): String {
        return gson.toJson(map ?: emptyMap<String, RegisteredUser>())
    }

    @TypeConverter
    fun jsonToRegisteredUsersMap(json: String?): Map<String, RegisteredUser> {
        if (json.isNullOrBlank()) return emptyMap()
        val type = object : TypeToken<Map<String, RegisteredUser>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
}

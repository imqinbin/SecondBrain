package com.qb.secondbrain.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.qb.secondbrain.data.model.ImagePath

class Converters {

    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromImagePathList(value: List<ImagePath>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toImagePathList(value: String): List<ImagePath> {
        val type = object : TypeToken<List<ImagePath>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }
}
package com.anyoneide.app.data.room

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.anyoneide.app.model.BuildArtifactData

/**
 * Type converters for Room database
 */
class Converters {
    private val gson = Gson()
    
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }
    
    @TypeConverter
    fun fromStringMap(value: Map<String, String>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toStringMap(value: String): Map<String, String> {
        val mapType = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(value, mapType) ?: emptyMap()
    }
    
    @TypeConverter
    fun fromLongList(value: List<Long>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toLongList(value: String): List<Long> {
        val listType = object : TypeToken<List<Long>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }
    
    @TypeConverter
    fun fromBuildArtifactList(value: List<BuildArtifactData>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toBuildArtifactList(value: String): List<BuildArtifactData> {
        val listType = object : TypeToken<List<BuildArtifactData>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }
}
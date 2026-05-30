package com.example.suffixtrainer.data

import androidx.room.TypeConverter
import com.example.suffixtrainer.model.Category

/**
 * Room type converters. [Category] is stored as its enum name so the value is
 * stable across reorderings of the enum's ordinal positions.
 */
class Converters {
    @TypeConverter
    fun categoryToString(category: Category): String = category.name

    @TypeConverter
    fun stringToCategory(value: String): Category = Category.valueOf(value)
}


package backend

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    // Converts a List<String> to a JSON String for Room storage
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return if (value == null) null else Gson().toJson(value)
    }

    // Converts a JSON String from Room storage back to a List<String>
    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return if (value == null) null else Gson().fromJson(value, object : TypeToken<List<String>>() {}.type)
    }
}
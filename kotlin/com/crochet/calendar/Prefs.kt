package com.crochet.calendar

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class Prefs(context: Context) {
    private val prefs = context.getSharedPreferences("holidays_prefs", Context.MODE_PRIVATE)

    fun saveHolidays(holidays: List<holiday>) {
        val array = JSONArray()
        holidays.forEach { holiday ->
            val obj = JSONObject()
            obj.put("name", holiday.name)
            obj.put("month", holiday.month)
            obj.put("day", holiday.day)
            obj.put("emoji", holiday.emoji)
            obj.put("prefix", holiday.prefix)
            array.put(obj)
        }
        prefs.edit().putString("holidays", array.toString()).apply()
    }

    fun loadHolidays(): List<holiday> {
        val json = prefs.getString("holidays", null) ?: return emptyList()
        return try {
            val list = mutableListOf<holiday>()
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(holiday(
                    obj.getString("name"),
                    obj.getInt("month"),
                    obj.getInt("day"),
                    obj.getString("emoji"),
                    obj.getString("prefix")
                ))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
    fun saveBirthdays(birthdays: List<birthday>) {
        val array = JSONArray()
        birthdays.forEach { birthday ->
            val obj = JSONObject()
            obj.put("name", birthday.name)
            obj.put("month", birthday.month)
            obj.put("day", birthday.day)
            obj.put("yours", birthday.yours)
            array.put(obj)
        }
        prefs.edit().putString("birthdays", array.toString()).apply()
    }

    fun loadBirthdays(): List<birthday> {
        val json = prefs.getString("birthdays", null) ?: return emptyList()
        return try {
            val list = mutableListOf<birthday>()
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(birthday(
                    obj.getString("name"),
                    obj.getInt("month"),
                    obj.getInt("day"),
                    obj.getBoolean("yours")
                ))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
}

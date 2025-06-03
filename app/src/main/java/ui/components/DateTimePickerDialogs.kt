package ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import java.util.Calendar
import java.util.Date

// You might already have a basic one, but here's a more complete one for Compose usage.

fun showDatePicker(context: Context, initialCalendar: Calendar, onDateSelected: (Calendar) -> Unit) {
    val year = initialCalendar.get(Calendar.YEAR)
    val month = initialCalendar.get(Calendar.MONTH)
    val day = initialCalendar.get(Calendar.DAY_OF_MONTH)

    DatePickerDialog(context, { _, selectedYear, selectedMonth, selectedDay ->
        initialCalendar.set(selectedYear, selectedMonth, selectedDay)
        onDateSelected(initialCalendar)
    }, year, month, day).show()
}

fun showTimePicker(context: Context, initialCalendar: Calendar, onTimeSelected: (Calendar) -> Unit) {
    val hour = initialCalendar.get(Calendar.HOUR_OF_DAY)
    val minute = initialCalendar.get(Calendar.MINUTE)

    TimePickerDialog(context, { _, selectedHour, selectedMinute ->
        initialCalendar.set(Calendar.HOUR_OF_DAY, selectedHour)
        initialCalendar.set(Calendar.MINUTE, selectedMinute)
        onTimeSelected(initialCalendar)
    }, hour, minute, false).show() // false for 12-hour format, true for 24-hour format
}
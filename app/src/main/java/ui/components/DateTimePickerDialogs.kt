package ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import java.util.Calendar
import java.util.Date

fun showDatePicker(context: Context, initialCalendar: Calendar, onDateSelected: (Calendar) -> Unit) {
    val year = initialCalendar.get(Calendar.YEAR)
    val month = initialCalendar.get(Calendar.MONTH)
    val day = initialCalendar.get(Calendar.DAY_OF_MONTH)

    DatePickerDialog(context, { _, selectedYear, selectedMonth, selectedDay ->
        val newCalendar = Calendar.getInstance().apply {
            set(selectedYear, selectedMonth, selectedDay,
                initialCalendar.get(Calendar.HOUR_OF_DAY),
                initialCalendar.get(Calendar.MINUTE)
            )
        }
        onDateSelected(newCalendar)
    }, year, month, day).show()
}

fun showTimePicker(context: Context, initialCalendar: Calendar, onTimeSelected: (Calendar) -> Unit) {
    val hour = initialCalendar.get(Calendar.HOUR_OF_DAY)
    val minute = initialCalendar.get(Calendar.MINUTE)

    TimePickerDialog(context, { _, selectedHour, selectedMinute ->
        val newCalendar = Calendar.getInstance().apply {
            timeInMillis = initialCalendar.timeInMillis
            set(Calendar.HOUR_OF_DAY, selectedHour)
            set(Calendar.MINUTE, selectedMinute)
        }
        onTimeSelected(newCalendar)
    }, hour, minute, false).show()
}
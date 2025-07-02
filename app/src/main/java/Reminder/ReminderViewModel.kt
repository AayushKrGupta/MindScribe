
package com.example.mindscribe.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindscribe.model.Reminder
import com.example.mindscribe.repository.ReminderRepository
import kotlinx.coroutines.launch

class ReminderViewModel(private val repository: ReminderRepository) : ViewModel() {
    val reminders = repository.allReminders

    fun addReminder(title: String, description: String?, timestamp: Long) {
        viewModelScope.launch {
            repository.insert(
                Reminder(
                    title = title,
                    description = description,
                    timestamp = timestamp
                )
            )
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            repository.delete(reminder)
        }
    }
}
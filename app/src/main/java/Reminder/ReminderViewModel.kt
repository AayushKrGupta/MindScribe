package Reminder

// com.example.mindscribe.viewmodel/ReminderViewModel.kt


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import Reminder.Reminder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

class ReminderViewModel : ViewModel() {

    private val _reminders = MutableStateFlow<List<Reminder>>(emptyList())
    val reminders: StateFlow<List<Reminder>> = _reminders.asStateFlow()

    // Simple ID generator for in-memory reminders
    private val nextId = AtomicInteger(0)

    fun addReminder(title: String, description: String?, timestamp: Long) {
        viewModelScope.launch {
            val newReminder = Reminder(
                id = nextId.incrementAndGet(),
                title = title,
                description = description,
                timestamp = timestamp
            )
            _reminders.value = _reminders.value + newReminder
            // In a real app, you'd save to Room here
            // reminderRepository.insert(newReminder)
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            _reminders.value = _reminders.value.filter { it.id != reminder.id }
            // In a real app, you'd delete from Room here
            // reminderRepository.delete(reminder)
        }
    }
}
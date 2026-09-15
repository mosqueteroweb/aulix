package com.seguimiento.clases.ui.screens.alarm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.seguimiento.clases.alarm.AlarmScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class AlarmUiState(
    val selectedHour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
    val selectedMinute: Int = Calendar.getInstance().get(Calendar.MINUTE),
    val isAlarmActive: Boolean = false,
    val scheduledTriggerMillis: Long = 0L,
    val remainingTimeText: String = "",
    val feedbackMessage: String? = null
)

class AlarmViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val scheduler = AlarmScheduler(application.applicationContext)

    private val _uiState = MutableStateFlow(
        AlarmUiState(
            selectedHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
            selectedMinute = Calendar.getInstance().get(Calendar.MINUTE),
            isAlarmActive = scheduler.isAlarmActive(),
            scheduledTriggerMillis = scheduler.getTriggerMillis(),
            remainingTimeText = computePreviewText(
                Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                Calendar.getInstance().get(Calendar.MINUTE)
            )
        )
    )
    val uiState: StateFlow<AlarmUiState> = _uiState.asStateFlow()

    init {
        // Ticker en segundo plano para actualizar el tiempo restante cada 30 segundos
        viewModelScope.launch {
            while (true) {
                delay(30_000L)
                refreshState()
            }
        }
    }

    /**
     * Se invoca al entrar en la pantalla.
     * Si ya hay una alarma activa, muestra la hora de la alarma programada.
     * Si no hay alarma, inicializa las ruedas a la hora y minutos actuales.
     */
    fun initializeToCurrentTime() {
        val isCurrentlyActive = scheduler.isAlarmActive()
        val hour: Int
        val minute: Int

        if (isCurrentlyActive) {
            val triggerMillis = scheduler.getTriggerMillis()
            val cal = Calendar.getInstance().apply { timeInMillis = triggerMillis }
            hour = cal.get(Calendar.HOUR_OF_DAY)
            minute = cal.get(Calendar.MINUTE)
        } else {
            val now = Calendar.getInstance()
            hour = now.get(Calendar.HOUR_OF_DAY)
            minute = now.get(Calendar.MINUTE)
        }

        _uiState.update { state ->
            state.copy(
                selectedHour = hour,
                selectedMinute = minute,
                isAlarmActive = isCurrentlyActive,
                scheduledTriggerMillis = if (isCurrentlyActive) scheduler.getTriggerMillis() else 0L,
                remainingTimeText = if (isCurrentlyActive) {
                    AlarmScheduler.formatRemainingTime(scheduler.getTriggerMillis())
                } else {
                    computePreviewText(hour, minute)
                }
            )
        }
    }

    fun onTimeChanged(hour: Int, minute: Int) {
        if (_uiState.value.isAlarmActive) return // Bloqueado si hay alarma activa
        _uiState.update { state ->
            state.copy(
                selectedHour = hour,
                selectedMinute = minute,
                remainingTimeText = computePreviewText(hour, minute)
            )
        }
    }

    fun toggleAlarm() {
        if (_uiState.value.isAlarmActive) {
            cancelAlarm()
        } else {
            activateAlarm()
        }
    }

    fun activateAlarm() {
        if (scheduler.isAlarmActive()) {
            _uiState.update { it.copy(feedbackMessage = "Ya hay una alarma activa. Debes desactivarla antes de poner otra.") }
            return
        }

        val hour = _uiState.value.selectedHour
        val minute = _uiState.value.selectedMinute
        val triggerMillis = scheduler.scheduleAlarm(hour, minute)
        val remaining = AlarmScheduler.formatRemainingTime(triggerMillis)

        _uiState.update {
            it.copy(
                isAlarmActive = true,
                scheduledTriggerMillis = triggerMillis,
                remainingTimeText = remaining,
                feedbackMessage = "Alarma activada para las ${String.format("%02d:%02d", hour, minute)}"
            )
        }
    }

    fun cancelAlarm() {
        scheduler.cancelAlarm()
        val hour = _uiState.value.selectedHour
        val minute = _uiState.value.selectedMinute

        _uiState.update {
            it.copy(
                isAlarmActive = false,
                scheduledTriggerMillis = 0L,
                remainingTimeText = computePreviewText(hour, minute),
                feedbackMessage = "Alarma desactivada"
            )
        }
    }

    fun clearFeedbackMessage() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    fun refreshState() {
        val isActive = scheduler.isAlarmActive()
        val triggerMillis = scheduler.getTriggerMillis()

        _uiState.update { state ->
            state.copy(
                isAlarmActive = isActive,
                scheduledTriggerMillis = triggerMillis,
                remainingTimeText = if (isActive) {
                    AlarmScheduler.formatRemainingTime(triggerMillis)
                } else {
                    computePreviewText(state.selectedHour, state.selectedMinute)
                }
            )
        }
    }

    private fun computePreviewText(hour: Int, minute: Int): String {
        val nextTrigger = AlarmScheduler.calculateNextTriggerMillis(hour, minute)
        return AlarmScheduler.formatRemainingTime(nextTrigger)
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AlarmViewModel(application) as T
                }
            }
    }
}

package com.example.safewalk.ui.sos

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safewalk.data.repository.SosRepository
import com.example.safewalk.service.enqueueSos
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SosState {
    object Idle : SosState
    object Sending : SosState
    object Sent : SosState
    object Failed : SosState
}

@HiltViewModel
class SosViewModel @Inject constructor(
    private val repo: SosRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow<SosState>(SosState.Idle)
    val state: StateFlow<SosState> = _state

    fun triggerSos(source: String = "manual") {
        viewModelScope.launch {
            _state.value = SosState.Sending
            val success = repo.triggerSos(source)
            if (success) {
                _state.value = SosState.Sent
            } else {
                // Fallback to background WorkManager upload
                enqueueSos(context, source)
                _state.value = SosState.Failed
            }
        }
    }

    fun resetState() {
        _state.value = SosState.Idle
    }
}

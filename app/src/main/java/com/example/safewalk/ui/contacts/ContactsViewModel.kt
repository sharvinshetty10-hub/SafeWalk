package com.example.safewalk.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safewalk.data.local.ContactEntity
import com.example.safewalk.data.repository.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val repo: ContactRepository
) : ViewModel() {

    val contacts = repo.contacts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addContact(name: String, phone: String) = viewModelScope.launch {
        repo.addContact(name, phone)
    }

    fun deleteContact(contact: ContactEntity) = viewModelScope.launch {
        repo.deleteContact(contact)
    }
}

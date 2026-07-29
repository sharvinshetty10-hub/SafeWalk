package com.example.safewalk.data.repository

import com.example.safewalk.data.local.ContactDao
import com.example.safewalk.data.local.ContactEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ContactRepository @Inject constructor(
    private val dao: ContactDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    val contacts: Flow<List<ContactEntity>> = dao.getAll()

    suspend fun addContact(name: String, phone: String) {
        val uid = auth.currentUser?.uid ?: return
        val doc = firestore.collection("users").document(uid)
            .collection("contacts").document()
        
        doc.set(mapOf("name" to name, "phone" to phone)).await()
        
        dao.insert(
            ContactEntity(
                name = name,
                phone = phone,
                firestoreUid = doc.id
            )
        )
    }

    suspend fun deleteContact(contact: ContactEntity) {
        val uid = auth.currentUser?.uid ?: return
        if (contact.firestoreUid.isNotEmpty()) {
            try {
                firestore.collection("users").document(uid)
                    .collection("contacts").document(contact.firestoreUid)
                    .delete().await()
            } catch (e: Exception) {
                // Silently swallow Firestore failure or log it, but proceed with local deletion
            }
        }
        dao.delete(contact)
    }
}

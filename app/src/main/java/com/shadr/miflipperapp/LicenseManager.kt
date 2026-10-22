package com.shadr.miflipperapp

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class LicenseManager(private val firestore: FirebaseFirestore) {

    suspend fun grantMonthlyLicense(userId: String) {
        val userDoc = firestore.collection("licenses").document(userId).get().await()

        val currentExpiryDate = userDoc.getLong("expiryDate") ?: System.currentTimeMillis()

        val now = System.currentTimeMillis()

        val startDate = if (currentExpiryDate > now) currentExpiryDate else now

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startDate
        calendar.add(Calendar.DAY_OF_YEAR, 30)

        val newExpiryTime = calendar.timeInMillis

        val updates = hashMapOf<String, Any>(
            "isPremium" to true,
            "expiryDate" to newExpiryTime
        )

        firestore.collection("licenses").document(userId).update(updates).await()
    }
}
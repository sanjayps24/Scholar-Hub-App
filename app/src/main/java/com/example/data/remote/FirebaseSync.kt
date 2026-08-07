package com.example.data.remote

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirebaseSync {
    private const val TAG = "FirebaseSync"

    // Safe helper to check if Firebase is configured & active in this app build
    fun isFirebaseAvailable(context: Context): Boolean {
        return try {
            val app = FirebaseApp.getInstance()
            app != null
        } catch (e: Exception) {
            // Firebase app is not initialized because google-services.json is missing/invalid
            false
        }
    }

    fun getFirebaseAuth(context: Context): FirebaseAuth? {
        return if (isFirebaseAvailable(context)) {
            try {
                FirebaseAuth.getInstance()
            } catch (e: Exception) {
                null
            }
        } else null
    }

    fun getFirestore(context: Context): FirebaseFirestore? {
        return if (isFirebaseAvailable(context)) {
            try {
                FirebaseFirestore.getInstance()
            } catch (e: Exception) {
                null
            }
        } else null
    }

    fun getCurrentUser(context: Context): FirebaseUser? {
        return getFirebaseAuth(context)?.currentUser
    }

    // Standard Sign Out
    fun signOut(context: Context) {
        try {
            getFirebaseAuth(context)?.signOut()
        } catch (e: Exception) {
            Log.e(TAG, "Sign out failure", e)
        }
    }

    // Safe mock synchronization for student study notes
    suspend fun uploadNotesToCloud(context: Context, materialId: Long, title: String, content: String, summary: String): Boolean {
        val firestore = getFirestore(context) ?: return false
        val user = getCurrentUser(context) ?: return false
        
        return try {
            val docData = mapOf(
                "userId" to user.uid,
                "materialId" to materialId,
                "title" to title,
                "content" to content,
                "summary" to summary,
                "timestamp" to System.currentTimeMillis()
            )
            firestore.collection("study_materials")
                .document("material_${user.uid}_$materialId")
                .set(docData)
                .await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Cloud sync failed (will cache locally)", e)
            false
        }
    }
}

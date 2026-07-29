package com.example.safewalk.service

import android.content.Context
import androidx.work.*
import com.example.safewalk.data.repository.SosRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class SosWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            SosWorkerEntryPoint::class.java
        )
        val repo = entryPoint.sosRepository()
        
        val source = inputData.getString("source") ?: "workmanager"
        val success = repo.triggerSos(source)
        
        return if (success) {
            Result.success()
        } else {
            Result.retry()
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SosWorkerEntryPoint {
        fun sosRepository(): SosRepository
    }
}

fun enqueueSos(context: Context, source: String = "manual") {
    val data = workDataOf("source" to source)
    val request = OneTimeWorkRequestBuilder<SosWorker>()
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        )
        .setInputData(data)
        .build()
    WorkManager.getInstance(context).enqueue(request)
}

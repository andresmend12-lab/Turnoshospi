package com.example.turnoshospi.data.repository

import com.example.turnoshospi.data.firebase.FirebaseFlowHelpers
import com.example.turnoshospi.data.firebase.FirebaseRefs
import com.example.turnoshospi.domain.model.AppNotification
import com.example.turnoshospi.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class FirebaseNotificationRepository : NotificationRepository {

    override fun observeNotifications(userId: String): Flow<List<AppNotification>> {
        val query = FirebaseRefs.getRootRef().child("user_notifications").child(userId).limitToLast(20)
        return FirebaseFlowHelpers.observeList(query, AppNotification::class.java)
    }

    override suspend fun markAsRead(userId: String, notificationId: String) {
        FirebaseRefs.getRootRef().child("user_notifications").child(userId).child(notificationId)
            .child("read").setValue(true).await()
    }
}
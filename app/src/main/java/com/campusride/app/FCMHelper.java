package com.campusride.app;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

public class FCMHelper {

    private static FCMHelper instance;
    private FirebaseFirestore db;

    private FCMHelper() {
        db = FirebaseFirestore.getInstance();
    }

    public static FCMHelper getInstance() {
        if (instance == null) {
            instance = new FCMHelper();
        }
        return instance;
    }

    // ─── SAVE TOKEN ──────────────────────────────────────

    // Call this right after user logs in
    public void saveToken(String uid) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String token = task.getResult();
                        // Save token to user's Firestore document
                        db.collection("users").document(uid)
                                .update("fcmToken", token);
                    }
                });
    }

    // ─── DELETE TOKEN ─────────────────────────────────────

    // Call this when user logs out
    public void deleteToken(String uid) {
        db.collection("users").document(uid)
                .update("fcmToken", null);
    }
}
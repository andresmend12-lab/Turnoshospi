const functions = require("firebase-functions/v1");
const admin = require("firebase-admin");
admin.initializeApp();

exports.sendNotification = functions.database.ref('/user_notifications/{userId}/{notificationId}')
    .onCreate(async (snapshot, context) => {
        const notification = snapshot.val();
        const userId = context.params.userId;

        console.log('Nueva notificacion para:', userId);

        const userSnapshot = await admin.database().ref(`/users/${userId}/fcmToken`).once('value');
        const fcmToken = userSnapshot.val();

        if (!fcmToken) return console.log('Sin token para usuario:', userId);

        const payload = {
            notification: {
                title: "Turnoshospi",
                body: notification.message
            },
            data: {
                screen: notification.targetScreen || "MainMenu",
                click_action: "FLUTTER_NOTIFICATION_CLICK"
            }
        };

        return admin.messaging().sendToDevice(fcmToken, payload);
    });
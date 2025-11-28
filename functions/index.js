const functions = require("firebase-functions/v1");
const admin = require("firebase-admin");
admin.initializeApp();

exports.sendNotification = functions.database.ref('/user_notifications/{userId}/{notificationId}')
    .onCreate(async (snapshot, context) => {
        const notification = snapshot.val();
        const userId = context.params.userId;

        console.log('Nueva notificacion para:', userId);

        // 1. Obtener el token
        const userSnapshot = await admin.database().ref(`/users/${userId}/fcmToken`).once('value');
        const fcmToken = userSnapshot.val();

        if (!fcmToken) {
            console.log('Sin token para usuario:', userId);
            return null;
        }

        // 2. Construir el mensaje con la NUEVA ESTRUCTURA (API v1)
        // Ya no se usa "payload" genérico, sino bloques específicos (android, apns...)
        const message = {
            token: fcmToken, // El token va aquí dentro ahora
            notification: {
                title: "Turnoshospi",
                body: notification.message
            },
            // Configuración específica de Android
            android: {
                priority: "high", // Para despertar al móvil en Doze mode
                notification: {
                    channelId: "turnoshospi_sound_v2", // IMPORTANTE: camelCase (channelId, no android_channel_id)
                    sound: "default",
                    priority: "high", // Prioridad visual máxima
                    defaultSound: true
                }
            },
            // Datos personalizados
            data: {
                screen: notification.targetScreen || "MainMenu",
                targetId: notification.targetId || "",
                click_action: "FLUTTER_NOTIFICATION_CLICK"
            }
        };

        try {
            // 3. Usar el nuevo método .send()
            const response = await admin.messaging().send(message);
            console.log('Mensaje enviado con éxito:', response);
            return response;
        } catch (error) {
            console.error('Error enviando notificación:', error);
            return null;
        }
    });
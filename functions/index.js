const functions = require("firebase-functions/v1");
const admin = require("firebase-admin");
admin.initializeApp();

exports.sendNotification = functions.database.ref('/user_notifications/{userId}/{notificationId}')
    .onCreate(async (snapshot, context) => {
        const notification = snapshot.val();
        const userId = context.params.userId;

        console.log('Nueva notificacion para:', userId);

        // 1. Obtener el token del usuario
        const userSnapshot = await admin.database().ref(`/users/${userId}/fcmToken`).once('value');
        const fcmToken = userSnapshot.val();

        if (!fcmToken) {
            console.log('Sin token para usuario:', userId);
            return null;
        }

        // 2. Construir el mensaje con la NUEVA ESTRUCTURA (API HTTP v1)
        // Esta estructura es obligatoria para las versiones nuevas de firebase-admin
        const message = {
            token: fcmToken, // El token ahora va dentro del objeto mensaje
            notification: {
                title: "Turnoshospi",
                body: notification.message
            },
            // Configuración específica para Android
            android: {
                priority: "high", // Para despertar el móvil si está en reposo (Doze)
                notification: {
                    channelId: "turnoshospi_sound_v2", // El canal con sonido que configuramos en la App
                    sound: "default",
                    priority: "high", // Prioridad visual máxima
                    defaultSound: true,
                    icon: "ic_logo_hospi_round" // Asegúrate de que este recurso existe, si no, usa 'default'
                }
            },
            // Datos adicionales para la navegación
            data: {
                screen: notification.targetScreen || "MainMenu",
                targetId: notification.targetId || "",
                click_action: "FLUTTER_NOTIFICATION_CLICK"
            }
        };

        try {
            // 3. Usar el nuevo método .send() en lugar de sendToDevice()
            const response = await admin.messaging().send(message);
            console.log('Mensaje enviado con éxito:', response);
            return response;
        } catch (error) {
            console.error('Error enviando notificación:', error);
            return null;
        }
    });
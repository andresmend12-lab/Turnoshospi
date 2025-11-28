const functions = require("firebase-functions/v1");
const admin = require("firebase-admin");
admin.initializeApp();

// 1. Notificaciones Generales (Sistema existente)
exports.sendNotification = functions.database.ref('/user_notifications/{userId}/{notificationId}')
    .onCreate(async (snapshot, context) => {
        const notification = snapshot.val();
        const userId = context.params.userId;

        console.log('Nueva notificacion para:', userId);

        const userSnapshot = await admin.database().ref(`/users/${userId}/fcmToken`).once('value');
        const fcmToken = userSnapshot.val();

        if (!fcmToken) {
            console.log('Sin token para usuario:', userId);
            return null;
        }

        const message = {
            token: fcmToken,
            notification: {
                title: "Turnoshospi",
                body: notification.message
            },
            android: {
                priority: "high",
                notification: {
                    channelId: "turnoshospi_sound_v2",
                    sound: "default",
                    priority: "high",
                    defaultSound: true,
                    icon: "ic_logo_hospi_round"
                }
            },
            data: {
                screen: notification.targetScreen || "MainMenu",
                targetId: notification.targetId || "",
                click_action: "FLUTTER_NOTIFICATION_CLICK"
            }
        };

        try {
            const response = await admin.messaging().send(message);
            console.log('Mensaje enviado con éxito:', response);
            return response;
        } catch (error) {
            console.error('Error enviando notificación:', error);
            return null;
        }
    });

// 2. NUEVO: Notificaciones de Chat
exports.sendChatNotification = functions.database.ref('/direct_messages/{chatId}/{messageId}')
    .onCreate(async (snapshot, context) => {
        const messageData = snapshot.val();
        const chatId = context.params.chatId;
        const senderId = messageData.senderId;
        const receiverId = messageData.receiverId; // Aseguraremos que la app envíe esto
        const text = messageData.text;

        if (!receiverId) return console.log("No receiverId found");

        console.log(`Nuevo mensaje de ${senderId} para ${receiverId}`);

        // A. Incrementar contador de no leídos en la metadata del usuario receptor
        const chatMetaRef = admin.database().ref(`/user_direct_chats/${receiverId}/${chatId}`);

        await chatMetaRef.transaction((currentData) => {
            if (currentData) {
                return {
                    ...currentData,
                    unreadCount: (currentData.unreadCount || 0) + 1,
                    lastMessage: text,
                    timestamp: admin.database.ServerValue.TIMESTAMP
                };
            }
            return currentData; // Si no existe el chat en metadata, no lo creamos aquí (lo hace la app)
        });

        // B. Enviar Notificación Push
        const userSnapshot = await admin.database().ref(`/users/${receiverId}/fcmToken`).once('value');
        const fcmToken = userSnapshot.val();

        if (!fcmToken) return console.log('Sin token para receptor de chat:', receiverId);

        // Obtener nombre del remitente para la notificación
        const senderSnapshot = await admin.database().ref(`/users/${senderId}`).once('value');
        const senderName = senderSnapshot.val() ?
                           `${senderSnapshot.val().firstName} ${senderSnapshot.val().lastName}` :
                           "Compañero";

        const pushMessage = {
            token: fcmToken,
            notification: {
                title: senderName,
                body: text
            },
            android: {
                priority: "high",
                notification: {
                    channelId: "turnoshospi_sound_v2", // Usamos el mismo canal con sonido
                    sound: "default",
                    priority: "high",
                    defaultSound: true,
                    icon: "ic_logo_hospi_round",
                    tag: chatId // Agrupa notificaciones del mismo chat
                }
            },
            data: {
                screen: "DirectChat",
                chatId: chatId,
                otherUserId: senderId,
                otherUserName: senderName,
                click_action: "FLUTTER_NOTIFICATION_CLICK"
            }
        };

        try {
            return await admin.messaging().send(pushMessage);
        } catch (error) {
            console.error('Error enviando push de chat:', error);
            return null;
        }
    });
const functions = require("firebase-functions/v1");
const admin = require("firebase-admin");
admin.initializeApp();

// ==================================================================
// 1. Notificaciones Generales (Sistema existente activado por la App)
// ==================================================================
exports.sendNotification = functions.database.ref('/user_notifications/{userId}/{notificationId}')
    .onCreate(async (snapshot, context) => {
        const notification = snapshot.val();
        const userId = context.params.userId;

        const userSnapshot = await admin.database().ref(`/users/${userId}/fcmToken`).once('value');
        const fcmToken = userSnapshot.val();

        if (!fcmToken) return null;

        const message = {
            token: fcmToken,
            notification: {
                title: notification.title || "Turnoshospi",
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
                plantId: notification.targetId || "", // A veces targetId se usa como plantId
                argument: notification.argument || "",
                click_action: "FLUTTER_NOTIFICATION_CLICK"
            }
        };

        try {
            return await admin.messaging().send(message);
        } catch (error) {
            console.error('Error enviando notificación:', error);
            return null;
        }
    });

// ==================================================================
// 2. Notificaciones de Chat
// ==================================================================
exports.sendChatNotification = functions.database.ref('/plants/{plantId}/direct_chats/{chatId}/messages/{messageId}')
    .onCreate(async (snapshot, context) => {
        const messageData = snapshot.val();
        const plantId = context.params.plantId;
        const chatId = context.params.chatId;

        const senderId = messageData.senderId;
        const text = messageData.text;

        const ids = chatId.split("_");
        const receiverId = (ids[0] === senderId) ? ids[1] : ids[0];

        if (!receiverId) return null;

        // Actualizar metadatos del chat (no leídos, último mensaje)
        const chatMetaRef = admin.database().ref(`/user_direct_chats/${receiverId}/${chatId}`);
        await chatMetaRef.transaction((currentData) => {
            const count = (currentData && currentData.unreadCount) ? currentData.unreadCount + 1 : 1;
            return {
                ...currentData,
                unreadCount: count,
                lastMessage: text,
                timestamp: admin.database.ServerValue.TIMESTAMP,
                plantId: plantId,
                otherUserId: senderId
            };
        });

        // Enviar Push
        const userSnapshot = await admin.database().ref(`/users/${receiverId}/fcmToken`).once('value');
        const fcmToken = userSnapshot.val();
        if (!fcmToken) return null;

        const senderSnapshot = await admin.database().ref(`/users/${senderId}`).once('value');
        const senderUser = senderSnapshot.val();
        const senderName = senderUser ? `${senderUser.firstName} ${senderUser.lastName}` : "Compañero";

        const pushMessage = {
            token: fcmToken,
            notification: {
                title: senderName,
                body: text
            },
            android: {
                priority: "high",
                notification: {
                    channelId: "turnoshospi_sound_v2",
                    sound: "default",
                    icon: "ic_logo_hospi_round",
                    tag: chatId
                }
            },
            data: {
                screen: "DirectChat",
                chatId: chatId,
                plantId: plantId,
                otherUserId: senderId,
                otherUserName: senderName,
                click_action: "FLUTTER_NOTIFICATION_CLICK"
            }
        };

        return admin.messaging().send(pushMessage);
    });

// ==================================================================
// 4. Notificar al Supervisor (OPTIMIZADO CON ID DIRECTO)
// ==================================================================
exports.notifySupervisorOnPending = functions.database.ref('/plants/{plantId}/shift_requests/{requestId}')
    .onUpdate(async (change, context) => {
        const before = change.before.val();
        const after = change.after.val();
        const plantId = context.params.plantId;
        const requestId = context.params.requestId;

        // Detectar cambio a AWAITING_SUPERVISOR
        if (before.status !== 'AWAITING_SUPERVISOR' && after.status === 'AWAITING_SUPERVISOR') {
            const requesterName = after.requesterName || "Usuario";
            const targetName = after.targetUserName || "Compañero";
            const shiftDate = after.requesterShiftDate;
            const supervisorIds = after.supervisorIds; // <-- Leemos la lista que guardamos desde Android

            console.log(`Cambio pendiente: ${requesterName} <-> ${targetName}. IDs Supervisores:`, supervisorIds);

            // Función auxiliar para enviar a un ID
            const notifyUser = (userId) => {
                return admin.database().ref(`/users/${userId}/fcmToken`).once('value')
                    .then(tokenSnap => {
                        const fcmToken = tokenSnap.val();
                        if (!fcmToken) return null;

                        const message = {
                            token: fcmToken,
                            notification: {
                                title: "Solicitud Requiere Aprobación",
                                body: `${targetName} aceptó el cambio con ${requesterName} (${shiftDate}).`
                            },
                            android: {
                                priority: "high",
                                notification: {
                                    channelId: "turnoshospi_sound_v2",
                                    icon: "ic_logo_hospi_round"
                                }
                            },
                            data: {
                                screen: "ShiftChangeScreen",
                                plantId: plantId,
                                argument: requestId, // Pasamos el ID de la solicitud
                                click_action: "FLUTTER_NOTIFICATION_CLICK"
                            }
                        };
                        return admin.messaging().send(message);
                    })
                    .catch(e => console.error(`Error enviando a supervisor ${userId}:`, e));
            };

            const promises = [];

            // A. Usar la lista directa si existe (Método rápido)
            if (supervisorIds && Array.isArray(supervisorIds) && supervisorIds.length > 0) {
                // Guardar también en DB interna para cada supervisor
                for (const supId of supervisorIds) {
                    const notifRef = admin.database().ref(`/user_notifications/${supId}`).push();
                    promises.push(notifRef.set({
                        title: "Solicitud de Cambio",
                        message: `${targetName} y ${requesterName} solicitan aprobación para el ${shiftDate}.`,
                        timestamp: admin.database.ServerValue.TIMESTAMP,
                        read: false,
                        targetScreen: "ShiftChangeScreen",
                        targetId: plantId,
                        argument: requestId
                    }));
                    // Enviar Push
                    promises.push(notifyUser(supId));
                }
            }
            // B. Fallback (Método lento buscando en userPlants) por si acaso falla la lista
            else {
                const usersSnap = await admin.database().ref(`/plants/${plantId}/userPlants`).once('value');
                if (usersSnap.exists()) {
                    usersSnap.forEach((userChild) => {
                        const role = userChild.child("staffRole").val() || "";
                        const userId = userChild.key;
                        if (role.toLowerCase().includes("supervisor")) {
                            promises.push(notifyUser(userId));
                        }
                    });
                }
            }

            return Promise.all(promises);
        }
        return null;
    });

// ==================================================================
// 5. Notificar a Usuarios (Supervisor Aprueba o Rechaza)
// ==================================================================
exports.notifyUsersOnStatusChange = functions.database.ref('/plants/{plantId}/shift_requests/{requestId}')
    .onUpdate(async (change, context) => {
        const before = change.before.val();
        const after = change.after.val();
        const plantId = context.params.plantId;
        const requestId = context.params.requestId;

        const requesterId = after.requesterId;
        const targetUserId = after.targetUserId;
        const requesterName = after.requesterName;
        const targetName = after.targetUserName;
        const shiftDate = after.requesterShiftDate;

        const promises = [];

        // Función auxiliar que GUARDA en DB y ENVÍA Push
        const sendFullNotification = async (userId, title, body) => {
            if (!userId || userId.startsWith("UNREGISTERED")) return;

            // 1. Guardar en user_notifications (Para que salga en la lista de la app)
            const notifRef = admin.database().ref(`/user_notifications/${userId}`).push();
            await notifRef.set({
                title: title,
                message: body,
                timestamp: admin.database.ServerValue.TIMESTAMP,
                read: false,
                targetScreen: "ShiftChangeScreen",
                targetId: plantId,
                argument: requestId
            });

            // 2. Enviar Push Notification
            const tokenSnap = await admin.database().ref(`/users/${userId}/fcmToken`).once('value');
            const fcmToken = tokenSnap.val();
            if (!fcmToken) return;

            const message = {
                token: fcmToken,
                notification: { title, body },
                android: {
                    priority: "high",
                    notification: {
                        channelId: "turnoshospi_sound_v2",
                        icon: "ic_logo_hospi_round"
                    }
                },
                data: {
                    screen: "ShiftChangeScreen",
                    plantId: plantId,
                    argument: requestId,
                    click_action: "FLUTTER_NOTIFICATION_CLICK"
                }
            };
            return admin.messaging().send(message);
        };

        // APROBADO
        if (before.status !== 'APPROVED' && after.status === 'APPROVED') {
            if (requesterId) promises.push(sendFullNotification(requesterId, "¡Cambio Aprobado!", `El supervisor aprobó tu cambio con ${targetName} (${shiftDate}).`));
            if (targetUserId) promises.push(sendFullNotification(targetUserId, "¡Cambio Aprobado!", `El supervisor aprobó el cambio con ${requesterName} (${shiftDate}).`));
        }

        // RECHAZADO
        if (before.status !== 'REJECTED' && after.status === 'REJECTED') {
            if (requesterId) promises.push(sendFullNotification(requesterId, "Solicitud Rechazada", `La solicitud de cambio para el ${shiftDate} ha sido rechazada.`));
            if (targetUserId) promises.push(sendFullNotification(targetUserId, "Solicitud Rechazada", `La solicitud de cambio con ${requesterName} ha sido rechazada.`));
        }

        return Promise.all(promises);
    });

// ==================================================================
// 6. NUEVO: Notificar al Destinatario cuando recibe una PROPUESTA
// ==================================================================
exports.notifyTargetOnProposal = functions.database.ref('/plants/{plantId}/shift_requests/{requestId}')
    .onUpdate(async (change, context) => {
        const before = change.before.val();
        const after = change.after.val();
        const plantId = context.params.plantId;
        const requestId = context.params.requestId;

        // Detectar cambio a PENDING_PARTNER (Propuesta directa enviada)
        if (before.status !== 'PENDING_PARTNER' && after.status === 'PENDING_PARTNER') {
            const targetUserId = after.targetUserId;
            const requesterName = after.requesterName || "Un compañero";
            const requesterShiftDate = after.requesterShiftDate;
            const targetShiftDate = after.targetShiftDate;

            // Si no hay usuario destino o es no registrado, salimos
            if (!targetUserId || targetUserId.startsWith("UNREGISTERED")) return null;

            console.log(`Nueva propuesta directa enviada a: ${targetUserId}`);

            const title = "Te han propuesto un cambio";
            const body = `${requesterName} quiere cambiar su turno del ${requesterShiftDate} por tu turno del ${targetShiftDate}.`;

            // 1. Guardar en DB Interna (Para que salga en NotificationsScreen)
            const notifRef = admin.database().ref(`/user_notifications/${targetUserId}`).push();
            await notifRef.set({
                title: title,
                message: body,
                timestamp: admin.database.ServerValue.TIMESTAMP,
                read: false,
                targetScreen: "ShiftChangeScreen",
                targetId: plantId,
                argument: requestId
            });

            // 2. Enviar Push Notification
            const tokenSnap = await admin.database().ref(`/users/${targetUserId}/fcmToken`).once('value');
            const fcmToken = tokenSnap.val();

            if (!fcmToken) return null;

            const message = {
                token: fcmToken,
                notification: {
                    title: title,
                    body: body
                },
                android: {
                    priority: "high",
                    notification: {
                        channelId: "turnoshospi_sound_v2",
                        icon: "ic_logo_hospi_round"
                    }
                },
                data: {
                    screen: "ShiftChangeScreen",
                    plantId: plantId,
                    argument: requestId,
                    click_action: "FLUTTER_NOTIFICATION_CLICK"
                }
            };

            return admin.messaging().send(message);
        }
        return null;
    });
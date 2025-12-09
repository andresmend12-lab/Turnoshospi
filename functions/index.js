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

// 2. Notificaciones de Chat (Sistema existente)
exports.sendChatNotification = functions.database.ref('/plants/{plantId}/direct_chats/{chatId}/messages/{messageId}')
    .onCreate(async (snapshot, context) => {
        const messageData = snapshot.val();
        const plantId = context.params.plantId;
        const chatId = context.params.chatId;

        const senderId = messageData.senderId;
        const text = messageData.text;

        const ids = chatId.split("_");
        const receiverId = (ids[0] === senderId) ? ids[1] : ids[0];

        if (!receiverId) return console.log("No receiverId found in chat:", chatId);

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

        const userSnapshot = await admin.database().ref(`/users/${receiverId}/fcmToken`).once('value');
        const fcmToken = userSnapshot.val();

        if (!fcmToken) return console.log('Sin token para receptor de chat:', receiverId);

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
                    priority: "high",
                    defaultSound: true,
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

        try {
            return await admin.messaging().send(pushMessage);
        } catch (error) {
            console.error('Error enviando push de chat:', error);
            return null;
        }
    });

// 3. Notificaciones de Bolsa de Turnos (Filtrado por Rol)
exports.notifyNewShiftMarketplace = functions.database.ref('/plants/{plantId}/shift_requests/{requestId}')
    .onCreate(async (snapshot, context) => {
        const request = snapshot.val();
        const plantId = context.params.plantId;

        if (!request || request.status !== 'SEARCHING') return null;

        const requesterId = request.requesterId;
        const requesterName = request.requesterName || "Un compañero";
        const requesterRole = request.requesterRole || "";
        const shiftDate = request.requesterShiftDate;
        const shiftName = request.requesterShiftName;

        const areRolesCompatible = (roleA, roleB) => {
            if (!roleA || !roleB) return false;
            const rA = roleA.trim().toLowerCase();
            const rB = roleB.trim().toLowerCase();
            const isNurse = (r) => r.includes("enfermer");
            const isAux = (r) => r.includes("auxiliar");
            return (isNurse(rA) && isNurse(rB)) || (isAux(rA) && isAux(rB));
        };

        const usersInPlantSnap = await admin.database().ref(`/plants/${plantId}/userPlants`).once('value');
        if (!usersInPlantSnap.exists()) return null;

        const promises = [];

        usersInPlantSnap.forEach((userChild) => {
            const userId = userChild.key;
            const targetRole = userChild.child("staffRole").val() || "";

            if (userId !== requesterId && areRolesCompatible(requesterRole, targetRole)) {
                const p = admin.database().ref(`/users/${userId}/fcmToken`).once('value')
                    .then(tokenSnap => {
                        const fcmToken = tokenSnap.val();
                        if (!fcmToken) return null;

                        const message = {
                            token: fcmToken,
                            notification: {
                                title: "Nueva oferta en tu categoría",
                                body: `${requesterName} ofrece: ${shiftName} el ${shiftDate}`
                            },
                            android: {
                                priority: "high",
                                notification: {
                                    channelId: "turnoshospi_sound_v2",
                                    sound: "default",
                                    icon: "ic_logo_hospi_round"
                                }
                            },
                            data: {
                                screen: "ShiftMarketplaceScreen",
                                plantId: plantId,
                                click_action: "FLUTTER_NOTIFICATION_CLICK"
                            }
                        };
                        return admin.messaging().send(message);
                    })
                    .catch(error => console.error(`Error enviando a usuario ${userId}:`, error));
                promises.push(p);
            }
        });

        return Promise.all(promises);
    });

// 4. Notificar al Supervisor cuando se acepta un cambio (y pasa a pendiente)
exports.notifySupervisorOnPending = functions.database.ref('/plants/{plantId}/shift_requests/{requestId}')
    .onUpdate(async (change, context) => {
        const before = change.before.val();
        const after = change.after.val();
        const plantId = context.params.plantId;

        // Solo actuar si el estado cambia a AWAITING_SUPERVISOR
        if (before.status !== 'AWAITING_SUPERVISOR' && after.status === 'AWAITING_SUPERVISOR') {
            const requesterName = after.requesterName || "Usuario";
            const targetName = after.targetUserName || "Compañero";
            const shiftDate = after.requesterShiftDate;

            console.log(`Cambio pendiente de aprobación en planta ${plantId}: ${requesterName} <-> ${targetName}`);

            // 1. Obtener usuarios de la planta
            const usersSnap = await admin.database().ref(`/plants/${plantId}/userPlants`).once('value');
            if (!usersSnap.exists()) return null;

            const promises = [];

            // 2. Buscar Supervisores y notificar
            usersSnap.forEach((userChild) => {
                const role = userChild.child("staffRole").val() || "";
                const userId = userChild.key;

                if (role.toLowerCase().includes("supervisor")) {
                    const p = admin.database().ref(`/users/${userId}/fcmToken`).once('value')
                        .then(tokenSnap => {
                            const fcmToken = tokenSnap.val();
                            if (!fcmToken) return null;

                            const message = {
                                token: fcmToken,
                                notification: {
                                    title: "Solicitud de Cambio Pendiente",
                                    body: `${targetName} aceptó el cambio con ${requesterName} (${shiftDate}). Requiere aprobación.`
                                },
                                android: {
                                    priority: "high",
                                    notification: {
                                        channelId: "turnoshospi_sound_v2",
                                        sound: "default",
                                        icon: "ic_logo_hospi_round"
                                    }
                                },
                                data: {
                                    screen: "ShiftChangeScreen",
                                    plantId: plantId,
                                    click_action: "FLUTTER_NOTIFICATION_CLICK"
                                }
                            };
                            return admin.messaging().send(message);
                        });
                    promises.push(p);
                }
            });

            return Promise.all(promises);
        }
        return null;
    });

// 5. NUEVO: Notificar a los Usuarios cuando el Supervisor APRUEBA
exports.notifyUsersOnApproval = functions.database.ref('/plants/{plantId}/shift_requests/{requestId}')
    .onUpdate(async (change, context) => {
        const before = change.before.val();
        const after = change.after.val();
        const plantId = context.params.plantId;

        // Detectar cambio a APPROVED
        if (before.status !== 'APPROVED' && after.status === 'APPROVED') {
            const requesterId = after.requesterId;
            const targetUserId = after.targetUserId;

            const requesterName = after.requesterName || "Compañero";
            const targetName = after.targetUserName || "Compañero";
            const shiftDate = after.requesterShiftDate;

            console.log(`Cambio APROBADO por supervisor: ${requesterName} <-> ${targetName}`);

            const promises = [];

            // Función auxiliar para enviar
            const sendToUser = (userId, title, body) => {
                return admin.database().ref(`/users/${userId}/fcmToken`).once('value')
                    .then(tokenSnap => {
                        const fcmToken = tokenSnap.val();
                        if (!fcmToken) return null;

                        const message = {
                            token: fcmToken,
                            notification: { title: title, body: body },
                            android: {
                                priority: "high",
                                notification: {
                                    channelId: "turnoshospi_sound_v2",
                                    sound: "default",
                                    icon: "ic_logo_hospi_round"
                                }
                            },
                            data: {
                                screen: "ShiftChangeScreen",
                                plantId: plantId,
                                click_action: "FLUTTER_NOTIFICATION_CLICK"
                            }
                        };
                        return admin.messaging().send(message);
                    })
                    .catch(e => console.error(`Error enviando a ${userId}:`, e));
            };

            // Notificar al Solicitante Original
            if (requesterId) {
                promises.push(sendToUser(
                    requesterId,
                    "¡Cambio Aprobado!",
                    `El supervisor ha aprobado tu cambio con ${targetName} para el día ${shiftDate}.`
                ));
            }

            // Notificar al que cubre el turno (Target)
            if (targetUserId) {
                promises.push(sendToUser(
                    targetUserId,
                    "¡Cambio Aprobado!",
                    `El supervisor ha aprobado el cambio con ${requesterName} para el día ${shiftDate}.`
                ));
            }

            return Promise.all(promises);
        }
        return null;
    });
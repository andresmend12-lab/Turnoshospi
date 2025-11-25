import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();
const db = admin.firestore();

type SwapMode = "strict" | "flexible";

export const findMatchesForPlantMonth = functions.https.onCall(async (data, context) => {
  const { plantId, monthKey, mode } = data as { plantId: string; monthKey: string; mode: SwapMode };
  // TODO: add auth & role checks
  const preferencesSnap = await db.collection(`plants/${plantId}/preferences`).where("monthKey", "==", monthKey).get();
  const shiftsSnap = await db.collection(`plants/${plantId}/shifts`).where("monthKey", "==", monthKey).get();
  // TODO: apply ShiftRules server-side and build swapRequests
  const suggestions = preferencesSnap.docs.slice(0, 5).map((doc) => ({
    preferenceId: doc.id,
    candidateShift: shiftsSnap.docs.find(() => true)?.id,
    mode,
  }));
  return { suggestions };
});

export const applyApprovedSwapRequest = functions.https.onCall(async (data, context) => {
  const { swapRequestId, plantId } = data as { swapRequestId: string; plantId: string };
  // TODO: verify supervisor auth from context.token
  const swapRef = db.doc(`plants/${plantId}/swapRequests/${swapRequestId}`);
  const swapSnap = await swapRef.get();
  if (!swapSnap.exists) {
    throw new functions.https.HttpsError("not-found", "swapRequest not found");
  }
  const swap = swapSnap.data()!;
  // TODO: reload affected shifts and run ShiftRules
  await swapRef.update({ status: "approved", updatedAt: admin.firestore.FieldValue.serverTimestamp() });
  await db.collection(`plants/${plantId}/history`).add({
    swapRequestId,
    description: "Cambio aplicado por función",
    timestamp: admin.firestore.FieldValue.serverTimestamp(),
  });
  // TODO: fan-out notifications via FCM
  return { ok: true };
});

export const suggestedSwapsForSupervisor = functions.https.onCall(async (data) => {
  const { plantId, monthKey } = data as { plantId: string; monthKey: string };
  const swapSnap = await db.collection(`plants/${plantId}/swapRequests`).where("status", "==", "pending_supervisor").get();
  const recommendations = swapSnap.docs.map((doc) => ({ id: doc.id, summary: doc.data().type }));
  return { recommendations, monthKey };
});

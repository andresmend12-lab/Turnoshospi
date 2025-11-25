import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

// Optional trigger that prepares metadata after a plant is created.
// TODO: wire into Firestore if automatic suggestions are required.
export const onPlantCreated = functions.firestore
  .document("plants/{plantId}")
  .onCreate(async (snapshot, context) => {
    const plantId = context.params.plantId;
    const plantData = snapshot.data();

    // TODO: Implement logic to generate automatic shift suggestions or
    // other metadata such as default calendars.
    console.log("Plant created", plantId, plantData?.name);

    return admin.firestore().collection("plantMetadata").doc(plantId).set({
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      status: "pending-automation",
    });
  });

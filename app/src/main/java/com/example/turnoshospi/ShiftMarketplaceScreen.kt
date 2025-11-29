package com.example.turnoshospi

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftMarketplaceScreen(
    plantId: String,
    currentUserId: String,
    currentUserName: String,
    onBack: () -> Unit,
    onSaveNotification: (String, String, String, String, String?, (Boolean) -> Unit) -> Unit
) {
    val database = FirebaseDatabase.getInstance("https://turnoshospi-f4870-default-rtdb.firebaseio.com/")
    val context = LocalContext.current

    // CORRECCIÓN: Usar 'val' y '=' en lugar de 'var' y 'by' para mutableStateMapOf
    val scoresMap = remember { mutableStateMapOf<String, Int>() }

    // Para estados simples como Int sí se usa 'by'
    var marketplaceRequests by remember { mutableStateOf<List<ShiftChangeRequest>>(emptyList()) }
    var myScore by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(plantId) {
        // 1. Cargar TODAS las solicitudes activas (SEARCHING)
        database.getReference("plants/$plantId/shift_requests")
            .orderByChild("status")
            .equalTo("SEARCHING")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<ShiftChangeRequest>()
                    snapshot.children.forEach { child ->
                        val req = child.getValue(ShiftChangeRequest::class.java)
                        // Filtramos: Mostramos todas las que NO sean mías
                        if (req != null && req.requesterId != currentUserId) {
                            list.add(req)
                        }
                    }
                    marketplaceRequests = list
                    isLoading = false
                }
                override fun onCancelled(error: DatabaseError) { isLoading = false }
            })

        // 2. Cargar Puntuaciones (Favores)
        database.getReference("plants/$plantId/scores")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    scoresMap.clear()
                    snapshot.children.forEach { child ->
                        val score = child.getValue(Int::class.java) ?: 0
                        // Asegúrate de que la clave no sea nula
                        child.key?.let { key ->
                            scoresMap[key] = score
                        }
                    }
                    myScore = scoresMap[currentUserId] ?: 0
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Bolsa de Turnos", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // --- TARJETA DE PUNTUACIÓN ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF54C7EC))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Mi Balance de Favores", color = Color.Gray, fontSize = 12.sp)
                        Text(
                            text = if (myScore > 0) "+$myScore" else "$myScore",
                            color = if (myScore >= 0) Color(0xFF4CAF50) else Color(0xFFE91E63),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(Icons.Default.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(32.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Turnos Disponibles para Cubrir", color = Color.White, fontWeight = FontWeight.Bold)
            Text("Acepta un turno para ganar 1 punto.", color = Color.Gray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                CircularProgressIndicator(color = Color(0xFF54C7EC), modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (marketplaceRequests.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay turnos en la bolsa actualmente.", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(marketplaceRequests) { req ->
                        MarketplaceItem(
                            req = req,
                            requesterScore = scoresMap[req.requesterId] ?: 0,
                            onAccept = {
                                acceptCoverage(
                                    database, plantId, req, currentUserId, currentUserName,
                                    onSuccess = {
                                        onSaveNotification(
                                            req.requesterId,
                                            "SHIFT_COVERED",
                                            "$currentUserName ha aceptado cubrir tu turno del ${req.requesterShiftDate}. ¡Se ha actualizado tu balance!",
                                            "ShiftMarketplaceScreen",
                                            req.id,
                                            {}
                                        )
                                        Toast.makeText(context, "¡Turno aceptado! Has ganado 1 punto.", Toast.LENGTH_LONG).show()
                                    },
                                    onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MarketplaceItem(
    req: ShiftChangeRequest,
    requesterScore: Int,
    onAccept: () -> Unit
) {
    val scoreColor = if (requesterScore >= 0) Color(0xFF4CAF50) else Color(0xFFE91E63)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0x11FFFFFF)),
        border = BorderStroke(1.dp, Color(0x22FFFFFF))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Solicita: ${req.requesterName}", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Su Balance: $requesterScore", color = scoreColor, fontSize = 12.sp)
                }
                Badge(containerColor = getShiftColor(req.requesterShiftName)) {
                    Text(req.requesterShiftName, color = Color.Black)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Fecha: ${req.requesterShiftDate}", color = Color(0xFF54C7EC), fontSize = 18.sp, fontWeight = FontWeight.Bold)

            // Si tiene fechas preferidas, las mostramos como info
            if (req.offeredDates.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Buscaba cambio por: ${req.offeredDates.joinToString(", ")}", color = Color.Gray, fontSize = 11.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAccept,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50), contentColor = Color.White)
            ) {
                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("CUBRIR TURNO (+1 Pto)")
            }
        }
    }
}

// LÓGICA DE TRANSACCIÓN: Reemplazar usuario en turno + Ajustar Puntos + Cerrar Solicitud
fun acceptCoverage(
    database: FirebaseDatabase,
    plantId: String,
    req: ShiftChangeRequest,
    covererId: String,
    covererName: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val turnosRef = database.reference.child("plants/$plantId/turnos/turnos-${req.requesterShiftDate}")

    turnosRef.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (!snapshot.exists()) { onError("Error: El día ya no existe en el calendario."); return }

            val shiftRef = snapshot.child(req.requesterShiftName)
            if (!shiftRef.exists()) { onError("Error: El turno ya no existe."); return }

            // Buscar dónde está el usuario original para reemplazarlo
            var pathFound: String? = null
            var slotKey: String? = null
            var fieldToUpdate: String? = null // "primary" o "secondary"

            fun findInNode(nodeName: String) {
                shiftRef.child(nodeName).children.forEach { slot ->
                    if (slot.child("primary").value == req.requesterName) {
                        pathFound = nodeName; slotKey = slot.key; fieldToUpdate = "primary"
                    } else if (slot.child("secondary").value == req.requesterName) {
                        pathFound = nodeName; slotKey = slot.key; fieldToUpdate = "secondary"
                    }
                }
            }

            findInNode("nurses")
            if (pathFound == null) findInNode("auxiliaries")

            if (pathFound != null && slotKey != null && fieldToUpdate != null) {
                val updates = mutableMapOf<String, Any?>()

                // 1. Cambiar nombre en el turno (El Coverer reemplaza al Requester)
                updates["plants/$plantId/turnos/turnos-${req.requesterShiftDate}/${req.requesterShiftName}/$pathFound/$slotKey/$fieldToUpdate"] = covererName

                // 2. Cerrar la solicitud (Se marca como APROBADA/CUBIERTA)
                updates["plants/$plantId/shift_requests/${req.id}/status"] = RequestStatus.APPROVED
                updates["plants/$plantId/shift_requests/${req.id}/targetUserId"] = covererId

                // 3. Transacción de Puntos
                database.reference.child("plants/$plantId/scores").runTransaction(object : com.google.firebase.database.Transaction.Handler {
                    override fun doTransaction(currentData: com.google.firebase.database.MutableData): com.google.firebase.database.Transaction.Result {
                        val requesterScore = currentData.child(req.requesterId).getValue(Int::class.java) ?: 0
                        val covererScore = currentData.child(covererId).getValue(Int::class.java) ?: 0

                        currentData.child(req.requesterId).value = requesterScore - 1
                        currentData.child(covererId).value = covererScore + 1
                        return com.google.firebase.database.Transaction.success(currentData)
                    }

                    override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                        if (committed) {
                            // Si los puntos se guardaron, ejecutamos los cambios en Turnos y Requests
                            database.reference.updateChildren(updates).addOnSuccessListener { onSuccess() }
                        } else {
                            onError("Error actualizando la puntuación.")
                        }
                    }
                })

            } else {
                onError("No se encontró al usuario original en el turno. Quizás ya fue modificado.")
            }
        }
        override fun onCancelled(error: DatabaseError) { onError("Error de conexión.") }
    })
}
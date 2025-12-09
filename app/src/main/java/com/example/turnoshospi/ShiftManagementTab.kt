package com.example.turnoshospi

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.turnoshospi.ui.theme.ShiftColors
import java.time.LocalDate

@Composable
fun ShiftManagementTab(
    currentUserId: String,
    currentUserName: String,
    isSupervisor: Boolean,
    userSchedules: Map<String, Map<LocalDate, String>>,
    mySchedule: Map<LocalDate, String>,
    openRequests: List<ShiftChangeRequest>,
    peerPendingRequests: List<ShiftChangeRequest>,
    historyRequests: List<ShiftChangeRequest>,
    supervisorRequests: List<ShiftChangeRequest>,
    shiftColors: ShiftColors,
    onDeleteRequest: (ShiftChangeRequest) -> Unit,
    onAcceptByPartner: (ShiftChangeRequest) -> Unit,
    onRejectByPartner: (ShiftChangeRequest) -> Unit,
    onApproveBySupervisor: (ShiftChangeRequest) -> Unit,
    onRejectBySupervisor: (ShiftChangeRequest) -> Unit,
    staffIdToUserId: Map<String, String>,
    plantStaffMap: Map<String, RegisteredUser>
) {
    var previewReq by remember { mutableStateOf<ShiftChangeRequest?>(null) }
    var isPreviewSupervisorMode by remember { mutableStateOf(false) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(24.dp), contentPadding = PaddingValues(16.dp)) {

        // 1. SUPERVISOR
        if (isSupervisor && supervisorRequests.isNotEmpty()) {
            item { Text(stringResource(R.string.header_supervisor_pending), color = Color(0xFFE91E63), fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            items(supervisorRequests) { req ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0x22E91E63))) {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.dialog_simulation_title), color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("${req.requesterName} (${req.requesterShiftDate})", color = Color(0xCCFFFFFF), fontSize = 13.sp)
                        Icon(Icons.Default.SwapHoriz, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        // Si viene de Marketplace, targetShiftDate puede ser null, ponemos "CUBRIR"
                        Text("${req.targetUserName} (${req.targetShiftDate ?: "CUBRIR"})", color = Color(0xCCFFFFFF), fontSize = 13.sp)

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = {
                                previewReq = req
                                isPreviewSupervisorMode = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth().height(35.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Visibility, null, modifier = Modifier.size(16.dp), tint = Color(0xFF54C7EC))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.btn_view_simulation), color = Color(0xFF54C7EC), fontSize = 12.sp)
                        }

                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { onRejectBySupervisor(req) }) { Text(stringResource(R.string.btn_reject), color = Color(0xFFFFB4AB)) }
                            Button(onClick = { onApproveBySupervisor(req) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text(stringResource(R.string.btn_approve)) }
                        }
                    }
                }
            }
        }

        // 2. EN PROCESO
        if (peerPendingRequests.isNotEmpty()) {
            item { Text(stringResource(R.string.header_in_process), color = Color(0xFFFFA000), fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            items(peerPendingRequests) { req ->
                val amITarget = req.targetUserId == currentUserId || req.targetUserName.equals(currentUserName, ignoreCase = true)
                val isAwaitingSupervisor = req.status == RequestStatus.AWAITING_SUPERVISOR

                val containerColor = when {
                    isAwaitingSupervisor -> Color(0x22E91E63)
                    amITarget -> Color(0x224CAF50)
                    else -> Color(0x22FFA000)
                }

                Card(colors = CardDefaults.cardColors(containerColor = containerColor)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isAwaitingSupervisor) {
                                Icon(Icons.Default.Info, null, tint = Color(0xFFE91E63), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.status_pending_supervisor), color = Color(0xFFE91E63), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            } else if (amITarget) {
                                Icon(Icons.Default.Info, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.status_proposal_received), color = Color(0xFF4CAF50), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.Refresh, null, tint = Color(0xFFFFA000), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                val targetName = req.targetUserName?.uppercase() ?: "?"
                                Text(stringResource(R.string.status_waiting_partner, targetName), color = Color(0xFFFFA000), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                        Spacer(Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.label_origin), color = Color.Gray, fontSize = 10.sp)
                                Text(req.requesterName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(req.requesterShiftDate, color = Color(0xCCFFFFFF), fontSize = 12.sp)
                            }
                            Icon(Icons.Default.SwapHoriz, null, tint = Color.White, modifier = Modifier.padding(horizontal = 8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.label_destination), color = Color.Gray, fontSize = 10.sp)
                                Text(req.targetUserName ?: "?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(req.targetShiftDate ?: "?", color = Color(0xCCFFFFFF), fontSize = 12.sp)
                            }
                        }

                        if (amITarget && !isAwaitingSupervisor) {
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    previewReq = req
                                    isPreviewSupervisorMode = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                modifier = Modifier.fillMaxWidth().height(35.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.Visibility, null, modifier = Modifier.size(16.dp), tint = Color(0xFF54C7EC))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.btn_view_simulation), color = Color(0xFF54C7EC), fontSize = 12.sp)
                            }
                        }

                        if (isAwaitingSupervisor) {
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = { onDeleteRequest(req) }, modifier = Modifier.align(Alignment.End)) {
                                Text(stringResource(R.string.btn_cancel), color = Color(0xFFFFB4AB), fontSize = 12.sp)
                            }
                        } else if (amITarget) {
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { onRejectByPartner(req) },
                                    border = BorderStroke(1.dp, Color(0xFFFFB4AB)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFB4AB))
                                ) {
                                    Text(stringResource(R.string.btn_reject))
                                }
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = { onAcceptByPartner(req) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF54C7EC), contentColor = Color.Black)
                                ) {
                                    Text(stringResource(R.string.btn_accept))
                                }
                            }
                        } else {
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = { onRejectByPartner(req) }, modifier = Modifier.align(Alignment.End)) {
                                Text(stringResource(R.string.btn_cancel), color = Color(0xFFFFB4AB), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // 3. MIS BÚSQUEDAS
        if (openRequests.isNotEmpty()) {
            item { Text(stringResource(R.string.header_my_searches), color = Color(0xFF54C7EC), fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            items(openRequests) { req ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0x1154C7EC)), border = BorderStroke(1.dp, Color(0x3354C7EC))) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, null, tint = Color(0xFF54C7EC), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.label_searching_candidate), color = Color(0xFF54C7EC), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { onDeleteRequest(req) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, null, tint = Color(0xFFFFB4AB), modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.label_offering, "${req.requesterShiftDate} (${req.requesterShiftName})"), color = Color.White, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.label_suggestion_hint), color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }

        // 4. HISTORIAL
        if (historyRequests.isNotEmpty()) {
            item { Text(stringResource(R.string.header_history), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            items(historyRequests) { req ->
                val statusText = stringResource(R.string.status_approved)
                val statusColor = Color(0xFF4CAF50)

                Card(colors = CardDefaults.cardColors(containerColor = Color(0x11FFFFFF))) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).background(statusColor, CircleShape))
                            Spacer(Modifier.width(8.dp))
                            Text(statusText, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))

                        Text("${req.requesterName} ↔ ${req.targetUserName}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("${req.requesterShiftDate} / ${req.targetShiftDate}", color = Color.Gray, fontSize = 12.sp)

                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { onDeleteRequest(req) }, modifier = Modifier.align(Alignment.End)) {
                            Text(stringResource(R.string.btn_delete_history), color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                }
            }
        } else if (openRequests.isEmpty() && peerPendingRequests.isEmpty()) {
            item {
                Text(stringResource(R.string.msg_no_recent_activity), color = Color.Gray, modifier = Modifier.fillMaxWidth().padding(32.dp), textAlign = TextAlign.Center)
            }
        }
    }

    if (previewReq != null) {
        val req = previewReq!!
        val dateReq = LocalDate.parse(req.requesterShiftDate)
        val dateTarget = if(req.targetShiftDate != null) LocalDate.parse(req.targetShiftDate) else null
        val labelMe = stringResource(R.string.label_me)

        val (row1Schedule, row1Name, row1DateOut, row1DateIn, row1ShiftIn) = if (isPreviewSupervisorMode) {
            Quintuple(
                userSchedules[req.requesterId] ?: emptyMap(),
                req.requesterName,
                dateReq, dateTarget, req.targetShiftName
            )
        } else {
            Quintuple(
                mySchedule,
                labelMe,
                dateTarget, dateReq, req.requesterShiftName
            )
        }

        val (row2Schedule, row2Name, row2DateOut, row2DateIn, row2ShiftIn) = if (isPreviewSupervisorMode) {
            Quintuple(
                userSchedules[req.targetUserId] ?: emptyMap(),
                req.targetUserName ?: "?",
                dateTarget, dateReq, req.requesterShiftName
            )
        } else {
            Quintuple(
                userSchedules[req.requesterId] ?: emptyMap(),
                req.requesterName,
                dateReq, dateTarget, req.targetShiftName
            )
        }

        SchedulePreviewDialog(
            onDismiss = { previewReq = null },
            row1Schedule = row1Schedule,
            row1Name = row1Name,
            row1DateToRemove = row1DateOut,
            row1DateToAdd = row1DateIn,
            row1ShiftToAdd = row1ShiftIn,
            row2Schedule = row2Schedule,
            row2Name = row2Name,
            row2DateToRemove = row2DateOut,
            row2DateToAdd = row2DateIn,
            row2ShiftToAdd = row2ShiftIn,
            shiftColors = shiftColors
        )
    }
}

@Composable
fun MyRequestsForSuggestionsTab(
    myRequests: List<ShiftChangeRequest>,
    onSeeCandidates: (ShiftChangeRequest) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text(stringResource(R.string.header_search_candidates), color = Color(0xFF54C7EC), fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            Text(stringResource(R.string.desc_search_candidates), color = Color.Gray, fontSize = 12.sp)
        }

        if (myRequests.isEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(top = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Info, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Text(stringResource(R.string.msg_no_active_offers), color = Color.Gray)
                    Text(stringResource(R.string.desc_create_offer), color = Color.Gray, fontSize = 12.sp)
                }
            }
        }

        items(myRequests) { req ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x2254C7EC)),
                border = BorderStroke(1.dp, Color(0x4454C7EC))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.label_offering_shift), color = Color(0xCCFFFFFF), fontSize = 12.sp)
                            Text("${req.requesterShiftDate} (${req.requesterShiftName})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Badge(containerColor = Color(0xFF54C7EC), contentColor = Color.Black) { Text(stringResource(R.string.badge_active)) }
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { onSeeCandidates(req) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF54C7EC), contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_see_candidates))
                    }
                }
            }
        }
    }
}
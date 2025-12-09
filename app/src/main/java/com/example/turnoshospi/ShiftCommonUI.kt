package com.example.turnoshospi

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.turnoshospi.ui.theme.ShiftColors

@Composable
fun PlantShiftCard(
    shift: PlantShift,
    shiftColors: ShiftColors,
    onAction: () -> Unit,
    onPreview: () -> Unit
) {
    val shiftColor = getShiftColorDynamic(shift.shiftName, false, shiftColors)
    val initials = shift.userName.split(" ").take(2).mapNotNull { it.firstOrNull()?.toString() }.joinToString("").uppercase()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(modifier = Modifier.fillMaxHeight().width(6.dp).background(shiftColor))
            Column(modifier = Modifier.padding(8.dp).fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(36.dp).background(Color(0xFF334155), CircleShape), contentAlignment = Alignment.Center) {
                        Text(text = initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = shift.userName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(text = shift.userRole, color = Color(0xFF64748B), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Schedule, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "${shift.date} • ${shift.shiftName}", color = Color(0xFF94A3B8), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onPreview, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF54C7EC)), border = BorderStroke(1.dp, Color(0xFF54C7EC)), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp), modifier = Modifier.height(28.dp)) {
                            Text(stringResource(R.string.btn_preview), fontSize = 11.sp)
                        }
                        Button(onClick = onAction, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF54C7EC)), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp), modifier = Modifier.height(28.dp)) {
                            Text(stringResource(R.string.btn_choose), color = Color.Black, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
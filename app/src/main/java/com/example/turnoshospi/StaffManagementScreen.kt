package com.example.turnoshospi

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffManagementScreen(
    plant: Plant?,
    onBack: () -> Unit,
    onAddStaff: (String, RegisteredUser, (Boolean) -> Unit) -> Unit,
    onEditStaff: (String, RegisteredUser, (Boolean) -> Unit) -> Unit,
    onDeleteStaff: (String, String, (Boolean) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val database = remember { FirebaseDatabase.getInstance("https://turnoshospi-f4870-default-rtdb.firebaseio.com/") }

    val defaultRole = stringResource(id = R.string.role_nurse_generic)
    var staff by remember(plant?.id) {
        mutableStateOf(
            plant?.personal_de_planta
                ?.map { (id, member) -> if (member.id.isBlank()) member.copy(id = id) else member }
                ?.toList()
                ?: emptyList()
        )
    }

    val linkedStaffToUser = remember { mutableStateMapOf<String, String>() }

    DisposableEffect(plant?.id) {
        if (plant?.id != null) {
            val ref = database.reference.child("plants").child(plant.id).child("userPlants")
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    linkedStaffToUser.clear()
                    for (child in snapshot.children) {
                        val staffId = child.child("staffId").getValue(String::class.java) ?: continue
                        val userId = child.key ?: continue
                        linkedStaffToUser[staffId] = userId
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            }
            ref.addValueEventListener(listener)
            onDispose { ref.removeEventListener(listener) }
        } else {
            linkedStaffToUser.clear()
            onDispose { }
        }
    }

    var editorVisible by remember { mutableStateOf(false) }
    var editorId by remember { mutableStateOf<String?>(null) }
    var editorName by remember { mutableStateOf("") }
    var editorRole by remember { mutableStateOf(defaultRole) }
    var isSaving by remember { mutableStateOf(false) }
    var editorError by remember { mutableStateOf<String?>(null) }
    var memberToDelete by remember { mutableStateOf<RegisteredUser?>(null) }

    LaunchedEffect(plant?.personal_de_planta) {
        staff = plant?.personal_de_planta
            ?.map { (id, member) -> if (member.id.isBlank()) member.copy(id = id) else member }
            ?.toList()
            ?: emptyList()
    }

    fun resetEditor(member: RegisteredUser?) {
        editorId = member?.id
        editorName = member?.name.orEmpty()
        editorRole = member?.role ?: defaultRole
        editorError = null
        editorVisible = true
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.plant_manage_staff_option), color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.desc_back), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            if (plant != null) {
                FloatingActionButton(
                    onClick = { resetEditor(null) },
                    containerColor = Color(0xFF54C7EC),
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.plant_add_staff_option))
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Transparent)
        ) {
            if (plant == null) {
                StaffInfoMessage(message = stringResource(id = R.string.error_plant_not_loaded))
            } else {
                val sortedStaff = staff.sortedBy { it.name.lowercase() }
                if (sortedStaff.isEmpty()) {
                    StaffInfoMessage(message = stringResource(id = R.string.staff_list_dialog_empty))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 80.dp)
                    ) {
                        items(sortedStaff, key = { it.id }) { member ->
                            val linkedUserId = linkedStaffToUser[member.id]
                            StaffRow(
                                member = member,
                                linkedUserId = linkedUserId,
                                onEdit = { resetEditor(member) },
                                onDelete = { memberToDelete = member }
                            )
                        }
                    }
                }
            }
        }
    }

    if (editorVisible && plant != null) {
        StaffEditorDialog(
            staffName = editorName,
            onStaffNameChange = { editorName = it },
            staffRole = editorRole,
            onStaffRoleChange = { editorRole = it },
            isSaving = isSaving,
            errorMessage = editorError,
            title = if (editorId == null) stringResource(id = R.string.staff_dialog_title) else stringResource(id = R.string.edit_staff_member),
            confirmButtonText = if (editorId == null) stringResource(id = R.string.staff_dialog_save_action) else stringResource(id = R.string.register_button),
            onDismiss = { editorVisible = false; editorError = null },
            onConfirm = {
                if (editorName.isBlank()) {
                    editorError = context.getString(R.string.staff_dialog_error)
                    return@StaffEditorDialog
                }
                val plantId = plant.id
                if (plantId.isBlank()) {
                    editorError = context.getString(R.string.error_plant_not_loaded)
                    return@StaffEditorDialog
                }
                isSaving = true
                val newStaff = RegisteredUser(editorId ?: UUID.randomUUID().toString(), editorName, editorRole, "", "plant_staff")
                val callback: (Boolean) -> Unit = { success ->
                    isSaving = false
                    if (success) {
                        staff = staff.map { if (it.id == newStaff.id) newStaff else it } +
                                if (staff.none { it.id == newStaff.id }) listOf(newStaff) else emptyList()
                        editorVisible = false
                        editorError = null
                    } else {
                        editorError = context.getString(R.string.staff_dialog_save_error)
                    }
                }
                if (editorId == null) onAddStaff(plantId, newStaff, callback) else onEditStaff(plantId, newStaff, callback)
            }
        )
    }

    if (memberToDelete != null && plant != null) {
        val target = memberToDelete!!
        AlertDialog(
            onDismissRequest = { memberToDelete = null },
            title = { Text(stringResource(id = R.string.staff_delete_title)) },
            text = { Text(stringResource(id = R.string.staff_delete_message, target.name)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteStaff(plant.id, target.id) { success ->
                            if (success) {
                                staff = staff.filterNot { it.id == target.id }
                                memberToDelete = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(id = R.string.btn_delete_confirm)) }
            },
            dismissButton = { TextButton(onClick = { memberToDelete = null }) { Text(stringResource(id = R.string.cancel_label)) } },
            containerColor = Color(0xEE0B1021),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }
}

@Composable
private fun StaffRow(
    member: RegisteredUser,
    linkedUserId: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x22000000)),
        border = BorderStroke(1.dp, Color(0x22FFFFFF))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    Column {
                        Text(member.name.ifBlank { member.email.ifBlank { member.role } }, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(member.role, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF54C7EC)) }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF5350)) }
                }
            }
            val linkedText = if (linkedUserId != null) stringResource(id = R.string.staff_linked_simple) else stringResource(id = R.string.staff_not_linked)
            Text(linkedText, color = if (linkedUserId != null) Color(0xFF54C7EC) else Color.Gray, style = MaterialTheme.typography.bodySmall)
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
        }
    }
}

@Composable
private fun StaffEditorDialog(
    staffName: String,
    onStaffNameChange: (String) -> Unit,
    staffRole: String,
    onStaffRoleChange: (String) -> Unit,
    isSaving: Boolean,
    errorMessage: String?,
    title: String,
    confirmButtonText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val roles = remember {
        mutableStateListOf(
            R.string.role_nurse_generic,
            R.string.role_aux_generic
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
            ) { Text(confirmButtonText, color = Color.White) }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isSaving) { Text(stringResource(id = R.string.cancel_label)) } },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = staffName,
                    onValueChange = onStaffNameChange,
                    label = { Text(stringResource(id = R.string.staff_dialog_name_label)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF54C7EC),
                        unfocusedBorderColor = Color(0x66FFFFFF)
                    )
                )
                Text(stringResource(id = R.string.staff_dialog_role_label), color = Color.White)
                roles.forEach { roleRes ->
                    val label = stringResource(id = roleRes)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = staffRole == label,
                            onClick = { onStaffRoleChange(label) },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF54C7EC), unselectedColor = Color.White)
                        )
                        Text(label, color = Color.White)
                    }
                }
                if (errorMessage != null) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        containerColor = Color(0xEE0B1021),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

@Composable
private fun StaffInfoMessage(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x22000000)),
        border = BorderStroke(1.dp, Color(0x22FFFFFF))
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(text = message, color = Color.White, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

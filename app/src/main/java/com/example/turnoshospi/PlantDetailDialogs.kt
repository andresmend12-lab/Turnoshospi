package com.example.turnoshospi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@Composable
fun AddStaffDialog(
    staffName: String,
    onStaffNameChange: (String) -> Unit,
    staffRole: String,
    onStaffRoleChange: (String) -> Unit,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.staff_dialog_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = staffName,
                    onValueChange = onStaffNameChange,
                    label = { Text(stringResource(id = R.string.staff_dialog_name_label)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = staffRole,
                    onValueChange = onStaffRoleChange,
                    label = { Text(stringResource(id = R.string.staff_dialog_role_label)) },
                    singleLine = true
                )
                if (errorMessage != null) {
                    Text(text = errorMessage, color = Color.Red)
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !isSaving) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(id = R.string.save))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

@Composable
fun StaffListDialog(
    plantName: String,
    staff: Collection<RegisteredUser>,
    isSupervisor: Boolean,
    onDismiss: () -> Unit,
    onSaveEdit: (RegisteredUser, (Boolean) -> Unit) -> Unit
) {
    val staffList = remember(staff) { staff.toList() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.plant_staff_list_option, plantName)) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(staffList) { member ->
                    StaffListRow(member = member, isSupervisor = isSupervisor, onSaveEdit = onSaveEdit)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.btn_close)) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StaffListRow(
    member: RegisteredUser,
    isSupervisor: Boolean,
    onSaveEdit: (RegisteredUser, (Boolean) -> Unit) -> Unit
) {
    val nameState = remember { mutableStateOf(member.name) }
    val roleState = remember { mutableStateOf(member.role) }
    Column(
        modifier = Modifier.fillMaxWidth().background(Color(0x11FFFFFF)).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        OutlinedTextField(
            value = nameState.value,
            onValueChange = { nameState.value = it },
            label = { Text(stringResource(id = R.string.staff_dialog_name_label)) },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = roleState.value,
            onValueChange = { roleState.value = it },
            label = { Text(stringResource(id = R.string.staff_dialog_role_label)) },
            modifier = Modifier.fillMaxWidth()
        )
        if (isSupervisor) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val edited = member.copy(name = nameState.value, role = roleState.value)
                TextButton(onClick = { onSaveEdit(edited) { } }) {
                    Text(stringResource(id = R.string.save))
                }
            }
        }
    }
}

@Composable
fun VacationDaysDialog(
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    val selectedDates = remember { mutableStateListOf<LocalDate>() }
    val today = remember { LocalDate.now() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.vacation_days_label), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(id = R.string.dialog_vacation_instruction),
                    color = Color.Gray,
                    textAlign = TextAlign.Start
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { selectedDates.add(today) }) {
                        Text(text = today.toString())
                    }
                    Button(onClick = { selectedDates.add(today.plusDays(1)) }) {
                        Text(text = today.plusDays(1).toString())
                    }
                }
                if (selectedDates.isNotEmpty()) {
                    Text(
                        text = selectedDates.joinToString(
                            prefix = stringResource(id = R.string.dialog_vacation_instruction) + "\n"
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedDates.map { it.toString() }) }) {
                Text(stringResource(id = R.string.btn_confirm_days, selectedDates.size))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.cancel)) }
        }
    )
}

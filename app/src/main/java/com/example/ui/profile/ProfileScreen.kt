package com.example.ui.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudentProfileEntity
import com.example.ui.JimViewModel
import com.example.ui.components.JimTopBar
import com.example.ui.theme.JimPrimaryBlue

@Composable
fun ProfileScreen(
    viewModel: JimViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val profile by viewModel.profile.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("student_profile_screen")
    ) {
        JimTopBar(
            title = "الملف الشخصي",
            onBackClick = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Avatar & Name matching screenshot
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(JimPrimaryBlue.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = profile.name.take(1),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = JimPrimaryBlue
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = profile.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = profile.titleRole,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Profile Information Card matching the screenshot
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    ProfileFieldRow(label = "الرقم الجامعي", value = profile.universityId)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 10.dp))

                    ProfileFieldRow(label = "الجامعة", value = profile.university)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 10.dp))

                    ProfileFieldRow(label = "الكلية", value = profile.college)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 10.dp))

                    ProfileFieldRow(label = "التخصص", value = profile.major)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 10.dp))

                    ProfileFieldRow(label = "المستوى الدراسي", value = profile.academicLevel)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 10.dp))

                    ProfileFieldRow(label = "المشرف الأكاديمي", value = profile.academicAdvisor)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Edit Profile Button matching the screenshot
            Button(
                onClick = { showEditDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .testTag("edit_profile_button")
            ) {
                Text(
                    text = "تعديل البيانات",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showEditDialog) {
        EditProfileDialog(
            currentProfile = profile,
            onDismiss = { showEditDialog = false },
            onSave = { updated ->
                viewModel.updateProfile(updated)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun ProfileFieldRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun EditProfileDialog(
    currentProfile: StudentProfileEntity,
    onDismiss: () -> Unit,
    onSave: (StudentProfileEntity) -> Unit
) {
    var name by remember { mutableStateOf(currentProfile.name) }
    var uniId by remember { mutableStateOf(currentProfile.universityId) }
    var university by remember { mutableStateOf(currentProfile.university) }
    var college by remember { mutableStateOf(currentProfile.college) }
    var major by remember { mutableStateOf(currentProfile.major) }
    var level by remember { mutableStateOf(currentProfile.academicLevel) }
    var advisor by remember { mutableStateOf(currentProfile.academicAdvisor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل الملف الشخصي", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("الاسم") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = uniId, onValueChange = { uniId = it }, label = { Text("الرقم الجامعي") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = university, onValueChange = { university = it }, label = { Text("الجامعة") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = college, onValueChange = { college = it }, label = { Text("الكلية") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = major, onValueChange = { major = it }, label = { Text("التخصص") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = level, onValueChange = { level = it }, label = { Text("المستوى الدراسي") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = advisor, onValueChange = { advisor = it }, label = { Text("المشرف الأكاديمي") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        currentProfile.copy(
                            name = name,
                            universityId = uniId,
                            university = university,
                            college = college,
                            major = major,
                            academicLevel = level,
                            academicAdvisor = advisor
                        )
                    )
                }
            ) {
                Text("حفظ التغييرات")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

package com.example.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.JimViewModel
import com.example.ui.components.JimTopBar
import com.example.ui.theme.JimPrimaryBlue

@Composable
fun SettingsScreen(
    viewModel: JimViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val profile by viewModel.profile.collectAsState()

    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showDeveloperDialog by remember { mutableStateOf(false) }
    var showNotificationSettingsDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen")
    ) {
        JimTopBar(
            title = "الإعدادات",
            onBackClick = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Theme setting matching screenshot (المظهر: الوضع الداكن / الوضع الفاتح)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (profile.isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = JimPrimaryBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "المظهر",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (profile.isDarkMode) "الوضع الداكن" else "الوضع الفاتح",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = profile.isDarkMode,
                        onCheckedChange = { viewModel.toggleDarkMode() },
                        modifier = Modifier.testTag("dark_mode_switch")
                    )
                }
            }

            // Setting items list matching the screenshot
            SettingsTileItem(
                icon = Icons.Default.Notifications,
                title = "الإشعارات",
                subtitle = if (profile.notificationsEnabled) "مفعلة للمهام والاختبارات" else "معطلة",
                onClick = { showNotificationSettingsDialog = true }
            )

            SettingsTileItem(
                icon = Icons.Default.Backup,
                title = "النسخ الاحتياطي واستعادة البيانات",
                subtitle = "تصدير أو استيراد بيانات المواد والجدول بدون إنترنت",
                onClick = { showBackupDialog = true }
            )

            SettingsTileItem(
                icon = Icons.Default.Settings,
                title = "إعدادات التطبيق",
                subtitle = "نظام المعدل ${profile.maxGpaScale} • اللغة العربية",
                onClick = {
                    Toast.makeText(context, "إعدادات التطبيق مضبوطة بشكل مثالي", Toast.LENGTH_SHORT).show()
                }
            )

            SettingsTileItem(
                icon = Icons.Default.Info,
                title = "عن التطبيق",
                subtitle = "Jim (جيم) • رفيقك الجامعي.. تنظيم دراستك، بكل ذكاء",
                onClick = { showAboutDialog = true }
            )

            SettingsTileItem(
                icon = Icons.Default.Code,
                title = "عن المطور",
                subtitle = "تم بناء التطبيق بأحدث تقنيات أندرويد الحديثة",
                onClick = { showDeveloperDialog = true }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Backup Dialog
    if (showBackupDialog) {
        val backupJson = remember { viewModel.exportBackup() }
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("النسخ الاحتياطي للبيانات", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("تم إنشاء نسخة احتياطية محلية لكافة المواد والتقييمات والمحاضرات والملاحظات بنجاح.")
                    Text("يمكنك نسخ كود النسخة الاحتياطية أو استعادة نسخة سابقة:", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Jim Backup", backupJson)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "تم نسخ كود النسخة الاحتياطية إلى الحافظة", Toast.LENGTH_LONG).show()
                        showBackupDialog = false
                    }
                ) {
                    Text("نسخ إلى الحافظة")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showBackupDialog = false
                        showRestoreDialog = true
                    }
                ) {
                    Text("استعادة نسخة سابقة")
                }
            }
        )
    }

    // Restore Dialog
    if (showRestoreDialog) {
        var restoreText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("استعادة البيانات من كود النسخة", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("الصق كود النسخة الاحتياطية (JSON) هنا:")
                    OutlinedTextField(
                        value = restoreText,
                        onValueChange = { restoreText = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreBackup(restoreText) { success ->
                            if (success) {
                                Toast.makeText(context, "تمت استعادة البيانات بنجاح!", Toast.LENGTH_SHORT).show()
                                showRestoreDialog = false
                            } else {
                                Toast.makeText(context, "فشل قراءة كود النسخة الاحتياطية، تأكد من صحة التنسيق", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = restoreText.isNotBlank()
                ) {
                    Text("تأكيد الاستعادة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // Notification Settings Dialog
    if (showNotificationSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationSettingsDialog = false },
            title = { Text("إعدادات الإشعارات والتنبيهات", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("قاعدة الخصوصية: لا يتم إرسال أي إشعارات مزعجة للمحاضرات العادية، بل للمهام والاختبارات العاجلة فقط.")
                    Button(
                        onClick = {
                            viewModel.triggerTestNotification(context)
                            Toast.makeText(context, "تم إرسال إشعار تجريبي!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("إرسال إشعار تجريبي الآن")
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showNotificationSettingsDialog = false }) {
                    Text("تم")
                }
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("عن تطبيق جيم (Jim)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("تطبيق جيم (Jim) - الإصدار 1.0.0")
                    Text("الشعار: رفيقك الجامعي.. تنظيم دراستك، بكل ذكاء.")
                    Text("صُمم خصيصاً لمساعدة الطلاب الجامعيين في إدارة المواد، المحاضرات، الجداول، التقييمات، المكتبة، والملاحظات الدراسية بنظام تخزين محلي متكامل Offline-First.")
                }
            },
            confirmButton = {
                Button(onClick = { showAboutDialog = false }) { Text("إغلاق") }
            }
        )
    }

    // Developer Dialog
    if (showDeveloperDialog) {
        AlertDialog(
            onDismissRequest = { showDeveloperDialog = false },
            title = { Text("عن المطور", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("تطبيق جيم الأكاديمي تم تطويره بأحدث معايير هندسة البرمجيات وأندرويد الحديثة (Jetpack Compose & Room Database) لتوفير تجربة مستخدم سلسلة وفائقة السرعة.")
                }
            },
            confirmButton = {
                Button(onClick = { showDeveloperDialog = false }) { Text("حسناً") }
            }
        )
    }
}

@Composable
fun SettingsTileItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("settings_tile_${title.replace(" ", "_")}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = JimPrimaryBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

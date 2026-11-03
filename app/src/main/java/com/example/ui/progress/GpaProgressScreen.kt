package com.example.ui.progress

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.JimViewModel
import com.example.ui.components.CircularGpaGauge
import com.example.ui.components.JimTopBar
import com.example.ui.theme.CourseAmber
import com.example.ui.theme.CourseBlue
import com.example.ui.theme.CourseGreen
import com.example.ui.theme.CoursePurple
import com.example.ui.theme.CourseRed
import com.example.ui.theme.JimCyanAccent
import com.example.ui.theme.JimPrimaryBlue

@Composable
fun GpaProgressScreen(
    viewModel: JimViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val profile by viewModel.profile.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val assessments by viewModel.assessments.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("لوحة الأداء", "المواد", "التقارير")

    // What-If Goal Simulator state
    var desiredTargetGpa by remember { mutableDoubleStateOf(profile.targetGpa) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("gpa_progress_screen")
    ) {
        JimTopBar(
            title = "التقدم الأكاديمي",
            onBackClick = onBack
        )

        // Sub-tabs: لوحة الأداء, المواد, التقارير
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.background,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = JimPrimaryBlue
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTabIndex == index) JimPrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Circular Gauge Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularGpaGauge(
                            currentGpa = profile.currentGpa,
                            maxGpa = profile.maxGpaScale,
                            targetGpa = profile.targetGpa
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Credit Hours Stat Cards Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            StatBox(title = "ساعات الفصل", value = "${profile.semesterCredits}")
                            StatBox(title = "الساعات المنجزة", value = "${profile.completedCredits}")
                            StatBox(title = "إجمالي التخرج", value = "${profile.totalCredits}")
                        }
                    }
                }
            }

            // Section: Course Performance Bar Chart (أداء المواد) matching the screenshot
            item {
                Text(
                    text = "أداء المواد الدراسية",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Sample Course GPA scores matching screenshot bars (CS301: 3.8, MAT201: 3.5, DBS302: 3.2, AI301: 3.7)
                        val sampleCourseScores = listOf(
                            Triple("CS301", 3.8, CourseBlue),
                            Triple("MAT201", 3.5, CourseGreen),
                            Triple("DBS302", 3.2, CourseAmber),
                            Triple("AI301", 3.7, CoursePurple),
                            Triple("ENG101", 3.4, CourseRed)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .padding(top = 10.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            sampleCourseScores.forEach { (code, score, color) ->
                                val heightFraction = (score / profile.maxGpaScale).toFloat().coerceIn(0.1f, 1f)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(44.dp)
                                ) {
                                    Text(
                                        text = String.format("%.1f", score),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(36.dp)
                                            .fillMaxHeight(fraction = heightFraction)
                                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(color, color.copy(alpha = 0.6f))
                                                )
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = code,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section: Target GPA Calculator / What-If Simulator
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "محاكي المعدل المستهدف (What-If Calculator)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "احسب المعدل الفصلي المطلوب تحقيقه في الساعات القادمة للوصول إلى معدلك التراكمي المنشود.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = String.format("%.2f", desiredTargetGpa),
                                onValueChange = {
                                    desiredTargetGpa = it.toDoubleOrNull() ?: desiredTargetGpa
                                },
                                label = { Text("المعدل المستهدف") },
                                modifier = Modifier.weight(1f)
                            )

                            Button(
                                onClick = {
                                    viewModel.updateProfile(profile.copy(targetGpa = desiredTargetGpa))
                                },
                                modifier = Modifier.align(Alignment.CenterVertically)
                            ) {
                                Text("تحديث الهدف")
                            }
                        }

                        // Formula calculation: Required Semester GPA = (Target*(Completed+Semester) - Current*Completed) / Semester
                        val completed = profile.completedCredits.toDouble()
                        val semCredits = profile.semesterCredits.toDouble()
                        val currentG = profile.currentGpa
                        val targetG = desiredTargetGpa
                        val requiredSemGpa = ((targetG * (completed + semCredits)) - (currentG * completed)) / semCredits
                        val boundedSemGpa = requiredSemGpa.coerceIn(2.0, profile.maxGpaScale)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(JimPrimaryBlue.copy(alpha = 0.12f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "المعدل الفصلي المطلوب هذا الفصل: ${String.format("%.2f", boundedSemGpa)} من ${profile.maxGpaScale}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = JimPrimaryBlue
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun StatBox(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

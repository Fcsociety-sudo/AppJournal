package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.theme.MyApplicationTheme
import com.example.util.PdfGenerator
import com.example.viewmodel.TeacherViewModel
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val viewModel: TeacherViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
        TeacherApp(viewModel = viewModel)
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherApp(viewModel: TeacherViewModel) {
  val context = LocalContext.current
  val classes by viewModel.allClasses.collectAsStateWithLifecycle()
  val selectedClassId by viewModel.selectedClassId.collectAsStateWithLifecycle()
  val selectedClass by viewModel.selectedClass.collectAsStateWithLifecycle()
  val students by viewModel.studentsOfSelectedClass.collectAsStateWithLifecycle()
  val grades by viewModel.gradesOfSelectedClass.collectAsStateWithLifecycle()

  // Dialog visual control variables
  var showAddClassDialog by remember { mutableStateOf(false) }
  var showAddStudentDialog by remember { mutableStateOf(false) }
  var showAddGradeDialog by remember { mutableStateOf(false) }
  var studentForNewGrade by remember { mutableStateOf<Student?>(null) }

  Scaffold(
      topBar = {
        TopAppBar(
            title = {
              Column {
                Text(
                    text = if (selectedClass != null) selectedClass!!.name else "Журнал Учителя",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                if (selectedClass != null) {
                  Text(
                      text = selectedClass!!.subject,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                  )
                }
              }
            },
            navigationIcon = {
              if (selectedClass != null) {
                IconButton(onClick = { viewModel.selectClass(null) }) {
                  Icon(
                      imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                      contentDescription = "Назад к списку классов"
                  )
                }
              } else {
                Icon(
                    imageVector = Icons.Filled.School,
                    contentDescription = "Школа",
                    modifier = Modifier.padding(start = 16.dp, end = 8.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
              }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            actions = {
              if (selectedClass != null) {
                // PDF Export trigger button
                Button(
                    onClick = {
                      PdfGenerator.generateAndPrint(context, selectedClass!!, students, grades)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                  Icon(
                      imageVector = Icons.Filled.PictureAsPdf,
                      contentDescription = "Печать в PDF",
                      modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Печать (PDF)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
              }
            }
        )
      },
      floatingActionButton = {
        if (selectedClass == null) {
          ExtendedFloatingActionButton(
              onClick = { showAddClassDialog = true },
              icon = { Icon(Icons.Filled.Add, "Добавить класс") },
              text = { Text("Создать класс") },
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary
          )
        } else {
          ExtendedFloatingActionButton(
              onClick = { showAddStudentDialog = true },
              icon = { Icon(Icons.Filled.PersonAdd, "Добавить ученика") },
              text = { Text("Ученик") },
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary
          )
        }
      }
  ) { innerPadding ->
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(MaterialTheme.colorScheme.background)
    ) {
      if (selectedClass == null) {
        // SCREEN 1: Classes list
        ClassesListScreen(
            classes = classes,
            onClassClick = { classId -> viewModel.selectClass(classId) },
            onDeleteClass = { schoolClass -> viewModel.deleteClass(schoolClass) },
            onAddClassClick = { showAddClassDialog = true }
        )
      } else {
        // SCREEN 2: Student list & gradebook
        ClassDetailScreen(
            schoolClass = selectedClass!!,
            students = students,
            grades = grades,
            onAddGradeClick = { student ->
              studentForNewGrade = student
              showAddGradeDialog = true
            },
            onDeleteStudent = { student -> viewModel.deleteStudent(student) },
            onDeleteGrade = { grade -> viewModel.deleteGrade(grade) }
        )
      }

      // --- POPUP DIALOGS ---
      if (showAddClassDialog) {
        AddClassDialog(
            onDismiss = { showAddClassDialog = false },
            onConfirm = { name, subject ->
              viewModel.addClass(name, subject)
              showAddClassDialog = false
            }
        )
      }

      if (showAddStudentDialog && selectedClass != null) {
        AddStudentDialog(
            className = selectedClass!!.name,
            onDismiss = { showAddStudentDialog = false },
            onConfirm = { name ->
              viewModel.addStudent(selectedClass!!.id, name)
              showAddStudentDialog = false
            }
        )
      }

      if (showAddGradeDialog && studentForNewGrade != null && selectedClass != null) {
        AddGradeDialog(
            student = studentForNewGrade!!,
            onDismiss = {
              showAddGradeDialog = false
              studentForNewGrade = null
            },
            onConfirm = { gradeValue, date, topic ->
              viewModel.addGrade(
                  studentId = studentForNewGrade!!.id,
                  classId = selectedClass!!.id,
                  gradeValue = gradeValue,
                  date = date,
                  topic = topic
              )
              showAddGradeDialog = false
              studentForNewGrade = null
            }
        )
      }
    }
  }
}

// ==================== SCREEN COMPONENTS ====================

@Composable
fun ClassesListScreen(
    classes: List<SchoolClass>,
    onClassClick: (Int) -> Unit,
    onDeleteClass: (SchoolClass) -> Unit,
    onAddClassClick: () -> Unit
) {
  if (classes.isEmpty()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
      Box(
          modifier = Modifier
              .size(100.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center
      ) {
        Icon(
            imageVector = Icons.Filled.School,
            contentDescription = "Школа",
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
      }
      Spacer(modifier = Modifier.height(24.dp))
      Text(
          text = "Добро пожаловать!",
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
          text = "Создайте свой первый класс, чтобы начать офлайн учёт оценок и успеваемости.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = 16.dp)
      )
      Spacer(modifier = Modifier.height(32.dp))
      Button(onClick = onAddClassClick) {
        Icon(Icons.Filled.Add, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Создать класс")
      }
    }
  } else {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      item {
        Text(
            text = "Ваши учебные классы:",
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
      }
      items(classes) { schoolClass ->
        ClassCard(
            schoolClass = schoolClass,
            onClick = { onClassClick(schoolClass.id) },
            onDelete = { onDeleteClass(schoolClass) }
        )
      }
    }
  }
}

@Composable
fun ClassCard(
    schoolClass: SchoolClass,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
  Card(
      modifier = Modifier
          .fillMaxWidth()
          .clickable { onClick() },
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Row(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
          modifier = Modifier
              .size(52.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(MaterialTheme.colorScheme.secondaryContainer),
          contentAlignment = Alignment.Center
      ) {
        Text(
            text = schoolClass.name.take(3),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
      }
      Spacer(modifier = Modifier.width(16.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
            text = "Класс: ${schoolClass.name}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Предмет: ${schoolClass.subject}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      var showDeleteConfirm by remember { mutableStateOf(false) }

      if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Удалить класс?") },
            text = { Text("Все связанные ученики и их оценки будут удалены навсегда!") },
            confirmButton = {
              TextButton(
                  onClick = {
                    onDelete()
                    showDeleteConfirm = false
                  }
              ) {
                Text("Удалить", color = MaterialTheme.colorScheme.error)
              }
            },
            dismissButton = {
              TextButton(onClick = { showDeleteConfirm = false }) {
                Text("Отмена")
              }
            }
        )
      }

      IconButton(onClick = { showDeleteConfirm = true }) {
        Icon(
            imageVector = Icons.Filled.Delete,
            contentDescription = "Удалить класс",
            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
        )
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClassDetailScreen(
    schoolClass: SchoolClass,
    students: List<Student>,
    grades: List<Grade>,
    onAddGradeClick: (Student) -> Unit,
    onDeleteStudent: (Student) -> Unit,
    onDeleteGrade: (Grade) -> Unit
) {
  val studentGradesMap = remember(grades) { grades.groupBy { it.studentId } }

  Column(modifier = Modifier.fillMaxSize()) {
    // 1. STATS DASHBOARD HERO CONTAINER
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
      Row(
          modifier = Modifier
              .padding(16.dp)
              .fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
              text = "Показатели успеваемости:",
              fontWeight = FontWeight.SemiBold,
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.primary
          )
          Spacer(modifier = Modifier.height(4.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.People,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${students.size} уч.",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                imageVector = Icons.Filled.Grade,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.width(4.dp))
            val numericalGrades = grades.mapNotNull { it.gradeValue.toDoubleOrNull() }
            val classAvg = if (numericalGrades.isEmpty()) "—" else DecimalFormat("#.##").format(numericalGrades.average())
            Text(
                text = "Ср. балл класса: $classAvg",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
          }
        }
        Icon(
            imageVector = Icons.Filled.TrendingUp,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
      }
    }

    if (students.isEmpty()) {
      Column(
          modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
              .padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
      ) {
        Icon(
            imageVector = Icons.Filled.Group,
            contentDescription = "Пустой класс",
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "В классе пока нет учеников",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Нажмите «Ученик» в правом нижнем углу экрана, чтобы создать список класса.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
        )
      }
    } else {
      LazyColumn(
          modifier = Modifier.weight(1f),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(students) { student ->
          val studentGrades = studentGradesMap[student.id] ?: emptyList()
          
          StudentGridRow(
              student = student,
              studentGrades = studentGrades,
              onAddGrade = { onAddGradeClick(student) },
              onDeleteStudent = { onDeleteStudent(student) },
              onDeleteGrade = { grade -> onDeleteGrade(grade) }
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StudentGridRow(
    student: Student,
    studentGrades: List<Grade>,
    onAddGrade: () -> Unit,
    onDeleteStudent: () -> Unit,
    onDeleteGrade: (Grade) -> Unit
) {
  Card(
      modifier = Modifier.fillMaxWidth(),
      elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
              imageVector = Icons.Filled.Person,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
              tint = MaterialTheme.colorScheme.secondary
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
              text = student.name,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
          )
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
          // Average grade indicator label
          val valGrades = studentGrades.mapNotNull { it.gradeValue.toDoubleOrNull() }
          if (valGrades.isNotEmpty()) {
            val sAvg = DecimalFormat("#.##").format(valGrades.average())
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
              Text(
                  text = "ср: $sAvg",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onPrimaryContainer
              )
            }
            Spacer(modifier = Modifier.width(6.dp))
          }

          var showDeleteStudentConfirm by remember { mutableStateOf(false) }

          if (showDeleteStudentConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteStudentConfirm = false },
                title = { Text("Удалить ученика?") },
                text = { Text("Весь список его оценок будет удален. Это действие необратимо.") },
                confirmButton = {
                  TextButton(
                      onClick = {
                        onDeleteStudent()
                        showDeleteStudentConfirm = false
                      }
                  ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                  }
                },
                dismissButton = {
                  TextButton(onClick = { showDeleteStudentConfirm = false }) {
                    Text("Отмена")
                  }
                }
            )
          }

          IconButton(
              onClick = { showDeleteStudentConfirm = true },
              modifier = Modifier.size(24.dp)
          ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Удалить ученика",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // List of grades + Add button inside a fluid row
      Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
      ) {
        if (studentGrades.isEmpty()) {
          Text(
              text = "Пока нет оценок",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
              modifier = Modifier.weight(1f)
          )
        } else {
          // Display horizontal flow row of grades
          LazyRow(
              modifier = Modifier.weight(1f),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically
          ) {
            items(studentGrades) { grade ->
              var showDeleteGradeDialog by remember { mutableStateOf(false) }

              if (showDeleteGradeDialog) {
                // Formatting date nicely
                val humanReadableDate = try {
                  val parts = grade.date.split("-")
                  if (parts.size == 3) "${parts[2]}.${parts[1]}" else grade.date
                } catch (e: Exception) {
                  grade.date
                }

                AlertDialog(
                    onDismissRequest = { showDeleteGradeDialog = false },
                    title = { Text("Оценка за $humanReadableDate") },
                    text = {
                      Column {
                        Text("Оценка: ${grade.gradeValue}", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Тема/Работа: ${grade.topic}")
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Вы хотите удалить эту оценку из журнала?",
                            color = MaterialTheme.colorScheme.error
                        )
                      }
                    },
                    confirmButton = {
                      TextButton(
                          onClick = {
                            onDeleteGrade(grade)
                            showDeleteGradeDialog = false
                          }
                      ) {
                        Text("Удалить", color = MaterialTheme.colorScheme.error)
                      }
                    },
                    dismissButton = {
                      TextButton(onClick = { showDeleteGradeDialog = false }) {
                        Text("Закрыть")
                      }
                    }
                )
              }

              Box(
                  modifier = Modifier
                      .clip(RoundedCornerShape(8.dp))
                      .background(getGradeColor(grade.gradeValue))
                      .combinedClickable(
                          onClick = { showDeleteGradeDialog = true },
                          onLongClick = { showDeleteGradeDialog = true }
                      )
                      .padding(horizontal = 8.dp, vertical = 4.dp),
                  contentAlignment = Alignment.Center
              ) {
                Text(
                    text = grade.gradeValue,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
              }
            }
          }
        }

        // Quick grading triggers
        IconButton(
            onClick = onAddGrade,
            modifier = Modifier
                .padding(start = 8.dp)
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
        ) {
          Icon(
              imageVector = Icons.Filled.Add,
              contentDescription = "Выставить оценку",
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(16.dp)
          )
        }
      }
    }
  }
}

// Function mapper for grading colorizations
fun getGradeColor(value: String): Color {
  return when (value) {
    "5" -> Color(0xFF2E7D32) // Soft deep green
    "4" -> Color(0xFF1976D2) // Rich primary blue
    "3" -> Color(0xFFEF6C00) // Dark amber amber
    "2" -> Color(0xFFC62828) // Deep warning crimson
    "Н" -> Color(0xFF5D6D7E) // Slate absence grey
    else -> Color(0xFF884EA0) // Purple alternative grades
  }
}

// ==================== DIALOG BOXES ====================

@Composable
fun AddClassDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, subject: String) -> Unit
) {
  var name by remember { mutableStateOf("") }
  var subject by remember { mutableStateOf("") }

  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text("Новый класс", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
              value = name,
              onValueChange = { name = it },
              label = { Text("Наименование (например, 9А)") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
              value = subject,
              onValueChange = { subject = it },
              label = { Text("Предмет (например, Физика)") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
            onClick = { if (name.isNotBlank() && subject.isNotBlank()) onConfirm(name, subject) },
            enabled = name.isNotBlank() && subject.isNotBlank()
        ) {
          Text("Создать")
        }
      },
      dismissButton = {
        TextButton(onClick = onDismiss) {
          Text("Отмена")
        }
      }
  )
}

@Composable
fun AddStudentDialog(
    className: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String) -> Unit
) {
  var name by remember { mutableStateOf("") }

  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text("Новый ученик в $className", fontWeight = FontWeight.Bold) },
      text = {
        Column {
          OutlinedTextField(
              value = name,
              onValueChange = { name = it },
              label = { Text("ФИО Ученика") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
            onClick = { if (name.isNotBlank()) onConfirm(name) },
            enabled = name.isNotBlank()
        ) {
          Text("Добавить")
        }
      },
      dismissButton = {
        TextButton(onClick = onDismiss) {
          Text("Отмена")
        }
      }
  )
}

@Composable
fun AddGradeDialog(
    student: Student,
    onDismiss: () -> Unit,
    onConfirm: (gradeValue: String, date: String, topic: String) -> Unit
) {
  // Fast grading presets
  val presets = listOf("5", "4", "3", "2", "Н")
  var selectedValue by remember { mutableStateOf("5") }
  
  // Setup standard system date pre-filled
  val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
  var dateStr by remember { mutableStateOf(sdf.format(Date())) }
  var topic by remember { mutableStateOf("") }

  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text("Выставить оценку", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("Ученик: ${student.name}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
          
          Text("Оценка:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
          
          // Row of large buttons for quick tapping!
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
          ) {
            presets.forEach { preset ->
              val isSelected = selectedValue == preset
              Box(
                  modifier = Modifier
                      .size(46.dp)
                      .clip(RoundedCornerShape(8.dp))
                      .background(
                          if (isSelected) getGradeColor(preset) else MaterialTheme.colorScheme.surfaceVariant
                      )
                      .clickable { selectedValue = preset }
                      .padding(4.dp),
                  contentAlignment = Alignment.Center
              ) {
                Text(
                    text = preset,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
              }
            }
          }

          OutlinedTextField(
              value = topic,
              onValueChange = { topic = it },
              label = { Text("Тема урока / Вид работы") },
              placeholder = { Text("Классная работа, Ответ у доски") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
              value = dateStr,
              onValueChange = { dateStr = it },
              label = { Text("Дата (ГГГГ-ММ-ДД)") },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
            onClick = {
              if (selectedValue.isNotBlank() && dateStr.isNotBlank()) {
                onConfirm(selectedValue, dateStr, topic)
              }
            },
            enabled = selectedValue.isNotBlank() && dateStr.isNotBlank()
        ) {
          Text("Сохранить")
        }
      },
      dismissButton = {
        TextButton(onClick = onDismiss) {
          Text("Отмена")
        }
      }
  )
}

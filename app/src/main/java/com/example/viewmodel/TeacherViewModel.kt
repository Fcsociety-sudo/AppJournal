package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TeacherViewModel(application: Application) : AndroidViewModel(application) {
    private val database = TeacherDatabase.getDatabase(application)
    private val repository = TeacherRepository(database.teacherDao)

    val allClasses: StateFlow<List<SchoolClass>> = repository.allClasses
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedClassId = MutableStateFlow<Int?>(null)
    val selectedClassId: StateFlow<Int?> = _selectedClassId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedClass: StateFlow<SchoolClass?> = _selectedClassId
        .flatMapLatest { id ->
            if (id == null) {
                flowOf(null)
            } else {
                repository.allClasses.map { list -> list.find { it.id == id } }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val studentsOfSelectedClass: StateFlow<List<Student>> = _selectedClassId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.getStudentsForClass(id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val gradesOfSelectedClass: StateFlow<List<Grade>> = _selectedClassId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.getGradesForClass(id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun selectClass(classId: Int?) {
        _selectedClassId.value = classId
    }

    // --- CLASS OPERATIONS ---
    fun addClass(name: String, subject: String) {
        viewModelScope.launch {
            if (name.isNotBlank() && subject.isNotBlank()) {
                repository.insertClass(SchoolClass(name = name, subject = subject))
            }
        }
    }

    fun deleteClass(schoolClass: SchoolClass) {
        viewModelScope.launch {
            if (_selectedClassId.value == schoolClass.id) {
                _selectedClassId.value = null
            }
            repository.deleteClass(schoolClass)
        }
    }

    // --- STUDENT OPERATIONS ---
    fun addStudent(classId: Int, name: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                repository.insertStudent(Student(classId = classId, name = name))
            }
        }
    }

    fun deleteStudent(student: Student) {
        viewModelScope.launch {
            repository.deleteStudent(student)
        }
    }

    // --- GRADE OPERATIONS ---
    fun addGrade(studentId: Int, classId: Int, gradeValue: String, date: String, topic: String) {
        viewModelScope.launch {
            if (gradeValue.isNotBlank() && date.isNotBlank()) {
                repository.insertGrade(
                    Grade(
                        studentId = studentId,
                        classId = classId,
                        gradeValue = gradeValue,
                        date = date,
                        topic = topic.ifBlank { "Ответ на уроке" }
                    )
                )
            }
        }
    }

    fun deleteGrade(grade: Grade) {
        viewModelScope.launch {
            repository.deleteGrade(grade)
        }
    }
}

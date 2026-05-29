package com.example.data

import kotlinx.coroutines.flow.Flow

class TeacherRepository(private val teacherDao: TeacherDao) {
    val allClasses: Flow<List<SchoolClass>> = teacherDao.getAllClasses()

    suspend fun getClassById(classId: Int): SchoolClass? = teacherDao.getClassById(classId)

    suspend fun insertClass(schoolClass: SchoolClass): Long = teacherDao.insertClass(schoolClass)

    suspend fun updateClass(schoolClass: SchoolClass) = teacherDao.updateClass(schoolClass)

    suspend fun deleteClass(schoolClass: SchoolClass) = teacherDao.deleteClass(schoolClass)

    fun getStudentsForClass(classId: Int): Flow<List<Student>> = teacherDao.getStudentsForClass(classId)

    suspend fun getStudentById(id: Int): Student? = teacherDao.getStudentById(id)

    suspend fun insertStudent(student: Student): Long = teacherDao.insertStudent(student)

    suspend fun updateStudent(student: Student) = teacherDao.updateStudent(student)

    suspend fun deleteStudent(student: Student) = teacherDao.deleteStudent(student)

    fun getGradesForClass(classId: Int): Flow<List<Grade>> = teacherDao.getGradesForClass(classId)

    fun getGradesForStudent(studentId: Int): Flow<List<Grade>> = teacherDao.getGradesForStudent(studentId)

    suspend fun insertGrade(grade: Grade): Long = teacherDao.insertGrade(grade)

    suspend fun updateGrade(grade: Grade) = teacherDao.updateGrade(grade)

    suspend fun deleteGrade(grade: Grade) = teacherDao.deleteGrade(grade)
}

package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.print.PrintAttributes
import android.print.PrintManager
import android.widget.Toast
import com.example.data.Grade
import com.example.data.SchoolClass
import com.example.data.Student
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {

    fun generateAndPrint(
        context: Context,
        schoolClass: SchoolClass,
        students: List<Student>,
        grades: List<Grade>
    ) {
        val pdfDocument = PdfDocument()
        val paint = Paint()

        // Page sizes (A4 in points: 595 x 842)
        val pageWidth = 595
        val pageHeight = 842
        val leftMargin = 36f
        val rightMargin = 595f - 36f
        val topMargin = 40f
        val bottomMargin = 842f - 40f

        val studentsPerPage = 16
        val rowHeight = 38f

        // Group grades by student
        val gradesByStudent = grades.groupBy { it.studentId }

        var pageNumber = 1
        var studentIndex = 0

        // Get dynamic local date
        val currentDateFormatted = try {
            val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            sdf.format(Date())
        } catch (e: Exception) {
            "29.05.2026"
        }

        while (studentIndex < students.size || students.isEmpty()) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // 1. Draw header (only on page 1)
            var currentY = topMargin
            if (pageNumber == 1) {
                // Title
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 20f
                paint.color = Color.BLACK
                canvas.drawText("ВЕДОМОСТЬ УСПЕВАЕМОСТИ", leftMargin, currentY + 24f, paint)
                
                // Details
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.textSize = 12f
                paint.color = Color.DKGRAY
                canvas.drawText("Класс: ${schoolClass.name}  |  Предмет: ${schoolClass.subject}", leftMargin, currentY + 46f, paint)

                // Date of document
                canvas.drawText("Дата выгрузки: $currentDateFormatted", leftMargin, currentY + 62f, paint)

                // Decorative double line
                paint.color = Color.rgb(30, 80, 150) // Beautiful royal blue divider
                paint.strokeWidth = 2f
                canvas.drawLine(leftMargin, currentY + 75f, rightMargin, currentY + 75f, paint)
                paint.strokeWidth = 0.5f
                canvas.drawLine(leftMargin, currentY + 79f, rightMargin, currentY + 79f, paint)

                currentY += 95f
            } else {
                currentY += 20f
                // Mini header for subsequent pages
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                paint.textSize = 10f
                paint.color = Color.DKGRAY
                canvas.drawText("Ведомость класса ${schoolClass.name} — ${schoolClass.subject} (Продолжение)", leftMargin, currentY, paint)
                paint.color = Color.GRAY
                canvas.drawLine(leftMargin, currentY + 6f, rightMargin, currentY + 6f, paint)
                currentY += 25f
            }

            // 2. Draw Table Header
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 10f
            paint.color = Color.WHITE

            // Header background
            val headerPaint = Paint().apply {
                color = Color.rgb(55, 71, 79) // Slate gray header
                style = Paint.Style.FILL
            }
            canvas.drawRect(leftMargin, currentY, rightMargin, currentY + 24f, headerPaint)

            // Header Column Titles
            paint.color = Color.WHITE
            canvas.drawText("№", leftMargin + 8f, currentY + 16f, paint)
            canvas.drawText("ФИО Ученика", leftMargin + 30f, currentY + 16f, paint)
            canvas.drawText("Оценки (с датами)", leftMargin + 180f, currentY + 16f, paint)
            canvas.drawText("Ср. балл", rightMargin - 66f, currentY + 16f, paint)

            currentY += 24f

            // 3. Draw Student Rows
            val listToDraw = if (students.isEmpty()) emptyList() else students.subList(studentIndex, minOf(studentIndex + studentsPerPage, students.size))
            
            val rowBgPaintEven = Paint().apply {
                color = Color.rgb(244, 246, 249) // Light background tint for readability
                style = Paint.Style.FILL
            }
            val rowBgPaintOdd = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            val borderPaint = Paint().apply {
                color = Color.rgb(207, 216, 220)
                strokeWidth = 0.5f
                style = Paint.Style.STROKE
            }

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 10f

            if (students.isEmpty()) {
                // Empty state within the PDF document
                paint.color = Color.GRAY
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                canvas.drawRect(leftMargin, currentY, rightMargin, currentY + rowHeight, rowBgPaintOdd)
                canvas.drawRect(leftMargin, currentY, rightMargin, currentY + rowHeight, borderPaint)
                canvas.drawText("Список учеников пока пуст", leftMargin + 30f, currentY + 22f, paint)
                currentY += rowHeight
            } else {
                for (i in listToDraw.indices) {
                    val s = listToDraw[i]
                    val rank = studentIndex + i + 1

                    // Alternating background
                    val bg = if (i % 2 == 0) rowBgPaintEven else rowBgPaintOdd
                    canvas.drawRect(leftMargin, currentY, rightMargin, currentY + rowHeight, bg)

                    // Draw borders
                    canvas.drawRect(leftMargin, currentY, rightMargin, currentY + rowHeight, borderPaint)

                    // No.
                    paint.color = Color.BLACK
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    canvas.drawText(rank.toString(), leftMargin + 8f, currentY + 22f, paint)

                    // Student Name (Truncated if it overflows allocated column width)
                    val originalName = s.name
                    var displayName = originalName
                    if (paint.measureText(displayName) > 135f) {
                        while (displayName.isNotEmpty() && paint.measureText("$displayName...") > 135f) {
                            displayName = displayName.substring(0, displayName.length - 1)
                        }
                        displayName = "$displayName..."
                    }
                    canvas.drawText(displayName, leftMargin + 30f, currentY + 22f, paint)

                    // Grades chronological display
                    val studentGrades = gradesByStudent[s.id] ?: emptyList()
                    val gradesStr = if (studentGrades.isEmpty()) {
                        "—"
                    } else {
                        studentGrades.joinToString(", ") { grade ->
                            val cleanDate = try {
                                val parts = grade.date.split("-")
                                if (parts.size == 3) "${parts[2]}.${parts[1]}" else grade.date
                            } catch (e: Exception) {
                                grade.date
                            }
                            "${grade.gradeValue} ($cleanDate)"
                        }
                    }

                    // Wrap or truncate grades listing to avoid visual page overflow
                    var displayGrades = gradesStr
                    val maxGradesWidth = 260f
                    if (paint.measureText(displayGrades) > maxGradesWidth) {
                        while (displayGrades.isNotEmpty() && paint.measureText("$displayGrades...") > maxGradesWidth) {
                            displayGrades = displayGrades.substring(0, displayGrades.length - 1)
                        }
                        displayGrades = "$displayGrades..."
                    }
                    paint.color = Color.rgb(69, 90, 100)
                    canvas.drawText(displayGrades, leftMargin + 180f, currentY + 22f, paint)

                    // Numerical Cumulative Grade Point Average
                    val numericalGrades = studentGrades.mapNotNull { it.gradeValue.toDoubleOrNull() }
                    val avgStr = if (numericalGrades.isEmpty()) {
                        "—"
                    } else {
                        val avg = numericalGrades.average()
                        val df = DecimalFormat("#.##")
                        df.format(avg)
                    }
                    paint.color = Color.BLACK
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText(avgStr, rightMargin - 45f, currentY + 22f, paint)

                    currentY += rowHeight
                }
            }

            // 4. Draw Footer
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 8f
            paint.color = Color.GRAY
            canvas.drawText("Страница $pageNumber", rightMargin - 60f, bottomMargin - 10f, paint)
            canvas.drawText("Журнал Учителя — Автономный оффлайн-экспорт PDF ведомостей", leftMargin, bottomMargin - 10f, paint)

            pdfDocument.finishPage(page)

            if (students.isEmpty()) {
                break
            }
            studentIndex += studentsPerPage
            pageNumber++
        }

        // Export generated bytes to system print task
        try {
            val file = File(context.cacheDir, "Vedomost_${schoolClass.name.replace(" ", "_")}.pdf")
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()

            // Pass byte print descriptor task directly to native Android PrintManager
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            val jobName = "Ведомость - ${schoolClass.name} - ${schoolClass.subject}"
            
            printManager.print(
                jobName,
                FilePrintAdapter(file),
                PrintAttributes.Builder().build()
            )

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Ошибка печати: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    class FilePrintAdapter(private val file: File) : android.print.PrintDocumentAdapter() {
        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes,
            cancellationSignal: android.os.CancellationSignal?,
            callback: LayoutResultCallback,
            extras: android.os.Bundle?
        ) {
            if (cancellationSignal?.isCanceled == true) {
                callback.onLayoutCancelled()
                return
            }
            val info = android.print.PrintDocumentInfo.Builder(file.name)
                .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .build()
            callback.onLayoutFinished(info, true)
        }

        override fun onWrite(
            pages: Array<out android.print.PageRange>?,
            destination: android.os.ParcelFileDescriptor?,
            cancellationSignal: android.os.CancellationSignal?,
            callback: WriteResultCallback
        ) {
            try {
                val input = java.io.FileInputStream(file)
                val output = java.io.FileOutputStream(destination?.fileDescriptor)
                input.copyTo(output)
                callback.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
            } catch (e: Exception) {
                callback.onWriteFailed(e.message)
            }
        }
    }
}

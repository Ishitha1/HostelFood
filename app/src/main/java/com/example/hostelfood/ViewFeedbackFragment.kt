package com.example.hostelfood

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hostelfood.databinding.FragmentViewFeedbackBinding
import com.google.firebase.firestore.FirebaseFirestore
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ViewFeedbackFragment : Fragment() {

    private var _binding: FragmentViewFeedbackBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentViewFeedbackBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvAllFeedbacks.layoutManager = LinearLayoutManager(requireContext())
        loadFeedbacksGroupedByDay()

        binding.btnSaveExcel.setOnClickListener {
            exportToExcel()
        }

        binding.btnClearOldData.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun loadFeedbacksGroupedByDay() {
        db.collection("feedbacks")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val grouped = mutableMapOf<String, MutableList<FeedbackItem>>()

                for (doc in documents) {
                    val rollNo = doc.getString("rollNumber") ?: "Unknown"
                    val meal = doc.getString("mealType") ?: "Unknown"
                    val day = doc.getString("day") ?: "Unknown"
                    val comment = doc.getString("comment") ?: ""
                    val timestamp = doc.getTimestamp("timestamp")
                    val dateKey = timestamp?.toDate()?.let {
                        SimpleDateFormat("EEEE (dd-MM-yyyy)", Locale.getDefault()).format(it)
                    } ?: "Unknown"
                    val ratings = doc.get("ratings") as? Map<String, String> ?: emptyMap()

                    if (comment.trim().isEmpty()) continue

                    val item = FeedbackItem(rollNo, meal, day, ratings, comment, timestamp)
                    grouped.getOrPut(dateKey) { mutableListOf() }.add(item)
                }

                if (grouped.isEmpty()) {
                    Toast.makeText(requireContext(), "No feedback with comments yet", Toast.LENGTH_SHORT).show()
                }

                val sortedGrouped = grouped.toSortedMap(compareByDescending { it })
                binding.rvAllFeedbacks.adapter = DayGroupedFeedbackAdapter(sortedGrouped)
                //binding.rvAllFeedbacks.adapter = DayGroupedFeedbackAdapter(grouped)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load feedbacks", Toast.LENGTH_SHORT).show()
            }
    }

   private fun exportToExcel() {

       val file = File(
           requireContext().getExternalFilesDir(null),
           "Weekly_Feedback.xlsx"
       )

       db.collection("feedbacks")
           .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
           .get()
           .addOnSuccessListener { documents ->

               val workbook = if (file.exists()) {
                   XSSFWorkbook(file.inputStream())
               } else {
                   XSSFWorkbook()
               }

               val sheet = if (workbook.numberOfSheets > 0) {
                   workbook.getSheetAt(0)
               } else {
                   workbook.createSheet("Hostel Feedback")
               }

               // ✅ HEADER (only once)
               if (sheet.physicalNumberOfRows == 0) {
                   val header = sheet.createRow(0)
                   arrayOf("Key", "Date", "Day", "Roll No", "Meal", "Comment")
                       .forEachIndexed { i, text ->
                           header.createCell(i).setCellValue(text)
                       }
               }

               // ✅ READ EXISTING KEYS
               val existingKeys = mutableSetOf<String>()
               for (i in 1..sheet.lastRowNum) {
                   val row = sheet.getRow(i) ?: continue
                   val key = row.getCell(0)?.stringCellValue ?: continue
                   existingKeys.add(key)
               }

               var rowNum = sheet.lastRowNum + 1
               var addedCount = 0

               // ✅ ADD NEW DATA
               for (doc in documents) {

                   val timestamp = doc.getTimestamp("timestamp")
                   val roll = doc.getString("rollNumber") ?: ""
                   val meal = doc.getString("mealType") ?: ""
                   val day = doc.getString("day") ?: ""
                   val comment = doc.getString("comment") ?: ""

                   if (comment.trim().isEmpty()) continue

                   val key = "${timestamp?.seconds}_${roll}_$meal"

                   // ❌ Skip duplicates
                   if (existingKeys.contains(key)) continue

                   val row = sheet.createRow(rowNum++)

                   row.createCell(0).setCellValue(key) // hidden unique key
                   row.createCell(1).setCellValue(
                       timestamp?.toDate()?.let {
                           SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(it)
                       } ?: ""
                   )
                   row.createCell(2).setCellValue(day)
                   row.createCell(3).setCellValue(roll)
                   row.createCell(4).setCellValue(meal)
                   row.createCell(5).setCellValue(comment)

                   existingKeys.add(key)
                   addedCount++
               }

               // hide key column
               sheet.setColumnHidden(0, true)

               try {
                   FileOutputStream(file).use { workbook.write(it) }
                   workbook.close()

                   Toast.makeText(
                       requireContext(),
                       "Excel Updated ✅ Added: $addedCount rows\n${file.absolutePath}",
                       Toast.LENGTH_LONG
                   ).show()

               } catch (e: Exception) {
                   Toast.makeText(
                       requireContext(),
                       "Save Failed: ${e.message}",
                       Toast.LENGTH_SHORT
                   ).show()
               }
           }
   }
    private fun deleteLast7DaysData() {

        val calendar = Calendar.getInstance()

        // NOW
        val end = com.google.firebase.Timestamp(calendar.time)

        // 7 days ago
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val start = com.google.firebase.Timestamp(calendar.time)

        db.collection("feedbacks")
            .whereGreaterThanOrEqualTo("timestamp", start)
            .whereLessThanOrEqualTo("timestamp", end)
            .get()
            .addOnSuccessListener { documents ->

                Toast.makeText(requireContext(), "Docs found: ${documents.size()}", Toast.LENGTH_LONG).show()

                if (documents.isEmpty) {
                    Toast.makeText(requireContext(), "No recent data found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val batch = db.batch()

                for (doc in documents) {
                    batch.delete(doc.reference)
                }

                batch.commit()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Last 7 days deleted!", Toast.LENGTH_LONG).show()
                        Toast.makeText(requireContext(), "Docs: ${documents.size()}", Toast.LENGTH_LONG).show()
                        loadFeedbacksGroupedByDay()
                    }
            }
    }
    private fun showDeleteConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Weekly Data")
            .setMessage("Are you sure you want to delete this week's feedback?\nThis action cannot be undone.")
            .setPositiveButton("Yes") { _, _ ->
                deleteLast7DaysData()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
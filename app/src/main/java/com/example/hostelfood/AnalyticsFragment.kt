package com.example.hostelfood

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.hostelfood.databinding.FragmentAnalyticsBinding
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.firestore.FirebaseFirestore
import com.itextpdf.text.Document
import com.itextpdf.text.Paragraph
import com.itextpdf.text.pdf.PdfWriter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import com.itextpdf.text.*
import com.itextpdf.text.pdf.*
import com.itextpdf.text.pdf.draw.LineSeparator

class AnalyticsFragment : Fragment() {

    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    private var fromDate: Date? = null
    private var toDate: Date? = null
    val dayFont = Font(Font.FontFamily.HELVETICA, 16f, Font.BOLD, BaseColor(33, 150, 243)) // blue
    val mealFont = Font(Font.FontFamily.HELVETICA, 14f, Font.BOLD, BaseColor(76, 175, 80)) // green
    val normalFont = Font(Font.FontFamily.HELVETICA, 11f)
    val headerFont = Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD, BaseColor.WHITE)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadTodayAnalytics()

        binding.btnDownloadPDF.setOnClickListener {
            showDateRangePicker()
        }
    }

    private fun loadTodayAnalytics() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        val start = com.google.firebase.Timestamp(calendar.time)

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        val end = com.google.firebase.Timestamp(calendar.time)

        loadAnalyticsData(start, end)
    }

    private fun showDateRangePicker() {
        val calendar = Calendar.getInstance()

        DatePickerDialog(requireContext(), { _, year, month, day ->

            val fromCal = Calendar.getInstance().apply {
                set(year, month, day, 0, 0, 0)
            }
            fromDate = fromCal.time

            DatePickerDialog(requireContext(), { _, y, m, d ->

                val toCal = Calendar.getInstance().apply {
                    set(y, m, d, 23, 59, 59)
                }
                toDate = toCal.time

                val start = com.google.firebase.Timestamp(fromDate!!)
                val end = com.google.firebase.Timestamp(toDate!!)

                // 🔥 FETCH DATA HERE (IMPORTANT)
                db.collection("feedbacks")
                    .whereGreaterThanOrEqualTo("timestamp", start)
                    .whereLessThanOrEqualTo("timestamp", end)
                    .get()
                    .addOnSuccessListener { snapshot ->

                        loadAnalyticsData(start, end) // still show charts

                        // ✅ PASS REAL DATA TO PDF
                        downloadAnalyticsAsPDF(snapshot.documents)
                    }

            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()

        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun loadAnalyticsData(start: com.google.firebase.Timestamp, end: com.google.firebase.Timestamp) {
        db.collection("feedbacks")
            .whereGreaterThanOrEqualTo("timestamp", start)
            .whereLessThanOrEqualTo("timestamp", end)
            .get()
            .addOnSuccessListener { documents ->
                val mealRatings = mutableMapOf<String, MutableMap<String, MutableList<Float>>>()

                for (doc in documents) {
                    val mealType = doc.getString("mealType") ?: continue
                    val ratings = doc.get("ratings") as? Map<String, String> ?: continue

                    ratings.forEach { (itemName, ratingStr) ->
                        val score = when (ratingStr.lowercase()) {
                            "excellent" -> 4f
                            "good" -> 3f
                            "avg", "average" -> 2f
                            "poor" -> 1f
                            else -> 0f
                        }
                        mealRatings.getOrPut(mealType) { mutableMapOf() }
                            .getOrPut(itemName) { mutableListOf() }.add(score)
                    }
                }

                setupBarChart(binding.chartBreakfast, mealRatings["Breakfast"] ?: emptyMap())
                setupBarChart(binding.chartLunch, mealRatings["Lunch"] ?: emptyMap())
                setupBarChart(binding.chartSnacks, mealRatings["Snacks"] ?: emptyMap())
                setupBarChart(binding.chartDinner, mealRatings["Dinner"] ?: emptyMap())
            }
    }

    private fun setupBarChart(chart: com.github.mikephil.charting.charts.BarChart, dataMap: Map<String, List<Float>>) {
        val entries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()
        var index = 0f

        dataMap.forEach { (item, scores) ->
            val avg = if (scores.isNotEmpty()) scores.average().toFloat() else 0f
            entries.add(BarEntry(index, avg))
            labels.add(item)
            index++
        }

        val dataSet = BarDataSet(entries, "Average Rating")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()

        chart.data = BarData(dataSet)
        chart.xAxis.valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels)
        chart.xAxis.granularity = 1f
        chart.xAxis.labelRotationAngle = -45f
        chart.description.isEnabled = false
        chart.animateY(800)
        chart.invalidate()
    }

    private fun downloadAnalyticsAsPDF(documents: List<com.google.firebase.firestore.DocumentSnapshot>) {

        try {
            val file = File(requireContext().getExternalFilesDir(null), "Analytics_Report.pdf")

            val titleFont = Font(Font.FontFamily.HELVETICA, 18f, Font.BOLD, BaseColor(41, 3, 64))
            val subTitleFont = Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD, BaseColor.DARK_GRAY)
            val normalFont = Font(Font.FontFamily.HELVETICA, 10f, Font.NORMAL)
            val headerFont = Font(Font.FontFamily.HELVETICA, 11f, Font.BOLD, BaseColor.WHITE)
            val dayFont = Font(Font.FontFamily.HELVETICA, 14f, Font.BOLD)
            val mealFont = Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD, BaseColor(143, 132, 16))
            val document = Document()
            PdfWriter.getInstance(document, FileOutputStream(file))
            document.open()

            val sdfDay = SimpleDateFormat("EEEE", Locale.getDefault())
            val sdfDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())


            val title = Paragraph("HOSTEL FOOD ANALYTICS REPORT\n\n", titleFont)
            title.alignment = Element.ALIGN_CENTER
            document.add(title)

            val fromTo = Paragraph(
                "From: ${SimpleDateFormat("dd MMM yyyy").format(fromDate!!)}\n" +
                        "To: ${SimpleDateFormat("dd MMM yyyy").format(toDate!!)}\n\n",
                subTitleFont
            )
            fromTo.alignment = Element.ALIGN_CENTER
            document.add(fromTo)

            // 🔥 GROUP BY DATE → MEAL → ITEMS
            val grouped = mutableMapOf<String, MutableMap<String, MutableMap<String, MutableList<Float>>>>()

            for (doc in documents) {

                val timestamp = doc.getTimestamp("timestamp")?.toDate() ?: continue
                val dateKey = sdfDate.format(timestamp)
                val dayName = sdfDay.format(timestamp)

                val meal = doc.getString("mealType") ?: continue
                val ratings = doc.get("ratings") as? Map<String, String> ?: continue

                val fullDateKey = "$dayName ($dateKey)"

                ratings.forEach { (item, ratingStr) ->

                    val score = when (ratingStr.lowercase()) {
                        "excellent" -> 4f
                        "good" -> 3f
                        "avg", "average" -> 2f
                        "poor" -> 1f
                        else -> 0f
                    }

                    grouped
                        .getOrPut(fullDateKey) { mutableMapOf() }
                        .getOrPut(meal) { mutableMapOf() }
                        .getOrPut(item) { mutableListOf() }
                        .add(score)
                }
            }

            // 🔥 PRINT INTO PDF
            grouped.forEach { (day, meals) ->

                document.add(Paragraph("\n$day", dayFont))

                val line = LineSeparator()
                line.lineColor = BaseColor.LIGHT_GRAY
                document.add(Chunk(line))

                meals.forEach { (meal, items) ->

                    document.add(Paragraph("\n$meal", mealFont))

                    var totalResponses = 0

                    // TABLE HEADER
                    val table = PdfPTable(2)
                    table.widthPercentage = 100f
                    table.spacingBefore = 8f
                    table.spacingAfter = 8f

                    // Header cells
                    val header1 = PdfPCell(Phrase("Item", headerFont))
                    header1.backgroundColor = BaseColor(105, 0, 168) // Indigo
                    header1.horizontalAlignment = Element.ALIGN_CENTER

                    val header2 = PdfPCell(Phrase("Avg Rating", headerFont))
                    header2.backgroundColor = BaseColor(105, 0, 168)
                    header2.horizontalAlignment = Element.ALIGN_CENTER

                    table.addCell(header1)
                    table.addCell(header2)

                    // Data rows
                    items.forEach { (item, scores) ->

                        val avg = if (scores.isNotEmpty()) scores.average() else 0.0
                        //totalResponses += scores.size
                        // ✅ count number of feedback documents (users)
                        totalResponses = documents.count { doc ->
                            val timestamp = doc.getTimestamp("timestamp")?.toDate() ?: return@count false
                            val dateKey = sdfDate.format(timestamp)
                            val dayName = sdfDay.format(timestamp)
                            val fullDateKey = "$dayName ($dateKey)"

                            val mealType = doc.getString("mealType") ?: return@count false

                            fullDateKey == day && mealType == meal
                        }

                        val cell1 = PdfPCell(Phrase(item, normalFont))
                        val cell2 = PdfPCell(Phrase("%.2f".format(avg), normalFont))

                        cell1.paddingLeft = 6f
                        cell2.paddingLeft = 6f

                        table.addCell(cell1)
                        table.addCell(cell2)
                    }

                    document.add(table)
                    document.add(Paragraph("Responses: $totalResponses\n", normalFont))
                }
            }

            document.close()

            Toast.makeText(requireContext(),
                "PDF saved:\n${file.absolutePath}",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "PDF error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
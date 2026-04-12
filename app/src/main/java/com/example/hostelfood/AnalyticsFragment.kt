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

class AnalyticsFragment : Fragment() {

    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    private var fromDate: Date? = null
    private var toDate: Date? = null

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
            val fromCal = Calendar.getInstance().apply { set(year, month, day, 0, 0, 0) }
            fromDate = fromCal.time

            DatePickerDialog(requireContext(), { _, y, m, d ->
                val toCal = Calendar.getInstance().apply { set(y, m, d, 23, 59, 59) }
                toDate = toCal.time

                val start = com.google.firebase.Timestamp(fromDate!!)
                val end = com.google.firebase.Timestamp(toDate!!)

                loadAnalyticsData(start, end)
                downloadAnalyticsAsPDF()   // Auto download after selecting range

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

    private fun downloadAnalyticsAsPDF() {
        if (fromDate == null || toDate == null) {
            Toast.makeText(requireContext(), "Please select date range first", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("feedbacks")
            .whereGreaterThanOrEqualTo("timestamp", com.google.firebase.Timestamp(fromDate!!))
            .whereLessThanOrEqualTo("timestamp", com.google.firebase.Timestamp(toDate!!))
            .get()
            .addOnSuccessListener { documents ->
                try {
                    val fileName = "Analytics_Report_${SimpleDateFormat("ddMMyyyy").format(Date())}.pdf"
                    val file = File(requireContext().getExternalFilesDir(null), fileName)

                    val document = Document()
                    PdfWriter.getInstance(document, FileOutputStream(file))
                    document.open()

                    document.add(Paragraph("HOSTEL FOOD ANALYTICS REPORT"))
                    document.add(Paragraph("From: ${SimpleDateFormat("dd MMM yyyy").format(fromDate!!)}"))
                    document.add(Paragraph("To: ${SimpleDateFormat("dd MMM yyyy").format(toDate!!)}\n\n"))

                    val mealStats = mutableMapOf<String, Pair<Int, Double>>()

                    for (doc in documents) {
                        val mealType = doc.getString("mealType") ?: continue
                        val ratings = doc.get("ratings") as? Map<String, String> ?: continue

                        var totalScore = 0
                        ratings.values.forEach { rating ->
                            totalScore += when (rating.lowercase()) {
                                "excellent" -> 4
                                "good" -> 3
                                "avg", "average" -> 2
                                "poor" -> 1
                                else -> 0
                            }
                        }
                        val avg = if (ratings.isNotEmpty()) totalScore.toDouble() / ratings.size else 0.0

                        val current = mealStats[mealType] ?: Pair(0, 0.0)
                        mealStats[mealType] = Pair(current.first + 1, current.second + avg)
                    }

                    mealStats.forEach { (meal, data) ->
                        val count = data.first
                        val avg = data.second / count
                        document.add(Paragraph("$meal"))
                        document.add(Paragraph("Responses: $count"))
                        document.add(Paragraph("Average Rating: %.2f / 4.0".format(avg)))
                        document.add(Paragraph("----------------------------------------"))
                    }

                    document.add(Paragraph("\nGenerated on: ${Date()}"))
                    document.close()

                    Toast.makeText(requireContext(), "✅ PDF Generated Successfully!\n${file.absolutePath}", Toast.LENGTH_LONG).show()

                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "PDF Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
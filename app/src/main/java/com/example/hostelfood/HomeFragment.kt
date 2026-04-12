package com.example.hostelfood

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.hostelfood.databinding.FragmentHomeBinding
import com.google.android.material.card.MaterialCardView
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private var rollNumber: String? = null
    private var currentActiveMeal: String = ""
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rollNumber = arguments?.getString("rollNumber")

        val today = LocalDate.now()
            .dayOfWeek
            .name
            .lowercase()
            .replaceFirstChar { it.uppercase() }
        binding.tvTodayDate.text = "Today's Menu - $today"

        loadTodayMenu()

        binding.btnGiveFeedback.setOnClickListener {
            if (currentActiveMeal.isNotEmpty()) {
                val intent = Intent(requireContext(), FeedbackActivity::class.java).apply {
                    putExtra("rollNumber", rollNumber)
                    putExtra("mealType", currentActiveMeal)
                    putExtra("day", binding.tvTodayDate.text.toString().replace("Today's Menu - ", ""))
                }
                startActivity(intent)
            } else {
                Toast.makeText(requireContext(), "No meal is active right now", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun highlight(card: MaterialCardView) {
        card.setCardBackgroundColor(
            ContextCompat.getColor(requireContext(), R.color.highlight)
        )
    }

    private fun resetCardColors() {
        val defaultColor = ContextCompat.getColor(requireContext(), R.color.card_default)

        binding.cardBreakfast.setCardBackgroundColor(defaultColor)
        binding.cardLunch.setCardBackgroundColor(defaultColor)
        binding.cardSnacks.setCardBackgroundColor(defaultColor)
        binding.cardDinner.setCardBackgroundColor(defaultColor)
    }
//change this
    @RequiresApi(Build.VERSION_CODES.O)
    private fun highlightCurrentMeal(
    bfStart: String, bfEnd: String,
    lnStart: String, lnEnd: String,
    snStart: String, snEnd: String,
    dnStart: String, dnEnd: String
) {
    val now = LocalTime.now()
    resetCardColors()

    // Check which meal is currently active
    when {
        isTimeInRange(now, bfStart, bfEnd) -> {
            highlight(binding.cardBreakfast)
            currentActiveMeal = "Breakfast"
            binding.btnGiveFeedback.isEnabled = true
            binding.btnGiveFeedback.text = "Give Feedback for Breakfast"
        }
        isTimeInRange(now, lnStart, lnEnd) -> {
            highlight(binding.cardLunch)
            currentActiveMeal = "Lunch"
            binding.btnGiveFeedback.isEnabled = true
            binding.btnGiveFeedback.text = "Give Feedback for Lunch"
        }
        isTimeInRange(now, snStart, snEnd) -> {
            highlight(binding.cardSnacks)
            currentActiveMeal = "Snacks"
            binding.btnGiveFeedback.isEnabled = true
            binding.btnGiveFeedback.text = "Give Feedback for Snacks"
        }
        isTimeInRange(now, dnStart, dnEnd) -> {
            highlight(binding.cardDinner)
            currentActiveMeal = "Dinner"
            binding.btnGiveFeedback.isEnabled = true
            binding.btnGiveFeedback.text = "Give Feedback for Dinner"
        }
        else -> {
            // No meal is active → Highlight the NEXT upcoming meal
            currentActiveMeal = ""
            binding.btnGiveFeedback.isEnabled = false
            binding.btnGiveFeedback.text = "Give Feedback for Current Meal"

            when {
                now.isBefore(LocalTime.parse(bfStart)) -> highlight(binding.cardBreakfast)
                now.isBefore(LocalTime.parse(lnStart)) -> highlight(binding.cardLunch)
                now.isBefore(LocalTime.parse(snStart)) -> highlight(binding.cardSnacks)
                else -> highlight(binding.cardDinner)   // After dinner → next is breakfast tomorrow
            }
        }
    }
}

    // Helper function
    @RequiresApi(Build.VERSION_CODES.O)
    private fun isTimeInRange(now: LocalTime, start: String, end: String): Boolean {
        return try {
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            val s = LocalTime.parse(start, formatter)
            val e = LocalTime.parse(end, formatter)
            now.isAfter(s) && now.isBefore(e)
        } catch (e: Exception) {
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadTodayMenu() {
        var bfStart = ""
        var bfEnd = ""
        var lnStart = ""
        var lnEnd = ""
        var snStart = ""
        var snEnd = ""
        var dnStart = ""
        var dnEnd = ""

        val currentDay = LocalDate.now()
            .dayOfWeek
            .name
            .lowercase()
            .replaceFirstChar { it.uppercase() }

        db.collection("menu")
            .whereEqualTo("day", currentDay)
            .addSnapshotListener { documents, error ->

                if (error != null) {
                    Toast.makeText(requireContext(), "Error loading data", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (documents == null || documents.isEmpty) {
                    Toast.makeText(requireContext(), "No menu for $currentDay", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                for (doc in documents) {

                    val mealType = doc.getString("mealType") ?: ""
                    val itemsList = doc.get("items") as? List<String> ?: emptyList()
                    val startTime = doc.getString("startTime") ?: ""
                    val endTime = doc.getString("endTime") ?: ""

                    val itemsText = itemsList.joinToString("\n") { "• $it" }
                    val timeText = "$startTime - $endTime"

                    when (mealType.lowercase()) {

                        "breakfast" -> {
                            bfStart = startTime
                            bfEnd = endTime
                            binding.tvBreakfastTime.text = timeText
                            binding.tvBreakfastItems.text = itemsText
                        }

                        "lunch" -> {
                            lnStart = startTime
                            lnEnd = endTime
                            binding.tvLunchTime.text = timeText
                            binding.tvLunchItems.text = itemsText
                        }

                        "snacks" -> {
                            snStart = startTime
                            snEnd = endTime
                            binding.tvSnacksTime.text = timeText
                            binding.tvSnacksItems.text = itemsText
                        }

                        "dinner" -> {
                            dnStart = startTime
                            dnEnd = endTime
                            binding.tvDinnerTime.text = timeText
                            binding.tvDinnerItems.text = itemsText
                        }
                    }
                }
                if (bfStart.isEmpty() || bfEnd.isEmpty() ||
                    lnStart.isEmpty() || lnEnd.isEmpty() ||
                    snStart.isEmpty() || snEnd.isEmpty() ||
                    dnStart.isEmpty() || dnEnd.isEmpty()
                ) {
                    return@addSnapshotListener
                }
                highlightCurrentMeal(
                    bfStart, bfEnd,
                    lnStart, lnEnd,
                    snStart, snEnd,
                    dnStart, dnEnd
                )
            }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
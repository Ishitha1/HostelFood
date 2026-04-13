package com.example.hostelfood

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.hostelfood.databinding.FragmentWeekMenuBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate

class WeekMenuFragment : Fragment() {

    private var _binding: FragmentWeekMenuBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWeekMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        generateNext7DaysChips()
    }

    private fun generateNext7DaysChips() {
        val linearDates = binding.linearDates
        linearDates.removeAllViews()

        val today = LocalDate.now()

        for (i in 0..6) {
            val date = today.plusDays(i.toLong())
            val dayShort = date.dayOfWeek.toString().take(3)
            val dayNum = date.dayOfMonth
            val fullDayName = date.dayOfWeek.name.lowercase()
                .replaceFirstChar { it.uppercase() }
            val chip = layoutInflater.inflate(R.layout.item_date_chip, linearDates, false) as TextView
            chip.text = "$dayShort\n$dayNum"

            chip.setOnClickListener {
                highlightSelected(chip)
                loadMenuForDate(fullDayName, date)
            }

            if (i == 0) {
                highlightSelected(chip)
                loadMenuForDate(fullDayName, date)
            }

            linearDates.addView(chip)
        }
    }

    private fun highlightSelected(view: View) {
        val parent = binding.linearDates

        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i) as TextView

            // Reset background
            child.setBackgroundResource(R.drawable.date_chip_background)

            // Set default text color (dark)
            child.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
        }

        val selected = view as TextView

        // Apply selected background
        selected.setBackgroundResource(R.drawable.date_chip_background_selected)

        // Set selected text color (white)
        selected.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.system_text_primary_inverse_dark))
    }

    private fun loadMenuForDate(dayName: String, date: LocalDate) {
        binding.tvSelectedDay.text =
            "$dayName, ${date.dayOfMonth} ${date.month.name.lowercase().replaceFirstChar { it.uppercase() }}"
        // Clear previous
        binding.tvBreakfast.text = "Loading..."
        binding.tvLunch.text = "Loading..."
        binding.tvSnacks.text = "Loading..."
        binding.tvDinner.text = "Loading..."

        db.collection("menu")
            .whereEqualTo("day", dayName)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    binding.tvBreakfast.text = "No menu found for $dayName"
                    binding.tvLunch.text = "-"
                    binding.tvSnacks.text = "-"
                    binding.tvDinner.text = "-"
                    return@addOnSuccessListener
                }

                for (doc in documents) {
                    val mealType = doc.getString("mealType") ?: ""
                    val itemsList = doc.get("items") as? List<String> ?: emptyList()
                    val itemsText = if (itemsList.isNotEmpty()) {
                        itemsList.joinToString("\n") { "• $it" }
                    } else {
                        "No items"
                    }
                    when (mealType.lowercase()) {
                        "breakfast" -> binding.tvBreakfast.text = itemsText.ifEmpty { "No items" }
                        "lunch" -> binding.tvLunch.text = itemsText.ifEmpty { "No items" }
                        "snacks" -> binding.tvSnacks.text = itemsText.ifEmpty { "No items" }
                        "dinner" -> binding.tvDinner.text = itemsText.ifEmpty { "No items" }
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Firebase Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
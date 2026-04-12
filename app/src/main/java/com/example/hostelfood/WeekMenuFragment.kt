package com.example.hostelfood

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hostelfood.databinding.FragmentWeekMenuBinding
import com.google.firebase.firestore.FirebaseFirestore

class WeekMenuFragment : Fragment() {

    private var _binding: FragmentWeekMenuBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWeekMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvWeekDays.layoutManager = LinearLayoutManager(requireContext())
        loadWeekMenu()
    }

    private fun loadWeekMenu() {
        val days = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

        db.collection("menu")
            .get()
            .addOnSuccessListener { documents ->
                val grouped = documents.groupBy { it.getString("day") ?: "" }

                val dayList = days.map { day ->
                    val meals = grouped[day]?.map { doc ->
                        val mealType = doc.getString("mealType") ?: ""
                        val items = (doc.get("items") as? List<String>)?.joinToString(", ") ?: ""
                        "$mealType: $items"
                    } ?: listOf("No menu added")
                    DayMenu(day, meals)
                }

                binding.rvWeekDays.adapter = WeekMenuAdapter(dayList) { selectedDay ->
                    Toast.makeText(requireContext(), "Showing full menu for $selectedDay", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load week menu", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
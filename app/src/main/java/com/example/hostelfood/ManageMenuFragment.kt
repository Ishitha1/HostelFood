package com.example.hostelfood

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hostelfood.databinding.FragmentManageMenuBinding
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class ManageMenuFragment : Fragment() {

    private var _binding: FragmentManageMenuBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    private var currentItems = mutableListOf<String>()
    private lateinit var adapter: MenuItemsAdapter
    private var currentDocId = ""
    private var currentMode = "view" // view, add, update, delete

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentManageMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinners()
        setupRecyclerView()

        binding.btnAddMode.setOnClickListener { switchToAddMode() }
        binding.btnUpdateMode.setOnClickListener { switchToUpdateMode() }
        binding.btnDeleteMode.setOnClickListener { switchToDeleteMode() }

        binding.spinnerDay.onItemSelectedListener = spinnerListener
        binding.spinnerMealType.onItemSelectedListener = spinnerListener
        binding.dynamicActionArea.visibility = View.GONE
    }

    private fun setupSpinners() {
        val days = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val meals = listOf("Breakfast", "Lunch", "Snacks", "Dinner")

        binding.spinnerDay.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, days)
        binding.spinnerMealType.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, meals)
    }

    private fun setupRecyclerView() {
        adapter = MenuItemsAdapter(currentItems) { item ->
            if (currentMode == "delete") {
                deleteItem(item)
            } else if (currentMode == "update") {
                startUpdateItem(item)
            }
        }
        binding.rvCurrentItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCurrentItems.adapter = adapter
    }

    private val spinnerListener = object : android.widget.AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
            loadCurrentMenu()
        }
        override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
    }

    private fun generateDocId(day: String, meal: String): String {
        val dayShort = day.take(3).lowercase()

        val mealShort = when (meal.lowercase()) {
            "breakfast" -> "bf"
            "lunch" -> "lunch"
            "snacks" -> "snacks"
            "dinner" -> "dinner"
            else -> ""
        }

        return "${dayShort}_${mealShort}"
    }
    private fun loadCurrentMenu() {
        val day = binding.spinnerDay.selectedItem.toString()
        val mealType = binding.spinnerMealType.selectedItem.toString()
        currentDocId = generateDocId(day, mealType)

        db.collection("menu").document(currentDocId)
            .get()
            .addOnSuccessListener { doc ->
                currentItems.clear()
                currentItems.addAll(doc.get("items") as? List<String> ?: emptyList())
                val start = doc.getString("startTime") ?: "--:--"
                val end = doc.getString("endTime") ?: "--:--"
                binding.tvTimings.text = "$start - $end"
                adapter.notifyDataSetChanged()
            }
    }

    // ==================== MODE SWITCHING ====================

    private fun switchToAddMode() {
        currentMode = "add"
        binding.dynamicActionArea.removeAllViews()
        val view = layoutInflater.inflate(R.layout.item_add_form, binding.dynamicActionArea, false)
        binding.dynamicActionArea.addView(view)
        binding.dynamicActionArea.visibility = View.VISIBLE

        view.findViewById<Button>(R.id.btnAddNewItem).setOnClickListener {
            val newItem = view.findViewById<EditText>(R.id.etNewItem).text.toString().trim()
            if (newItem.isNotEmpty()) {
                addItemToMenu(newItem)
            }
        }

        view.findViewById<Button>(R.id.btnCancelAdd).setOnClickListener {
            binding.dynamicActionArea.visibility = View.GONE
            currentMode = "view"
        }
    }

    private fun updateMealTime(start: String, end: String) {
        if (currentDocId.isEmpty()) return

        db.collection("menu").document(currentDocId)
            .update(
                mapOf(
                    "startTime" to start,
                    "endTime" to end
                )
            )
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Time updated!", Toast.LENGTH_SHORT).show()
                loadCurrentMenu()
                binding.dynamicActionArea.visibility = View.GONE
                currentMode = "view"
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to update", Toast.LENGTH_SHORT).show()
            }
    }
    private fun switchToUpdateMode() {
        currentMode = "update"

        binding.dynamicActionArea.removeAllViews()
        val view = layoutInflater.inflate(R.layout.item_update_time, binding.dynamicActionArea, false)
        binding.dynamicActionArea.addView(view)
        binding.dynamicActionArea.visibility = View.VISIBLE

        val etStart = view.findViewById<EditText>(R.id.etUpdateStart)
        val etEnd = view.findViewById<EditText>(R.id.etUpdateEnd)

        // Optional: Pre-fill current timings
        val currentTime = binding.tvTimings.text.toString().split(" - ")
        if (currentTime.size == 2) {
            etStart.setText(currentTime[0])
            etEnd.setText(currentTime[1])
        }

        view.findViewById<Button>(R.id.btnSaveUpdate).setOnClickListener {
            val newStart = etStart.text.toString().trim()
            val newEnd = etEnd.text.toString().trim()

            if (newStart.isEmpty() || newEnd.isEmpty()) {
                Toast.makeText(requireContext(), "Enter both times", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            updateMealTime(newStart, newEnd)
        }

        view.findViewById<Button>(R.id.btnCancelUpdate).setOnClickListener {
            binding.dynamicActionArea.visibility = View.GONE
            currentMode = "view"
        }
    }

    private fun switchToDeleteMode() {
        currentMode = "delete"
        Toast.makeText(requireContext(), "Tap on an item from list to delete", Toast.LENGTH_LONG).show()
    }

    private fun addItemToMenu(newItem: String) {
        if (currentDocId.isEmpty()) return

        db.collection("menu").document(currentDocId)
            .set(
                mapOf("items" to FieldValue.arrayUnion(newItem)),
                com.google.firebase.firestore.SetOptions.merge()
            )
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Item added!", Toast.LENGTH_SHORT).show()
                loadCurrentMenu()
                binding.dynamicActionArea.visibility = View.GONE
                currentMode = "view"
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun startUpdateItem(oldItem: String) {
        // You can implement a dialog for new name if needed
        Toast.makeText(requireContext(), "Update feature - coming in next update", Toast.LENGTH_SHORT).show()
    }

    private fun deleteItem(item: String) {
        if (currentDocId.isEmpty()) return

        db.collection("menu").document(currentDocId)
            .update("items", FieldValue.arrayRemove(item))
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Item deleted!", Toast.LENGTH_SHORT).show()
                loadCurrentMenu()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
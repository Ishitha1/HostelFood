package com.example.hostelfood

import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.hostelfood.databinding.ActivityFeedbackBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate

class FeedbackActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFeedbackBinding
    private val db = FirebaseFirestore.getInstance()

    private var currentMealType: String = ""
    private var currentDay: String = ""
    private var userRollNumber: String = "Unknown"

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedbackBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Receive data from HomeFragment
        userRollNumber = intent.getStringExtra("rollNumber") ?: "Unknown"
        currentMealType = intent.getStringExtra("mealType") ?: ""
        currentDay = intent.getStringExtra("day") ?: LocalDate.now().dayOfWeek.toString()

        if (currentMealType.isEmpty()) {
            binding.tvMealTitle.text = "No active meal"
            binding.btnSubmitFeedback.isEnabled = false
        } else {
            binding.tvMealTitle.text = "$currentMealType - $currentDay (Active Now)"
            binding.btnSubmitFeedback.isEnabled = true
            loadItemsForMeal()
        }

        binding.btnSubmitFeedback.setOnClickListener {
            submitFeedback()
        }
    }

    private fun loadItemsForMeal() {
        db.collection("menu")
            .whereEqualTo("day", currentDay)
            .whereEqualTo("mealType", currentMealType)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val doc = documents.documents[0]
                    val itemsList = doc.get("items") as? List<String> ?: emptyList()
                    createDynamicRatingFields(itemsList)
                }
            }
    }

    private fun createDynamicRatingFields(items: List<String>) {
        binding.linearRatings.removeAllViews()
        for (item in items) {
            val row = layoutInflater.inflate(R.layout.item_rating_row, binding.linearRatings, false)
            val tvName = row.findViewById<TextView>(R.id.tvItemName)
            val rg = row.findViewById<RadioGroup>(R.id.rgRating)
            tvName.text = item
            rg.tag = item
            binding.linearRatings.addView(row)
        }
    }

    private fun submitFeedback() {
        val comment = binding.etComment.text.toString().trim()
        val ratingsMap = hashMapOf<String, String>()

        // Collect all ratings
        for (i in 0 until binding.linearRatings.childCount) {
            val row = binding.linearRatings.getChildAt(i)
            val rg = row.findViewById<RadioGroup>(R.id.rgRating)
            val itemName = rg.tag as String

            val selectedId = rg.checkedRadioButtonId
            if (selectedId != -1) {
                val selectedRating = findViewById<RadioButton>(selectedId).text.toString()
                ratingsMap[itemName] = selectedRating
            } else {
                Toast.makeText(this, "Please rate all items", Toast.LENGTH_SHORT).show()
                return
            }
        }

        if (currentMealType.isEmpty()) {
            Toast.makeText(this, "No active meal found", Toast.LENGTH_SHORT).show()
            return
        }

        val feedbackData = hashMapOf(
            "rollNumber" to userRollNumber,
            "mealType" to currentMealType,
            "day" to currentDay,
            "timestamp" to com.google.firebase.Timestamp.now(),
            "ratings" to ratingsMap,
            "comment" to comment
        )

        db.collection("feedbacks")
            .add(feedbackData)
            .addOnSuccessListener {
                Toast.makeText(this, "Feedback for $currentMealType submitted!", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to submit", Toast.LENGTH_SHORT).show()
            }
    }
}
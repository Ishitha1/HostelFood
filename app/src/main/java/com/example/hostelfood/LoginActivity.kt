package com.example.hostelfood

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.hostelfood.databinding.ActivityLoginBinding
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {

            val rollNo = binding.etRollNumber.text.toString().trim()
            val enteredPassword = binding.etPassword.text.toString().trim()

            if (rollNo.isEmpty() || enteredPassword.isEmpty()) {
                Toast.makeText(this, "Enter all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            db.collection("users").document(rollNo)
                .get()
                .addOnSuccessListener { document ->

                    if (document.exists()) {

                        val name = document.getString("name") ?: "Student"
                        val role = document.getString("role") ?: "student"

                        // 🔥 Expected Password = RollNo + Name
                        val expectedPassword = rollNo + name

                        if (!enteredPassword.equals(expectedPassword, ignoreCase = true)) {
                            Toast.makeText(this, "Invalid Credentials", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        Toast.makeText(this, "Welcome, $name!", Toast.LENGTH_SHORT).show()

                        val intent = if (role == "admin") {
                            Intent(this, AdminDashboardActivity::class.java)
                        } else {
                            Intent(this, MainActivity::class.java).apply {
                                putExtra("rollNumber", rollNo)
                                putExtra("name", name)
                                putExtra("role", role)
                            }
                        }

                        startActivity(intent)
                        finish()

                    } else {
                        Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Connection Error", Toast.LENGTH_SHORT).show()
                }
        }

        binding.tvAdminLogin.setOnClickListener {

            // Clear text
            binding.etRollNumber.setText("")
            binding.etPassword.setText("")

            // ✅ Change hints properly
            binding.tilRollNumber.hint = "Admin"
            binding.tilPassword.hint = "Password"

            // Optional: prefill admin
            binding.etRollNumber.setText("admin")
        }
    }
}
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
                        //val rollNo = document.id   // Important: document ID is the roll number

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

                        // Pass user data
                        /*intent.putExtra("rollNumber", rollNo)
                        intent.putExtra("name", name)
                        intent.putExtra("role", role)*/

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
            binding.etRollNumber.setText("admin")
            binding.etPassword.setText("adminAdmin") // admin + admin
        }
    }
}
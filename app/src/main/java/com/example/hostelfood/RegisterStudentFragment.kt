package com.example.hostelfood

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.hostelfood.databinding.FragmentRegisterStudentBinding
import com.google.firebase.firestore.FirebaseFirestore

class RegisterStudentFragment : Fragment() {

    private var _binding: FragmentRegisterStudentBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterStudentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        binding.btnRegisterStudent.setOnClickListener {
            val roll = binding.etRollNumber.text.toString().trim()
            val name = binding.etName.text.toString().trim()

            if (roll.isEmpty() || name.isEmpty()) {
                Toast.makeText(requireContext(), "Enter all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userData = hashMapOf(
                "name" to name,
                "role" to "student"
            )

            // Check if already exists
            db.collection("users").document(roll)
                .get()
                .addOnSuccessListener { doc ->

                    if (doc.exists()) {
                        // Already exists
                        Toast.makeText(requireContext(), "User already exists!", Toast.LENGTH_SHORT).show()

                    } else {
                        //Create new user
                        db.collection("users").document(roll)
                            .set(userData)
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "Student Registered!", Toast.LENGTH_SHORT).show()
                                binding.etRollNumber.text?.clear()
                                binding.etName.text?.clear()
                            }
                            .addOnFailureListener {
                                Toast.makeText(requireContext(), "Failed to register", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Error checking user", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
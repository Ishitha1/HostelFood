package com.example.hostelfood

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.hostelfood.databinding.ActivityAdminDashboardBinding

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.admin_fragment_container, AdminHomeFragment())
                .commit()
        }

        binding.bottomNavigationAdmin.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_admin_home -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.admin_fragment_container, AdminHomeFragment())
                        .commit()
                    true
                }
                R.id.nav_admin_manage -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.admin_fragment_container, ManageMenuFragment())
                        .commit()
                    true
                }
                R.id.nav_admin_feedback -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.admin_fragment_container, ViewFeedbackFragment())
                        .commit()
                    true
                }
                R.id.nav_admin_analytics -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.admin_fragment_container, AnalyticsFragment())
                        .commit()
                    true
                }

                R.id.nav_admin_register -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.admin_fragment_container, RegisterStudentFragment())
                        .commit()
                    true
                }
                else -> false
            }
        }
    }
}
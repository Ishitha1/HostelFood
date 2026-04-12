package com.example.hostelfood

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.hostelfood.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var rollNumber: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        rollNumber = intent.getStringExtra("rollNumber")
        //Load Home Fragment
        if (savedInstanceState == null) {
            val homeFragment = HomeFragment()
            val bundle = Bundle()
            bundle.putString("rollNumber", rollNumber)
            homeFragment.arguments = bundle

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, homeFragment)
                .commit()
        }

        // Bottom Navigation
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_today -> {
                    val homeFragment = HomeFragment()
                    val bundle = Bundle()
                    bundle.putString("rollNumber", rollNumber)
                    homeFragment.arguments = bundle

                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, homeFragment)
                        .commit()
                    true
                }
                R.id.nav_week -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, WeekMenuFragment())   // ← Use Fragment
                        .commit()
                    true
                }
                else -> false
            }
        }

    }
    private fun loadHomeFragment(rollNumber: String?) {
        val fragment = HomeFragment()
        val bundle = Bundle()
        bundle.putString("rollNumber", rollNumber)
        fragment.arguments = bundle

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
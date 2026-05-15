package com.example.hs_spacelink

import android.os.Bundle
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.example.hs_spacelink.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        replaceFragment(AvailableFragment())

        // 툴바 설정
        setSupportActionBar(binding.topAppBar)

        // 메뉴 연결
        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.topAppBar,
            R.string.open_drawer,
            R.string.close_drawer
        )

        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // 메뉴 클릭 이벤트
        binding.navigationView.setNavigationItemSelectedListener {

            when (it.itemId) {

                R.id.menu_available -> {
                    supportActionBar?.title = "예약 가능 조회"
                    replaceFragment(AvailableFragment())
                }

                R.id.menu_status -> {
                    supportActionBar?.title = "예약 현황"
                    replaceFragment(StatusFragment())
                }

                R.id.menu_setting -> {
                    supportActionBar?.title = "설정"
                    replaceFragment(SettingFragment())
                }
            }

            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }
    private fun replaceFragment(fragment: Fragment) {

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
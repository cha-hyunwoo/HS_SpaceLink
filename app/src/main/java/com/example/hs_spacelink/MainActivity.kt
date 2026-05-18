package com.example.hs_spacelink

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.View
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

        // 최상단 배터리/시계 상태바까지 딥 네이비로 강제 도색 (프로급 퀄리티 마감)
        window.statusBarColor = android.graphics.Color.parseColor("#0F1E36")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 네이비 배경이므로 상단 아이콘들을 흰색조로 유지
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        }

        replaceFragment(AvailableFragment())

        // 툴바 설정 및 제안서 시안 색상 매칭
        setSupportActionBar(binding.topAppBar)
        binding.topAppBar.setBackgroundColor(android.graphics.Color.parseColor("#0F1E36")) // 딥 네이비
        binding.topAppBar.setTitleTextColor(android.graphics.Color.WHITE) // 화이트 타이틀

        // 내비게이션 드로어 삼선 아이콘 화이트 고정 규칙 연동
        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.topAppBar,
            R.string.open_drawer,
            R.string.close_drawer
        )
        toggle.drawerArrowDrawable.color = android.graphics.Color.WHITE
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
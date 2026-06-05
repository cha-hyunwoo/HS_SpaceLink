package com.example.hs_spacelink

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import com.example.hs_spacelink.StatusFragment.Companion.trackingNotificationList
import com.example.hs_spacelink.StatusFragment.Companion.canceledRoomName
import com.example.hs_spacelink.StatusFragment.Companion.canceledTimeRange

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle
    // ★ [전역 변수 승격] onNewIntent에서도 접근할 수 있도록 전역으로 선언합니다.
    private lateinit var navigationView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        // 다크모드 방지 및 라이트모드 강제 고정
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 안드로이드 13(API 33) 이상 런타임 푸시 알림 권한 유도
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        drawerLayout = findViewById(R.id.drawerLayout)
        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        navigationView = findViewById<NavigationView>(R.id.navigationView)

        // 상단바 겹침 디바이스 대응 패딩/마진 바인딩
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val params = view.layoutParams as android.view.ViewGroup.MarginLayoutParams
            params.topMargin = systemBars.top
            view.layoutParams = params
            insets
        }

        setSupportActionBar(toolbar)

        toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, AvailableFragment())
                .commit()
            supportActionBar?.title = "예약 가능 조회"
        }

        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_search -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, AvailableFragment())
                        .commit()
                    supportActionBar?.title = "예약 가능 조회"
                }
                R.id.nav_rules -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, RuleFragment())
                        .commit()
                    supportActionBar?.title = "시설 이용 수칙"
                }
                R.id.nav_status -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, StatusFragment())
                        .commit()
                    supportActionBar?.title = "예약 현황"
                }
                R.id.nav_map -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, MapFragment.newInstance())
                        .commit()
                    supportActionBar?.title = "캠퍼스 지도"
                }
                R.id.nav_settings -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, SettingFragment())
                        .commit()
                    supportActionBar?.title = "설정"
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    // ★ [알림 터치 시 다른 탭에서 예약 현황 탭으로 자동 이동 및 드로어 메뉴 동기화 처리]
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // 새로 들어온 인텐트를 기존 인텐트 프레임워크로 교체

        // 1. 화면의 프래그먼트를 예약 현황(StatusFragment)으로 강제 전환 및 상단 타이틀 매핑
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, StatusFragment())
            .commit()
        supportActionBar?.title = "예약 현황"

        // 2. 좌측 내비게이션 드로어 메뉴의 포커스 체크 상태도 '예약 현황' 항목으로 안전하게 동기화
        if (::navigationView.isInitialized) {
            navigationView.setCheckedItem(R.id.nav_status)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (trackingNotificationList.isNotEmpty()) {
                val targetItem = trackingNotificationList[0]

                // 방 이름 추출 파트
                try {
                    val dateEndIndex = targetItem.indexOf("] ")
                    val timeStartIndex = targetItem.lastIndexOf(" (")
                    canceledRoomName = if (dateEndIndex != -1 && timeStartIndex != -1 && dateEndIndex < timeStartIndex) {
                        targetItem.substring(dateEndIndex + 2, timeStartIndex).trim()
                    } else {
                        when {
                            targetItem.contains("그룹스터디실") -> "그룹스터디실(6F)"
                            targetItem.contains("코딩라운지") -> "코딩라운지 세미나실"
                            else -> "상상파크 플러스"
                        }
                    }
                } catch (e: Exception) {
                    canceledRoomName = "그룹스터디실(6F)"
                }

                // 시간대 추출 파트
                var extractedTime = "신청 시간대"
                try {
                    val openBracketIndex = targetItem.lastIndexOf("(")
                    val closeBracketIndex = targetItem.lastIndexOf(")")
                    if (openBracketIndex != -1 && closeBracketIndex != -1 && openBracketIndex < closeBracketIndex) {
                        val pureTime = targetItem.substring(openBracketIndex + 1, closeBracketIndex)
                        if (pureTime.contains("~")) {
                            extractedTime = pureTime.trim()
                        }
                    }
                } catch (e: Exception) {
                    extractedTime = "지정 시간"
                }

                canceledTimeRange = extractedTime
                trackingNotificationList.removeAt(0)

                // 백그라운드 서비스 기동 및 안전 인텐트 규격 전송
                val serviceIntent = Intent(this, RoomTrackingService::class.java).apply {
                    putExtra("ROOM_NAME", canceledRoomName)
                    putExtra("TIME_RANGE", extractedTime)
                    putExtra("START", extractedTime.split("~").getOrNull(0) ?: "14:00")
                    putExtra("END", extractedTime.split("~").getOrNull(1) ?: "15:00")
                    putExtra("FORCE_CANCEL_SIGNAL", true)
                }
                startService(serviceIntent)
            } else {
                Toast.makeText(this, "예약 현황 탭에서 [취소 알림 신청]을 먼저 완료해 주세요.", Toast.LENGTH_SHORT).show()
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
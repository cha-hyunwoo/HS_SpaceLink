package com.example.hs_spacelink

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        // ★ [다크 모드 강제 차단] 핸드폰 설정 상태와 무관하게 앱 전체를 무조건 라이트 모드로 강력 고정
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // [팩트 세팅] 현우님의 activity_main.xml 고유 ID 명세와 완벽 동기화
        drawerLayout = findViewById(R.id.drawerLayout)
        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        val navigationView = findViewById<NavigationView>(R.id.navigationView)

        // 액션바 역할을 대신 수행할 커스텀 툴바 바인딩
        setSupportActionBar(toolbar)

        // 1. 최신 안드로이드 컴파일러 호환 패키지 기반 토글 엔진 가동
        toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // 2. 최신 안드로이드 생명주기(API 33+) 대응 완벽 뒤로가기 콜백 (onBackPressed 빨간 줄 무조건 클리어)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    // 왼쪽 드로어 탭이 닫혀있다면 정상적으로 운영체제 시스템 백그라운드로 이탈
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        // 앱 처음 진입 시 프래그먼트 공간(fragmentContainer)에 AvailableFragment 우선 매핑
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, AvailableFragment())
                .commit()
            supportActionBar?.title = "예약 가능 조회"
        }

        // 왼쪽 드로어 탭 메뉴 아이템 클릭 리스너 체계 바인딩
        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_search -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, AvailableFragment())
                        .commit()
                    supportActionBar?.title = "예약 가능 조회"
                }
                R.id.nav_rules -> {
                    // 새롭게 정립한 스터디룸 규정 안내 화면 로드
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, RuleFragment())
                        .commit()
                    supportActionBar?.title = "시설 이용 수칙"
                }
                R.id.nav_status -> {
                    // 기존 StatusFragment 연동 파트 바인딩 완료
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, StatusFragment())
                        .commit()
                    supportActionBar?.title = "예약 현황"
                }
                // ★ [연동 규격 동기화] 드로어 메뉴에서 지도 아이템 선택 시 동적 SVG 맵 프래그먼트로 전환 ★
                R.id.nav_map -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, MapFragment.newInstance())
                        .commit()
                    supportActionBar?.title = "캠퍼스 지도"
                }
                R.id.nav_settings -> {
                    // 기존 SettingFragment 연동 파트 바인딩 완료
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, SettingFragment())
                        .commit()
                    supportActionBar?.title = "설정"
                }
            }

            // 아이템 클릭 타겟팅이 끝나면 드로어 탭 자동 폴딩
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
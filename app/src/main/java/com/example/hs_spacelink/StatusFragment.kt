package com.example.hs_spacelink

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.*

class StatusFragment : Fragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var calendarView: CalendarView
    private lateinit var resultContainer: LinearLayout
    private lateinit var textTitle: TextView

    private var selectedDateString = ""
    private var currentUrl = "https://www.hansung.ac.kr/hsel/2153/subview.do"

    private val debounceHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var debounceRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_status, container, false)

        tabLayout = view.findViewById(R.id.tabLayoutStatus)
        calendarView = view.findViewById(R.id.calendarViewStatus)
        resultContainer = view.findViewById(R.id.statusResultContainer)
        textTitle = view.findViewById(R.id.textStatusTitle)

        tabLayout.setBackgroundColor(android.graphics.Color.parseColor("#FFFFFF"))
        tabLayout.setTabTextColors(
            android.graphics.Color.parseColor("#94A3B8"),
            android.graphics.Color.parseColor("#0F1E36")
        )
        tabLayout.setSelectedTabIndicatorColor(android.graphics.Color.parseColor("#0F1E36"))

        // calendarView.date 믿지 않고 직접 오늘 날짜로 초기화
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.KOREAN)
        selectedDateString = sdf.format(Date())
        updateTitleText()

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentUrl = when (tab?.position) {
                    0 -> "https://www.hansung.ac.kr/hsel/2153/subview.do"
                    1 -> "https://www.hansung.ac.kr/cncschool/4182/subview.do"
                    else -> "https://www.hansung.ac.kr/cncschool/4181/subview.do"
                }
                updateTitleText()
                triggerDebouncedSearch()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // 유저가 실제로 탭한 날짜만 신뢰
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDateString = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            updateTitleText()
            triggerDebouncedSearch()
        }

        loadReservationStatus()
        return view
    }

    private fun triggerDebouncedSearch() {
        debounceRunnable?.let { debounceHandler.removeCallbacks(it) }
        debounceRunnable = Runnable { loadReservationStatus() }
        debounceHandler.postDelayed(debounceRunnable!!, 300)
    }

    private fun updateTitleText() {
        val facilityName = when (tabLayout.selectedTabPosition) {
            0 -> "학술정보관"
            1 -> "코딩라운지"
            else -> "상상파크 플러스"
        }
        textTitle.text = "📊 $selectedDateString [$facilityName] 실시간 현황"
        textTitle.setTextColor(android.graphics.Color.parseColor("#0F1E36"))
    }

    private fun loadReservationStatus() {
        Thread {
            try {
                val dateParts = selectedDateString.split("-")
                if (dateParts.size < 3) return@Thread

                val selectedYear = dateParts[0]
                val selectedMonth = dateParts[1].toInt().toString()
                val selectedDay = dateParts[2].toInt().toString()
                val formattedMonth = dateParts[1] // "06" 형태 유지

                val document = when (tabLayout.selectedTabPosition) {

                    // ── 학술정보관: 쿠키 불필요, 기존 방식 유지
                    0 -> {
                        val url = "$currentUrl?year=$selectedYear&month=$selectedMonth"
                        Log.d("STATUS_DEBUG", "[학술정보관] 요청 URL: $url")
                        Jsoup.connect(url)
                            .userAgent("Mozilla/5.0")
                            .get()
                    }

                    // ── 코딩라운지 / 상상파크: 쿠키 세션 먼저 획득 후 요청
                    else -> {
                        val baseUrl = currentUrl
                        val targetUrl = "$baseUrl?viewType=m&rentDate=$selectedYear-$formattedMonth-01"
                        Log.d("STATUS_DEBUG", "[코딩라운지/상상파크] 베이스 URL 쿠키 획득: $baseUrl")
                        Log.d("STATUS_DEBUG", "[코딩라운지/상상파크] 실제 요청 URL: $targetUrl")

                        // 1단계: 메인 페이지 GET으로 세션 쿠키 획득
                        val cookieResponse = Jsoup.connect(baseUrl)
                            .userAgent("Mozilla/5.0")
                            .execute()
                        val cookies = cookieResponse.cookies()
                        Log.d("STATUS_DEBUG", "획득한 쿠키: $cookies")

                        // 2단계: 쿠키 들고 원하는 달 요청
                        Jsoup.connect(targetUrl)
                            .userAgent("Mozilla/5.0")
                            .cookies(cookies)
                            .get()
                    }
                }

                // ── 날짜 셀 매칭
                val dayCell = document.select("td").firstOrNull { cell ->
                    val daySpan = cell.selectFirst("span")
                    if (daySpan != null) {
                        val pureSpanText = daySpan.text().replace(".", "").trim()
                        pureSpanText == selectedDay
                    } else false
                }

                Log.d("STATUS_DEBUG", "dayCell: ${if (dayCell != null) "✅ 찾음" else "❌ null"}")

                val statusList = mutableListOf<Pair<String, String>>()

                dayCell?.select("div.conBox")?.forEach { box ->
                    val lines = box.html().split("<br>")
                    if (lines.size >= 2) {
                        val roomName = Jsoup.parse(lines[0]).text().trim()
                        val reservedTime = Jsoup.parse(lines[1]).text().trim()
                        Log.d("STATUS_DEBUG", "roomName='$roomName' time='$reservedTime'")
                        if (reservedTime.contains("~")) {
                            statusList.add(Pair(roomName, reservedTime))
                        }
                    }
                }

                if (!isAdded) return@Thread

                requireActivity().runOnUiThread {
                    resultContainer.removeAllViews()

                    if (statusList.isEmpty()) {
                        val emptyText = TextView(requireContext()).apply {
                            text = "✨\n\n해당 날짜는 비어있는 클린 데이입니다!\n원하는 시간을 선점하기 아주 좋은 타이밍이네요."
                            textSize = 15f
                            setTextColor(android.graphics.Color.parseColor("#475569"))
                            setPadding(40, 100, 40, 40)
                            gravity = Gravity.CENTER
                        }
                        resultContainer.addView(emptyText)
                    } else {
                        statusList.forEach { (room, time) ->
                            addStatusCard(resultContainer, room, time)
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("STATUS_ERROR", "❌ ${e.javaClass.simpleName}: ${e.message}")
                Log.e("STATUS_ERROR", e.stackTraceToString())
            }
        }.start()
    }

    private fun addStatusCard(container: LinearLayout, roomName: String, timeRange: String) {
        val card = com.google.android.material.card.MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, 24) }
            radius = 20f
            cardElevation = 0f
            strokeWidth = 2
            strokeColor = android.graphics.Color.parseColor("#E2E8F0")
            setCardBackgroundColor(android.graphics.Color.parseColor("#FFFFFF"))
        }

        val inner = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        val nameText = TextView(requireContext()).apply {
            text = roomName
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#0F1E36"))
        }

        val badgeLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20, 10, 20, 10)
            layoutParams = LinearLayout.LayoutParams(-2, -2).apply { setMargins(0, 14, 0, 0) }
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.parseColor("#EEF2F6"))
                cornerRadius = 10f
            }
        }

        val statusText = TextView(requireContext()).apply {
            text = "🔒 예약 선점  |  $timeRange"
            textSize = 12f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#1E3A8A"))
        }

        badgeLayout.addView(statusText)
        inner.addView(nameText)
        inner.addView(badgeLayout)
        card.addView(inner)
        container.addView(card)
    }

    override fun onDestroyView() {
        debounceRunnable?.let { debounceHandler.removeCallbacks(it) }
        super.onDestroyView()
    }
}
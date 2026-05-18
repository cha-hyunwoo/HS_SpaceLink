package com.example.hs_spacelink

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_status, container, false)

        tabLayout = view.findViewById(R.id.tabLayoutStatus)
        calendarView = view.findViewById(R.id.calendarViewStatus)
        resultContainer = view.findViewById(R.id.statusResultContainer)
        textTitle = view.findViewById(R.id.textStatusTitle)

        // 탭바 스타일 시안과 100% 매칭
        tabLayout.setBackgroundColor(android.graphics.Color.parseColor("#0F1E36"))
        tabLayout.setTabTextColors(android.graphics.Color.parseColor("#94A3B8"), android.graphics.Color.parseColor("#FFFFFF"))
        tabLayout.setSelectedTabIndicatorColor(android.graphics.Color.parseColor("#1E3A8A"))

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.KOREAN)
        selectedDateString = sdf.format(Date(calendarView.date))
        updateTitleText()

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentUrl = when (tab?.position) {
                    0 -> "https://www.hansung.ac.kr/hsel/2153/subview.do"
                    1 -> "https://www.hansung.ac.kr/cncschool/4182/subview.do"
                    else -> "https://www.hansung.ac.kr/cncschool/4181/subview.do"
                }
                loadReservationStatus()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDateString = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            updateTitleText()
            loadReservationStatus()
        }

        loadReservationStatus()

        return view
    }

    private fun updateTitleText() {
        val facilityName = when (tabLayout.selectedTabPosition) {
            0 -> "학술정보관"
            1 -> "코딩라운지"
            else -> "상상파크 플러스"
        }
        textTitle.text = "$selectedDateString [$facilityName] 실시간 현황판"
        textTitle.setTextColor(android.graphics.Color.parseColor("#0F1E36"))
    }

    private fun loadReservationStatus() {
        Thread {
            try {
                val selectedDay = selectedDateString.substringAfterLast("-").toInt().toString()
                val document = Jsoup.connect(currentUrl).userAgent("Mozilla/5.0").get()
                val dayCell = document.select("td").firstOrNull { it.select("span").text().trim() == selectedDay }

                val statusList = mutableListOf<Pair<String, String>>()

                dayCell?.select("div.conBox")?.forEach { box ->
                    val lines = box.html().split("<br>")
                    if (lines.size >= 2) {
                        val roomName = Jsoup.parse(lines[0]).text().trim()
                        val reservedTime = Jsoup.parse(lines[1]).text().trim()

                        if (reservedTime.contains("~")) {
                            statusList.add(Pair(roomName, reservedTime))
                        }
                    }
                }

                requireActivity().runOnUiThread {
                    resultContainer.removeAllViews()

                    if (statusList.isEmpty()) {
                        val emptyText = TextView(requireContext()).apply {
                            text = "🎉 해당 날짜는 모든 공간이 비어있습니다!"
                            textSize = 16f
                            setTextColor(android.graphics.Color.parseColor("#1E3A8A"))
                            setPadding(40, 60, 40, 60)
                            gravity = android.view.Gravity.CENTER
                        }
                        resultContainer.addView(emptyText)
                    } else {
                        statusList.forEach { (room, time) ->
                            addStatusCard(resultContainer, room, time)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("STATUS_ERROR", e.toString())
            }
        }.start()
    }

    private fun addStatusCard(container: LinearLayout, roomName: String, timeRange: String) {
        val card = com.google.android.material.card.MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, 32) }
            radius = 24f
            cardElevation = 2f
            strokeWidth = 2
            strokeColor = android.graphics.Color.parseColor("#E2E8F0")
            setCardBackgroundColor(android.graphics.Color.parseColor("#F8FAFC"))
        }

        val inner = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 40, 48, 40)
        }

        val nameText = TextView(requireContext()).apply {
            text = roomName
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#0F1E36"))
        }

        val badgeLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(24, 12, 24, 12)
            layoutParams = LinearLayout.LayoutParams(-2, -2).apply { setMargins(0, 16, 0, 0) }
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.parseColor("#EEF2F6")) // 예약 완료 그레이블루 뱃지
                cornerRadius = 12f
            }
        }

        val statusText = TextView(requireContext()).apply {
            text = "🔒 예약 완료  |  $timeRange"
            textSize = 13f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#1E3A8A"))
        }

        badgeLayout.addView(statusText)
        inner.addView(nameText)
        inner.addView(badgeLayout)
        card.addView(inner)
        container.addView(card)
    }
}
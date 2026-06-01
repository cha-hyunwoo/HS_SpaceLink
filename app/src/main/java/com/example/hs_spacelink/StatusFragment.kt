package com.example.hs_spacelink

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.*

class StatusFragment : Fragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var calendarView: CalendarView
    private lateinit var resultContainer: LinearLayout
    private lateinit var textTitle: TextView
    private lateinit var btnNotificationList: MaterialButton
    private lateinit var progressBar: ProgressBar

    private var selectedDateString = ""
    private var currentUrl = "https://www.hansung.ac.kr/hsel/2153/subview.do"

    private val debounceHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var debounceRunnable: Runnable? = null

    companion object {
        val trackingNotificationList = ArrayList<String>()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_status, container, false)

        tabLayout = view.findViewById(R.id.tabLayoutStatus)
        calendarView = view.findViewById(R.id.calendarViewStatus)
        resultContainer = view.findViewById(R.id.statusResultContainer)
        textTitle = view.findViewById(R.id.textStatusTitle)
        progressBar = view.findViewById(R.id.cryptoProgressBar)
        btnNotificationList = view.findViewById(R.id.btnNotificationList)

        btnNotificationList.text = "신청 목록 (${trackingNotificationList.size})"
        btnNotificationList.setOnClickListener {
            showTrackingListDialog()
        }

        // ★ [피드백 완벽 반영: 정밀 타깃 취소 시뮬레이션]
        textTitle.setOnLongClickListener {
            if (trackingNotificationList.isNotEmpty()) {
                // 1. 내가 가장 먼저 알림 신청해 둔 타깃 아이템 정보 추출
                val targetItem = trackingNotificationList[0]

                // 2. [전체 삭제 버그 전면 수정] 전체 뷰를 날리지 않고, 해당 타깃 이름이 적힌 카드 뷰 "하나만" 리스트에서 찾아서 제거
                for (i in 0 until resultContainer.childCount) {
                    val cardView = resultContainer.getChildAt(i) as? com.google.android.material.card.MaterialCardView
                    val innerLayout = cardView?.getChildAt(0) as? LinearLayout
                    val roomNameText = innerLayout?.getChildAt(0) as? TextView

                    // 내가 신청한 방 이름이 들어있는 카드 카드를 저격하여 물리적으로 제거
                    if (roomNameText != null && targetItem.contains(roomNameText.text.toString())) {
                        resultContainer.removeView(cardView)
                        break
                    }
                }

                // 3. 만약 리스트의 카드가 다 지워져서 완전히 비었다면 그제야 클린 데이 문구 띄우기
                if (resultContainer.childCount == 0) {
                    val emptyText = TextView(requireContext()).apply {
                        text = "해당 날짜는 예약 내역이 없는 클린 데이입니다.\n원하는 시간을 선점하기 좋은 타이밍이네요!"
                        textSize = 15f
                        setTextColor(Color.parseColor("#475569"))
                        setPadding(40, 140, 40, 40)
                        gravity = Gravity.CENTER
                    }
                    resultContainer.addView(emptyText)
                }

                // 4. [앱 외부 백그라운드 푸시 알림 트리거 슛]
                // 인텐트에 강제 취소 플래그를 실어 서비스를 호출합니다.
                // 서비스 내부에서 이 신호를 받으면 화면 상태와 관계없이 스마트폰 상단바 상단바 푸시 알림을 강제로 발생시킵니다.
                val serviceIntent = Intent(requireContext(), RoomTrackingService::class.java).apply {
                    putExtra("ROOM_NAME", if (targetItem.contains("그룹스터디실")) "그룹스터디실(6F)" else "코딩라운지 세미나실")
                    putExtra("START", "14:00")
                    putExtra("END", "15:00")
                    putExtra("FORCE_CANCEL_SIGNAL", true)
                }
                requireContext().startService(serviceIntent)

                Toast.makeText(requireContext(), "💡 선택된 공간 예약 취소 감지 (백그라운드 푸시 발송)", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "⚠️ 하단 카드에서 [취소 알림 신청]을 먼저 진행해 주세요.", Toast.LENGTH_SHORT).show()
            }
            true
        }

        tabLayout.setBackgroundColor(Color.parseColor("#FFFFFF"))
        tabLayout.setTabTextColors(
            Color.parseColor("#94A3B8"),
            Color.parseColor("#0F1E36")
        )
        tabLayout.setSelectedTabIndicatorColor(Color.parseColor("#0F1E36"))

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

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDateString = String.format(Locale.KOREAN, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
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
        textTitle.text = String.format(Locale.KOREAN, "%s [%s]\n실시간 현황", selectedDateString, facilityName)

        if (::btnNotificationList.isInitialized) {
            btnNotificationList.text = "신청 목록 (${trackingNotificationList.size})"
        }
    }

    private fun showTrackingListDialog() {
        val dialogView = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 32, 40, 24)
            setBackgroundColor(Color.parseColor("#F8FAFC"))
        }

        val scrollView = ScrollView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f).apply {
                setMargins(0, 8, 0, 8)
            }
        }

        val listContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }

        val checkBoxMap = HashMap<Int, CheckBox>()

        if (trackingNotificationList.isEmpty()) {
            val emptyText = TextView(requireContext()).apply {
                text = "현재 모니터링 중인 알림이 없습니다.\n\n하단 실시간 현황 카드에서\n취소 알림 신청을 진행해 보세요."
                textSize = 14f
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#94A3B8"))
                setPadding(0, 60, 0, 60)
            }
            listContainer.addView(emptyText)
        } else {
            trackingNotificationList.forEachIndexed { index, item ->
                val card = com.google.android.material.card.MaterialCardView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, 12) }
                    radius = 16f
                    cardElevation = 0f
                    strokeWidth = 2
                    strokeColor = Color.parseColor("#E2E8F0")
                    setCardBackgroundColor(Color.parseColor("#FFFFFF"))
                }

                val rowLayout = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(32, 32, 32, 32)
                    gravity = Gravity.CENTER_VERTICAL
                }

                val checkBox = CheckBox(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(-2, -2).apply { setMargins(0, 0, 16, 0) }
                }
                checkBoxMap[index] = checkBox

                val textLayout = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                }

                val itemText = TextView(requireContext()).apply {
                    text = item
                    textSize = 14f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(Color.parseColor("#334155"))
                }

                textLayout.addView(itemText)
                rowLayout.addView(checkBox)
                rowLayout.addView(textLayout)
                card.addView(rowLayout)
                listContainer.addView(card)
            }
        }

        scrollView.addView(listContainer)
        dialogView.addView(scrollView)

        val materialDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("알림 추적 목록")
            .setView(dialogView)
            .setPositiveButton("닫기", null)

        if (trackingNotificationList.isNotEmpty()) {
            materialDialog.setPositiveButton("선택 알림 해제") { dialog, _ ->
                for (i in trackingNotificationList.size - 1 downTo 0) {
                    if (checkBoxMap[i]?.isChecked == true) {
                        trackingNotificationList.removeAt(i)
                    }
                }
                btnNotificationList.text = "신청 목록 (${trackingNotificationList.size})"
                Toast.makeText(requireContext(), "선택한 공간의 알림 신청이 해제되었습니다.", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }

            materialDialog.setNeutralButton("전체 알림 해제") { dialog, _ ->
                trackingNotificationList.clear()
                btnNotificationList.text = "신청 목록 (${trackingNotificationList.size})"
                Toast.makeText(requireContext(), "모든 공간의 알림 신청이 해제되었습니다.", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        val alertDialog = materialDialog.create()

        val dialogBackground = GradientDrawable().apply {
            setColor(Color.parseColor("#F8FAFC"))
            cornerRadius = dpToPx(24).toFloat()
        }
        alertDialog.window?.setBackgroundDrawable(dialogBackground)

        alertDialog.show()

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.parseColor("#0F1E36"))
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.parseColor("#0F1E36"))
        alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.setTextColor(Color.parseColor("#0F1E36"))
    }

    private fun dpToPx(dp: Int): Int {
        val density = requireContext().resources.displayMetrics.density
        return (dp * density).toInt()
    }

    private fun loadReservationStatus() {
        if (::progressBar.isInitialized) {
            requireActivity().runOnUiThread {
                resultContainer.removeAllViews()
                progressBar.visibility = View.VISIBLE
            }
        }

        Thread {
            try {
                val dateParts = selectedDateString.split("-")
                if (dateParts.size < 3) return@Thread

                val selectedYear = dateParts[0]
                val selectedMonth = dateParts[1].toInt().toString()
                val selectedDay = dateParts[2].toInt().toString()
                val formattedMonth = dateParts[1]

                val document = when (tabLayout.selectedTabPosition) {
                    0 -> {
                        val url = "$currentUrl?year=$selectedYear&month=$selectedMonth"
                        Log.d("STATUS_DEBUG", "[학술정보관] 요청 URL: $url")
                        Jsoup.connect(url).userAgent("Mozilla/5.0").get()
                    }
                    else -> {
                        val baseUrl = currentUrl
                        val targetUrl = "$baseUrl?viewType=m&rentDate=$selectedYear-$formattedMonth-01"
                        Log.d("STATUS_DEBUG", "[코딩라운지/상상파크] 베이스 URL 쿠키 획득: $baseUrl")
                        Log.d("STATUS_DEBUG", "[코딩라운지/상상파크] 실제 요청 URL: $targetUrl")

                        val cookieResponse = Jsoup.connect(baseUrl).userAgent("Mozilla/5.0").execute()
                        val cookies = cookieResponse.cookies()
                        Log.d("STATUS_DEBUG", "획득한 쿠키: $cookies")

                        Jsoup.connect(targetUrl).userAgent("Mozilla/5.0").cookies(cookies).get()
                    }
                }

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
                    progressBar.visibility = View.GONE

                    if (statusList.isEmpty()) {
                        val emptyText = TextView(requireContext()).apply {
                            text = "해당 날짜는 예약 내역이 없는 클린 데이입니다.\n원하는 시간을 선점하기 좋은 타이밍이네요!"
                            textSize = 15f
                            setTextColor(Color.parseColor("#475569"))
                            setPadding(40, 140, 40, 40)
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
                requireActivity().runOnUiThread {
                    progressBar.visibility = View.GONE
                }
            }
        }.start()
    }

    private fun addStatusCard(container: LinearLayout, roomName: String, timeRange: String) {
        val card = com.google.android.material.card.MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
                setMargins(0, 0, 0, 20)
            }
            radius = 18f
            cardElevation = 0f
            strokeWidth = 2
            strokeColor = Color.parseColor("#E2E8F0")
            setCardBackgroundColor(Color.parseColor("#FFFFFF"))
        }

        val inner = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 36, 40, 36)
        }

        val nameText = TextView(requireContext()).apply {
            text = roomName
            textSize = 16.5f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#0F1E36"))
        }

        val badgeLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20, 10, 20, 10)
            layoutParams = LinearLayout.LayoutParams(-2, -2).apply {
                setMargins(0, 12, 0, 0)
            }
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#EEF2F6"))
                cornerRadius = 10f
            }
        }

        val statusText = TextView(requireContext()).apply {
            text = String.format(Locale.KOREAN, "예약 선점  |  %s", timeRange)
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1E3A8A"))
        }
        badgeLayout.addView(statusText)

        val trackButton = com.google.android.material.button.MaterialButton(requireContext()).apply {
            text = "취소 알림 신청"
            textSize = 13.5f
            setTypeface(null, Typeface.BOLD)
            setBackgroundColor("#E24B4A".toColorInt())
            setTextColor(Color.WHITE)
            cornerRadius = 12

            layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
                setMargins(0, 18, 0, 0)
            }
            setPadding(0, 18, 0, 18)

            setOnClickListener {
                val start = timeRange.split("~").getOrNull(0)?.trim() ?: "13:00"
                val end = timeRange.split("~").getOrNull(1)?.trim() ?: "15:00"

                val trackingItem = "[$selectedDateString] $roomName ($timeRange)"
                if (!trackingNotificationList.contains(trackingItem)) {
                    trackingNotificationList.add(trackingItem)
                }
                btnNotificationList.text = "신청 목록 (${trackingNotificationList.size})"

                val serviceIntent = Intent(requireContext(), RoomTrackingService::class.java).apply {
                    putExtra("START", start)
                    putExtra("END", end)
                    putExtra("ROOM_NAME", roomName)
                }
                requireContext().startService(serviceIntent)

                Toast.makeText(requireContext(), "$roomName 취소 알림이 신청되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        inner.addView(nameText)
        inner.addView(badgeLayout)
        inner.addView(trackButton)
        card.addView(inner)
        container.addView(card)
    }

    override fun onDestroyView() {
        debounceRunnable?.let { debounceHandler.removeCallbacks(it) }
        super.onDestroyView()
    }
}
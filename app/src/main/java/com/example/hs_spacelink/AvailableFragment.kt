package com.example.hs_spacelink

import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.google.android.material.datepicker.MaterialDatePicker
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.*

class AvailableFragment : Fragment() {

    private val libraryRooms = listOf(
        "그룹스터디실(6F)", "그룹스터디실(5F)", "그룹스터디실(4F)", "그룹스터디실(3F-2)", "그룹스터디실(3F-1)",
        "회의실(5F상상커먼스)", "세미나룸(5F상상커먼스)"
    )
    private val sangsangRooms = listOf(
        "소모임실 Challenge", "소모임실 Collaboration", "소모임실 Communication",
        "소모임실 Convergence", "소모임실 Creativity", "소모임실 Critical Thinking"
    )
    private val codingRooms = listOf(
        "세미나실 101호", "세미나실 102호", "세미나실 103호", "세미나실 104호", "세미나실 105호",
        "세미나실 110호", "세미나실 111호", "세미나실 112호", "세미나실 113호"
    )

    private lateinit var textResultCount: TextView
    private lateinit var resultContainer: LinearLayout
    private lateinit var progressBar: ProgressBar

    private var currentSelectedDate = ""
    private var currentStartTime = ""
    private var currentEndTime = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_available, container, false)

        resultContainer = view.findViewById(R.id.resultContainer)
        textResultCount = view.findViewById(R.id.textResultCount)
        progressBar = view.findViewById(R.id.cryptoProgressBar)

        val searchButton = view.findViewById<Button>(R.id.btnSearch)
        val editDate = view.findViewById<EditText>(R.id.editDate)
        val editStartTime = view.findViewById<EditText>(R.id.editStartTime)
        val editEndTime = view.findViewById<EditText>(R.id.editEndTime)
        val checkLibrary = view.findViewById<CheckBox>(R.id.checkLibrary)
        val checkCoding = view.findViewById<CheckBox>(R.id.checkCoding)
        val checkSangsang = view.findViewById<CheckBox>(R.id.checkSangsang)

        searchButton.setBackgroundColor("#0F1E36".toColorInt())
        searchButton.setTextColor(Color.WHITE)

        showInitialGuide()
        setupPickers(editDate, editStartTime, editEndTime)

        searchButton.setOnClickListener {
            val selectedDate = editDate.text.toString()
            val startTime = editStartTime.text.toString()
            val endTime = editEndTime.text.toString()

            if (!checkLibrary.isChecked && !checkCoding.isChecked && !checkSangsang.isChecked) {
                Toast.makeText(requireContext(), "시설을 하나 이상 선택해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedDate.isBlank() || startTime.isBlank() || endTime.isBlank()) {
                Toast.makeText(requireContext(), "날짜와 시간을 지정해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            currentSelectedDate = selectedDate
            currentStartTime = startTime
            currentEndTime = endTime

            // ★ [UX 정제] "조회 결과" 기본 타이틀 톤앤매너 유지 및 이전 결과 뷰 비우기
            textResultCount.text = "조회 결과"
            textResultCount.setTextColor("#64748B".toColorInt())
            resultContainer.removeAllViews()
            progressBar.visibility = View.VISIBLE // 동글동글이만 우아하게 시작

            Thread {
                try {
                    val selectedDay = selectedDate.substringAfterLast("-").toInt().toString()
                    val reservedRooms = mutableSetOf<String>()

                    if (checkLibrary.isChecked) parseUrlData("https://www.hansung.ac.kr/hsel/2153/subview.do", selectedDay, startTime, endTime, reservedRooms)
                    if (checkSangsang.isChecked) parseUrlData("https://www.hansung.ac.kr/cncschool/4181/subview.do", selectedDay, startTime, endTime, reservedRooms)
                    if (checkCoding.isChecked) parseUrlData("https://www.hansung.ac.kr/cncschool/4182/subview.do", selectedDay, startTime, endTime, reservedRooms)

                    requireActivity().runOnUiThread {
                        progressBar.visibility = View.GONE // 크롤링 완료 후 동글동글이 아웃

                        val totalSelected = mutableListOf<String>()
                        if (checkLibrary.isChecked) totalSelected.addAll(libraryRooms)
                        if (checkCoding.isChecked) totalSelected.addAll(codingRooms)
                        if (checkSangsang.isChecked) totalSelected.addAll(sangsangRooms)

                        var availableCount = 0
                        totalSelected.forEach { room ->
                            val normalized = room.replace("-", "").replace(" ", "").lowercase(Locale.ROOT)

                            if (!reservedRooms.contains(normalized)) {
                                availableCount++
                                val category = when {
                                    libraryRooms.contains(room) -> "학술정보관"
                                    codingRooms.contains(room) -> "코딩라운지"
                                    else -> "상상파크 플러스"
                                }
                                val statusMsg = String.format(Locale.KOREAN, "%s ~ %s [예약 가능]", startTime, endTime)
                                addResultCard(resultContainer, category, room, statusMsg)
                            }
                        }

                        // 결과 도출 시 최종 카운트 표기 최신화
                        textResultCount.text = String.format(Locale.KOREAN, "조회 결과 %d개", availableCount)
                        textResultCount.setTextColor("#0F1E36".toColorInt())

                        if (availableCount == 0) showNoResultView()
                    }
                } catch (e: Exception) {
                    Log.e("HTML_ERROR", e.toString())
                    requireActivity().runOnUiThread {
                        progressBar.visibility = View.GONE
                        textResultCount.text = "조회 실패"
                        Toast.makeText(requireContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        }
        return view
    }

    private fun showInitialGuide() {
        resultContainer.removeAllViews()
        textResultCount.text = "조회 결과"
        val guideText = TextView(requireContext()).apply {
            text = "\n\n원하시는 시설과 일정을 위 창에 입력하신 뒤\n[조회하기] 버튼을 누르면 실시간 맵핑이 시작됩니다!"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor("#64748B".toColorInt())
            setPadding(40, 150, 40, 40)
        }
        resultContainer.addView(guideText)
    }

    private fun showNoResultView() {
        val emptyText = TextView(requireContext()).apply {
            text = "\n\n선택하신 시간대에는 이미 모든 예약이 선점되어\n남아있는 빈 공간이 없습니다. 다른 시간을 설정해보세요!"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor("#94A3B8".toColorInt())
            setPadding(40, 150, 40, 40)
        }
        resultContainer.addView(emptyText)
    }

    private fun parseUrlData(url: String, selectedDay: String, startTime: String, endTime: String, reservedRooms: MutableSet<String>) {
        val document = Jsoup.connect(url).userAgent("Mozilla/5.0").get()
        val dayCell = document.select("td").firstOrNull { it.select("span").text().trim() == selectedDay }

        dayCell?.select("div.conBox")?.forEach { box ->
            val lines = box.html().split("<br>")
            if (lines.size >= 2) {
                val rawRoomName = Jsoup.parse(lines[0]).text().replace("-", "").replace(" ", "").trim().lowercase(Locale.ROOT)
                val reservedTime = Jsoup.parse(lines[1]).text().trim()

                if (reservedTime.contains("~") && isTimeOverlap(startTime, endTime, reservedTime)) {
                    reservedRooms.add(rawRoomName)
                }
            }
        }
    }

    private fun setupPickers(date: EditText, start: EditText, end: EditText) {
        date.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker().setSelection(MaterialDatePicker.todayInUtcMilliseconds()).build()
            picker.addOnPositiveButtonClickListener { date.setText(SimpleDateFormat("yyyy-MM-dd", Locale.KOREAN).format(Date(it))) }
            picker.show(parentFragmentManager, "DATE")
        }
        start.setOnClickListener { showScrollTimePicker(start, 13) }
        end.setOnClickListener { showScrollTimePicker(end, 15) }
    }

    private fun showScrollTimePicker(target: EditText, defaultHour: Int) {
        val timePickerDialog = TimePickerDialog(
            requireContext(), 3,
            { _, hourOfDay, minute -> target.setText(String.format(Locale.KOREAN, "%02d:%02d", hourOfDay, minute)) },
            defaultHour, 0, true
        )
        timePickerDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        timePickerDialog.show()
    }

    private fun isTimeOverlap(uS: String, uE: String, res: String): Boolean {
        fun tM(t: String) = t.trim().split(":").let { it[0].toInt() * 60 + it[1].toInt() }
        val (rS, rE) = res.split("~").let { tM(it[0]) to tM(it[1]) }
        return !(tM(uE) <= rS || tM(uS) >= rE)
    }

    private fun addResultCard(container: LinearLayout, category: String, room: String, status: String) {
        val card = com.google.android.material.card.MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, 24) }
            radius = 20f
            cardElevation = 2f
            strokeWidth = 2
            strokeColor = "#E2E8F0".toColorInt()
            setCardBackgroundColor("#F8FAFC".toColorInt())
        }

        val inner = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        val placeText = TextView(requireContext()).apply {
            text = category
            textSize = 12f
            setTextColor("#94A3B8".toColorInt())
        }

        val roomText = TextView(requireContext()).apply {
            text = room
            textSize = 17f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor("#0F1E36".toColorInt())
            setPadding(0, 6, 0, 10)
        }

        val badgeLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20, 10, 20, 10)
            layoutParams = LinearLayout.LayoutParams(-2, -2).apply { setMargins(0, 0, 0, 24) }
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor("#E0F2FE".toColorInt())
                cornerRadius = 10f
            }
        }

        val statusText = TextView(requireContext()).apply {
            text = String.format(Locale.KOREAN, "%s", status)
            textSize = 12f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor("#0369A1".toColorInt())
        }
        badgeLayout.addView(statusText)

        val reserveButton = com.google.android.material.button.MaterialButton(requireContext()).apply {
            text = "지금 바로 예약"
            textSize = 13f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setBackgroundColor("#1E3A8A".toColorInt())
            setTextColor(Color.WHITE)
            cornerRadius = 12

            setOnClickListener {
                val officialUrl = when (category) {
                    "학술정보관" -> "https://www.hansung.ac.kr/hsel/2153/subview.do"
                    "코딩라운지" -> "https://www.hansung.ac.kr/cncschool/4182/subview.do"
                    else -> "https://www.hansung.ac.kr/cncschool/4181/subview.do"
                }

                try {
                    val intent = Intent(Intent.ACTION_VIEW, officialUrl.toUri())
                    context.startActivity(intent)
                } catch (unUsed: Exception) {
                    Toast.makeText(context, "브라우저를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val mapButton = com.google.android.material.button.MaterialButton(requireContext()).apply {
            text = "🗺 위치 보기"
            textSize = 13f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setBackgroundColor("#0F6E56".toColorInt())
            setTextColor(Color.WHITE)
            cornerRadius = 12
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 12, 0, 0) }

            setOnClickListener {
                val key = when (category) {
                    "학술정보관" -> "coding"
                    "코딩라운지" -> "coding"
                    else -> "coding"
                }
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, MapFragment.newInstance(key))
                    .addToBackStack(null)
                    .commit()

                (requireActivity() as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar?.title = "캠퍼스 지도"
            }
        }

        val calendarButton = com.google.android.material.button.MaterialButton(requireContext()).apply {
            text = "내 캘린더에 일정 추가"
            textSize = 13f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setBackgroundColor("#475569".toColorInt())
            setTextColor(Color.WHITE)
            cornerRadius = 12
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 12, 0, 0) }

            setOnClickListener {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREAN)
                try {
                    val startMillis: Long = sdf.parse("$currentSelectedDate $currentStartTime")?.time ?: System.currentTimeMillis()
                    val endMillis: Long = sdf.parse("$currentSelectedDate $currentEndTime")?.time ?: (startMillis + 2 * 60 * 60 * 1000)

                    val intent = Intent(Intent.ACTION_EDIT).apply {
                        type = "vnd.android.cursor.item/event"
                        putExtra(android.provider.CalendarContract.Events.TITLE, "[스터디룸] $room")
                        putExtra(android.provider.CalendarContract.Events.EVENT_LOCATION, "한성대학교 $category")
                        putExtra(android.provider.CalendarContract.Events.DESCRIPTION, "HS SpaceLink를 통해 매칭에 성공한 예약 스케줄입니다.")
                        putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
                        putExtra(android.provider.CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
                    }
                    context.startActivity(intent)
                } catch (unUsed: Exception) {
                    Toast.makeText(context, "캘린더 앱을 호출할 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        inner.addView(placeText)
        inner.addView(roomText)
        inner.addView(badgeLayout)
        inner.addView(reserveButton)
        inner.addView(mapButton)
        inner.addView(calendarButton)
        card.addView(inner)
        container.addView(card)
    }
}
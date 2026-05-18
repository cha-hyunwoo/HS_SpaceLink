package com.example.hs_spacelink

import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
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
        "세미나실 106호", "세미나실 107호", "세미나실 108호", "세미나실 109호", "세미나실 110호",
        "세미나실 111호", "세미나실 112호", "세미나실 113호"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_available, container, false)

        val resultContainer = view.findViewById<LinearLayout>(R.id.resultContainer)
        val searchButton = view.findViewById<Button>(R.id.btnSearch)
        val editDate = view.findViewById<EditText>(R.id.editDate)
        val editStartTime = view.findViewById<EditText>(R.id.editStartTime)
        val editEndTime = view.findViewById<EditText>(R.id.editEndTime)
        val checkLibrary = view.findViewById<CheckBox>(R.id.checkLibrary)
        val checkCoding = view.findViewById<CheckBox>(R.id.checkCoding)
        val checkSangsang = view.findViewById<CheckBox>(R.id.checkSangsang)

        // 조회하기 버튼 딥 네이비 깔춤
        searchButton.setBackgroundColor(android.graphics.Color.parseColor("#0F1E36"))
        searchButton.setTextColor(android.graphics.Color.WHITE)

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
                Toast.makeText(requireContext(), "날짜와 시간을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Thread {
                try {
                    val selectedDay = selectedDate.substringAfterLast("-").toInt().toString()
                    val reservedRooms = mutableSetOf<String>()

                    if (checkLibrary.isChecked) {
                        parseUrlData("https://www.hansung.ac.kr/hsel/2153/subview.do", selectedDay, startTime, endTime, reservedRooms)
                    }
                    if (checkSangsang.isChecked) {
                        parseUrlData("https://www.hansung.ac.kr/cncschool/4181/subview.do", selectedDay, startTime, endTime, reservedRooms)
                    }
                    if (checkCoding.isChecked) {
                        parseUrlData("https://www.hansung.ac.kr/cncschool/4182/subview.do", selectedDay, startTime, endTime, reservedRooms)
                    }

                    requireActivity().runOnUiThread {
                        resultContainer.removeAllViews()
                        val totalSelected = mutableListOf<String>()

                        if (checkLibrary.isChecked) totalSelected.addAll(libraryRooms)
                        if (checkCoding.isChecked) totalSelected.addAll(codingRooms)
                        if (checkSangsang.isChecked) totalSelected.addAll(sangsangRooms)

                        totalSelected.forEach { room ->
                            val normalized = room.replace("-", "").replace(" ", "").lowercase()

                            if (!reservedRooms.contains(normalized)) {
                                val category = when {
                                    libraryRooms.contains(room) -> "학술정보관"
                                    codingRooms.contains(room) -> "코딩라운지"
                                    else -> "상상파크 플러스"
                                }

                                val bookingUrl = when (category) {
                                    "학술정보관" -> "https://www.hansung.ac.kr/resve/hsel/14/artclRegistView.do"
                                    "코딩라운지" -> "https://www.hansung.ac.kr/resve/cncschool/18/artclRegistView.do"
                                    else -> "https://www.hansung.ac.kr/resve/cncschool/17/artclRegistView.do"
                                }

                                addResultCard(resultContainer, category, room, "$startTime ~ $endTime [예약 가능]", bookingUrl)
                            }
                        }

                        if (resultContainer.childCount == 0) {
                            val emptyText = TextView(requireContext()).apply {
                                text = "선택하신 시간에 빈 공간이 없습니다."
                                textSize = 16f
                                setPadding(40, 40, 40, 40)
                            }
                            resultContainer.addView(emptyText)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("HTML_ERROR", e.toString())
                }
            }.start()
        }
        return view
    }

    private fun parseUrlData(url: String, selectedDay: String, startTime: String, endTime: String, reservedRooms: MutableSet<String>) {
        val document = Jsoup.connect(url).userAgent("Mozilla/5.0").get()
        val dayCell = document.select("td").firstOrNull { it.select("span").text().trim() == selectedDay }

        dayCell?.select("div.conBox")?.forEach { box ->
            val lines = box.html().split("<br>")
            if (lines.size >= 2) {
                val rawRoomName = Jsoup.parse(lines[0]).text().replace("-", "").replace(" ", "").trim().lowercase()
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
            requireContext(),
            android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
            { _, hourOfDay, minute -> target.setText(String.format("%02d:%02d", hourOfDay, minute)) },
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

    private fun addResultCard(container: LinearLayout, category: String, room: String, status: String, url: String) {
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
            setPadding(48, 48, 48, 48)
        }

        val placeText = TextView(requireContext()).apply {
            text = category
            textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#94A3B8"))
        }

        val roomText = TextView(requireContext()).apply {
            text = room
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#0F1E36"))
            setPadding(0, 8, 0, 12)
        }

        val badgeLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(24, 12, 24, 12)
            layoutParams = LinearLayout.LayoutParams(-2, -2).apply { setMargins(0, 0, 0, 32) }
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.parseColor("#E0F2FE")) // 시안 감성 스카이블루 배지
                cornerRadius = 12f
            }
        }

        val statusText = TextView(requireContext()).apply {
            text = "🔓 $status"
            textSize = 13f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#0369A1"))
        }
        badgeLayout.addView(statusText)

        val reserveButton = com.google.android.material.button.MaterialButton(requireContext()).apply {
            text = "지금 바로 예약"
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setBackgroundColor(android.graphics.Color.parseColor("#1E3A8A")) // 다크블루 라운드 버튼
            setTextColor(android.graphics.Color.WHITE)
            cornerRadius = 16

            setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }
        }

        inner.addView(placeText)
        inner.addView(roomText)
        inner.addView(badgeLayout)
        inner.addView(reserveButton)
        card.addView(inner)
        container.addView(card)
    }
}
package com.example.hs_spacelink

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment

class SettingFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_setting, container, false)

        val btnClearCache = view.findViewById<Button>(R.id.btnClearCache)

        // 인앱 캐시 데이터 초기화 로직 가동 (안정성 패치 완료)
        btnClearCache.setOnClickListener {
            try {
                val cacheDir = context?.cacheDir

                if (cacheDir != null && cacheDir.exists()) {
                    // 1. 재귀적 전면 삭제 수행 및 성공 여부 리턴값 확보
                    val isDeleted = cacheDir.deleteRecursively()

                    // 2. 디렉토리 구조 즉시 재건 (앱 오작동 및 파일 쓰기 에러 원천 차단)
                    if (isDeleted) {
                        cacheDir.mkdirs()
                    }

                    // 3. 성공/실패 여부에 따른 정교한 UX 메시지 분기
                    val msg = if (isDeleted) "인앱 캐시가 완전히 초기화되었습니다." else "일부 캐시 초기화 실패"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "초기화할 캐시 디렉토리가 존재하지 않습니다.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "시스템 오류로 인한 초기화 실패", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }
}
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

        // 인앱 캐시 데이터 초기화 로직 가동
        btnClearCache.setOnClickListener {
            try {
                val cacheDir = context?.cacheDir

                if (cacheDir != null && cacheDir.exists()) {
                    cacheDir.deleteRecursively() // 캐시 폴더 완전 삭제
                }

                Toast.makeText(context, "⚡ 인앱 캐시 데이터가 정교하게 초기화되었습니다.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "최적화 실패: 관리자 권한을 확인하세요.", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }
}
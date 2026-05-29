package com.example.hs_spacelink

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout

class RuleFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // fragment_rule.xml 레이아웃 인플레이트 바인딩
        val view = inflater.inflate(R.layout.fragment_rule, container, false)

        // XML 컴포넌트 뷰 아이디 매칭 바인딩
        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayoutRule)
        val layoutLibrary = view.findViewById<View>(R.id.layoutLibraryRule)
        val layoutCoding = view.findViewById<View>(R.id.layoutCodingRule)
        val layoutSangsang = view.findViewById<View>(R.id.layoutSangsangRule)

        // 탭 선택 상태 모니터링 이벤트 바인딩
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                // 새로운 탭을 누르면 일단 모든 카드를 숨김(GONE) 처리
                layoutLibrary.visibility = View.GONE
                layoutCoding.visibility = View.GONE
                layoutSangsang.visibility = View.GONE

                // 현우님이 클릭한 탭의 인덱스(0, 1, 2)에 따라 타겟 카드만 노출(VISIBLE)
                when (tab?.position) {
                    0 -> layoutLibrary.visibility = View.VISIBLE
                    1 -> layoutCoding.visibility = View.VISIBLE
                    2 -> layoutSangsang.visibility = View.VISIBLE
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        return view
    }
}
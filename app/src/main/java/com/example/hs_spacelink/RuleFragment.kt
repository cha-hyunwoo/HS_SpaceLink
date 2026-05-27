package com.example.hs_spacelink

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class RuleFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // fragment_rule.xml 디자인 레이아웃을 바인딩하여 화면에 렌더링합니다.
        return inflater.inflate(R.layout.fragment_rule, container, false)
    }
}
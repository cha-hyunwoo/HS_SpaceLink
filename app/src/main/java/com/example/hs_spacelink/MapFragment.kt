package com.example.hs_spacelink

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.fragment.app.Fragment

class MapFragment : Fragment() {

    companion object {
        fun newInstance(highlightBuilding: String = ""): MapFragment {
            val fragment = MapFragment()
            val args = Bundle()
            args.putString("HIGHLIGHT", highlightBuilding)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val context = requireContext()

        val frameLayout = FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#F8FAFC"))
        }

        val webView = WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
        }

        val highlight = arguments?.getString("HIGHLIGHT") ?: ""

        val html = """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; }
  body { background: #f8fafc; font-family: sans-serif; padding: 16px; }
  .loc-btns { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 10px; }
  .loc-btn {
    padding: 9px 16px; border-radius: 20px;
    border: 1.5px solid #cbd5e1;
    background: #fff; color: #334155;
    font-size: 13px; cursor: pointer;
  }
  .loc-btn.active { background: #E24B4A; border-color: #E24B4A; color: #fff; }
  svg { width: 100%; border-radius: 12px; }
  .info-box {
    margin-top: 10px; padding: 14px 16px;
    border-radius: 12px; background: #f1f5f9;
    border: 1.5px solid #e2e8f0;
    font-size: 13px; color: #475569;
    line-height: 2;
  }
</style>
</head>
<body>

<div class="loc-btns">
  <span style="font-size:13px;color:#94a3b8;align-self:center">내 위치:</span>
  <button class="loc-btn" onclick="setLoc('sikdang')">학식당</button>
  <button class="loc-btn" onclick="setLoc('jandi')">잔디광장</button>
</div>

<svg viewBox="0 0 360 410" xmlns="http://www.w3.org/2000/svg">
  <rect width="360" height="410" rx="12" fill="#eef4e8"/>

  <rect x="14" y="22" width="38" height="70" rx="4" fill="#bde0f5" stroke="#5599cc" stroke-width="1"/>
  <text x="33" y="61" text-anchor="middle" font-size="9" fill="#2266aa">농구장</text>

  <rect x="58" y="22" width="100" height="70" rx="6" fill="#c8e898" stroke="#7ab850" stroke-width="2" stroke-dasharray="5 3"/>
  <text x="108" y="60" text-anchor="middle" font-size="11" fill="#3a7a20" font-weight="600">잔디광장</text>

  <rect x="164" y="22" width="92" height="110" rx="6" fill="#e2f0fd" stroke="#4a90e2" stroke-width="2"/>
  <text x="210" y="70" text-anchor="middle" font-size="11" fill="#0f2c4a" font-weight="700">상상관</text>
  <text x="210" y="90" text-anchor="middle" font-size="9" fill="#334155">로비·라운지</text>

  <rect x="252" y="45" width="8" height="40" fill="#fff" stroke="#4a90e2" stroke-width="1"/>

  <rect id="mapBuildingHsel" x="256" y="22" width="90" height="110" rx="6" fill="#d8eaf8" stroke="#5b9bd5" stroke-width="2"/>
  <text x="301" y="55" text-anchor="middle" font-size="11" fill="#1a4a7a" font-weight="700">미래관</text>
  <text x="301" y="75" text-anchor="middle" font-size="10" fill="#1a4a7a">(학술정보관)</text>
  <text x="301" y="95" text-anchor="middle" font-size="8" fill="#2d6aa0">스터디룸 3~6F</text>
  <text x="301" y="112" text-anchor="middle" font-size="8" fill="#2d6aa0">2F 로비 연결</text>

  <rect x="14" y="140" width="120" height="70" rx="8" fill="#e2ecf5" stroke="#94a3b8" stroke-width="1.5"/>
  <text x="74" y="180" text-anchor="middle" font-size="11" fill="#475569" font-weight="600">연구관(상상파크)</text>

  <rect x="14" y="225" width="110" height="55" rx="6" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="1.5"/>
  <text x="69" y="258" text-anchor="middle" font-size="10" fill="#64748b" font-weight="600">지선관</text>

  <rect x="14" y="315" width="200" height="90" rx="8" fill="#fff3d4" stroke="#854F0B" stroke-width="2"/>
  <text x="114" y="343" text-anchor="middle" font-size="13" fill="#412402" font-weight="700">공학관 A동</text>
  <text x="114" y="363" text-anchor="middle" font-size="10" fill="#854F0B">• 코딩라운지 (지상 1F)</text>
  <text x="114" y="379" text-anchor="middle" font-size="10" fill="#aa6600">• 상상파크플러스 (지하 B1F)</text>

  <rect x="232" y="180" width="112" height="80" rx="8" fill="#fce8e8" stroke="#c0392b" stroke-width="2"/>
  <text x="288" y="214" text-anchor="middle" font-size="12" fill="#7b1d1d" font-weight="700">창의관</text>
  <text x="288" y="234" text-anchor="middle" font-size="11" fill="#c0392b">학생식당</text>

  <path id="p1" d="M288 180 L288 132" stroke="#c0392b" stroke-width="2.5" stroke-dasharray="5 3" fill="none" opacity="0" marker-end="url(#ar)"/>
  <path id="p3" d="M232 215 L183 215 L183 290 L114 290 L114 315" stroke="#c0392b" stroke-width="2.5" stroke-dasharray="5 3" fill="none" opacity="0" marker-end="url(#ar)"/>
  <path id="p7" d="M108 92 L108 150 L183 150 L183 115 L210 115 L210 75 L280 75" stroke="#7ab850" stroke-width="2.5" stroke-dasharray="5 3" fill="none" opacity="0" marker-end="url(#ar)"/>
  <path id="p9" d="M108 92 L108 150 L183 150 L183 290 L114 290 L114 315" stroke="#7ab850" stroke-width="2.5" stroke-dasharray="5 3" fill="none" opacity="0" marker-end="url(#ar)"/>

  <circle id="pin-r" cx="-99" cy="-99" r="16" fill="#E24B4A" opacity="0"/>
  <circle id="pin" cx="-99" cy="-99" r="8" fill="#E24B4A" opacity="0"/>

  <defs>
    <marker id="ar" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
      <path d="M2 1L8 5L2 9" fill="none" stroke="context-stroke" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
    </marker>
  </defs>
</svg>

<div class="info-box" id="box">상단에서 출발지를 선택하시거나, 예약 조회 화면에서 [위치 보기]를 누르면 실시간 매핑 가이드가 활성화됩니다.</div>

<script>
const data = {
  sikdang: {
    x:288, y:220, c:'#c0392b', label:'학식당(창의관)',
    show:['p1','p3'],
    info:'<b>미래관 (학술정보관) 가는 길</b><br>• <b>지상 이동</b>: 학식당 바로 앞 마당 출구로 나오면 미래관 정문 앞쪽으로 즉시 연결 (도보 1분)<br><b>공학관 A동 (코딩라운지) 가는 길</b><br>• <b>상상파크와 학식당 사잇길</b>에 있는 오르막 계단으로 진입 후, 지선관 뒷길을 따라 공학관 1층 코딩라운지로 연결 (도보 4분)'
  },
  jandi: {
    x:108, y:57, c:'#7ab850', label:'잔디광장',
    show:['p7','p9'],
    info:'<b>미래관 (학술정보관) 가는 길</b><br>• 잔디광장에서 사잇길로 내려가던 중 갈림길 코너에서 상상관 아래쪽 정면 입구로 진입하여, <b>2층 연결통로를 거쳐 미래관 내부로 관통</b>합니다 (도보 3분 소요).<br><b>공학관 A동 (코딩라운지) 가는 길</b><br>• 갈림길 코너에서 꺾지 않고 <b>연구관(상상파크)과 창의관 건물 사잇길 외부 통로 계단</b>을 따라 내려간 뒤 지선관을 지나 진입합니다 (도보 4~5분)'
  },
  coding: {
    x:301, y:75, c:'#5b9bd5', label:'미래관 (학술정보관)',
    show:['p1','p7'],
    info:'<b>미래관 학술정보관 타겟 매핑 완료</b><br>• <b>상상관 출발 시</b>: 창의관 쪽 메인 입구로 진입 후 2층 내부 연결통로를 통해 바로 이동 가능합니다 (도보 3분 소요).<br>• <b>학식당 출발 시</b>: 미래관 정문 학식당 앞쪽 마당을 통해 도보 1분 내 진입 가능합니다.'
  }
};

function setLoc(k) {
  if (!data[k]) return;
  
  // 모든 점선 가이드라인 투명도 0 리셋
  document.querySelectorAll('svg path').forEach(e => e.setAttribute('opacity','0'));
  document.querySelectorAll('.loc-btn').forEach(b => b.classList.remove('active'));
  
  const d = data[k];
  // 활성화된 점선 경로만 표출
  d.show.forEach(id => { const e = document.getElementById(id); if(e) e.setAttribute('opacity','1'); });
  
  const idx = {sikdang:0, jandi:1, coding:2}[k];
  if(idx !== undefined && idx < 2) {
    document.querySelectorAll('.loc-btn')[idx].classList.add('active');
  }
  
  const pin = document.getElementById('pin');
  const pinR = document.getElementById('pin-r');
  if(pin && pinR) {
    [pin, pinR].forEach(e => { e.setAttribute('cx', d.x); e.setAttribute('cy', d.y); e.setAttribute('fill', d.c); });
    pin.setAttribute('opacity','1'); pinR.setAttribute('opacity','0.2');
  }
  
  const box = document.getElementById('box');
  box.innerHTML = '<b style="color:' + d.c + '">' + d.label + '</b> 타겟 가이드 활성화<br>' + d.info;
  box.style.borderColor = d.c;
}
</script>
</body>
</html>
        """.trimIndent()

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (highlight.isNotEmpty()) {
                    webView.evaluateJavascript("javascript:setLoc('$highlight');", null)
                }
            }
        }

        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)

        frameLayout.addView(webView)
        return frameLayout
    }
}
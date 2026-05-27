package com.example.hs_spacelink

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment

class WebFragment : Fragment() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private var targetReservationUrl: String = ""
    private var hasRedirectedToTarget = false

    companion object {
        fun newInstance(loginUrl: String, targetUrl: String): WebFragment {
            val fragment = WebFragment()
            val args = Bundle()
            args.putString("LOGIN_URL", loginUrl)
            args.putString("TARGET_URL", targetUrl)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_web, container, false)

        webView = view.findViewById(R.id.commonWebView)
        progressBar = view.findViewById(R.id.webProgressBar)

        val loginUrl = arguments?.getString("LOGIN_URL") ?: "https://www.hansung.ac.kr"
        targetReservationUrl = arguments?.getString("TARGET_URL") ?: ""

        // [해결책 ①] 디바이스 물리 뒤로가기 시 웹뷰 히스토리에 갇히지 않고 이전 화면(AvailableFragment)으로 즉시 무조건 이탈
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                parentFragmentManager.popBackStack()
            }
        })

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            javaScriptCanOpenWindowsAutomatically = true
            // 학교 보안 모듈과의 세션 정합성을 위한 기본 쿠키 허용 설정 체계 수립
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        // 웹뷰 내부 쿠키 매니저 동기화 활성화
        val cookieManager = android.webkit.CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()

                // [해결책 ②] 중간 로그인 처리 주소(proc.do)는 통과시키고, 완전히 가동된 메인 도메인(index.do 등)만 정밀 타겟팅하여 가로채기
                if ((url.contains("index.do") || url.contains("main.do") || url.contains("subview.do"))
                    && !url.contains("login") && !hasRedirectedToTarget) {

                    if (targetReservationUrl.isNotEmpty()) {
                        hasRedirectedToTarget = true
                        view?.loadUrl(targetReservationUrl) // 인증 세션이 완전히 구워진 상태로 예약 페이지 정석 안착
                        return true
                    }
                }
                return super.shouldOverrideUrlLoading(view, request)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE

                // 특정 조건 하에 완벽히 한성대 메인 인덱스 구조로 진입했을 때 예외 복구 분기 작동
                if (url != null && (url.contains("index.do") || url.contains("main.do") || url.contains("subview.do"))
                    && !url.contains("login") && !hasRedirectedToTarget) {

                    if (targetReservationUrl.isNotEmpty()) {
                        hasRedirectedToTarget = true
                        view?.loadUrl(targetReservationUrl)
                    }
                }
            }
        }

        webView.loadUrl(loginUrl)
        return view
    }
}
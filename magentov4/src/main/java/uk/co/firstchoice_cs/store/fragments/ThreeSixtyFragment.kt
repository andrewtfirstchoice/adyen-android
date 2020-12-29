package uk.co.firstchoice_cs.store.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_three_sixty.*
import uk.co.firstchoice_cs.firstchoice.R

class ThreeSixtyFragment : Fragment(R.layout.fragment_three_sixty) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(false)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val bundle = arguments
        if (bundle != null) {
            val url = bundle.getString("url")
            val webView = view.findViewById<WebView>(R.id.webView)
            webView.settings.javaScriptEnabled = true
            webView.settings.loadWithOverviewMode = true
            webView.webChromeClient = WebChromeClient()
            webView.settings.userAgentString = "user" //this is important on some phones ??????
            webView.loadUrl(url?:"")
        }

        closeButton.setOnClickListener{
            findNavController().navigateUp()
        }
        super.onViewCreated(view, savedInstanceState)
    }
}
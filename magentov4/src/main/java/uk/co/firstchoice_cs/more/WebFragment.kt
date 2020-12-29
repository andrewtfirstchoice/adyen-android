package uk.co.firstchoice_cs.more

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import org.koin.core.KoinComponent
import org.koin.core.inject
import uk.co.firstchoice_cs.AppStatus
import uk.co.firstchoice_cs.Settings.ABOUT_BLANK_URL
import uk.co.firstchoice_cs.Settings.FC_USER_AGENT
import uk.co.firstchoice_cs.core.listeners.DefaultCurrentActivityListener
import uk.co.firstchoice_cs.firstchoice.R
import uk.co.firstchoice_cs.firstchoice.databinding.FragmentWebBinding
import uk.co.firstchoice_cs.store.vm.MainActivityViewModel

class WebFragment : Fragment(R.layout.fragment_web) , KoinComponent {
    private val defaultCurrentActivityListener: DefaultCurrentActivityListener by inject()
    private val act = defaultCurrentActivityListener.currentActivity as AppCompatActivity
    private var mUrl: String? = null
    private var mTitle: String? = null
    private var currentUrl: String? = null
    private lateinit var backMenuItem: MenuItem
    private lateinit var forwardMenuItem: MenuItem
    private lateinit var mainViewModel: MainActivityViewModel
    lateinit var binding:FragmentWebBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val arg = arguments
        if (arg != null) {
            mUrl = arg.getString(ARG_PARAM_URL)
            mTitle = arg.getString(ARG_PARAM_TITLE)
        }
        currentUrl = mUrl
        setHasOptionsMenu(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        // action with ID action_refresh was selected
        if (item.itemId == R.id.action_back) {
            if (binding.webView.canGoBack()) binding.webView.goBack()
        } else if (item.itemId == R.id.action_forward) {
            if (binding.webView.canGoForward()) binding.webView.goForward()
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onResume() {
        super.onResume()
        internetStatusChanged()
    }

    private fun setUpToolbar() {
        act.setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { NavHostFragment.findNavController(this@WebFragment).navigateUp() }
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.action_menu_web_fragment, menu)
        backMenuItem = menu.findItem(R.id.action_back)
        forwardMenuItem = menu.findItem(R.id.action_forward)
        super.onCreateOptionsMenu(menu, menuInflater)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentWebBinding.bind(view)
        mainViewModel = ViewModelProvider(this).get(MainActivityViewModel::class.java)
        mainViewModel.internetStatusChanged.observe(viewLifecycleOwner, {

        })
        binding.toolbar.title = mTitle
        binding.webView.webViewClient = CustomWebViewClient()
        val webSetting = binding.webView.settings
        webSetting.javaScriptEnabled = true
        binding.webView.settings.userAgentString = FC_USER_AGENT
        binding.webView.loadUrl(mUrl?:"")
        setUpToolbar()

        binding.swipeRefresh.setOnRefreshListener {
            if(AppStatus.INTERNET_CONNECTED)
                binding.webView.loadUrl(currentUrl?:"")
            else
                binding.swipeRefresh.isRefreshing = false
        }

        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                NavHostFragment.findNavController(this@WebFragment).navigateUp()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private fun updateNav() {
        if (binding.webView.canGoBack()) DrawableCompat.setTint(backMenuItem.icon, ContextCompat.getColor(requireActivity(), R.color.white)) else DrawableCompat.setTint(backMenuItem.icon, ContextCompat.getColor(requireActivity(), R.color.fcBlue))
        if (binding.webView.canGoForward()) DrawableCompat.setTint(forwardMenuItem.icon, ContextCompat.getColor(requireActivity(), R.color.white)) else DrawableCompat.setTint(forwardMenuItem.icon, ContextCompat.getColor(requireActivity(), R.color.fcBlue))
    }

    override fun onStart() {
        super.onStart()
        WEB_FRAGMENT_SHOWING = true
    }

    override fun onStop() {
        super.onStop()
        WEB_FRAGMENT_SHOWING = false
    }


    private fun internetStatusChanged() {
        if (AppStatus.INTERNET_CONNECTED) {
           showWebPage()
        }
        else {
            showInternetWarning()
        }
    }

    private fun showWebPage() {
        binding.webView.loadUrl(currentUrl?:"")
        binding.webView.visibility = View.VISIBLE
        binding.noConnectionView.visibility = View.GONE
    }

    private fun showInternetWarning() {
        binding.webView.visibility = View.GONE
        binding.noConnectionView.visibility = View.VISIBLE
    }

    private inner class CustomWebViewClient : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            if (url != null) {
                if(!url.contains(ABOUT_BLANK_URL))
                    currentUrl = url
                    //updateNav()
            }
            super.onPageStarted(view, url, favicon)
        }

        fun removeScrollToTop()
        {
            val js = "document.getElementsByClassName('scrollToTop')[0].remove();"
            binding.webView.evaluateJavascript(js, fun(_: String) {})
        }

        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
            super.onReceivedError(view, request, error)
            showInternetWarning()
            view?.loadUrl(ABOUT_BLANK_URL)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            binding.swipeRefresh.isRefreshing = false
            removeScrollToTop()
            updateNav()
            super.onPageFinished(view, url)
        }

        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            return false
        }
    }

    companion object {
        const val ARG_PARAM_URL = "url"
        const val ARG_PARAM_TITLE = "title"
        var WEB_FRAGMENT_SHOWING =  false
    }
}
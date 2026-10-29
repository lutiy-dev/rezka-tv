package dev.alaev.rezkatv.rezka.login

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.EditText
import android.widget.Button
import android.widget.Toast
import java.io.IOException
import android.widget.TextView
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import dev.alaev.rezkatv.R
import dev.alaev.rezkatv.rezka.RezkaClient
import dev.alaev.rezkatv.rezka.openSite

class LoginActivity : Activity() {

    private val scope = MainScope()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var client: RezkaClient
    private lateinit var qr: ImageView
    private lateinit var qrCard: View
    private lateinit var url: TextView
    private lateinit var status: TextView

    private var server: LoginServer? = null
    private var siteOpened = false
    private val expire = Runnable {
        stopServer()
        hideCode()
        showStatus(R.string.login_status_expired, error = true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val phone = resources.configuration.smallestScreenWidthDp < 600
        setContentView(if (phone) R.layout.activity_login_phone else R.layout.activity_login)
        client = RezkaClient.get(this)
        qr = findViewById(R.id.qr)
        qrCard = findViewById(R.id.qr_card)
        url = findViewById(R.id.login_url)
        status = findViewById(R.id.login_status)
        findViewById<Button>(R.id.login_submit).setOnClickListener { submitCredentials() }
        findViewById<View>(R.id.login_on_tv).setOnClickListener {
            siteOpened = true
            openSite()
        }
        findViewById<View>(R.id.login_new_code).setOnClickListener { startCode() }
        startCode()
    }

    override fun onResume() {
        super.onResume()
        if (siteOpened) finish()
    }

    override fun onDestroy() {
        stopServer()
        handler.removeCallbacksAndMessages(null)
        scope.cancel()
        super.onDestroy()
    }

    private fun submitCredentials() {
        val email = findViewById<EditText>(R.id.login_email).text.toString().trim()
        val password = findViewById<EditText>(R.id.login_password).text.toString()
        if (email.isEmpty() || password.isEmpty()) {
            showStatus(R.string.login_fields_required, error = true)
            return
        }
        val submit = findViewById<Button>(R.id.login_submit)
        submit.isEnabled = false
        showStatus(R.string.login_status_checking)
        scope.launch {
            val result = withContext(RezkaClient.io) {
                try {
                    client.login(email, password)
                } catch (e: IOException) {
                    LoginResult(false, getString(R.string.load_failed))
                }
            }
            submit.isEnabled = true
            if (result.success) {
                showStatus(R.string.login_status_done)
                client.exportCookiesToWebView()
                setResult(RESULT_OK)
                handler.postDelayed({ finish() }, DONE_DELAY_MS)
            } else {
                showStatus(result.message, error = true)
            }
        }
    }

    private fun startCode() {
        stopServer()
        handler.removeCallbacks(expire)
        val address = LanAddress.find()
        if (address == null) {
            qrCard.visibility = View.INVISIBLE
            url.text = ""
            showStatus(R.string.login_status_no_network, error = true)
            return
        }
        val code = LoginCode()
        val newServer = LoginServer(address, code, Events()) { email, password -> client.login(email, password) }
        server = newServer
        scope.launch {
            val port = withContext(RezkaClient.io) { newServer.start() }
            if (server !== newServer) return@launch newServer.stop()
            if (port < 0) {
                qrCard.visibility = View.INVISIBLE
                return@launch showStatus(R.string.login_status_no_port, error = true)
            }
            val host = address.hostAddress.orEmpty()
            val bitmap = Qr.bitmap(code.phoneUrl(host, port), resources.getDimensionPixelSize(R.dimen.login_qr_size))
            bitmap.density = resources.displayMetrics.densityDpi
            qr.setImageBitmap(bitmap)
            qrCard.visibility = View.VISIBLE
            url.text = code.url(host, port)
            showStatus(R.string.login_status_waiting)
            handler.postDelayed(expire, LoginCode.LIFETIME_MS)
        }
    }

    private fun hideCode() {
        qrCard.visibility = View.INVISIBLE
        url.text = ""
    }

    private fun stopServer() {
        server?.stop()
        server = null
    }

    private fun showStatus(text: Int, error: Boolean = false) = showStatus(getString(text), error)

    private fun showStatus(text: String, error: Boolean = false) {
        status.text = text
        status.setTextColor(getColor(if (error) R.color.error else R.color.accent))
    }

    private inner class Events : LoginServer.Listener {
        override fun onPhoneConnected() = runOnUiThread { showStatus(R.string.login_status_connected) }

        override fun onChecking() = runOnUiThread { showStatus(R.string.login_status_checking) }

        override fun onResult(result: LoginResult, codeExhausted: Boolean) = runOnUiThread {
            when {
                result.success -> {
                    showStatus(R.string.login_status_done)
                    handler.removeCallbacks(expire)
                    client.exportCookiesToWebView()
                    setResult(RESULT_OK)
                    handler.postDelayed({ finish() }, DONE_DELAY_MS)
                }
                codeExhausted -> {
                    stopServer()
                    hideCode()
                    showStatus(getString(R.string.login_status_too_many, result.message), error = true)
                }
                else -> showStatus(result.message, error = true)
            }
        }
    }

    private companion object {
        const val DONE_DELAY_MS = 1500L
    }
}

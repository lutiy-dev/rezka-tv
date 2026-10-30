package dev.alaev.rezkatv.rezka

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import androidx.leanback.widget.VerticalGridView
import dev.alaev.rezkatv.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchActivity : Activity() {

    private enum class Layout(val letters: String, val label: String) {
        RU("абвгдеёжзийклмнопрстуфхцчшщъыьэюя", "АБВ"),
        EN("abcdefghijklmnopqrstuvwxyz", "ABC"),
        DIGITS("1234567890", "123"),
    }

    private val scope = MainScope()
    private val phoneMode by lazy { ResponsiveLayout.isPhone(resources.configuration.smallestScreenWidthDp) }
    private val query = StringBuilder()
    private var layout = Layout.RU
    private var searchJob: Job? = null

    private lateinit var client: RezkaClient
    private lateinit var loader: Loader
    private lateinit var queryView: TextView
    private lateinit var emptyView: TextView
    private lateinit var keyboard: VerticalGridView
    private lateinit var results: CardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(if (phoneMode) R.layout.activity_search_mobile else R.layout.activity_search)
        client = RezkaClient.get(this)
        queryView = findViewById(R.id.query)
        emptyView = findViewById(R.id.empty)
        loader = Loader(this, null)
        if (!phoneMode) {
            keyboard = findViewById(R.id.keyboard)
            keyboard.setNumColumns(KEY_COLUMNS)
            keyboard.itemAnimator = null
        }

        results = CardAdapter(onClick = { openDetails(it) })
        findViewById<VerticalGridView>(R.id.results).apply {
            setNumColumns(ResponsiveLayout.searchColumns(resources.configuration.screenWidthDp))
            val spacing = resources.getDimensionPixelSize(R.dimen.card_spacing)
            horizontalSpacing = spacing
            verticalSpacing = spacing
            itemAnimator = null
            adapter = results
        }

        findViewById<View>(R.id.voice_search).setOnClickListener { launchVoiceSearch() }
        if (phoneMode) {
            val input = queryView as EditText
            input.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                override fun afterTextChanged(s: Editable?) {
                    query.clear()
                    query.append(s?.toString().orEmpty())
                    onQueryChanged()
                }
            })
            input.setText(savedInstanceState?.getString(STATE_QUERY) ?: intent.getStringExtra(EXTRA_QUERY).orEmpty())
            input.setSelection(input.text.length)
            input.requestFocus()
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        } else {
            showKeyboard()
            keyboard.requestFocus()
            (savedInstanceState?.getString(STATE_QUERY) ?: intent.getStringExtra(EXTRA_QUERY))?.let {
                query.append(it.lowercase())
                onQueryChanged()
            }
        }
    }

    @Deprecated("Android speech recognition activity result")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VOICE_REQUEST && resultCode == RESULT_OK) {
            val spoken = VoiceSearchQuery.first(data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS))
            if (!spoken.isNullOrBlank()) {
                if (phoneMode) {
                    (queryView as EditText).setText(spoken)
                } else {
                    query.clear()
                    query.append(spoken)
                    onQueryChanged()
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        findViewById<VerticalGridView>(R.id.results)
            .setNumColumns(ResponsiveLayout.searchColumns(newConfig.screenWidthDp))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_QUERY, query.toString())
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun launchVoiceSearch() {
        try {
            val speech = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            }
            startActivityForResult(speech, VOICE_REQUEST)
        } catch (_: ActivityNotFoundException) {
            showEmpty(R.string.voice_search_unavailable)
        }
    }

    private fun showKeyboard() {
        val keys = layout.letters.map { it.uppercase() } + EDIT_KEYS + nextLayout().label
        keyboard.adapter = ChipAdapter(keys, layout = R.layout.item_key) { i -> onKey(keys[i]) }
    }

    private fun layoutSwitchPosition() = layout.letters.length + EDIT_KEYS.size

    private fun nextLayout() = Layout.entries[(layout.ordinal + 1) % Layout.entries.size]

    private fun onKey(key: String) {
        when (key) {
            SPACE -> if (query.isNotEmpty() && query.last() != ' ') query.append(' ')
            BACKSPACE -> if (query.isNotEmpty()) query.setLength(query.length - 1)
            CLEAR -> query.setLength(0)
            nextLayout().label -> {
                layout = nextLayout()
                showKeyboard()
                keyboard.selectedPosition = layoutSwitchPosition()
                return
            }
            else -> query.append(key.lowercase())
        }
        onQueryChanged()
    }

    private fun onQueryChanged() {
        if (!phoneMode) queryView.text = query.toString()
        searchJob?.cancel()
        val text = query.toString().trim()
        if (text.length < MIN_QUERY) {
            results.replace(emptyList())
            showEmpty(R.string.search_hint)
            return
        }
        searchJob = scope.launch {
            delay(if (phoneMode) MOBILE_TYPING_PAUSE_MS else TYPING_PAUSE_MS)
            emptyView.visibility = View.GONE
            val page = loader.load { client.page(client.searchUrl(text)) }
            if (page == null) {
                showEmpty(R.string.load_failed)
                return@launch
            }
            results.replace(page.cards)
            if (page.cards.isEmpty()) showEmpty(R.string.nothing_found)
        }
    }

    private fun showEmpty(text: Int) {
        emptyView.setText(text)
        emptyView.visibility = View.VISIBLE
    }

    companion object {
        const val EXTRA_QUERY = "query"
        private const val SPACE = "␣"
        private const val BACKSPACE = "⌫"
        private const val CLEAR = "✕"
        private val EDIT_KEYS = listOf(SPACE, BACKSPACE, CLEAR)
        private const val KEY_COLUMNS = 6
        private const val MIN_QUERY = 2
        private const val TYPING_PAUSE_MS = 600L
        private const val MOBILE_TYPING_PAUSE_MS = 400L
        private const val STATE_QUERY = "query_state"
        private const val VOICE_REQUEST = 1001
    }
}

internal object VoiceSearchQuery {
    fun first(results: ArrayList<String>?): String? = results?.firstOrNull()?.takeIf { it.isNotBlank() }
}

package io.agora.realtimemusicclass.ui.view

import android.text.Editable
import android.text.TextWatcher

class TextInputLimiter(private val max: Int,
                       private val listener: TextLimiterListener?) : TextWatcher {
    private var strBeforeChanged: String? = null

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        strBeforeChanged = s.toString()
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        if (s.length > max) {
            listener?.onCharCountExceed(strBeforeChanged,
                s.toString(), start, before, count)
        }
    }

    override fun afterTextChanged(e: Editable) {

    }
}

interface TextLimiterListener {
    /**
     * Called when the newly input text exceeds the up limit of
     * character count
     * @param original text before changed
     * @param after text after changed
     * @param start the start position of the changed text
     * @param before the number of characters from start is replaced
     * @param count the count of new characters from start that replace
     * the old characters
     */
    fun onCharCountExceed(original: String?, after: String, start: Int, before: Int, count: Int)
}

val default: Int
    get() = 8
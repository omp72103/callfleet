package com.callfleet.app.ui

import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Last-resort screen that displays an uncaught crash so it can be read and
 * reported, instead of the app just vanishing. Built in code so it can't itself
 * fail to inflate a layout.
 */
class CrashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val trace = intent.getStringExtra(EXTRA_TRACE) ?: "No details."

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
        }
        root.addView(TextView(this).apply {
            text = "The app hit an error"
            textSize = 20f
            setPadding(0, 0, 0, 8)
        })
        root.addView(TextView(this).apply {
            text = "Screenshot this and send it so it can be fixed. Also saved to Android/data/com.callfleet.app/files/last_crash.txt"
            setPadding(0, 0, 0, 16)
        })
        val copyBtn = Button(this).apply {
            text = "Copy error"
            setOnClickListener {
                val cm = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cm.setPrimaryClip(android.content.ClipData.newPlainText("crash", trace))
            }
        }
        root.addView(copyBtn)

        val scroll = ScrollView(this)
        scroll.addView(TextView(this).apply {
            text = trace
            typeface = android.graphics.Typeface.MONOSPACE
            textSize = 11f
            setTextIsSelectable(true)
        })
        root.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ).apply { gravity = Gravity.TOP })

        setContentView(root)
    }

    companion object {
        const val EXTRA_TRACE = "trace"
    }
}

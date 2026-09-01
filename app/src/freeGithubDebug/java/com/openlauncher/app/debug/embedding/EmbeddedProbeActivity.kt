package com.openlauncher.app.debug.embedding

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class EmbeddedProbeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(Tag, "probe onCreate display=${display?.displayId}")

        val status = TextView(this).apply {
            text = "Embedded probe\ndisplay ${display?.displayId}"
            textSize = 28f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        val button = Button(this).apply {
            text = "Tap probe"
            setOnClickListener {
                status.text = "Tap received\ndisplay ${display?.displayId}"
                Log.i(Tag, "probe button tapped display=${display?.displayId}")
            }
        }

        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setBackgroundColor(Color.rgb(15, 89, 104))
                addView(
                    status,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1f,
                    ),
                )
                addView(
                    button,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ),
                )
            },
        )
    }

    override fun onStart() {
        super.onStart()
        Log.i(Tag, "probe onStart display=${display?.displayId}")
    }

    override fun onResume() {
        super.onResume()
        Log.i(Tag, "probe onResume display=${display?.displayId}")
    }

    override fun onPause() {
        Log.i(Tag, "probe onPause display=${display?.displayId}")
        super.onPause()
    }

    override fun onStop() {
        Log.i(Tag, "probe onStop display=${display?.displayId}")
        super.onStop()
    }

    override fun onDestroy() {
        Log.i(Tag, "probe onDestroy display=${display?.displayId}")
        super.onDestroy()
    }

    companion object {
        const val Tag = "OpenLauncherVDPoc"
    }
}

package com.phoneagent.ui.overlay

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.phoneagent.MainActivity
import com.phoneagent.R

class AgentOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var tvActionText: TextView
    private lateinit var tvStepCounter: TextView

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.phoneagent.UPDATE_OVERLAY") {
                val step = intent.getIntExtra("STEP", 0)
                val total = intent.getIntExtra("TOTAL", 0)
                val desc = intent.getStringExtra("DESC") ?: ""
                
                tvStepCounter.text = "$step/$total"
                tvActionText.text = if (desc.length > 30) desc.take(27) + "..." else desc
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_agent_status, null)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = 32
            y = 32
        }

        tvActionText = overlayView.findViewById(R.id.tvActionText)
        tvStepCounter = overlayView.findViewById(R.id.tvStepCounter)
        
        // Pulse animation
        val dot = overlayView.findViewById<View>(R.id.dotPulse)
        val alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0.2f, 1f)
        val animator = ObjectAnimator.ofPropertyValuesHolder(dot, alpha).apply {
            duration = 1000
            repeatCount = ObjectAnimator.INFINITE
        }
        animator.start()

        // Dragging logic
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        overlayView.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX - (event.rawX - initialTouchX).toInt()
                    params.y = initialY - (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(overlayView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // Detect click vs drag
                    val diffX = Math.abs(event.rawX - initialTouchX)
                    val diffY = Math.abs(event.rawY - initialTouchY)
                    if (diffX < 10 && diffY < 10) {
                        val i = Intent(this, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(i)
                    }
                    true
                }
                else -> false
            }
        }

        overlayView.findViewById<Button>(R.id.btnStopOverlay).setOnClickListener {
            val i = Intent("com.phoneagent.STOP_AGENT")
            sendBroadcast(i)
            stopSelf()
        }

        windowManager.addView(overlayView, params)

        val filter = IntentFilter("com.phoneagent.UPDATE_OVERLAY")
        registerReceiver(updateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            windowManager.removeView(overlayView)
            unregisterReceiver(updateReceiver)
        } catch (e: Exception) {}
    }
}

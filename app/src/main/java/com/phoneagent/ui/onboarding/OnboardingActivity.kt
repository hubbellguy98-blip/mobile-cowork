package com.phoneagent.ui.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.phoneagent.MainActivity
import com.phoneagent.R
import com.phoneagent.databinding.ActivityOnboardingBinding
import com.phoneagent.ui.settings.dataStore
import com.phoneagent.utils.AccessibilityUtils
import com.phoneagent.utils.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if already completed
        lifecycleScope.launch {
            val isComplete = dataStore.data.map { it[booleanPreferencesKey("onboarding_complete")] ?: false }.first()
            if (isComplete) {
                startActivity(Intent(this@OnboardingActivity, MainActivity::class.java))
                finish()
                return@launch
            }
        }

        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val pages = listOf(R.layout.page_onboarding_1, R.layout.page_onboarding_2, R.layout.page_onboarding_3, R.layout.page_onboarding_4)
        
        binding.viewPager.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val view = LayoutInflater.from(parent.context).inflate(pages[viewType], parent, false)
                return object : RecyclerView.ViewHolder(view) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val view = holder.itemView
                if (position == 2) {
                    val btnAccessibility = view.findViewById<Button>(R.id.btnEnableAccessibility)
                    val tvStatus = view.findViewById<TextView>(R.id.tvAccessibilityStatus)
                    
                    fun updateStatus() {
                        val isEnabled = AccessibilityUtils.isAccessibilityServiceEnabled(this@OnboardingActivity)
                        tvStatus.text = if (isEnabled) "✅ Enabled" else "❌ Disabled"
                        btnAccessibility.visibility = if (isEnabled) View.GONE else View.VISIBLE
                    }
                    
                    updateStatus()
                    btnAccessibility.setOnClickListener {
                        AccessibilityUtils.openAccessibilitySettings(this@OnboardingActivity)
                    }
                    
                    // Simple refresh button or rely on onResume
                    view.findViewById<Button>(R.id.btnNextFrom3)?.setOnClickListener {
                        if (AccessibilityUtils.isAccessibilityServiceEnabled(this@OnboardingActivity)) {
                            binding.viewPager.currentItem = 3
                        }
                    }
                }
                
                if (position == 3) {
                    val etKey = view.findViewById<android.widget.EditText>(R.id.etApiKey)
                    view.findViewById<Button>(R.id.btnGetStarted)?.setOnClickListener {
                        val key = etKey.text.toString()
                        if (key.isNotBlank()) {
                            lifecycleScope.launch {
                                dataStore.edit {
                                    it[stringPreferencesKey(Constants.DATASTORE_API_KEY)] = key
                                    it[booleanPreferencesKey("onboarding_complete")] = true
                                }
                                startActivity(Intent(this@OnboardingActivity, MainActivity::class.java))
                                finish()
                            }
                        }
                    }
                }
            }

            override fun getItemCount() = pages.size
            override fun getItemViewType(position: Int) = position
        }

        binding.btnSkip.setOnClickListener {
            binding.viewPager.currentItem = 2
        }
    }
}

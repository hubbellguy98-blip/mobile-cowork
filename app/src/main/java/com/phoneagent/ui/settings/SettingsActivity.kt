package com.phoneagent.ui.settings

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.phoneagent.R
import com.phoneagent.databinding.ActivitySettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        val models = listOf("grok-2-vision-1212", "grok-vision-beta")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, models)
        binding.actvModel.setAdapter(adapter)

        binding.btnSave.setOnClickListener {
            val apiKey = binding.etApiKey.text.toString()
            if (apiKey.isBlank()) {
                Toast.makeText(this, getString(R.string.api_key_empty), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newState = SettingsState(
                apiKey = apiKey,
                model = binding.actvModel.text.toString(),
                maxSteps = binding.etMaxSteps.text.toString(),
                showSteps = binding.swShowSteps.isChecked,
                confirmSensitive = binding.swConfirmSensitive.isChecked,
                autoStop = binding.swAutoStop.isChecked
            )
            viewModel.saveSettings(newState)
            Toast.makeText(this, getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnClearHistory.setOnClickListener {
            viewModel.clearHistory()
            Toast.makeText(this, getString(R.string.history_cleared), Toast.LENGTH_SHORT).show()
        }

        binding.btnExportLogs.setOnClickListener {
            viewModel.exportLogs()
            Toast.makeText(this, getString(R.string.logs_exported), Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (binding.etApiKey.text.toString() != state.apiKey) {
                        binding.etApiKey.setText(state.apiKey)
                    }
                    if (binding.actvModel.text.toString() != state.model) {
                        binding.actvModel.setText(state.model, false)
                    }
                    if (binding.etMaxSteps.text.toString() != state.maxSteps) {
                        binding.etMaxSteps.setText(state.maxSteps)
                    }
                    binding.swShowSteps.isChecked = state.showSteps
                    binding.swConfirmSensitive.isChecked = state.confirmSensitive
                    binding.swAutoStop.isChecked = state.autoStop
                }
            }
        }
    }
}

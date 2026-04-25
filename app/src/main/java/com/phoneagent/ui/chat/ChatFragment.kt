package com.phoneagent.ui.chat

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View

import com.phoneagent.service.PhoneAgentAccessibilityService
import com.phoneagent.utils.AccessibilityUtils
\n
import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.phoneagent.service.ScreenCaptureService
import android.os.Build

import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.phoneagent.R
import com.phoneagent.databinding.FragmentChatBinding
import com.phoneagent.ui.chat.adapter.MessageAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChatFragment : Fragment() {
    
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by viewModels()
    private lateinit var messageAdapter: MessageAdapter

    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val intent = Intent(requireContext(), ScreenCaptureService::class.java).apply {
                putExtra("RESULT_CODE", result.resultCode)
                putExtra("DATA", result.data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireContext().startForegroundService(intent)
            } else {
                requireContext().startService(intent)
            }
        }
        viewModel.onScreenCapturePermissionResult(result.resultCode, result.data)
    }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Accessibility Check setup
        binding.btnEnableAccessibility.setOnClickListener {
            AccessibilityUtils.openAccessibilitySettings(requireContext())
        }\n        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        
        // Auto show keyboard when opened
        binding.etMessage.requestFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.etMessage, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun // Accessibility Check setup
        binding.btnEnableAccessibility.setOnClickListener {
            AccessibilityUtils.openAccessibilitySettings(requireContext())
        }\n        setupRecyclerView() {
        messageAdapter = MessageAdapter()
        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString()
            if (text.isNotBlank()) {
                viewModel.sendMessage(text)
                binding.etMessage.text?.clear()
                
                // Dismiss keyboard on send
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(binding.etMessage.windowToken, 0)
            }
        }

        binding.btnStopAgent.setOnClickListener {
            viewModel.stopAgent()
        }

        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.action_chatFragment_to_settingsActivity)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.

                launch {
                    viewModel.sensitiveActionPending.collect { action ->
                        if (action != null) {
                            AlertDialog.Builder(requireContext())
                                .setTitle("Confirm Action")
                                .setMessage("The agent wants to: ${action.stepDescription ?: action.thought}\n\nThis action may have consequences. Allow it?")
                                .setPositiveButton("Allow") { _, _ ->
                                    viewModel.confirmSensitiveAction(true)
                                }
                                .setNegativeButton("Stop Agent") { _, _ ->
                                    viewModel.confirmSensitiveAction(false)
                                }
                                .setCancelable(false)
                                .show()
                        }
                    }
                }
                launch {
                    viewModel.needsScreenCapturePermission.collect { needsPermission ->
                        if (needsPermission && !ScreenCaptureService.isRunning()) {
                            showScreenCaptureDialog()
                        }
                    }
                }

                launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.messagesList.collect { messages ->
                        messageAdapter.submitList(messages) {
                            if (messages.isNotEmpty()) {
                                binding.rvMessages.smoothScrollToPosition(messages.size - 1)
                            }
                        }
                    }
                }

                launch {
                    viewModel.isAgentRunning.collect { isRunning ->
                        binding.btnStopAgent.visibility = if (isRunning) View.VISIBLE else View.GONE
                        binding.agentStatusCard.visibility = if (isRunning) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.agentStatus.collect { status ->
                        if (viewModel.isAgentRunning.value) {
                            binding.tvAgentStatus.text = getString(
                                R.string.agent_working,
                                status.step,
                                status.totalSteps
                            )
                        }
                    }
                }
            }
        }
    }

    
    private fun showScreenCaptureDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Screen Capture Needed")
            .setMessage("PhoneAgent needs to see your screen so the AI can understand what's happening.\nYour screen content is only sent to Grok AI and never stored anywhere else.")
            .setPositiveButton("I Understand, Continue") { _, _ ->
                val manager = requireContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                screenCaptureLauncher.launch(manager.createScreenCaptureIntent())
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                viewModel.onScreenCapturePermissionResult(Activity.RESULT_CANCELED, null)
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.example.androidappsample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import android.content.Intent
import com.example.androidappsample.databinding.FragmentDashboardBinding
import com.example.androidappsample.data.SessionManager
import com.example.androidappsample.data.UserRepository
import com.example.androidappsample.users.UserListActivity
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.textCurrentUser.text = "Current session: ${SessionManager(requireContext()).displayName}"
        binding.buttonDirectory.setOnClickListener { startActivity(Intent(requireContext(), UserListActivity::class.java)) }
        binding.buttonRetry.setOnClickListener { loadDashboard() }
        loadDashboard()
    }

    private fun loadDashboard() {
        binding.progressBar.visibility = View.VISIBLE
        binding.buttonRetry.visibility = View.GONE
        binding.textServerStatus.text = "Checking hosted API…"
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching { UserRepository().getStats() }
                .onSuccess { (stats, latest) ->
                    binding.textTotal.text = stats.total.toString()
                    binding.textActive.text = stats.active.toString()
                    binding.textDisabled.text = stats.disabled.toString()
                    binding.textServerStatus.text = "Online • HTTPS API connected"
                    binding.textServerStatus.setTextColor(requireContext().getColor(R.color.success))
                    binding.textLatest.text = latest.joinToString("\n\n") { "${it.displayName}\n@${it.username}" }.ifBlank { "No accounts yet" }
                }
                .onFailure {
                    binding.textServerStatus.text = it.message ?: "Server unavailable"
                    binding.textServerStatus.setTextColor(requireContext().getColor(R.color.danger))
                    binding.buttonRetry.visibility = View.VISIBLE
                }
            binding.progressBar.visibility = View.GONE
        }
    }
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

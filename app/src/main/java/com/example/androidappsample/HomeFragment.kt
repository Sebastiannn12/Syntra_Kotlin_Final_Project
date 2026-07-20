package com.example.androidappsample

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.androidappsample.data.SessionManager
import com.example.androidappsample.databinding.FragmentHomeBinding
import com.example.androidappsample.users.UserListActivity

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val session = SessionManager(requireContext())
        binding.textGreeting.text = "Hello, ${session.displayName.substringBefore(' ')}"
        binding.textHandle.text = getString(R.string.signed_in_as, session.username)
        binding.buttonDirectory.setOnClickListener {
            startActivity(Intent(requireContext(), UserListActivity::class.java))
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

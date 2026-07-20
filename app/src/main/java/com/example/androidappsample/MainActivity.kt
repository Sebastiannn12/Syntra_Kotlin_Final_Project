package com.example.androidappsample

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.androidappsample.auth.LoginActivity
import com.example.androidappsample.auth.ChangePasswordActivity
import com.example.androidappsample.data.SessionManager
import com.example.androidappsample.data.UserRepository
import com.example.androidappsample.databinding.ActivityMainBinding
import com.example.androidappsample.users.UserDetailsDialogFragment
import com.example.androidappsample.users.UserListActivity
import com.example.androidappsample.users.UserFormActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val session by lazy { SessionManager(this) }
    private val repository = UserRepository()

    private val channelId = "demo_channel"
    private  val notificationId = 1

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            granted ->
                if (granted) sendNotification()
                else Toast.makeText(this, "Notification permission denied", Toast.LENGTH_LONG).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        if (!session.isSignedIn) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        enableEdgeToEdge()

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.app_name)
        supportActionBar?.subtitle = getString(R.string.signed_in_as, session.username)

        createNotificationChannel()

        val bottomNav = binding.bottomNavigation
        bottomNav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_dashboard -> DashboardFragment()
                R.id.nav_notifications -> NotificationFragment()
                else -> HomeFragment()
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
            true
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (savedInstanceState == null) bottomNav.selectedItemId = R.id.nav_home
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_users -> {
            startActivity(Intent(this, UserListActivity::class.java))
            true
        }
        R.id.action_profile -> {
            showProfile()
            true
        }
        R.id.action_edit_profile -> {
            startActivity(Intent(this, UserFormActivity::class.java).putExtra(UserFormActivity.EXTRA_USER_ID, session.userId))
            true
        }
        R.id.action_change_password -> {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
            true
        }
        R.id.action_settings -> {
            Toast.makeText(this, "Settings clicked.", Toast.LENGTH_LONG).show()
            true
        }
        R.id.action_about -> {
            MaterialAlertDialogBuilder(this)
                .setIcon(R.mipmap.ic_launcher)
                .setTitle(R.string.app_name)
                .setMessage(R.string.about_message)
                .setPositiveButton(android.R.string.ok, null)
                .show()
            true
        }
        R.id.action_notify -> {
            checkPermissionAndNotify()
            true
        }
        R.id.action_logout -> {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.logout)
                .setMessage("End this session on this device?")
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.logout) { _, _ -> logout() }
                .show()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun showProfile() {
        lifecycleScope.launch {
            runCatching { repository.me() }
                .onSuccess {
                    session.updateUser(it)
                    supportActionBar?.subtitle = getString(R.string.signed_in_as, it.username)
                    UserDetailsDialogFragment.newInstance(it).show(supportFragmentManager, "profile")
                }
                .onFailure { Toast.makeText(this@MainActivity, it.message, Toast.LENGTH_LONG).show() }
        }
    }

    private fun logout() {
        lifecycleScope.launch {
            runCatching { repository.logout() }
            session.clear()
            startActivity(Intent(this@MainActivity, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(channelId, "Demo channel", NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = "Syntra updates" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun checkPermissionAndNotify() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
        {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            sendNotification()
        }
    }

    private fun sendNotification() {
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.notifications)
            .setContentTitle("Demo notification")
            .setContentText("This is the notification body")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(notificationId, notification)
    }
}

@file:Suppress("DEPRECATION")

package com.shadr.miflipperapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import com.shadr.miflipperapp.databinding.FragmentSettingsBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FlipViewModel by lazy {
        (requireActivity().application as MiFlipperApplication).mainViewModel
    }

    private val DISCORD_INVITE_URL = "https://discord.gg/Dcy4UaZkm2"
    private val PREFS_NAME = "MiFlipperAppPrefs"
    private val KEY_LANGUAGE = "language_code"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupServerSpinner()
        setupLanguageSpinner()
        setupPremiumCheckbox()
        setupLogoutButton()
        setupNotificationMinProfitFilter()
        setupDiscordButton()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupLanguageSpinner() {
        val languages = arrayOf(
            getString(R.string.lang_english),
            getString(R.string.lang_spanish),
            getString(R.string.lang_portuguese)
        )
        val codes = arrayOf("en", "es", "pt")
        val prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, languages)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.languageSpinner.adapter = adapter

        val currentCode = LocaleHelper.getPersistedLocaleCode(requireContext())

        val initialPosition = codes.indexOf(currentCode)
        binding.languageSpinner.setSelection(initialPosition.coerceAtLeast(0), false)

        binding.languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCode = codes[position]
                setAppLocale(selectedCode)
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    private fun setAppLocale(languageCode: String) {
        LocaleHelper.setLocale(requireContext(), languageCode)

        val localeList = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(localeList)

        updateViewModelStatusText(requireContext(), languageCode)

        requireActivity().recreate()
    }

    private fun updateViewModelStatusText(context: Context, languageCode: String) {
        val localizedContext = LocaleHelper.setLocale(context, languageCode)

        val currentServerPosition = viewModel.selectedSpinnerPosition
        val serversINTENT = arrayOf("Stop Service", "Europe", "America", "Asia")

        if (currentServerPosition > 0) {
            val actualServerName = serversINTENT[currentServerPosition]
            val statusText = localizedContext.getString(R.string.service_started, actualServerName)
            viewModel.statusText.value = statusText
        } else {
            viewModel.statusText.value = localizedContext.getString(R.string.service_stopped)
        }
    }

    private fun setupServerSpinner() {
        val serversUI = arrayOf(
            getString(R.string.server_action_stop),
            getString(R.string.server_europe),
            getString(R.string.server_america),
            getString(R.string.server_asia)
        )
        val serversINTENT = arrayOf("Stop Service", "Europe", "America", "Asia")

        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, serversUI)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.serverSpinner.adapter = adapter

        binding.serverSpinner.setSelection(viewModel.selectedSpinnerPosition, false)

        binding.serverSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val currentContext = requireContext()

                if (position == viewModel.selectedSpinnerPosition) return

                viewModel.selectedSpinnerPosition = position

                requireActivity().stopService(Intent(requireContext(), FlipDetectionService::class.java))
                viewModel.clearFlipList()

                if (position > 0) {
                    val actualServerName = serversINTENT[position]

                    val serviceIntent = Intent(requireContext(), FlipDetectionService::class.java)
                    serviceIntent.putExtra("SERVER_NAME", actualServerName)
                    requireActivity().startService(serviceIntent)

                    viewModel.isServiceRunning.value = true

                    val statusText = currentContext.getString(R.string.service_started, actualServerName)
                    viewModel.statusText.value = statusText
                } else {
                    viewModel.isServiceRunning.value = false

                    viewModel.statusText.value = currentContext.getString(R.string.service_stopped)
                }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    private fun setupLogoutButton() {
        binding.logoutButton.setOnClickListener {

            viewModel.isServiceRunning.value = false
            viewModel.statusText.value = requireContext().getString(R.string.service_stopped)
            viewModel.selectedSpinnerPosition = 0

            requireActivity().stopService(Intent(requireContext(), FlipDetectionService::class.java))

            lifecycleScope.launch {
                delay(300)

                Firebase.auth.signOut()
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }
    }

    private fun setupDiscordButton() {
        binding.discordSupportButtonIcon.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DISCORD_INVITE_URL))
            startActivity(intent)
        }
    }

    private fun setupPremiumCheckbox() {
        val prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val initialPremiumState = prefs.getBoolean("is_premium", false)
        binding.premiumCheckbox.isChecked = initialPremiumState
        viewModel.setPremiumStatus(initialPremiumState)

        binding.premiumCheckbox.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("is_premium", isChecked).apply()
            viewModel.setPremiumStatus(isChecked)

            val intent = Intent(requireContext(), FlipDetectionService::class.java).apply {
                action = FlipDetectionService.ACTION_UPDATE_PREMIUM_STATUS
                putExtra(FlipDetectionService.EXTRA_PREMIUM_STATUS, isChecked)
            }
            requireActivity().startService(intent)
        }
    }


    private fun setupNotificationMinProfitFilter() {
        val prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val defaultProfit = 200000

        val savedMinProfit = prefs.getInt("min_notification_profit", defaultProfit)
        val isFilterActive = prefs.getBoolean("is_notification_filter_active", true)

        binding.notificationMinProfitEdittext?.setText(savedMinProfit.toString())
        binding.notificationFilterSwitch?.isChecked = isFilterActive

        binding.notificationMinProfitEdittext?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val minProfit = s.toString().toIntOrNull() ?: 0
                prefs.edit().putInt("min_notification_profit", minProfit).apply()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.notificationMinProfitEdittext?.setOnFocusChangeListener { view, hasFocus ->
            if (!hasFocus) {
                val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
            }
        }

        binding.notificationFilterSwitch?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("is_notification_filter_active", isChecked).apply()
        }
    }
}
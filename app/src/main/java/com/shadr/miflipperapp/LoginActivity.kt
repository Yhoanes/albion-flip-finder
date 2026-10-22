package com.shadr.miflipperapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.shadr.miflipperapp.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.FieldValue
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>
    private lateinit var gso: GoogleSignInOptions

    private val DISCORD_INVITE_URL = "https://discord.gg/Dcy4UaZkm2"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        stopService(Intent(this, FlipDetectionService::class.java))

        auth = Firebase.auth
        firestore = FirebaseFirestore.getInstance()

        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            binding.progressBar.visibility = View.VISIBLE

            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    firebaseAuthWithGoogle(account.idToken!!)
                } else {
                    showToastOnMain(getString(R.string.error_signin_null_account))
                    binding.progressBar.visibility = View.GONE
                }
            } catch (e: ApiException) {
                showToastOnMain(getString(R.string.error_signin_api, e.statusCode))
                binding.progressBar.visibility = View.GONE
            } catch (e: Exception) {
                showToastOnMain(getString(R.string.error_unexpected))
                binding.progressBar.visibility = View.GONE
            }
        }

        binding.loginButton.setOnClickListener {

            googleSignInClient.signOut()
            binding.progressBar.visibility = View.VISIBLE


            googleSignInClient = GoogleSignIn.getClient(this, gso)

            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }


        binding.discordIconContainer.setOnClickListener {
            openDiscordInvite()
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            binding.progressBar.visibility = View.VISIBLE
            checkLicenseAndProceed(currentUser.uid)
        } else {
            googleSignInClient.signOut()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        checkLicenseAndProceed(user.uid)
                    }
                } else {
                    showToastOnMain(getString(R.string.auth_failed))
                    binding.progressBar.visibility = View.GONE
                }
            }
    }

    private fun checkLicenseAndProceed(userId: String) = CoroutineScope(Dispatchers.IO).launch {
        try {
            val deviceId = getUniqueDeviceId()
            val licenseDoc = firestore.collection("licenses").document(userId).get().await()

            if (licenseDoc.exists()) {
                val storedDeviceId = licenseDoc.getString("deviceId")
                var expiryDate = licenseDoc.getLong("expiryDate") ?: 0
                var isPremium = licenseDoc.getBoolean("isPremium") ?: false

                val currentTime = System.currentTimeMillis()

                if (isPremium && expiryDate <= currentTime) {
                    val updates = hashMapOf<String, Any>(
                        "isPremium" to false,
                        "expiryDate" to 0L
                    )
                    firestore.collection("licenses").document(userId).update(updates).await()

                    isPremium = false
                    expiryDate = 0L

                    withContext(Dispatchers.Main) {
                        showToastOnMain(getString(R.string.msg_premium_expired))
                    }
                }

                val pendingDeviceId = licenseDoc.getString("pendingDeviceId")
                val transferAuthorized = licenseDoc.getBoolean("transferAuthorized") ?: false

                if (storedDeviceId == deviceId) {
                    if (pendingDeviceId != null || transferAuthorized) {
                        resetTransferFields(userId)
                    }

                    if (isPremium || expiryDate > currentTime) {
                        val statusMessage = getLicenseStatusMessage(isPremium, expiryDate, currentTime)
                        showToastOnMain(statusMessage)

                        startMainActivity()
                    } else {
                        showToastOnMain(getString(R.string.msg_trial_expired))
                        auth.signOut()
                        withContext(Dispatchers.Main) {
                            binding.progressBar.visibility = View.GONE
                        }
                    }
                } else {
                    if (isPremium) {
                        if (transferAuthorized) {
                            if (pendingDeviceId == deviceId) {
                                resetTransferFields(userId)
                                showToastOnMain(getString(R.string.msg_transfer_granted))
                                startMainActivity()
                            } else {
                                showToastOnMain(getString(R.string.security_error_auth_device))
                                auth.signOut()
                                withContext(Dispatchers.Main) {
                                    binding.progressBar.visibility = View.GONE
                                }
                            }

                        } else {
                            registerNewDeviceIdForTransfer(userId, deviceId)

                            showToastOnMain(getString(R.string.security_block_transfer_required))
                            auth.signOut()
                            withContext(Dispatchers.Main) {
                                binding.progressBar.visibility = View.GONE
                            }
                        }
                    } else {
                        showToastOnMain(getString(R.string.security_error_trial_transfer))
                        auth.signOut()
                        withContext(Dispatchers.Main) {
                            binding.progressBar.visibility = View.GONE
                        }
                    }
                }
            } else {
                val existingDeviceDocs = firestore.collection("licenses")
                    .whereEqualTo("deviceId", deviceId)
                    .get().await()

                if (existingDeviceDocs.isEmpty) {
                    createTrialLicense(userId, deviceId)
                } else {
                    showToastOnMain(getString(R.string.security_error_trial_used))
                    auth.signOut()
                    withContext(Dispatchers.Main) {
                        binding.progressBar.visibility = View.GONE
                    }
                }
            }
        } catch (e: Exception) {
            showToastOnMain(getString(R.string.error_critical_license, e.message ?: "Unknown Error"))
            auth.signOut()
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun registerNewDeviceIdForTransfer(userId: String, newDeviceId: String) = CoroutineScope(Dispatchers.IO).launch {
        try {
            val updates = hashMapOf<String, Any>(
                "pendingDeviceId" to newDeviceId,
                "transferAuthorized" to false
            )
            firestore.collection("licenses").document(userId).set(updates, SetOptions.merge()).await()
        } catch (e: Exception) {}
    }

    private fun updateLicenseDeviceIdAndResetTransfer(userId: String, newDeviceId: String) = CoroutineScope(Dispatchers.IO).launch {
        try {
            val updates = hashMapOf<String, Any>(
                "lastTransferDate" to System.currentTimeMillis(),
                "transferAuthorized" to false,
                "pendingDeviceId" to FieldValue.delete()
            )
            firestore.collection("licenses").document(userId).update(updates).await()
        } catch (e: Exception) {}
    }

    private fun resetTransferFields(userId: String) = CoroutineScope(Dispatchers.IO).launch {
        try {
            val updates = hashMapOf<String, Any>(
                "transferAuthorized" to false,
                "pendingDeviceId" to FieldValue.delete()
            )
            firestore.collection("licenses").document(userId).update(updates).await()
        } catch (e: Exception) {}
    }

    private fun createTrialLicense(userId: String, deviceId: String) = CoroutineScope(Dispatchers.IO).launch {
        try {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, 7)
            val expiryTime = calendar.timeInMillis

            val licenseData = hashMapOf<String, Any?>(
                "deviceId" to deviceId,
                "expiryDate" to expiryTime,
                "isPremium" to false,
                "trialActivated" to true,
                "pendingDeviceId" to null,
                "transferAuthorized" to false
            )

            firestore.collection("licenses").document(userId).set(licenseData).await()

            showToastOnMain(getString(R.string.msg_trial_activated))
            startMainActivity()

        } catch (e: Exception) {
            showToastOnMain(getString(R.string.error_critical_create_license))
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun getUniqueDeviceId(): String {
        return Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN_ID"
    }

    private fun getLicenseStatusMessage(isPremium: Boolean, expiryDate: Long, currentTime: Long): String {
        if (isPremium) {
            val date = Date(expiryDate)
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val formattedDate = dateFormat.format(date)
            return getString(R.string.status_premium, formattedDate)
        } else {
            val remainingMillis = expiryDate - currentTime
            val remainingDays = (remainingMillis / (1000 * 60 * 60 * 24)).toInt() + 1

            return if (remainingDays > 0) {
                getString(R.string.status_trial, remainingDays)
            } else {
                getString(R.string.status_expired)
            }
        }
    }

    private fun startMainActivity() {
        val intent = Intent(this@LoginActivity, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun openDiscordInvite() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DISCORD_INVITE_URL))
        startActivity(intent)
    }

    private fun showToastOnMain(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }
}
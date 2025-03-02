package com.example.talky.viewmodels

import android.app.Activity
import android.net.Uri
import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.talky.R
import com.example.talky.navigation.Screen
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class OtpState(
    val otpCode: String = "",
    val otpError: String? = null,
    val isSendingOtp: Boolean = false,
    val isVerifying: Boolean = false,
    val loginSuccess: Boolean = false,
    val verificationId: String? = null,
    val resendToken: PhoneAuthProvider.ForceResendingToken? = null,
    val timerSeconds: Int = 0,
    val canResend: Boolean = true
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authPreferences: AuthPreferences
) : ViewModel(){

private val auth = FirebaseAuth.getInstance()
    private val _otpState = MutableStateFlow(OtpState())
    val otpState: StateFlow<OtpState> = _otpState

    private var countDownTimer: CountDownTimer? = null
    private val timeoutSeconds = 60L

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    private val _isAuthenticated = MutableStateFlow(auth.currentUser != null)
    val isAuthenticated: StateFlow<Boolean> get() = _isAuthenticated


    private val tag = "AuthViewModel" // Log tag for this ViewModel

    fun checkUserAuth(navController: NavController) {
        viewModelScope.launch {
            Log.d(tag, "Checking user authentication...")

            val user = auth.currentUser
            val isLoggedIn = authPreferences.isUserLoggedIn()

            if (user != null && isLoggedIn) {
                Log.d(tag, "✅ User is logged in. Navigating to Main Screen...")
                navController.navigate(Screen.ChatList.route) {
                    popUpTo(0) // Clears back stack
                }
            } else {
                Log.d(tag, "🚫 No user found or not logged in. Navigating to Intro screen...")
                navController.navigate(Screen.Intro.route) {
                    popUpTo(0) // Clears back stack
                }
            }
        }
    }

    fun getCurrentUserId(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }


    fun getUserName(onResult: (String?) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return onResult(null)

        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("name") // Fetches the "name" field
                    onResult(name)
                } else {
                    Log.e(tag, "User document does not exist in Firestore")
                    onResult(null)
                }
            }
            .addOnFailureListener { exception ->
                Log.e(tag, "Error fetching user name: ${exception.message}")
                onResult(null)
            }
    }


    fun saveUserProfile(
        name: String,
        phoneNumber: String,
        profileUri: Uri?,
        profileResId: Int?,
        onComplete: (Boolean) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: return onComplete(false)
        Log.d(tag, "Saving user profile for user: $userId")

        // Save to preferences
        authPreferences.saveAuthStatus(true)
        authPreferences.savePhoneNumber(phoneNumber)

        // Profile image URL logic
        val profileImageUrl = when {
            profileUri != null -> null // URI means image will be uploaded
            profileResId != null -> getDefaultProfileImageUrl(profileResId) // Get URL for the default profile image
            else -> "" // No profile image
        }

        // If the profile image URL is not null, save it to Firestore directly
        if (profileImageUrl != null) {
            saveUserToFirestore(userId, name, phoneNumber, profileImageUrl, onComplete)
            return
        }

        // Upload profile image if a new image is selected
        val profileRef = storage.reference.child("profile_images/$userId.jpg")
        profileRef.putFile(profileUri!!)
            .addOnSuccessListener {
                profileRef.downloadUrl.addOnSuccessListener { uri ->
                    saveUserToFirestore(userId, name, phoneNumber, uri.toString(), onComplete)
                }
            }
            .addOnFailureListener {
                Log.e(tag, "Error uploading profile image: ${it.message}")
                onComplete(false)
            }
    }
    private fun getDefaultProfileImageUrl(profileResId: Int): String {
        // Return a URL or path to the default image based on the resource ID
        return when (profileResId) {
            R.drawable.man -> "https://firebasestorage.googleapis.com/v0/b/talky-23afa.firebasestorage.app/o/profile_images%2Fman.png?alt=media&token=6001e33f-86bc-4a71-8dd9-114843e997c8"
            // Add more cases for other default images as needed
            else -> "https://example.com/default_profile_picture.jpg" // Fallback to a generic default image
        }
    }
    private fun saveUserToFirestore(
        userId: String,
        name: String,
        phoneNumber: String,
        profileImageUrl: String,
        onComplete: (Boolean) -> Unit
    ) {
        Log.d(tag, "Saving user data to Firestore for user: $userId")
        val userMap = mapOf(
            "userId" to userId,
            "name" to name,
            "phoneNumber" to phoneNumber,
            "profileImageUrl" to profileImageUrl // Save the image URL (either from Firebase storage or default)
        )
        firestore.collection("users").document(userId).set(userMap)
            .addOnSuccessListener {
                Log.d(tag, "User saved successfully.")
                authPreferences.saveProfileSetupStatus(true)
                onComplete(true)
            }
            .addOnFailureListener {
                Log.e(tag, "Error saving user to Firestore: ${it.message}")
                onComplete(false)
            }
    }


    fun resetOtpState() {
        _otpState.value = OtpState() // Resets all fields to default values
        Log.d(tag, "OTP state reset")
    }

    fun startTimer() {
        // Start a new countdown timer
        countDownTimer = object : CountDownTimer(timeoutSeconds * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _otpState.update {
                    it.copy(timerSeconds = (millisUntilFinished / 1000).toInt(), canResend = false)
                }
                Log.d(tag, "Timer ticking: ${millisUntilFinished / 1000} seconds remaining")
            }

            override fun onFinish() {
                _otpState.update { it.copy(canResend = true) }
                Log.d(tag, "Timer finished. Resend enabled.")
            }
        }.start()
    }


    fun sendOtp(phoneNumber: String, activity: Activity?, navController: NavController) {
        Log.d(tag, "Sending OTP to phone number: $phoneNumber")

        if (!phoneNumber.matches("^\\+?[1-9]\\d{6,14}$".toRegex())) {
            _otpState.update { it.copy(otpError = "Invalid phone number format!") }
            Log.d(tag, "Invalid phone number format!")
            return
        }

        if (activity == null) {
            _otpState.update { it.copy(otpError = "Error: Activity is null") }
            Log.e(tag, "Activity is null, cannot send OTP.")
            return
        }

        _otpState.update { it.copy(isSendingOtp = true, otpError = null) }

        firestore.collection("users")
            .whereEqualTo("phoneNumber", phoneNumber)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    // New user, send OTP for verification
                    sendOtpRequest(phoneNumber, activity, navController)


                }
            }
            .addOnFailureListener { exception ->
                _otpState.update { it.copy(otpError = "Error checking user existence: ${exception.message}") }
                Log.e(tag, "Error checking user existence: ${exception.message}")
            }
    }

    private fun sendOtpRequest(
        phoneNumber: String,
        activity: Activity?,
        navController: NavController
    ) {
        val options = activity?.let { it ->
            PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .setActivity(it)
                .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        auth.signInWithCredential(credential).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                _otpState.update {
                                    it.copy(
                                        loginSuccess = true,
                                        isVerifying = false
                                    )
                                }
                                Log.d(tag, "Login successful, navigating to profile setup.")
                                navController.navigate(Screen.ProfileSetup.route)
                            } else {
                                _otpState.update {
                                    it.copy(
                                        otpError = "Auto login failed: ${task.exception?.message}",
                                        isVerifying = false
                                    )
                                }
                                Log.e(tag, "Auto login failed: ${task.exception?.message}")
                            }
                        }
                    }

                    override fun onVerificationFailed(e: FirebaseException) {
                        val errorMessage = when (e) {
                            is FirebaseAuthInvalidCredentialsException -> "Invalid phone number format!"
                            else -> "Verification failed. Please try again."
                        }
                        _otpState.update {
                            it.copy(
                                otpError = errorMessage,
                                isSendingOtp = false,
                                isVerifying = false
                            )
                        }
                        Log.e(tag, "Verification failed: $errorMessage")
                    }

                    override fun onCodeSent(
                        verificationId: String,
                        token: PhoneAuthProvider.ForceResendingToken
                    ) {
                        _otpState.update {
                            it.copy(
                                verificationId = verificationId,
                                resendToken = token,
                                isSendingOtp = false,
                                isVerifying = false
                            )
                        }
                        Log.d(tag, "OTP sent. Verification ID: $verificationId")
                        startTimer()
                    }
                })
                .build()
        }

        if (options != null) {
            PhoneAuthProvider.verifyPhoneNumber(options)
            Log.d(tag, "PhoneAuthProvider.verifyPhoneNumber called.")
        }
    }

    fun verifyOtp(activity: Activity?, navController: NavController) {
        val state = _otpState.value
        Log.d(tag, "Verifying OTP code: ${state.otpCode}")

        if (state.otpCode.length < 6 || state.verificationId == null) {
            _otpState.update { it.copy(otpError = "Enter a valid 6-digit OTP") }
            Log.d(tag, "OTP code is invalid.")
            return
        }

        if (activity == null) {
            _otpState.update { it.copy(otpError = "Error: Activity is null") }
            Log.e(tag, "Activity is null, cannot verify OTP.")
            return
        }

        _otpState.update { it.copy(isVerifying = true) }

        val credential = PhoneAuthProvider.getCredential(state.verificationId, state.otpCode)

        auth.signInWithCredential(credential)
            .addOnCompleteListener(activity) { task ->
                if (task.isSuccessful) {
                    authPreferences.saveAuthStatus(true)
                    _otpState.update { it.copy(isVerifying = false, loginSuccess = true) }
                    Log.d(tag, "OTP verified successfully. Navigating to profile setup.")
                    navController.navigate(Screen.ProfileSetup.route)

                    // Stop the timer after successful login
                    countDownTimer?.cancel()
                    Log.d(tag, "Timer stopped after successful login.")
                } else {
                    val errorMessage =
                        if (task.exception is FirebaseAuthInvalidCredentialsException) {
                            "Invalid OTP. Try again!"
                        } else {
                            "OTP Verification Failed: ${task.exception?.message}"
                        }
                    _otpState.update { it.copy(isVerifying = false, otpError = errorMessage) }
                    Log.e(tag, errorMessage)
                }
            }
    }

    fun setVerificationDetails(
        verificationId: String,
        resendToken: PhoneAuthProvider.ForceResendingToken
    ) {
        _otpState.update { it.copy(verificationId = verificationId, resendToken = resendToken) }
        Log.d(tag, "Verification details set: $verificationId")
    }

    fun updateOtpCode(newOtpCode: String) {
        _otpState.update { it.copy(otpCode = newOtpCode) }
        Log.d(tag, "OTP code updated: $newOtpCode")
    }

    fun resendOtp(phoneNumber: String, activity: Activity, navController: NavController) {
        Log.d(tag, "Resending OTP for phone number: $phoneNumber")
        sendOtpRequest(phoneNumber, activity, navController)
    }

    override fun onCleared() {
        super.onCleared()
        countDownTimer?.cancel()
        Log.d(tag, "ViewModel cleared and timer canceled.")
    }

}

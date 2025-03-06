package com.example.talky.viewmodels

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.talky.ApiService
import com.example.talky.navigation.Screen
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
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

data class UserState(
    val username: String? = null,
    val profilePicUrl: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authPreferences: AuthPreferences,
    private val apiService: ApiService,
    private val auth: FirebaseAuth,
) : ViewModel() {


    private val _otpState = MutableStateFlow(OtpState())
    val otpState: StateFlow<OtpState> = _otpState

    private var countDownTimer: CountDownTimer? = null
    private val timeoutSeconds = 60L


    private val _isAuthenticated = MutableStateFlow(auth.currentUser != null)
    val isAuthenticated: StateFlow<Boolean> get() = _isAuthenticated


    private val tag = "AuthViewModel" // Log tag for this ViewModel

    init {
        fetchUserData()
    }

    private val _userState = MutableStateFlow(UserState()) // ✅ Ensure it's MutableStateFlow
    val userState: StateFlow<UserState> = _userState

    internal fun fetchUserData() {
        val firebaseUser = auth.currentUser ?: return
        val uid = firebaseUser.uid.takeIf { it.isNotBlank() } ?: return

        viewModelScope.launch {
            try {
                val response = apiService.getUser(uid)
                if (response.isSuccessful) {
                    response.body()?.let { user ->
                        _userState.value = UserState(username = user.username, profilePicUrl = user.profilePic)
                    }
                } else {
                    Log.e(tag, "❌ Failed to fetch user data")
                }
            } catch (e: Exception) {
                Log.e(tag, "❌ Error fetching user data", e)
            }
        }
    }




    fun checkUserAuth(navController: NavController) {
        viewModelScope.launch {
            Log.d(tag, "Checking user authentication...")

            if (authPreferences.isUserLoggedIn()) {
                Log.d(tag, "✅ User is logged in. Navigating to ChatList screen...")
                navController.navigate(Screen.ChatList.route) {
                    popUpTo(0)
                }
            } else {
                Log.d(tag, "🚫 No valid user found. Navigating to Intro screen...")
                navController.navigate(Screen.Intro.route) {
                    popUpTo(0)
                }
            }
        }
    }


    fun getCurrentUserId(): String? = auth.currentUser?.uid


    fun saveUserProfile(
        name: String,  // ✅ This is the actual username entered by the user
        phoneNumber: String,
        profileUri: Uri?,
        context: Context,
        onComplete: (Boolean) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: return onComplete(false)
        Log.d(tag, "Saving user profile for user: $userId")

        authPreferences.saveAuthStatus(true)
        authPreferences.savePhoneNumber(phoneNumber)

        if (profileUri == null) {
            saveUserToBackend(name, phoneNumber, "", onComplete)
            return
        }

        // ✅ Pass actual username to uploadProfilePicture
        uploadProfilePicture(userId, name, profileUri, context) { uploadedImageUrl ->
            if (uploadedImageUrl != null) {
                saveUserToBackend(name, phoneNumber, uploadedImageUrl, onComplete)
            } else {
                onComplete(false)
            }
        }
    }


    private fun uploadProfilePicture(
        userId: String,
        username: String,  // ✅ Use the actual username provided by the user
        imageUri: Uri,
        context: Context,
        onComplete: (String?) -> Unit
    ) {
        val file = uriToFile(imageUri, context, userId)
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())

        val body = MultipartBody.Part.createFormData("profile", file.name, requestFile)
        val usernameRequestBody = username.toRequestBody("text/plain".toMediaTypeOrNull()) // ✅ Send actual username
        val uidRequestBody = userId.toRequestBody("text/plain".toMediaTypeOrNull()) // ✅ Send UID

        viewModelScope.launch {
            try {
                val response = apiService.uploadProfilePicture(body, usernameRequestBody, uidRequestBody)
                if (response.isSuccessful) {
                    val imageUrl = response.body()?.imageUrl
                    Log.d(tag, "✅ Profile picture uploaded: $imageUrl")
                    onComplete(imageUrl)
                } else {
                    Log.e(tag, "❌ Failed to upload profile picture: ${response.errorBody()?.string()}")
                    onComplete(null)
                }
            } catch (e: Exception) {
                Log.e(tag, "❌ Exception: ${e.message}")
                onComplete(null)
            }
        }
    }



    private fun uriToFile(uri: Uri, context: Context, userId: String): File {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val file = File(context.cacheDir, "$userId.jpg") // Unique file name
        inputStream?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        return file
    }

    private fun saveUserToBackend(
        name: String,
        phoneNumber: String,
        profileImageUrl: String,
        onComplete: (Boolean) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: return onComplete(false).also {
            Log.e(tag, "❌ Firebase UID is null. Cannot save user.")
        }

        val userMap = mapOf(
            "uid" to uid,
            "username" to name,
            "profilePic" to profileImageUrl,
            "phoneNumber" to phoneNumber
        )

        viewModelScope.launch {
            try {
                val response = apiService.saveUser(userMap)
                if (response.isSuccessful) {
                    Log.d(tag, "✅ User saved to PostgreSQL successfully.")
                    authPreferences.saveProfileSetupStatus(true)
                    onComplete(true)
                } else {
                    Log.e(tag, "❌ Failed to save user: ${response.errorBody()?.string()}")
                    onComplete(false)
                }
            } catch (e: Exception) {
                Log.e(tag, "❌ Exception: ${e.message}")
                onComplete(false)
            }
        }
    }



    fun resetOtpState() {
        _otpState.value = OtpState() // Resets all fields to default values
        Log.d(tag, "OTP state reset")
    }

    fun startTimer() {
        viewModelScope.launch {
            _otpState.update { it.copy(timerSeconds = timeoutSeconds.toInt(), canResend = false) }
            for (i in timeoutSeconds.toInt() downTo 0) {
                delay(1000)
                _otpState.update { it.copy(timerSeconds = i) }
            }
            _otpState.update { it.copy(canResend = true) }
        }
    }



    fun sendOtp(phoneNumber: String, activity: Activity?, navController: NavController) {
        Log.d(tag, "Sending OTP to phone number: $phoneNumber")

        if (!phoneNumber.matches("^\\+?[1-9]\\d{6,14}$".toRegex())) {
            _otpState.update { it.copy(otpError = "Invalid phone number format!") }
            return
        }

        if (activity == null) {
            _otpState.update { it.copy(otpError = "Error: Activity is null") }
            return
        }

        _otpState.update { it.copy(isSendingOtp = true, otpError = null) }

        sendOtpRequest(phoneNumber, activity, navController)
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

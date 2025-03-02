package com.example.talky.viewmodels

import android.content.Context
import android.content.SharedPreferences

class AuthPreferences(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    private val iSAUTHENTICATED = "isAuthenticated"
    private val uSERPHONENUMBER = "userPhoneNumber"
    private val pROFILESETUPCOMPLETED = "profileSetupCompleted"

    // Save authentication status
    fun saveAuthStatus(isAuthenticated: Boolean) {
        sharedPreferences.edit().putBoolean(iSAUTHENTICATED, isAuthenticated).apply()
    }

    // Save phone number
    fun savePhoneNumber(phoneNumber: String) {
        sharedPreferences.edit().putString(uSERPHONENUMBER, phoneNumber).apply()
    }

    // Save profile setup status
    fun saveProfileSetupStatus(isCompleted: Boolean) {
        sharedPreferences.edit().putBoolean(pROFILESETUPCOMPLETED, isCompleted).apply()
    }

    // Check if user is logged in
    fun isUserLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(iSAUTHENTICATED, false)
    }

    // Log out the user
    fun logoutUser() {
        sharedPreferences.edit()
            .remove(iSAUTHENTICATED)
            .remove(uSERPHONENUMBER)
            .remove(pROFILESETUPCOMPLETED)
            .apply()
    }
}

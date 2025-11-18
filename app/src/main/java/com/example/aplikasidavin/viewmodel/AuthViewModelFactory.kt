package com.example.aplikasidavin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.aplikasidavin.data.local.UserPreferences

class AuthViewModelFactory(private val prefs: UserPreferences) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AuthViewModel(prefs) as T
    }
}

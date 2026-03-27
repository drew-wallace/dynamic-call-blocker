package com.example.dynamiccallblocker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val repository: BlockRepository) : ViewModel() {
    val rules: StateFlow<BlockRules> = repository.rulesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BlockRules())

    private val _inputNumber = MutableStateFlow("")
    val inputNumber = _inputNumber.asStateFlow()

    fun onInputChanged(value: String) {
        _inputNumber.value = value
    }

    fun addRule(listType: ListType, mode: MatchMode) {
        val value = inputNumber.value
        viewModelScope.launch {
            repository.addRule(listType, mode, value)
            _inputNumber.value = ""
        }
    }

    fun removeRule(listType: ListType, mode: MatchMode, number: String) {
        viewModelScope.launch {
            repository.removeRule(listType, mode, number)
        }
    }

    fun setAllowContacts(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAllowContacts(enabled)
        }
    }

    fun setBlockingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setBlockingEnabled(enabled)
        }
    }
}

class MainViewModelFactory(private val repository: BlockRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(MainViewModel::class.java))
        return MainViewModel(repository) as T
    }
}

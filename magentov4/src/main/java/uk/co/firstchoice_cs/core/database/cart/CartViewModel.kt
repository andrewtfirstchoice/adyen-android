package uk.co.firstchoice_cs.core.database.cart

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class CartViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CartRepository

    val allCartItems: LiveData<List<CartItem>>

    init {
        val cartDao = CartDatabase.getDatabase(application, viewModelScope).cartDao()
        repository = CartRepository(cartDao)
        allCartItems = repository.allCartItems
    }

    fun insert(cartItem: CartItem) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(cartItem)
    }
    fun delete(cartItem: CartItem) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(cartItem)
    }
    fun deleteAll() = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteAll()
    }
}

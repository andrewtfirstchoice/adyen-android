package uk.co.firstchoice_cs.core.database.cart

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData

class CartRepository(private val cartDao: CartDao) {

    val allCartItems: LiveData<List<CartItem>> = cartDao.getCartItems()

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun insert(item: CartItem) {
        cartDao.insert(item)
    }

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun delete(item: CartItem) {
        cartDao.delete(item)
    }


    suspend fun deleteAll() {
        cartDao.deleteAll()
    }
}

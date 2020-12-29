package uk.co.firstchoice_cs.core.database.cart

import androidx.lifecycle.LiveData;
import androidx.room.*


@Dao
interface CartDao {

    @Query("SELECT * from cart_table ORDER BY id ASC")
    fun getCartItems(): LiveData<List<CartItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(cartItem: CartItem)

    @Delete
    fun delete(cartItem: CartItem)


    @Query("DELETE FROM cart_table")
    fun deleteAll()
}

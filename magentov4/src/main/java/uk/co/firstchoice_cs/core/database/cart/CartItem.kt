package uk.co.firstchoice_cs.core.database.cart

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.JsonObject
import org.json.JSONObject
import uk.co.firstchoice_cs.Settings
import uk.co.firstchoice_cs.core.api.customerAPI.Product

@Entity(tableName = "cart_table")
data class CartItem(@ColumnInfo(name = "JsonData") val data: String?,
                    @ColumnInfo(name = "PartDescription") val partDescription: String?,
                    @ColumnInfo(name = "Manufacturer") val mfrweb: String?,
                    @ColumnInfo(name = "PartNum") val partNum: String?,
                    @ColumnInfo(name = "FCCPart") val fccPart: String?,
                    @ColumnInfo(name = "Stock") val stock: Int?,
                    @ColumnInfo(name = "ClassDescription") val classDescription: String?,
                    @ColumnInfo(name = "Qty") var qty: Int?){
    @Transient
    var tempPrice: Product? = null

    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
    @Transient
    var tempQty: Int = 0
    @Transient
    var priceStatus: Settings.PriceStatus = Settings.PriceStatus.NONE
}



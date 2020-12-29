package uk.co.firstchoice_cs.core.helpers

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.koin.core.KoinComponent
import org.koin.core.inject
import uk.co.firstchoice_cs.core.listeners.DefaultCurrentActivityListener
import uk.co.firstchoice_cs.firstchoice.R

open class SwipeCallback internal constructor() : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT),
    KoinComponent {
    private val defaultCurrentActivityListener: DefaultCurrentActivityListener by inject()
    private val ctx = defaultCurrentActivityListener.context
    private val icon: Drawable? = ctx.let { ContextCompat.getDrawable(ctx, R.drawable.ic_delete_white) }
    private var background: ColorDrawable
    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        return ItemTouchHelper.Callback.makeMovementFlags(ItemTouchHelper.LEFT, ItemTouchHelper.LEFT)
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        return false
    }

    override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        val itemView = viewHolder.itemView
        val iconMargin = (itemView.height - icon!!.intrinsicHeight) / 2
        val iconTop = itemView.top + (itemView.height - icon.intrinsicHeight) / 2
        val iconBottom = iconTop + icon.intrinsicHeight
        when {
            dX < 0 -> {
                swipingToTheLeft(dX.toInt(), itemView, iconMargin, iconTop, iconBottom)
            }
            else -> {
                background.setBounds(0, 0, 0, 0)
            }
        }
        background.draw(c)
        icon.draw(c)
    }

    private fun swipingToTheLeft(dX: Int, itemView: View, iconMargin: Int, iconTop: Int, iconBottom: Int) {
        val iconLeft = itemView.right - iconMargin - icon!!.intrinsicWidth
        val iconRight = itemView.right - iconMargin
        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
        background.setBounds(itemView.right + dX - 20, itemView.top, itemView.right, itemView.bottom)
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        background = ColorDrawable(Color.WHITE)
    }

    init {
        background = ColorDrawable(Color.RED)
    }
}
package uk.co.firstchoice_cs.basket

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import uk.co.firstchoice_cs.App
import uk.co.firstchoice_cs.Constants
import uk.co.firstchoice_cs.firstchoice.R
import uk.co.firstchoice_cs.firstchoice.databinding.CountriesDialogBinding
import uk.co.firstchoice_cs.firstchoice.databinding.PaymentTypesListItemBinding

class PaymentMethodsDialog: DialogFragment(R.layout.payment_dialog) {

    private val adapter = MyAdapter()
    var selectedIndex = 0
    private lateinit var binding: CountriesDialogBinding
    var paymentMethods: Array<String> = arrayOf(Constants.PAYMENT_TYPE_ACCOUNT, Constants.PAYMENT_TYPE_CARD)

    companion object
    {
        fun newInstance(): PaymentMethodsDialog {
            return PaymentMethodsDialog()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = CountriesDialogBinding.inflate(LayoutInflater.from(requireContext()))

        return AlertDialog.Builder(requireActivity())
            .setView(binding.root)
            .create()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    fun init()
    {
        selectedIndex = paymentMethods.indexOf { App.selectedPaymentType }
        if(selectedIndex==-1)
            selectedIndex = 1 //card
        binding.recycler.layoutManager = LinearLayoutManager(activity)
        binding.recycler.itemAnimator = DefaultItemAnimator()
        binding.recycler.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
        binding.recycler.adapter = adapter

        binding.doneBtn.setOnClickListener {
            val result = paymentMethods[selectedIndex]
            setFragmentResult("paymentRequest", bundleOf("paymentResult" to result))
            dismiss()
        }
    }

    internal inner class MyAdapter : RecyclerView.Adapter<MyAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.payment_types_list_item, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val method = paymentMethods[position]



            holder.binding.view.setOnClickListener {
                selectedIndex = position
                notifyDataSetChanged()
            }

            if(position==selectedIndex)
            {
                holder.binding.check.visibility = View.VISIBLE
            }
            else
            {
                holder.binding.check.visibility = View.INVISIBLE
            }


            holder.binding.title.text = method
        }

        override fun getItemCount(): Int {
            return paymentMethods.size
        }

        inner class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val binding = PaymentTypesListItemBinding.bind(itemView)
        }
    }
}
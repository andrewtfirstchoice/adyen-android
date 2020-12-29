package uk.co.firstchoice_cs.core.shared

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_account.*
import uk.co.firstchoice_cs.App
import uk.co.firstchoice_cs.core.helpers.CartHelper.customerFullName
import uk.co.firstchoice_cs.firstchoice.R

class AccountFragment : Fragment(R.layout.fragment_account) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpToolbar()
        renderData()
    }

    private fun setUpToolbar() {
        val activity = activity as AppCompatActivity?
        activity?.setSupportActionBar(toolbar)
    }

    private fun renderData()
    {
        val customer = App.magentoCustomer
        if(customer!=null) {
            val fullName = customerFullName(customer)
            nameEdit.text = fullName
            emailEdit.text = customer.email
            companyEdit.text = "First Choice Ltd"
            customerEdit.text = "Customer"
        }
    }
}
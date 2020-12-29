package uk.co.firstchoice_cs.basket

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.CheckBox
import android.widget.CompoundButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import kotlinx.android.synthetic.main.fragment_account_order_details.*
import kotlinx.android.synthetic.main.fragment_add_new_address.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.co.firstchoice_cs.App
import uk.co.firstchoice_cs.core.api.AddressManager
import uk.co.firstchoice_cs.core.api.googlePlacesAPI.google_places_api.adapters.PlacesAutoCompleteAdapter
import uk.co.firstchoice_cs.core.api.googlePlacesAPI.google_places_api.entities.PlaceDetails
import uk.co.firstchoice_cs.core.api.googlePlacesAPI.google_places_api.entities.Prediction
import uk.co.firstchoice_cs.core.helpers.Helpers
import uk.co.firstchoice_cs.firstchoice.R
import uk.co.firstchoice_cs.firstchoice.databinding.FragmentAddNewAddressBinding
import java.util.ArrayList

class AddNewAddressFragment : Fragment(R.layout.fragment_add_new_address) {
    private var convertedAddress: AddressManager.Address? = null
    private var changed: Boolean = false
    private val predictions: List<Prediction> = ArrayList()
    private var placesAutoCompleteAdapter: PlacesAutoCompleteAdapter? = null
    private var defaultBilling = false
    private var defaultShipping = false
    private var streetNumber = ""
    private var route = ""
    private var addressLine1 = ""
    private var addressLine2 = ""
    private var town = ""
    private var company = ""
    private var country = ""
    private var county = ""
    private var postcode = ""
    private var political = ""
    private var email = ""
    lateinit var binding: FragmentAddNewAddressBinding
    private var mode:AddressManager.AddressTypes = AddressManager.AddressTypes.BILLING

    companion object{
        var addedAddress:AddressManager.AddedAddress? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        val args = arguments
        val res = args?.getInt("mode", 0)
        mode = if(res==0)
            AddressManager.AddressTypes.BILLING
        else
            AddressManager.AddressTypes.SHIPPING

        AddressManager.AddressTypes.SHIPPING.ordinal
        setFragmentResultListener("countryRequest") { _, bundle ->
            val result = bundle.getString("countryResult")
            binding.country.text = result
        }
    }


    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.action_menu_add_new_address, menu)
        super.onCreateOptionsMenu(menu, menuInflater)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_save) {
            saveAddress()
        }
        return super.onOptionsItemSelected(item)
    }


    private fun setUpToolbar(view: View) {
        val toolbar: MaterialToolbar = view.findViewById(R.id.toolbar)
        val activity = activity as AppCompatActivity?
        activity?.setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {

            if (changed) {
                AlertDialog.Builder(requireContext())
                    .setCancelable(false)
                    .setTitle("Are you sure?")
                    .setMessage("You have made changes that have not been saved")
                    .setPositiveButton(android.R.string.ok) { dialog, _ ->
                        dialog.dismiss()
                        NavHostFragment.findNavController(this@AddNewAddressFragment).navigateUp()
                    }
                    .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show()
            } else {
                if(convertedAddress!=null) {
                    val gson = Gson()
                    val jsonString = gson.toJson(convertedAddress)
                    setFragmentResult("addAddressRequest", bundleOf("addAddressResult" to jsonString,"mode" to mode))
                }
                NavHostFragment.findNavController(this@AddNewAddressFragment).navigateUp()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAddNewAddressBinding.bind(view)
        placesAutoCompleteAdapter = PlacesAutoCompleteAdapter(context, predictions)
        binding.locationSearch.threshold = 1
        binding.locationSearch.setAdapter(placesAutoCompleteAdapter)
        binding.locationSearch.onItemClickListener =
            AdapterView.OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
                val prediction = predictions[position]
                val placeID = prediction.placeId
                //use co-routine to get details
                lifecycleScope.launch {
                    val result = getPlaces(placeID)
                    if (result != null) {
                        onGetPlacesResult(result)
                    }
                }
            }

        binding.country.setOnClickListener{
            val countriesDialog = CountriesDialog.newInstance()
            countriesDialog.show(parentFragmentManager,"countryRequest")
        }


        val setAsDefaultBillingCheckBox = view.findViewById<CheckBox>(R.id.setAsDefaultBillingCheckBox)
        setAsDefaultBillingCheckBox.setOnCheckedChangeListener { _: CompoundButton?, b: Boolean -> defaultBilling = b }
        val setAsDefaultAddressCheckBox = view.findViewById<CheckBox>(R.id.setAsDefaultAddressCheckBox)
        setAsDefaultAddressCheckBox.setOnCheckedChangeListener { _: CompoundButton?, b: Boolean -> defaultShipping = b }
        setUpToolbar(view)
        listenForChanges()
    }
    private fun listenForChanges() {
        binding.emailEdit.addTextChangedListener(textWatcher)
        binding.firstNameEdit.addTextChangedListener(textWatcher)
        binding.lastNameEdit.addTextChangedListener(textWatcher)
        binding.mobileEdit.addTextChangedListener(textWatcher)
        binding.addressLine1.addTextChangedListener(textWatcher)
        binding.addressLine2.addTextChangedListener(textWatcher)
        binding.townEdit.addTextChangedListener(textWatcher)
        binding.countyFieldEdit.addTextChangedListener(textWatcher)
        binding.companyEdit.addTextChangedListener(textWatcher)
        binding.postcodeEdit.addTextChangedListener(textWatcher)
    }


    private fun saveAddress() {

        val email = binding.emailEdit.text.toString().trim()
        val firstName = binding.firstNameEdit.text.toString().trim()
        val lastName = binding.lastNameEdit.text.toString().trim()
        val mobile = binding.mobileEdit.text.toString().trim()
        addressLine1 = binding.addressLine1.text.toString().trim()
        addressLine2 = binding.addressLine2.text.toString().trim()
        town = binding.townEdit.text.toString().trim()
        county = binding.countyFieldEdit.text.toString().trim()
        company = binding.companyEdit.text.toString().trim()
        postcode = binding.postcodeEdit.text.toString().trim()
        val validIndex = Helpers.notEmpty(arrayOf(email,firstName, lastName, mobile, addressLine1, addressLine2, town, county, postcode,company))
        if (validIndex == -1) {
            val address = AddressManager.AddedAddress(
                city = town,
                company = company,
                country_id = "",
                default_billing = defaultBilling,
                default_shipping = defaultShipping,
                selected_billing = defaultBilling,
                selected_shipping = defaultShipping,
                firstname = firstName,
                lastname = lastName,
                middlename = "",
                postcode = postcode,
                addressLine1 = addressLine1,
                addressLine2 = addressLine2,
                telephone = mobile,
                fax = "",
                email = email)
            convertedAddress = App.addresses.addAddress(address)
            changed = false
            Snackbar.make(binding.mainCoordinator, "Address Saved", Snackbar.LENGTH_SHORT).show()

        } else {
            var message = ""
            when (validIndex) {
                0 -> message = "Please include your email"
                1 -> message = "Please include your first name"
                2 -> message = "Please include your last name"
                4 -> message = "Please include your mobile phone number"
                5 -> message = "Please include address line 1"
                6 -> message = "Please include address line 2"
                7 -> message = "Please include your town"
                8 -> message = "Please include your county"
                9 -> message = "Please include your postcode"
                10 -> message = "Please include your company"
            }
            Snackbar.make(binding.mainCoordinator, message, Snackbar.LENGTH_SHORT).show()
        }
    }

    fun update() {
        binding.addressLine1.setText(addressLine1.trim { it <= ' ' })
        binding.addressLine2.setText(addressLine2.trim { it <= ' ' })
        binding.emailEdit.setText(email.trim { it <= ' ' })
        binding.townEdit.setText(town.trim { it <= ' ' })
        binding.countyFieldEdit.setText(county.trim { it <= ' ' })
        binding.postcodeEdit.setText(postcode.trim { it <= ' ' })
        binding.country.text = country.trim { it <= ' ' }
        binding.companyEdit.setText(postcode.trim { it <= ' ' })
        //special cases to overwrite blanks addresses
        if (town.trim().isEmpty())
            binding.townEdit.setText(political.trim { it <= ' ' })

        Helpers.hideKeyboard(binding.locationSearch)
    }

    private suspend fun getPlaces(place: String): PlaceDetails? =
        withContext(Dispatchers.IO) {
            return@withContext placesAutoCompleteAdapter?.getPlaceDetails(place)
        }

    private fun onGetPlacesResult(placeDetail: PlaceDetails) {
        for (addressComponent in placeDetail.result.addressComponents) {
            when {
                addressComponent.types.contains("country") -> country = addressComponent.longName
                addressComponent.types.contains("street_number") -> streetNumber = addressComponent.longName
                addressComponent.types.contains("route") -> route = addressComponent.longName
                addressComponent.types.contains("postal_town") -> town = addressComponent.longName
                addressComponent.types.contains("administrative_area_level_2") -> county = addressComponent.longName
                addressComponent.types.contains("postal_code") -> postcode = addressComponent.longName
                addressComponent.types.contains("administrative_area_level_1") -> political = addressComponent.longName
            }
        }
        addressLine1 = "$streetNumber"
        addressLine2 = "$route"
        update()
    }

    private val textWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
        }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            changed = true
        }
    }
}
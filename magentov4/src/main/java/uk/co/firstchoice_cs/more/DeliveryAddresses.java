package uk.co.firstchoice_cs.more;

import androidx.navigation.fragment.NavHostFragment;

import uk.co.firstchoice_cs.core.shared.DeliveryAddressesFragment;
import uk.co.firstchoice_cs.firstchoice.R;

public class DeliveryAddresses extends DeliveryAddressesFragment {
    @Override
    protected void goToAddNewAddress() {

        super.goToAddNewAddress();
          NavHostFragment.findNavController(DeliveryAddresses.this).navigate(R.id.action_deliveryAddressFragment_to_addNewAddressFragment);
    }
}


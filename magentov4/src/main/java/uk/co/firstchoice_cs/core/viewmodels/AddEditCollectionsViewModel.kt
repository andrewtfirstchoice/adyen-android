package uk.co.firstchoice_cs.core.viewmodels

import androidx.lifecycle.ViewModel
import uk.co.firstchoice_cs.core.database.entities.CollectionList

class AddEditCollectionsViewModel : ViewModel() {
    var isEditMode = false
    @JvmField
    var currentCollection: CollectionList? = null
}
package uk.co.firstchoice_cs.core.viewmodels

import androidx.lifecycle.ViewModel
import uk.co.firstchoice_cs.core.api.v4API.Document
import uk.co.firstchoice_cs.core.api.v4API.EquipmentCategory
import uk.co.firstchoice_cs.core.api.v4API.Manufacturer
import uk.co.firstchoice_cs.core.api.v4API.ManufacturersWithManuals

class SearchViewModel : ViewModel() {
    var documents: List<Document>? = null
    var equipmentTypesResult: ManufacturersWithManuals?=null
    var selectedEquipment: EquipmentCategory?=null
    var selectedManufacturer: Manufacturer?=null
    var selectedModel: Document?=null
    var manufacturers: ManufacturersWithManuals?=null
    var equipmentTypes: ArrayList<EquipmentCategory>? = null
}
package uk.co.firstchoice_cs.core.shared

import uk.co.firstchoice_cs.core.document.DocumentEntry

interface AdapterInterface {
    fun adapterDataUpdated()
    fun removeCollectionListAndAllItems(id: Int)
    fun addCollectionItem(documentEntry: DocumentEntry?, collectionId: Int)
}
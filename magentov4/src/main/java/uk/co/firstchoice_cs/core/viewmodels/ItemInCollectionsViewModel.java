package uk.co.firstchoice_cs.core.viewmodels;

import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

import uk.co.firstchoice_cs.core.database.entities.CollectionItem;
import uk.co.firstchoice_cs.core.database.entities.CollectionList;
import uk.co.firstchoice_cs.core.document.DocumentEntry;

public class ItemInCollectionsViewModel extends ViewModel {

 //   private boolean editMode;
   // private CollectionList currentCollection;
     public CollectionItem collectionItem;
     public DocumentEntry focusedDocument;
     public List<CollectionList> collectionLists = new ArrayList<>();
     public List<CollectionItem> collectionItems = new ArrayList<>();

    void setCollectionLists(List<CollectionList> collectionLists) {
        this.collectionLists = collectionLists;
    }

    void setCollectionItems(List<CollectionItem> collectionItems) {
        this.collectionItems = collectionItems;
    }

    List<CollectionList> getCollectionLists() {
        return collectionLists;
    }

    List<CollectionItem> getCollectionItems() {
        return collectionItems;
    }

    CollectionItem getCollectionItem() {
        return collectionItem;
    }

    void setCollectionItem(CollectionItem collectionItem) {
        this.collectionItem = collectionItem;
    }

    DocumentEntry getFocusedDocument() {
        return focusedDocument;
    }

    void setFocusedDocument(DocumentEntry focusedDocument) {
        this.focusedDocument = focusedDocument;
    }

   // public void setEditMode(boolean editMode) {
    //    this.editMode = editMode;
    //}
}

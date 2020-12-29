package uk.co.firstchoice_cs.core.database;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import uk.co.firstchoice_cs.core.database.entities.CollectionItem;
import uk.co.firstchoice_cs.core.database.entities.CollectionList;
import uk.co.firstchoice_cs.core.database.entities.PreviousScanList;
import uk.co.firstchoice_cs.core.document.DocumentEntry;


public class DBViewModel extends AndroidViewModel {

    private FCRepository mRepository;
    private LiveData<List<PreviousScanList>> mAllLists;
    private LiveData<List<CollectionList>> mAllCollections;
    private LiveData<List<CollectionItem>> mAllCollectionItems;

    public DBViewModel(Application application) {
        super(application);
        mRepository = new FCRepository(application);
        mAllLists = mRepository.getAllPreviousScans();
        mAllCollections = mRepository.getAllCollections();
        mAllCollectionItems = mRepository.getAllCollectionItems();
    }

    public LiveData<List<PreviousScanList>> getPreviousScans() {
        return mAllLists;
    }

    public LiveData<List<CollectionList>> getCollectionLists() {
        return mAllCollections;
    }

    public LiveData<List<CollectionItem>> getCollectionItems() {
        return mAllCollectionItems;
    }

    public LiveData<List<CollectionItem>> getCollectionItems(long collectionId) {
        return mRepository.getCollectionItems((int)collectionId);
    }


    public void addCollectionItem(DocumentEntry documentEntry, int collectionId) {
        mRepository.addCollectionItem(documentEntry, collectionId);
    }


    public void insert(PreviousScanList jobList) {
        mRepository.insert(jobList);
    }

    public void clearScans() {
        mRepository.clear();
    }

    public void insert(CollectionList collectionList, FCRepository.CollectionListInterface callback ) {
        mRepository.insertCollection(collectionList,callback);
    }

    public void update(CollectionList collectionList ) {
        mRepository.updateCollectionList(collectionList);
    }


    public void clearCollections() {
        mRepository.clearCollections();
    }

    public void insert(CollectionItem collectionItem,FCRepository.CollectionItemInterface callback) {
        mRepository.insertCollectionItem(collectionItem,callback);
    }

    public void clearCollectionItem() {
        mRepository.clearCollectionItems();
    }

    public void deleteItemFromCollection(int id) {
        mRepository.deleteItemFromCollection(id);
    }

    public void deleteCollectionList(CollectionList collectionList,FCRepository.CollectionListInterface callback) {
        mRepository.deleteCollectionList(collectionList,callback);
    }

    public void deleteCollectionListItems(CollectionList collectionList,FCRepository.CollectionListInterface callback) {
        mRepository.deleteCollectionListItems(collectionList,callback);
    }

    public void deleteCollectionAndAllItems(CollectionList list,FCRepository.CollectionListInterface callback) {
        mRepository.deleteCollectionListAndAllItems(list,callback);
    }

    public void deleteCollectionListItemByName(String name,FCRepository.CollectionListInterface callback) {
        mRepository.deleteCollectionListItemByName(name,callback);
    }

    public void delete(PreviousScanList data) {
        mRepository.deleteScanData(data);
    }
}
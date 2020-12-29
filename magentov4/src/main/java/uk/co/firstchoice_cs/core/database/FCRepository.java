package uk.co.firstchoice_cs.core.database;

import android.app.Application;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import java.util.List;

import uk.co.firstchoice_cs.core.database.dao.CollectionDao;
import uk.co.firstchoice_cs.core.database.dao.CollectionItemDao;
import uk.co.firstchoice_cs.core.database.dao.ListDao;
import uk.co.firstchoice_cs.core.database.entities.CollectionItem;
import uk.co.firstchoice_cs.core.database.entities.CollectionList;
import uk.co.firstchoice_cs.core.database.entities.PreviousScanList;
import uk.co.firstchoice_cs.core.document.DocumentEntry;

public class FCRepository {

    private static CollectionDao mCollectionDao;
    private static CollectionItemDao mCollectionItemDao;
    private static ListDao mListDao;
    private static LiveData<List<PreviousScanList>> mAllScanLists;
    private static LiveData<List<CollectionList>> mAllCollectionLists;
    private static LiveData<List<CollectionItem>> mAllCollectionItems;

    FCRepository(Application application) {
        FCDataBase db = FCDataBase.getDatabase(application);
        mListDao = db.listDao();
        mCollectionDao = db.collectionDao();
        mCollectionItemDao = db.collectionItemDao();
        mAllScanLists = mListDao.getPreviousScanLists();
        mAllCollectionLists = mCollectionDao.getCollectionsLists();
        mAllCollectionItems = mCollectionItemDao.getCollectionItems();
    }


    // Room executes all queries on a separate thread.
    // Observed LiveData will notify the observer when the documentItem has cachedChanged.
    LiveData<List<PreviousScanList>> getAllPreviousScans() {
        return mAllScanLists;
    }

    LiveData<List<CollectionList>> getAllCollections() {
        return mAllCollectionLists;
    }

    LiveData<List<CollectionItem>> getAllCollectionItems() {
        return mAllCollectionItems;
    }

    void clear() {
        new clearPreviousScanListAsyncTask().execute();
    }

    void insert(PreviousScanList jobList) {
        new insertAsyncTask().execute(jobList);
    }

    void clearCollections() {
        new clearCollectionAsyncTask().execute();
    }

    void insertCollection(CollectionList coll,@NonNull CollectionListInterface callback) {
        new insertCollectionAsyncTask(mCollectionDao,callback).execute(coll);
    }


    void clearCollectionItems() {
        new clearCollectionItemAsyncTask().execute();
    }

    void insertCollectionItem(@NonNull CollectionItem collectionItem,@NonNull CollectionItemInterface callback) {
        new insertCollectionItemAsyncTask(callback).execute(collectionItem);
    }

    LiveData<List<CollectionItem>> getCollectionItems(long collectionId) {
        return mCollectionItemDao.getCollectionItemsById(collectionId);
    }

    void deleteItemFromCollection(int id) {
        new deleteItemFromCollectionAsyncTask().execute(id);
    }

    void addCollectionItem(DocumentEntry item, int collectionId) {

        new addCollectionItemAsyncTask(item, collectionId).execute();
    }

    void updateCollectionItem(CollectionItem collectionItem) {

        new updateCollectionItemAsyncTask(collectionItem).execute();
    }

    void updateCollectionList(CollectionList collectionList) {

        new updateCollectionListAsyncTask(collectionList).execute();
    }


    public void deleteCollectionList(CollectionList collectionList, CollectionListInterface callback) {
        new deleteCollectionListItemAsyncTask(collectionList,callback).execute();
    }

    public void deleteCollectionListItems(CollectionList collectionList, CollectionListInterface callback) {
        new deleteCollectionListItemsAsyncTask(collectionList,callback).execute();
    }

    public void deleteCollectionListItemByName(String name, CollectionListInterface callback) {
        new deleteCollectionListItemByNameAsyncTask(name,callback).execute();
    }

    public void deleteScanData(PreviousScanList data) {
        new deleteScanDataItemAsyncTask(mListDao, data).execute();
    }

    public void deleteCollectionListAndAllItems(CollectionList collectionList, CollectionListInterface callback) {
        new deleteCollectionListAndAllItemsAsyncTask(collectionList,callback).execute();
    }


    private static class deleteScanDataItemAsyncTask extends AsyncTask<Void, Void, Void> {
        PreviousScanList item;
        private ListDao mAsyncTaskDao;
        deleteScanDataItemAsyncTask(ListDao dao, PreviousScanList item) {
            this.item = item;
            mAsyncTaskDao = dao;
        }


        @Override
        protected Void doInBackground(final Void... params) {
            mAsyncTaskDao.deleteScanDataItemAsyncTask(this.item.getBarcode());
            return null;
        }
    }

    private static class deleteCollectionListItemByNameAsyncTask extends AsyncTask<Void, Void, Void> {
        String name;
        CollectionListInterface callback;
        deleteCollectionListItemByNameAsyncTask(String name,CollectionListInterface callback) {
            this.name = name;
            this.callback = callback;
        }
        @Override
        protected Void doInBackground(final Void... params) {
            mCollectionItemDao.deleteAllItemsWithName(this.name);
            return null;
        }

        @Override
        protected void onPostExecute(Void voids) {
            callback.onComplete();
        }
    }

    private static class deleteCollectionListAndAllItemsAsyncTask extends AsyncTask<Void, Void, Void> {
        CollectionList collectionList;
        CollectionListInterface callback;
        deleteCollectionListAndAllItemsAsyncTask(CollectionList item,CollectionListInterface callback) {
            collectionList = item;
            this.callback = callback;
        }


        @Override
        protected Void doInBackground(final Void... params) {
            mCollectionItemDao.deleteAllItemsWithID(collectionList.getId());
            mCollectionDao.delete(collectionList);
            return null;
        }

        @Override
        protected void onPostExecute(Void voids) { callback.onComplete(); }
    }


    private static class deleteCollectionListItemsAsyncTask extends AsyncTask<Void, Void, Void> {
        CollectionList collectionList;
        CollectionListInterface callback;
        deleteCollectionListItemsAsyncTask(CollectionList item,CollectionListInterface callback) {
            collectionList = item;
            this.callback = callback;
        }


        @Override
        protected Void doInBackground(final Void... params) {
            mCollectionItemDao.deleteAllItemsWithID(collectionList.getId());
            return null;
        }

        @Override
        protected void onPostExecute(Void voids) {
            callback.onComplete();
        }
    }

    private static class deleteCollectionListItemAsyncTask extends AsyncTask<Void, Void, Void> {
        CollectionList collItem;
        CollectionListInterface callback;
        deleteCollectionListItemAsyncTask(CollectionList item,CollectionListInterface callback) {
            collItem = item;
            this.callback = callback;
        }

        @Override
        protected Void doInBackground(final Void... params) {
            mCollectionDao.delete(collItem);
            return null;
        }

        @Override
        protected void onPostExecute(Void voids) {
            callback.onComplete();
        }
    }


    private static class updateCollectionListAsyncTask extends AsyncTask<Void, Void, Void> {
        CollectionList collectionList;
        updateCollectionListAsyncTask(CollectionList list) {
            collectionList = list;
        }
        @Override
        protected Void doInBackground(final Void... params) {
            mCollectionDao.update(collectionList);
            return null;
        }
    }


    private static class updateCollectionItemAsyncTask extends AsyncTask<Void, Void, Void> {
        CollectionItem collItem;
        updateCollectionItemAsyncTask(CollectionItem item) {
            collItem = item;
        }

        @Override
        protected Void doInBackground(final Void... params) {
            mCollectionItemDao.update(collItem);
            return null;
        }
    }


    private static class addCollectionItemAsyncTask extends AsyncTask<Void, Void, Void> {
        CollectionItem collItem;
        addCollectionItemAsyncTask(DocumentEntry item, int collectionId) {
            collItem = new CollectionItem(0, collectionId, item.document.getDisplayName(), item.document.address, item.manufacturer.Name, item.model.Name);
        }
        @Override
        protected Void doInBackground(final Void... params) {
            mCollectionItemDao.insert(collItem);
            return null;
        }
        @Override
        protected void onPostExecute(Void voids) {
        }
    }


    private static class deleteItemFromCollectionAsyncTask extends AsyncTask<Integer, Void, Void> {
        @Override
        protected Void doInBackground(final Integer... params) {
            mCollectionItemDao.deleteItemFromCollection(params[0]);
            return null;
        }
        @Override
        protected void onPostExecute(Void voids) {
        }
    }

    private static class insertAsyncTask extends AsyncTask<PreviousScanList, Void, Long> {

        @Override
        protected Long doInBackground(final PreviousScanList... params) {
            return mListDao.insert(params[0]);
        }
    }

    public interface CollectionListInterface{
        void onCollectionAdded(Long key);
        void onComplete();
    }
    public interface CollectionItemInterface{
        void onItemAdded(Long key);
        void onComplete();
    }

    private static class insertCollectionAsyncTask extends AsyncTask<CollectionList, Void, Long> {
        private CollectionDao mAsyncTaskDao;
        private CollectionListInterface callback;
        insertCollectionAsyncTask(CollectionDao dao,CollectionListInterface callback) {

            this.mAsyncTaskDao = dao;
            this.callback = callback;
        }

        @Override
        protected Long doInBackground(final CollectionList... params) {

            return mAsyncTaskDao.insert(params[0]);
        }

        @Override
        protected void onPostExecute(Long key) {
            if(key!=-1)
                callback.onCollectionAdded(key);
            callback.onComplete();
        }
    }


    private static class insertCollectionItemAsyncTask extends AsyncTask<CollectionItem, Void, Long> {
        private CollectionItemInterface callback;

        insertCollectionItemAsyncTask(@NonNull CollectionItemInterface callback) {
            this.callback = callback;
        }
        @Override
        protected Long doInBackground(final CollectionItem... params) {
            return mCollectionItemDao.insert(params[0]);
        }

        @Override
        protected void onPostExecute(Long key) {
            callback.onItemAdded(key);
            callback.onComplete();
        }
    }

    private static class clearPreviousScanListAsyncTask extends AsyncTask<PreviousScanList, Void, Void> {
        @Override
        protected Void doInBackground(final PreviousScanList... params) {
            mListDao.clear();
            return null;
        }
    }

    private static class clearCollectionAsyncTask extends AsyncTask<CollectionList, Void, Void> {
        @Override
        protected Void doInBackground(final CollectionList... params) {
            mCollectionDao.clear();
            return null;
        }
    }

    private static class clearCollectionItemAsyncTask extends AsyncTask<CollectionItem, Void, Void> {
        @Override
        protected Void doInBackground(final CollectionItem... params) {
            mCollectionItemDao.clear();
            return null;
        }
    }
}





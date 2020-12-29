package uk.co.firstchoice_cs.core.database;

/*
 * Copyright (C) 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import uk.co.firstchoice_cs.core.database.dao.CollectionDao;
import uk.co.firstchoice_cs.core.database.dao.CollectionItemDao;
import uk.co.firstchoice_cs.core.database.dao.ListDao;
import uk.co.firstchoice_cs.core.database.entities.CollectionItem;
import uk.co.firstchoice_cs.core.database.entities.CollectionList;
import uk.co.firstchoice_cs.core.database.entities.PreviousScanList;


/**
 * This is the backend. The database. This used to be done by the OpenHelper.
 * The fact that this has very few comments emphasizes its coolness.
 */

@Database(entities = {PreviousScanList.class, CollectionItem.class, CollectionList.class}, version = 18)
public abstract class FCDataBase extends RoomDatabase {

    // marking the instance as volatile to ensure atomic access to the variable
    private static volatile FCDataBase INSTANCE;
    /**
     * Override the onOpen method to populate the database.
     * For this sample, we clearScans the database every time it is created or opened.
     * <p>
     * If you want to populate the database only when the database is created for the 1st time,
     * override RoomDatabase.Callback()#onCreate
     */
    private static Callback sRoomDatabaseCallback = new Callback() {

        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);
            // If you want to keep the documentItem through app restarts,
            // comment out the following line.
            new PopulateDbAsync(INSTANCE).execute();
        }
    };

    static FCDataBase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (FCDataBase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            FCDataBase.class, "FC_DATABASE")
                            // Wipes and rebuilds instead of migrating if no Migration object.
                            // Migration is not part of this codelab.
                            .fallbackToDestructiveMigration()
                            .addCallback(sRoomDatabaseCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public abstract ListDao listDao();

    public abstract CollectionDao collectionDao();

    public abstract CollectionItemDao collectionItemDao();

    /**
     * Populate the database in the background.
     * If you want to start with more words, just add them.
     */
    private static class PopulateDbAsync extends AsyncTask<Void, Void, Void> {

        private final ListDao mListDao;
        private final CollectionDao mCollectionDao;
        private final CollectionItemDao mCollectionItemDao;

        PopulateDbAsync(FCDataBase db) {
            mListDao = db.listDao();
            mCollectionDao = db.collectionDao();
            mCollectionItemDao = db.collectionItemDao();
        }

        @Override
        protected Void doInBackground(final Void... params) {
            // Start the app with a clean database every time.
            // Not needed if you only populate on creation.
            //   mDao.deleteAll();
            //    mCollectionDao.clearScans();
            //    mCollectionItemDao.clearScans();
            //  mCollectionDao.insert(new CollectionList(1,"My Fav nav_manuals", Color.RED));
            //  mCollectionDao.insert(new CollectionList(2,"My second fav nav_manuals", Color.BLACK));
            //  mCollectionDao.insert(new CollectionList(3,"My third fav nav_manuals", Color.YELLOW));
            //   mCollectionItemDao.insert(new CollectionItem(0,1,"03037.pdf","https://fcgresources.blob.core.windows.net/fcgpdfs/03037.pdf","Rational","1234"));
            //    mCollectionItemDao.insert(new CollectionItem(0,1,"03037.pdf","https://fcgresources.blob.core.windows.net/fcgpdfs/03037.pdf","Rational","1234"));
            //   mCollectionItemDao.insert(new CollectionItem(0,1,"03037.pdf","https://fcgresources.blob.core.windows.net/fcgpdfs/03037.pdf","Rational","1234"));
            //   mCollectionItemDao.insert(new CollectionItem(0,1,"03037.pdf","https://fcgresources.blob.core.windows.net/fcgpdfs/03037.pdf","Rational","1234"));
            //   mCollectionItemDao.insert(new CollectionItem(0,2,"03037.pdf","https://fcgresources.blob.core.windows.net/fcgpdfs/03037.pdf","Rational","1234"));
            //   mCollectionItemDao.insert(new CollectionItem(0,3,"03037.pdf","https://fcgresources.blob.core.windows.net/fcgpdfs/03037.pdf","Rational","1234"));
            //   mCollectionItemDao.insert(new CollectionItem(0,3,"03037.pdf","https://fcgresources.blob.core.windows.net/fcgpdfs/03037.pdf","Rational","1234"));
            //   mCollectionItemDao.insert(new CollectionItem(0,3,"03037.pdf","https://fcgresources.blob.core.windows.net/fcgpdfs/03037.pdf","Rational","1234"));
            //   mCollectionItemDao.insert(new CollectionItem(0,3,"03037.pdf","https://fcgresources.blob.core.windows.net/fcgpdfs/03037.pdf","Rational","1234"));

            //  PreviousScanList word = new PreviousScanList("Hello",1);
            //  mListDao.insert(word);
            //  word = new PreviousScanList("World",1);
            //  mListDao.insert(word);
            return null;
        }
    }

}

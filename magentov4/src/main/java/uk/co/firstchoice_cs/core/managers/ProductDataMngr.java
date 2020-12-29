package uk.co.firstchoice_cs.core.managers;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import uk.co.firstchoice_cs.core.api.legacyAPI.models.Manufacturer;
import uk.co.firstchoice_cs.core.api.legacyAPI.models.Model;
import uk.co.firstchoice_cs.core.api.legacyAPI.models.RootManufacturers;
import uk.co.firstchoice_cs.core.api.legacyAPI.models.RootModels;
import uk.co.firstchoice_cs.core.api.legacyAPI.restful.ManufacturersApi;
import uk.co.firstchoice_cs.core.api.legacyAPI.restful.ModelsApi;
import uk.co.firstchoice_cs.core.api.legacyAPI.restful.OnCompletion;


public class ProductDataMngr {

    public static final String TAG = ProductDataMngr.class.getSimpleName();
    private static ProductDataMngr SINGLETON;
    private List<Manufacturer> mCachedManufacturers = new ArrayList<>();
    private List<ManufacturerListener> mManufacturerCallbacks = new ArrayList<>();
    private Map<String, List<Model>> mCachedModels = new WeakHashMap<>();
    private Map<String, List<ModelListener>> mModelCallbacks = new HashMap<>();
    private ManufacturersApi manuTask;

    private ProductDataMngr() {

    }

    public synchronized static ProductDataMngr getInstance() {
        if (SINGLETON == null) {
            SINGLETON = new ProductDataMngr();
        }
        return SINGLETON;
    }

    public interface ModelListener {
        void onError(int err, String msg);

        void onCompletion(List<Model> models);
    }

    public interface ManufacturerListener {
        void onError(int err, String msg);

        void onCompletion(List<Manufacturer> manufacturers);
    }

    private OnCompletion modelTaskListener = new OnCompletion<ModelsApi, RootModels>() {
        @Override
        public void onSuccess(ModelsApi task, RootModels results) {
            String key = task.getManufacturerId();
            if (!TextUtils.isEmpty(task.getCategoryId())) {
                key = key.concat(task.getCategoryId());
            }
            mCachedModels.put(key, results.Models);
            notifyCallbacksSuccess(task.getManufacturerId(), results.Models);
        }

        @Override
        public void onFailure(ModelsApi task, int errorCode, String errorMsg) {
            notifyCallbacksError(task.getManufacturerId(), errorCode, errorMsg);
        }
    };

    private OnCompletion manufacturerTaskListener = new OnCompletion<ManufacturersApi, RootManufacturers>() {
        @Override
        public void onSuccess(ManufacturersApi task, RootManufacturers results) {
            mCachedManufacturers = results.Manufacturers;
            notifyCallbacksSuccess(results.Manufacturers);
            manuTask = null;
        }

        @Override
        public void onFailure(ManufacturersApi task, int errorCode, String errorMsg) {
            notifyCallbacksError(errorCode, errorMsg);
            manuTask = null;
        }
    };

    private void addCallback(String manufacturerId, final ModelListener listener) {
        List<ModelListener> callbacks = mModelCallbacks.get(manufacturerId);
        if (callbacks == null) {
            callbacks = new ArrayList<>();
            mModelCallbacks.put(manufacturerId, callbacks);
        }
        callbacks.add(listener);
    }

    private void addCallback(final ManufacturerListener listener) {
        mManufacturerCallbacks.add(listener);
    }

    private void notifyCallbacksSuccess(String manufacturerId, List<Model> modelList) {
        List<ModelListener> callbacks = mModelCallbacks.get(manufacturerId);
        if (callbacks != null) {
            for (ModelListener l : callbacks) {
                l.onCompletion(modelList);
            }
            callbacks.clear();
        }
    }

    private void notifyCallbacksSuccess(List<Manufacturer> manufacturerList) {
        for (ManufacturerListener l : mManufacturerCallbacks) {
            l.onCompletion(manufacturerList);
        }
        mManufacturerCallbacks.clear();
    }


    private void notifyCallbacksError(String manufacturerId, int err, String msg) {
        List<ModelListener> callbacks = mModelCallbacks.get(manufacturerId);
        if (callbacks != null) {
            for (ModelListener l : callbacks) {
                l.onError(err, msg);
            }
            callbacks.clear();
        }
    }

    private void notifyCallbacksError(int err, String msg) {
        for (ManufacturerListener l : mManufacturerCallbacks) {
            l.onError(err, msg);
        }
        mManufacturerCallbacks.clear();
    }


    public void getModels(String manufacturerId, ModelListener listener, boolean force) {
        if (!force) {
            List<Model> modelList = mCachedModels.get(manufacturerId);
            if (modelList != null) {
                if (listener != null) {
                    listener.onCompletion(modelList);
                }
                return;
            }
        }
        ModelsApi modelTask = new ModelsApi(manufacturerId);
        addCallback(manufacturerId, listener);
        modelTask.execute(modelTaskListener);
    }

    public void getModels(String manufacturerId, String categoryCode, ModelListener listener, boolean force) {
        if (!force) {
            String key = manufacturerId.concat(categoryCode);
            List<Model> modelList = mCachedModels.get(key);
            if (modelList != null) {
                if (listener != null) {
                    listener.onCompletion(modelList);
                }
                return;
            }
        }
        ModelsApi modelTask = new ModelsApi(manufacturerId, categoryCode);
        addCallback(manufacturerId, listener);
        modelTask.execute(modelTaskListener);
    }

    private ManufacturersApi getManufacturersAsync() {
        if (manuTask == null) {
            manuTask = new ManufacturersApi();
            return manuTask;
        }
        return null;
    }

    public void getManufacturers(ManufacturerListener listener, boolean force) {
        if (!force) {
            if (!mCachedManufacturers.isEmpty()) {
                if (listener != null) {
                    listener.onCompletion(mCachedManufacturers);
                }
                return;
            }
        }
        // Then let's request
        addCallback(listener);
        ManufacturersApi manuTask = getManufacturersAsync();
        // If null is returned then there's a currently running task.  Only execute a fresh request.
        if (manuTask != null) {
            manuTask.execute(manufacturerTaskListener);
        }
    }

    public Manufacturer getManufacturer(String name) {
        for (Manufacturer m : mCachedManufacturers) {
            if (name.equalsIgnoreCase(m.Name)) {
                return m;
            }
        }
        return null;
    }


    public void removeListener(final ManufacturerListener listener) {
        mManufacturerCallbacks.remove(listener);
    }


    public void removeListener(final ModelListener listener) {
        mModelCallbacks.remove(listener);
    }
}

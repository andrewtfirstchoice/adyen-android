package uk.co.firstchoice_cs.core.api.legacyAPI.restful;

import android.text.TextUtils;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import uk.co.firstchoice_cs.core.api.legacyAPI.models.RootModels;
import uk.co.firstchoice_cs.core.api.legacyAPI.restful.api.Api;

/**
 * Created by steveparrish on 4/11/17.
 */
public class ModelsApi extends BaseAsync {

    public static final String TAG = ModelsApi.class.getSimpleName();

    private static final String API_MODELS = Api.MODELS;
    private static final String API_CATEGORY_MODELS = Api.CATEGORY_MODELS;
    private final String manufacturerId;
    private final String categoryId;

    public ModelsApi(String manufacturerId) {
        super();
        this.manufacturerId = manufacturerId;
        this.categoryId = null;
    }

    public ModelsApi(String manufacturerId, String categoryId) {
        super();
        this.manufacturerId = manufacturerId;
        this.categoryId = categoryId;
    }

    private Response<RootModels> searchAllModels() {
        Response<RootModels> response = null;
        ModelsApi.IApiModels models = retrofit.create(ModelsApi.IApiModels.class);
        Call<RootModels> request = models.listDetails(manufacturerId);
        try {
            response = request.execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    private Response<RootModels> searchCategoryModels() {
        Response<RootModels> response = null;
        ModelsApi.IApiCategoryModels models = retrofit.create(ModelsApi.IApiCategoryModels.class);
        Call<RootModels> request = models.listDetails(manufacturerId, categoryId);
        try {
            response = request.execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    @Override
    public Response<RootModels> search() {
        if (!TextUtils.isEmpty(categoryId)) {
            return searchCategoryModels();
        }
        return searchAllModels();
    }

    public String getManufacturerId() {
        return manufacturerId;
    }

    public String getCategoryId() {
        return categoryId;
    }

    private interface IApiModels {
        @Headers({
                "Accept: application/json",
        })
        @GET(API_MODELS)
        Call<RootModels> listDetails(@Path("Manufacturerid") String manufacturerId);
    }

    private interface IApiCategoryModels {
        @Headers({
                "Accept: application/json",
        })
        @GET(API_CATEGORY_MODELS)
        Call<RootModels> listDetails(@Path("Manufacturerid") String manufacturerId,
                                     @Path("Categoryid") String categoryId);
    }
}

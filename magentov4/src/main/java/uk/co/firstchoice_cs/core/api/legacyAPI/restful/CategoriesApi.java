package uk.co.firstchoice_cs.core.api.legacyAPI.restful;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import uk.co.firstchoice_cs.core.api.legacyAPI.models.RootCategories;
import uk.co.firstchoice_cs.core.api.legacyAPI.restful.api.Api;


/**
 * Created by steveparrish on 4/12/17.
 */
public class CategoriesApi extends BaseAsync {
    public static final String TAG = CategoriesApi.class.getSimpleName();

    private static final String API = Api.CATEGORIES;

    private interface IApiCategories {
        @Headers({
                "Accept: application/json",
        })
        @GET(API)
        Call<RootCategories> listDetails(@Path("Manufacturerid") String manufacturerId);
    }


    @Override
    public Response<RootCategories> search() {
        Response<RootCategories> response = null;
        CategoriesApi.IApiCategories models = retrofit.create(CategoriesApi.IApiCategories.class);
        Call<RootCategories> request = models.listDetails(manufacturerId);
        try {
            response = request.execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;

    }

    public String getManufacturerId() {
        return manufacturerId;
    }

    private final String manufacturerId;

    public CategoriesApi(String manufacturerId) {
        super();
        this.manufacturerId = manufacturerId;
    }
}

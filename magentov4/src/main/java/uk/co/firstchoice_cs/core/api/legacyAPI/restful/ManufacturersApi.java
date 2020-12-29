package uk.co.firstchoice_cs.core.api.legacyAPI.restful;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import uk.co.firstchoice_cs.core.api.legacyAPI.models.RootManufacturers;
import uk.co.firstchoice_cs.core.api.legacyAPI.restful.api.Api;


public class ManufacturersApi extends BaseAsync {
    public static final String TAG = ManufacturersApi.class.getSimpleName();

    public static final String API = Api.MANUFACTURERS;

    public ManufacturersApi() {
        super();
    }

    @Override
    public Response<RootManufacturers> search() {
        IApiManufacturers manufacturers = retrofit.create(IApiManufacturers.class);
        Call<RootManufacturers> request = manufacturers.listDetails();
        Response<RootManufacturers> response = null;
        try {
            response = request.execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    protected interface IApiManufacturers {
        @Headers({
                "Accept: application/json",
        })
        @GET(API)
        Call<RootManufacturers> listDetails();
    }
}




package uk.co.firstchoice_cs.core.api.legacyAPI.restful;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import uk.co.firstchoice_cs.core.api.legacyAPI.models.RootCatalogues;
import uk.co.firstchoice_cs.core.api.legacyAPI.restful.api.Api;


public class CataloguesApi extends BaseAsync {

    public static final String TAG = CataloguesApi.class.getSimpleName();

    private static final String API = Api.CATALOGUES;

    private interface IApiCatalogues {
        @Headers({
                "Accept: application/json",
        })
        @GET(API)
        Call<RootCatalogues> listDetails();
    }

    @Override
    public Response<RootCatalogues> search() {
        IApiCatalogues manufacturers = retrofit.create(IApiCatalogues.class);
        Call<RootCatalogues> request = manufacturers.listDetails();
        Response<RootCatalogues> response = null;
        try {
            response = request.execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    public CataloguesApi() { super(); }
}

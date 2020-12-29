package uk.co.firstchoice_cs.core.api.legacyAPI.restful;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import uk.co.firstchoice_cs.core.api.legacyAPI.models.RootDocuments;
import uk.co.firstchoice_cs.core.api.legacyAPI.restful.api.Api;


/**
 * Created by steveparrish on 4/11/17.
 */
public class DocumentsApi extends BaseAsync {

    public static final String TAG = DocumentsApi.class.getSimpleName();

    private static final String API = Api.DOCS;

    private interface IApiDocuments {
        @Headers({
                "Accept: application/json",
        })
        @GET(API)
        Call<RootDocuments> listDetails(@Path("Modelid") String modelId);
    }

    @Override
    public Response<RootDocuments> search() {
        Response<RootDocuments> response = null;
        IApiDocuments models = retrofit.create(IApiDocuments.class);
        Call<RootDocuments> request = models.listDetails(modelId);

        try {
            response = request.execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    public String getModelId() {
        return modelId;
    }

    private final String modelId;
    public DocumentsApi(String modelId) {
        super();
        this.modelId = modelId;
    }
}

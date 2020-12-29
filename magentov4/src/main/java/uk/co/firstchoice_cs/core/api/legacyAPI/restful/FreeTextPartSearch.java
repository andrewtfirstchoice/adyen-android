package uk.co.firstchoice_cs.core.api.legacyAPI.restful;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;
import uk.co.firstchoice_cs.core.api.legacyAPI.models.RootProducts;
import uk.co.firstchoice_cs.core.api.legacyAPI.restful.api.Api;

/**
 * Created by steveparrish on 10/6/17.
 */

public class FreeTextPartSearch extends BaseAsync {
    public static final String TAG = FreeTextPartSearch.class.getSimpleName();

    public static final String API_PART_SEARCH = Api.FREE_TEXT_PART_SEARCH;

    public static final String SEARCH_QUERY = "search";

    Map<String, String> queryParameters = new LinkedHashMap<String, String>();

    private interface IApiKeyword {
        @Headers({
                "Accept: application/json",
        })
        @GET(API_PART_SEARCH)
        Call<RootProducts> keyword(@Query("search") String keyword,
                                   @Query("currentpage") int currentPage,
                                   @Query("pagesize") int pageSize);
    }


    @Override
    public Response<RootProducts> search() {
        Response<RootProducts> response = null;
        String search = queryParameters.get(SEARCH_QUERY);
        // Then go for a search
        IApiKeyword parts = retrofit.create(IApiKeyword.class);
        Call<RootProducts> request = parts.keyword(search, currentPage, pageSize);
        try {
            response = request.execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }


    public void addFilter(String filter, String value) {
        queryParameters.put(filter, value);
    }
    final private int currentPage;
    final private int pageSize;

    public FreeTextPartSearch(int currentPage, int pageSize) {
        super();
        this.currentPage = currentPage;
        this.pageSize = pageSize;
    }
}

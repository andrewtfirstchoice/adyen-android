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
 * Created by steveparrish on 8/28/17.
 */

public class PartSearchApi extends BaseAsync {
    public static final String TAG = PartSearchApi.class.getSimpleName();

    private static final String API_PART_SEARCH = Api.PART_SEARCH;

    public static final String SEARCH_QUERY = "search";
    public static final String MANUFACTURER_QUERY = "manufacturer";
    public static final String MODEL_QUERY = "model";
    public static final String BARCODE_QUERY = "barcode";
    public static final String PART_NUMBER_QUERY = "part";
    public static final String FCC_PART_NUMBER_QUERY = "fccpart";

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

    private interface IApiBarcode {
        @Headers({
                "Accept: application/json",
        })
        @GET(API_PART_SEARCH)
        Call<RootProducts> barcode(@Query("barcode") String value,
                                   @Query("currentpage") int currentPage,
                                   @Query("pagesize") int pageSize);
    }

    private interface IApiPart {
        @Headers({
                "Accept: application/json",
        })
        @GET(API_PART_SEARCH)
        Call<RootProducts> partNumber(@Query("part") String value,
                                      @Query("currentpage") int currentPage,
                                      @Query("pagesize") int pageSize);
    }

    private interface IApiFccPart {
        @Headers({
                "Accept: application/json",
        })
        @GET(API_PART_SEARCH)
        Call<RootProducts> partNumber(@Query("fccpart") String value,
                                      @Query("currentpage") int currentPage,
                                      @Query("pagesize") int pageSize);
    }

    private interface IApiManufacturer {
        @Headers({
                "Accept: application/json",
        })
        @GET(API_PART_SEARCH)
        Call<RootProducts> manufacturer(@Query("mfr") String manufacturer,
                                        @Query("currentpage") int currentPage,
                                        @Query("pagesize") int pageSize);
    }

    private interface IApiManufacturerModel {
        @Headers({
                "Accept: application/json",
        })
        @GET(API_PART_SEARCH)
        Call<RootProducts> manufacturerModel(@Query("mfr") String manufacturer,
                                             @Query("model") String model,
                                             @Query("currentpage") int currentPage,
                                             @Query("pagesize") int pageSize);
    }

    private interface IApiManufacturerKeyword {
        @Headers({
                "Accept: application/json",
        })
        @GET(API_PART_SEARCH)
        Call<RootProducts> manufacturerKeyword(@Query("mfr") String manufacturer,
                                               @Query("search") String keyword,
                                               @Query("currentpage") int currentPage,
                                               @Query("pagesize") int pageSize);
    }

    private interface IApiManufacturerModelKeyword {
        @Headers({
                "Accept: application/json",
        })
        @GET(API_PART_SEARCH)
        Call<RootProducts> manufacturerModelKeyword(@Query("mfr") String manufacturer,
                                                    @Query("model") String model,
                                                    @Query("search") String keyword,
                                                    @Query("currentpage") int currentPage,
                                                    @Query("pagesize") int pageSize);
    }

    @Override
    public Response<RootProducts> search() {
        Call<RootProducts> request = null;
        Response<RootProducts> response = null;

        String manu = queryParameters.get(MANUFACTURER_QUERY);
        String model = queryParameters.get(MODEL_QUERY);
        String search = queryParameters.get(SEARCH_QUERY);
        String barcode = queryParameters.get(BARCODE_QUERY);
        String partNumber = queryParameters.get(PART_NUMBER_QUERY);
        String fccPartNumber =  queryParameters.get(FCC_PART_NUMBER_QUERY);

        if (barcode != null) {
            IApiBarcode parts = retrofit.create(IApiBarcode.class);
            request = parts.barcode(barcode, currentPage, pageSize);
        }
        else if (partNumber != null) {
            IApiPart parts = retrofit.create(IApiPart.class);
            request = parts.partNumber(partNumber, currentPage, pageSize);
        }
        else if (fccPartNumber != null) {
            IApiFccPart parts = retrofit.create(IApiFccPart.class);
            request = parts.partNumber(fccPartNumber, currentPage, pageSize);
        }
        else if (manu != null && model != null && search != null) {
            IApiManufacturerModelKeyword parts = retrofit.create(IApiManufacturerModelKeyword.class);
            request = parts.manufacturerModelKeyword(manu, model, search, currentPage, pageSize);
        }
        else if (manu != null && model != null) {
            IApiManufacturerModel parts = retrofit.create(IApiManufacturerModel.class);
            request = parts.manufacturerModel(manu, model, currentPage, pageSize);
        }
        else if (manu != null && search != null) {
            IApiManufacturerKeyword parts = retrofit.create(IApiManufacturerKeyword.class);
            request = parts.manufacturerKeyword(manu, search, currentPage, pageSize);
        }
        else if (manu != null) {
            IApiManufacturer parts = retrofit.create(IApiManufacturer.class);
            request = parts.manufacturer(manu, currentPage, pageSize);
        }
        else {
            // Then go for a search
            IApiKeyword parts = retrofit.create(IApiKeyword.class);
            request = parts.keyword(search, currentPage, pageSize);
        }

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
    public PartSearchApi(int currentPage, int pageSize) {
        super();
        this.currentPage = currentPage;
        this.pageSize = pageSize;
    }
}

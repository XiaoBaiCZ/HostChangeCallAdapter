package cc.xiaobaicz.calladapter.hostchange;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;

interface Api {
    @Ding
    @GET("/")
    Call<ResponseBody> index();
}

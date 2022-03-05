package cc.xiaobaicz.calladapter.hostchange;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void testHostChangeCallAdapter() {
        HostChangeCallAdapterFactory.registerAnnotation(Ding.class);
        Retrofit retrofit = new Retrofit.Builder().baseUrl("https://www.baidu.com")
                .addCallAdapterFactory(HostChangeCallAdapterFactory.create())
                .build();
        try {
            Response<ResponseBody> response = retrofit.create(Api.class).index().execute();
            System.out.println(response.body().string());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
# 自定义Retrofit2 CallAdapter

### HostChangeCallAdapter
- 多域名场景
- 动态改变 Retrofit2 URL
- 通过自定义注解标识
- 不影响原有 CallAdapter（可同时使用，但要先添加 HostChangeCallAdapter，否则不生效）

### 依赖
~~~ java
// Step 1. Add it in your root build.gradle at the end of repositories:
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
~~~
~~~ java
// Step 2. Add the dependency
dependencies {
    implementation 'com.github.XiaoBaiCZ:HostChangeCallAdapter:0.1.1'
}
~~~

### 使用
- 自定义注解
~~~ java
// AService.java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AService {
    // 定义该注解函数，默认值为BaseURL
    String baseUrl() default "https://www.aaa.com";
}
~~~

- 自定义注解
~~~ java
// BService.java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BService {
    // 定义该注解函数，默认值为BaseURL
    String baseUrl() default "https://www.bbb.com";
}
~~~

- Api 接口
~~~ java
// Api.java
interface Api {
    @GET("/index")
    Call<ResponseBody> index();     // 默认 baseUrl
    
    // 添加 AService 注解
    @AService @GET("/index")        // 真实请求 https://www.aaa.com/index
    Call<ResponseBody> aIndex();    // 自动替换为 AService 的 baseUrl
    
    // 添加 BService 注解
    @BService @GET("/index")        // 真实请求 https://www.bbb.com/index
    Single<ResponseBody> bIndex();  // 自动替换为 BService 的 baseUrl，不影响原有 CallAdapter
}
~~~

- UnitTest
~~~ java
public class ExampleUnitTest {
    @Test
    public void testHostChangeCallAdapter() {
        HostChangeCallAdapterFactory.registerAnnotation(AService.class);        // 只需要注册一次注解
        HostChangeCallAdapterFactory.registerAnnotation(BService.class);        // 只需要注册一次注解
        
        Retrofit retrofit = new Retrofit.Builder().baseUrl("https://www.xxx.com")
                // 先添加 HostChangeCallAdapterFactory
                .addCallAdapterFactory(HostChangeCallAdapterFactory.create())
                // 可以再添加其他 CallAdapter，使用 HostChange 的同时，不会影响再添加的 CallAdapter
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build();
                
        try {
            // 原 baseUrl 请求
            Response<ResponseBody> response1 = retrofit.create(Api.class).index().execute();
            System.out.println(response1.body().string());
            
            // AService baseUrl 请求
            Response<ResponseBody> response2 = retrofit.create(Api.class).aIndex().execute();
            System.out.println(response2.body().string());
            
            // BService baseUrl 请求
            Response<ResponseBody> response3 = retrofit.create(Api.class).bIndex().execute();
            System.out.println(response3.body().string());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
~~~
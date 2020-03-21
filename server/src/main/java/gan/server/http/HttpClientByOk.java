package gan.server.http;

import com.squareup.okhttp.*;
import gan.log.DebugLog;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class HttpClientByOk{
    private static final String TAG = HttpClientByOk.class.getName();

    private static HttpClientByOk instance;
    private static OkHttpClient sClient;
    static{
        instance = new HttpClientByOk();
        sClient = new OkHttpClient();
        sClient.setConnectTimeout(10,TimeUnit.SECONDS);
        sClient.setReadTimeout(30,TimeUnit.SECONDS);
    }

    private HttpClientByOk(){
    }

    public static HttpClientByOk getInstance() {
        return instance;
    }

    public Response get(String url)throws IOException {
        DebugLog.info("get url:"+url);
        final Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        return sClient.newCall(request).execute();
    }

    public Response post(String url,RequestParams requestParams)throws IOException {
        DebugLog.info("post url:"+url);
        Request request = new Request.Builder()
                .url(url)
                .post(buildRequestBody(requestParams))
                .build();
        return sClient.newCall(request).execute();
    }

    public Response delete(String url,RequestParams requestParams)throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .delete(buildRequestBody(requestParams))
                .build();
        return sClient.newCall(request).execute();
    }

    public Response put(String url,RequestParams requestParams) throws IOException{
        Request request = new Request.Builder()
                .url(url)
                .put(buildRequestBody(requestParams))
                .build();
        return null;
    }

    public RequestBody buildRequestBody(RequestParams requestParams){
        RequestBody requestBody;
        if(!requestParams.getFileParams().isEmpty()){
            MultipartBuilder builder = new MultipartBuilder()
                    .type(MultipartBuilder.FORM);
            for(RequestParams.FileWrapper fileWrapper: requestParams.getFileParams().values()){
                builder.addFormDataPart("file",fileWrapper.file.getName(),
                        RequestBody.create(MediaType.parse(fileWrapper.contentType),
                                fileWrapper.file));
            }
            for(String name: requestParams.getParams().keySet()){
                builder.addFormDataPart(name, requestParams.get(name));
            }
            requestBody = builder.build();
        }else{
            MultipartBuilder builder = new MultipartBuilder()
                    .type(MultipartBuilder.FORM);
            for(String name:requestParams.getParams().keySet()){
                builder.addFormDataPart(name, requestParams.get(name));
            }
            requestBody = builder.build();
        }
        return requestBody;
    }
}

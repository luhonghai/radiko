package com.gmail.radioserver2.radiko.token;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.gmail.radioserver2.R;
import com.gmail.radioserver2.utils.SimpleAppLog;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by luhonghai on 24/02/2015.
 */
public class TokenRequester {
    private static final String TAG = "TokenRequester";

    private final Context context;

    public TokenRequester(Context context) {
        this.context = context;
    }

    private String getHeaderValue(HttpResponse response, String header) {
        Header h = response.getFirstHeader(header);
        if (h != null) return  h.getValue();
        return "";
    }

    public String requestToken() throws IOException {
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("https",
                SSLSocketFactory.getSocketFactory(), 443));

        HttpParams params = new BasicHttpParams();

        SingleClientConnManager mgr = new SingleClientConnManager(params, schemeRegistry);

        HttpClient httpClient = new DefaultHttpClient(mgr, params);
        HttpPost httpPost = new HttpPost(context.getResources().getString(R.string.radiko_auth1_fms));
        httpPost.setHeader("pragma","no-cache");
        httpPost.setHeader("X-Radiko-App","pc_1");
        httpPost.setHeader("X-Radiko-App-Version","2.0.1");
        httpPost.setHeader("X-Radiko-User","test-stream");
        httpPost.setHeader("X-Radiko-Device","pc");
        httpPost.setEntity(new StringEntity("\r\n","UTF-8"));

        HttpResponse response = httpClient.execute(httpPost);
        String authToken =getHeaderValue(response, "x-radiko-authtoken").trim();
        String keyOffset = getHeaderValue(response, "x-radiko-keyoffset").trim();
        String keyLength = getHeaderValue(response, "x-radiko-keylength").trim();

        try {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                entity.consumeContent();
            }
        } catch (Exception ex) {

        }
        Log.i(TAG, "authToken: " + authToken);
        Log.i(TAG, "keyOffset: " + keyOffset);
        Log.i(TAG, "keyLength: " + keyLength);
        if (authToken.length() > 0 && keyOffset.length() > 0 && keyLength.length() > 0) {
            String partialKey = "";
            try {
                 partialKey = generatePartialKey(Integer.parseInt(keyOffset), Integer.parseInt(keyLength)).trim();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            Log.i(TAG, "partialKey: " + partialKey);
            if (partialKey.length() > 0) {
                httpPost = new HttpPost(context.getResources().getString(R.string.radiko_auth2_fms));
                httpPost.setHeader("pragma","no-cache");
                httpPost.setHeader("content-type","application/x-www-form-urlencoded");
                httpPost.setHeader("DNT","1");
                httpPost.setHeader("X-Radiko-App","pc_1");
                httpPost.setHeader("X-Radiko-App-Version","2.0.1");
                httpPost.setHeader("X-Radiko-User","test-stream");
                httpPost.setHeader("X-Radiko-Device","pc");
                httpPost.setHeader("X-Radiko-Authtoken",authToken);
                httpPost.setHeader("X-Radiko-Partialkey",partialKey);
                httpPost.setEntity(new StringEntity("\r\n", "UTF-8"));

                schemeRegistry = new SchemeRegistry();
                schemeRegistry.register(new Scheme("https",
                        SSLSocketFactory.getSocketFactory(), 443));

                params = new BasicHttpParams();

                mgr = new SingleClientConnManager(params, schemeRegistry);

                httpClient = new DefaultHttpClient(mgr, params);
                response = httpClient.execute(httpPost);
                InputStream is = null;
                try {
                    HttpEntity entity = response.getEntity();
                    is = entity.getContent();
                    String strRes = IOUtils.toString(is);
                    Log.i(TAG, "Response: " + strRes);
                    entity.consumeContent();
                } catch (Exception ex) {
                    SimpleAppLog.error("Could not get response",ex);
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (Exception e) {}
                    }
                }
                return authToken;
            }
        }
        return "";
    }

    public String generatePartialKey(int offset, int length) {
        InputStream inputStream = null;
        try {
            inputStream = context.getResources().openRawResource(R.raw.key_bin);
            byte[] bytes = new byte[length];
            inputStream.skip(offset);
            inputStream.read(bytes, 0, length);
            return Base64.encodeToString(bytes, Base64.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally{
            if (inputStream != null)
                try {
                    inputStream.close();
                } catch (Exception e) {}
        }
        return "";
    }
}

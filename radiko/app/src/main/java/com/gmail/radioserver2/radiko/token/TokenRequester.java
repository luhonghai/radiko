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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

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
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(context.getResources().getString(R.string.radiko_auth1_fms));
        httpPost.setHeader("pragma","no-cache");
        httpPost.setHeader("X-Radiko-App","pc_1");
        httpPost.setHeader("X-Radiko-App-Version","2.0.1");
        httpPost.setHeader("X-Radiko-User","test-stream");
        httpPost.setHeader("X-Radiko-Device","pc");
        httpPost.setEntity(new StringEntity("\r\n","UTF-8"));

        HttpResponse response = httpClient.execute(httpPost);
        String authToken =getHeaderValue(response, "x-radiko-authtoken");
        String keyOffset = getHeaderValue(response, "x-radiko-keyoffset");
        String keyLength = getHeaderValue(response, "x-radiko-keylength");
        SimpleAppLog.info("authToken: " + authToken);
        SimpleAppLog.info( "keyOffset: " + keyOffset);
        SimpleAppLog.info( "keyLength: " + keyLength);
        if (authToken.length() > 0 && keyOffset.length() > 0 && keyLength.length() > 0) {
            String partialKey = "";
            try {
                 partialKey = generatePartialKey(Integer.parseInt(keyOffset), Integer.parseInt(keyLength));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            Log.i(TAG, "partialKey: " + partialKey);
            if (partialKey.length() > 0) {
                HttpGet httpGet = new HttpGet(context.getResources().getString(R.string.radiko_auth2_fms));
                httpGet.setHeader("pragma","no-cache");
                httpGet.setHeader("X-Radiko-App","pc_1");
                httpGet.setHeader("X-Radiko-App-Version","2.0.1");
                httpGet.setHeader("X-Radiko-User","test-stream");
                httpGet.setHeader("X-Radiko-Device","pc");
                httpGet.setHeader("X-Radiko-Authtoken",authToken);
                httpGet.setHeader("X-Radiko-Partialkey",partialKey);
                //httpGet.setEntity(new StringEntity("\r\n", "UTF-8"));
                httpClient = new DefaultHttpClient();
                response = httpClient.execute(httpGet);
                String strRes = IOUtils.toString(response.getEntity().getContent());
                Log.i(TAG, "Response: " + strRes);
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

package com.dotohsoft.api;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;

/**
 * Created by luhonghai on 24/02/2015.
 */
public class TokenRequester {

    public interface TokenRequesterListener {

        public void onMessage(String message);

        public void onError(String message, Throwable e);
    }

    private final TokenRequesterListener requesterListener;

    private final File keyBin;

    public TokenRequester(TokenRequesterListener requesterListener, File keyBin) {
        this.requesterListener = requesterListener;
        this.keyBin = keyBin;
    }

    private String getHeaderValue(HttpResponse response, String header) {
        Header h = response.getFirstHeader(header);
        if (h != null) return  h.getValue();
        return "";
    }


    public String requestToken() throws IOException {
        HttpClient httpClient = getNewHttpClient();
        HttpPost httpPost = new HttpPost("https://radiko.jp/v2/api/auth1_fms");
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
        requesterListener.onMessage("authToken: " + authToken);
        requesterListener.onMessage("keyOffset: " + keyOffset);
        requesterListener.onMessage("keyLength: " + keyLength);
        if (authToken.length() > 0 && keyOffset.length() > 0 && keyLength.length() > 0) {
            String partialKey = "";
            try {
                partialKey = generatePartialKey(Integer.parseInt(keyOffset), Integer.parseInt(keyLength)).trim();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            requesterListener.onMessage("partialKey: " + partialKey);
            if (partialKey.length() > 0) {
                httpPost = new HttpPost("https://radiko.jp/v2/api/auth2_fms");
                httpPost.setHeader("pragma","no-cache");
                httpPost.setHeader("X-Radiko-App","pc_1");
                httpPost.setHeader("X-Radiko-App-Version","2.0.1");
                httpPost.setHeader("X-Radiko-User","test-stream");
                httpPost.setHeader("X-Radiko-Device","pc");
                httpPost.setHeader("X-Radiko-Authtoken",authToken);
                httpPost.setHeader("X-Radiko-Partialkey",partialKey);
                httpPost.setEntity(new StringEntity("\r\n", "UTF-8"));

                httpClient = getNewHttpClient();
                response = httpClient.execute(httpPost);
                InputStream is = null;
                try {
                    HttpEntity entity = response.getEntity();
                    is = entity.getContent();
                    String strRes = IOUtils.toString(is);
                    requesterListener.onMessage("Response: " + strRes);
                    entity.consumeContent();
                    return authToken;
                } catch (Exception ex) {
                    requesterListener.onError("Could not get response", ex);
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (Exception e) {}
                    }
                }
            }
        }
        return "";
    }

    public String generatePartialKey(int offset, int length) {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(keyBin);
            byte[] bytes = new byte[length];
            inputStream.skip(offset);
            inputStream.read(bytes, 0, length);
            return new String(Base64.encodeBase64(bytes), "UTF-8");
        } catch (Exception ex) {
            requesterListener.onError("Could not generate partial key", ex);
        } finally{
            if (inputStream != null)
                try {
                    inputStream.close();
                } catch (Exception e) {}
        }
        return "";
    }

    public HttpClient getNewHttpClient() {
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore
                    .getDefaultType());
            trustStore.load(null, null);

            SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
            sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            HttpParams params = new BasicHttpParams();
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

            SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", PlainSocketFactory
                    .getSocketFactory(), 80));
            registry.register(new Scheme("https", sf, 443));

            ClientConnectionManager ccm = new ThreadSafeClientConnManager(
                    params, registry);

            return new DefaultHttpClient(ccm, params);
        } catch (Exception e) {
            return new DefaultHttpClient();
        }
    }

    public static void main(String[] args) throws IOException {
        String keyBin = "/Volumes/DATA/Development/radiko/radiko/app/src/main/res/raw/key_bin";
        if (args != null && args.length == 1)
            keyBin = args[0];
        TokenRequester requester = new TokenRequester(new TokenRequesterListener() {
            @Override
            public void onMessage(String message) {
                System.out.println(message);
            }

            @Override
            public void onError(String message, Throwable e) {
                e.printStackTrace();
            }
        }, new File(keyBin));
        System.out.println(requester.requestToken());
    }
}
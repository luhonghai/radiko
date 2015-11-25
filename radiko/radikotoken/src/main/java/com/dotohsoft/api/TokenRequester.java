package com.dotohsoft.api;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Created by luhonghai on 24/02/2015.
 */
public class TokenRequester {
    private static final String RADIKO_APP = "aSmartPhone4";
    private static final String RADIKO_APP_VERSION = "4.0.3";
    private static final String RADIKO_USER = "test-stream";
    private static final String RADIKO_DEVICE = "android";

    public static interface TokenExchange {
        public byte[] exchange(byte[] input);
    }

    public static class TokenData {
        private String token;
        private String output;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getOutput() {
            return output;
        }

        public void setOutput(String output) {
            this.output = output;
        }
    }

    public interface TokenRequesterListener {

        void onMessage(String message);

        void onError(String message, Throwable e);
    }

    private final TokenRequesterListener requesterListener;

    private final File keyBin;

    private TokenExchange tokenExchange;

    public TokenRequester(TokenRequesterListener requesterListener, File keyBin) {
        this.requesterListener = requesterListener;
        this.keyBin = keyBin;
    }

    public TokenRequester(TokenRequesterListener requesterListener, File keyBin, TokenExchange tokenExchange) {
        this(requesterListener, keyBin);
        this.tokenExchange = tokenExchange;
    }

    private String getHeaderValue(HttpURLConnection urlConnection, String header) {
        String h = urlConnection.getHeaderField(header);
        if (h != null) return h;
        return "";
    }


    public TokenData requestToken() throws IOException {
        return requestToken(-1, -1);
    }

    public TokenData requestToken(double lat, double lon) throws IOException {
        HttpURLConnection httpURLConnection = getNewHttpConnection("https://radiko.jp/v2/api/auth1_fms");
        if (httpURLConnection != null) {
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setRequestProperty("pragma", "no-cache");
            httpURLConnection.setRequestProperty("X-Radiko-App", RADIKO_APP);
            httpURLConnection.setRequestProperty("X-Radiko-App-Version", RADIKO_APP_VERSION);
            httpURLConnection.setRequestProperty("X-Radiko-User", RADIKO_USER);
            httpURLConnection.setRequestProperty("X-Radiko-Device", RADIKO_DEVICE);
            DataOutputStream wr = new DataOutputStream(httpURLConnection.getOutputStream());
            String urlParameters = "\r\n";
            httpURLConnection.connect();
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();
            String authToken = getHeaderValue(httpURLConnection, "x-radiko-authtoken").trim();
            String keyOffset = getHeaderValue(httpURLConnection, "x-radiko-keyoffset").trim();
            String keyLength = getHeaderValue(httpURLConnection, "x-radiko-keylength").trim();
            httpURLConnection.disconnect();
            requesterListener.onMessage("authToken: " + authToken);
            requesterListener.onMessage("keyOffset: " + keyOffset);
            requesterListener.onMessage("keyLength: " + keyLength);
            if (authToken.length() > 0 && keyOffset.length() > 0 && keyLength.length() > 0) {
                String partialKey = "";
                try {
                    partialKey = generatePartialKey(Integer.parseInt(keyOffset), Integer.parseInt(keyLength), tokenExchange).trim();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                requesterListener.onMessage("partialKey: " + partialKey);
                if (partialKey.length() > 0) {
                    httpURLConnection = getNewHttpConnection("https://radiko.jp/v2/api/auth2_fms");
                    httpURLConnection.setRequestMethod("POST");
                    httpURLConnection.setRequestProperty("pragma", "no-cache");
                    httpURLConnection.setRequestProperty("X-Radiko-App", RADIKO_APP);
                    httpURLConnection.setRequestProperty("X-Radiko-App-Version", RADIKO_APP_VERSION);
                    httpURLConnection.setRequestProperty("X-Radiko-User", RADIKO_USER);
                    httpURLConnection.setRequestProperty("X-Radiko-Device", RADIKO_DEVICE);
                    httpURLConnection.setRequestProperty("X-Radiko-Authtoken", authToken);
                    httpURLConnection.setRequestProperty("X-Radiko-Partialkey", partialKey);
                    if (lon != -1 && lat != -1) {
                        httpURLConnection.setRequestProperty("X-Radiko-Location", lat + "," + lon + ",gps");
                    }
                    httpURLConnection.connect();
                    wr = new DataOutputStream(httpURLConnection.getOutputStream());
                    wr.writeBytes(urlParameters);
                    wr.flush();
                    wr.close();
                    InputStream is = null;
                    try {
                        is = httpURLConnection.getInputStream();
                        String strRes = IOUtils.toString(is);
                        requesterListener.onMessage("Request token response: " + strRes);
                        TokenData tokenData = new TokenData();
                        tokenData.setToken(authToken);
                        strRes = strRes.replace("\n", " ");
                        strRes = strRes.replace("\t", " ");
                        while (strRes.contains("  ")) {
                            strRes = strRes.replace("  ", " ");
                        }
                        strRes = strRes.trim();
                        if (strRes.equalsIgnoreCase("OUT")) {
                            strRes = "";
                        }
                        tokenData.setOutput(strRes);
                        return tokenData;
                    } catch (Exception ex) {
                        requesterListener.onError("Could not get response", ex);
                    } finally {
                        if (is != null) {
                            try {
                                is.close();
                            } catch (Exception e) {//}
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public String generatePartialKey(int offset, int length) {
        return generatePartialKey(offset, length, null);
    }

    public String generatePartialKey(int offset, int length, TokenExchange tokenExchange) {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(keyBin);
            byte[] data = IOUtils.toByteArray(inputStream);
            if (tokenExchange != null) {
                data = tokenExchange.exchange(data);
            }
            if (offset + length > data.length) {
                return "";
            }
            byte[] bytes = new byte[length];
            System.arraycopy(data, offset, bytes, 0, length);
            return new String(Base64.encodeBase64(bytes), "UTF-8");
        } catch (Exception ex) {
            requesterListener.onError("Could not generate partial key", ex);
        } finally {
            if (inputStream != null)
                try {
                    inputStream.close();
                } catch (Exception e) {
                    //
                }
        }
        return "";
    }

    public HttpURLConnection getNewHttpConnection(String urlString) {
        try {
            URL url = new URL(urlString);
            try {
                if (urlString.startsWith("http://")) {
                    HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                    httpURLConnection.setRequestProperty("Accept-Encoding", "identity");
                    return httpURLConnection;
                }

                KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                trustStore.load(null, null);
                String algorithm = KeyManagerFactory.getDefaultAlgorithm();
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
                kmf.init(trustStore, null);
                SSLContext context = SSLContext.getInstance("TLS");
                context.init(kmf.getKeyManagers(), trustAllCerts, null);
                HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setRequestProperty("Accept-Encoding", "identity");
                urlConnection.setSSLSocketFactory(context.getSocketFactory());
                return urlConnection;
            } catch (IOException | CertificateException | NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException | KeyManagementException e) {
                e.printStackTrace();
                try {
                    HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                    httpURLConnection.setRequestProperty("Accept-Encoding", "identity");
                    return httpURLConnection;
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[]{};
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }
    }};
}
package com.evature.evasdk.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;



public class DownloadUrl {
    private static final String TAG = "DownloadUrl";
    private static IDownloader  downloader;

    static {
        setDownloader(new DownloadUrl.DefaultDownloader());
    }

	// Iftah: I changed this to be non-static method so that a unit test can inject a mock class and verify URL without trouble
	//        but added @Singleton - which should make Guice reuse a single instance of this class for each instantiation
	public static String get(String myurl) throws IOException {
        int retries = 0;
        while (true) {
            try {
                return downloader.get(myurl);
            } catch (IOException e) {
                DLog.w(TAG, "IOException in request to URL: " + myurl);
                DLog.w(TAG, "Exception Message: " + e.getMessage());
                retries++;

                if (retries >= 3) {
                    throw e;
                }
                // else try again - but first wait 10ms to give OS time to free resources, maybe it will help
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    interface IDownloader {
        String get(String url) throws IOException;
    }

    public static void setDownloader(IDownloader downloader) {
        DownloadUrl.downloader = downloader;
    }


    static class DefaultDownloader implements DownloadUrl.IDownloader {
        private static final int CONNECT_TIMEOUT = 2500;
        private static final int READ_TIMEOUT = 3000;


        private StringBuilder inputStreamToString(InputStream is) throws UnsupportedEncodingException {
            String line = "";
            StringBuilder total = new StringBuilder();

            // Wrap a BufferedReader around the InputStream
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, "UTF-8"));

            // Read response until the end
            try {
                while ((line = rd.readLine()) != null) {
                    total.append(line);
                }
            } catch (IOException e) {
                DLog.e(TAG, "IOError reading inputStream", e);
            }

            // Return full string
            return total;
        }

        // Given a URL, establishes an HttpUrlConnection and retrieves
        // the web page content as a InputStream, which it returns as a string.
        public String get(String myurl) throws IOException {
            URL url = new URL(myurl);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(CONNECT_TIMEOUT);
            urlConnection.setReadTimeout(READ_TIMEOUT);
            urlConnection.setUseCaches(false);
            try {
                if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    return inputStreamToString(urlConnection.getInputStream()).toString();
                }
                else {
                    String error = inputStreamToString(urlConnection.getErrorStream()).toString();
                    DLog.w(TAG, "Error from server: "+error);
                    return error;
                }

            } finally {
                urlConnection.disconnect();
            }
        }
/*

            HttpGet request = new HttpGet(myurl);
            request.addHeader("Accept-Encoding", "gzip");

            HttpParams params = new BasicHttpParams();
            HttpProtocolParams.setContentCharset(params, "UTF-8");
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setUseExpectContinue(params, false);
            HttpConnectionParams.setConnectionTimeout(params, 2500); // wait X seconds to establish connection
            HttpConnectionParams.setSoTimeout(params, 3000); // wait X seconds to get first byte in response

            // Initialize the HTTP client
            HttpClient httpclient = new DefaultHttpClient(params);

            if (myurl.toLowerCase().startsWith("https:")) {
                SSLSocketFactory sf = null;
                try {
                    sf = new EvatureSSLSocketFactory(null);
                } catch (UnrecoverableKeyException e) {
                    DLog.e(TAG, "UnrecoverableKeyException", e);
                } catch (KeyStoreException e) {
                    DLog.e(TAG, "KeyStoreException", e);
                } catch (KeyManagementException e) {
                    DLog.e(TAG, "KeyManagementException", e);
                } catch (NoSuchAlgorithmException e) {
                    DLog.e(TAG, "NoSuchAlgorithmException", e);
                }
                sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
                Scheme sch = new Scheme("https", sf, 443);
                httpclient.getConnectionManager().getSchemeRegistry().register(sch);
            }


            HttpResponse response = httpclient.execute(request);

            HttpEntity entity = response.getEntity();
            InputStream inputStream = entity.getContent();
            Header contentEncoding = response.getFirstHeader("Content-Encoding");
            if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
                inputStream = new GZIPInputStream(inputStream);
            }

        }*/
    }
}

/*
 	Wraps a voice streamer in a http post request to Eva server
 */
package com.evature.evasdk.evaapis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

//import org.apache.http.NameValuePair;
//import org.apache.http.client.utils.URIUtils;
//import org.apache.http.client.utils.URLEncodedUtils;
//import org.apache.http.message.BasicNameValuePair;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;

import com.evature.evasdk.util.DLog;


/****
 * Send queue of encoded audio buffers to an http connection 
 * @author iftah
 */
@SuppressLint("DefaultLocale")
public class EvaVoiceClient {

	private static final int CONNECT_TIMEOUT = 5000;
	private static final int READ_TIMEOUT = 6000;

	private static final String TAG = "EvaVoiceClient";

	private String LANGUAGE = "ENUS";
	private String CODEC = "audio/x-flac;rate=16000";//"audio/x-speex;rate=16000";	//MP3
	private String RESULTS_FORMAT = "text/plain";


	private static short PORT = (short) 443;

	private String mEvaResponse;

	private boolean mInTransaction = false;
//	HttpPost mHttpPost = null;
	
	private final EvaComponent mEva;

	// debug time measurements
	public long timeSpentReadingResponse;
	public long timeSpentUploading;
	public long timeWaitingForServer;

	OutputStream mUploadStream;
	
	boolean hadError;
	private final boolean editLastUtterance;

	private Context mContext;

	private HttpURLConnection mConnection;

	private LinkedBlockingQueue<byte[]> mSpeechBufferQueue;


	private final static int MAX_WAIT_FOR_BUFFER = 5; // wait max seconds to get data from encoder 
	
	/****
	 * 
	 * @param context - android context
	 */
	public EvaVoiceClient(Context context, final EvaComponent eva,
			final LinkedBlockingQueue<byte[]> queue, final boolean editLastUtterance) {
		mEva = eva;
		mContext = context;
		mSpeechBufferQueue = queue;	
		this.editLastUtterance = editLastUtterance; 
	}

//	private static HttpClient getHttpClient() throws NoSuchAlgorithmException, KeyManagementException
//	{
//		HttpParams params = new BasicHttpParams();
//		HttpProtocolParams.setContentCharset(params, "UTF-8");
//		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
//		HttpProtocolParams.setUseExpectContinue(params, false);
//		HttpConnectionParams.setConnectionTimeout(params, CONNECT_TIMEOUT); 
//		HttpConnectionParams.setSoTimeout(params, READ_TIMEOUT);
//
//		// Initialize the HTTP client
//		HttpClient httpclient = new DefaultHttpClient(params);
//
//		SSLSocketFactory sf=null;
//		try {
//			sf = new EvatureSSLSocketFactory(null);
//		} catch (UnrecoverableKeyException e) {
//			DLog.e(TAG, "UnrecoverableKeyException", e);
//		} catch (KeyStoreException e) {
//			DLog.e(TAG, "KeyStoreException", e);
//		}
//		sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
//		Scheme sch = new Scheme("https", sf, PORT);
//		httpclient.getConnectionManager().getSchemeRegistry().register(sch);
//
//		return httpclient;
//	}


	private URL getURL() throws Exception
	{


		EvaComponent.EvaConfig config = mEva.mConfig;

        String evatureUrl = config.vproxyHost;

        evatureUrl += "/"+mEva.mConfig.apiVersion+"?";
        evatureUrl += ("site_code=" + mEva.getSiteCode());
        evatureUrl += ("&api_key=" + mEva.getApiKey());
        //evatureUrl += ("&language=" + mLanguage);
        evatureUrl += ("&session_id=" + mEva.getSessionId());
        evatureUrl += ("&sdk_version="+EvaComponent.SDK_VERSION);
        evatureUrl += "&ffi_chains=true&ffi_statement=true";
        String language = mEva.getPreferedLanguage();
        if (language != null && !"".equals(language)) {
            evatureUrl += "&language="+language.replaceAll("-.*$", "");
        }
        if (mEva.getLocale() != null) {
            evatureUrl += ("&locale="+ mEva.getLocale());
        }
        else {
            Locale currentLocale = Locale.getDefault();
            evatureUrl += "&locale="+ currentLocale.getCountry();
        }


        if (mEva.getDeviceId() != null) {
            evatureUrl += "&uid="+mEva.getDeviceId();
        }
        if (mEva.getContext() != null) {
            evatureUrl += "&context="+mEva.getContext();
        }
        if (mEva.getScope() != null) {
            evatureUrl += "&scope="+mEva.getScope();
        }
        if (mEva.mConfig.appVersion != null) {
            evatureUrl += "&app_version="+ mEva.mConfig.appVersion;
        }
        if (mEva.getAutoOpenMicrophone()) {
            evatureUrl += "&auto_open_mic=1";
        }
        if (mEva.semanticHighlightingEnabled()) {
            evatureUrl += "&add_text=1"; // ask Eva to reply with Semantic highlighting meta data
        }


        try {
            Location location = mEva.getLocation();
            if (location != null) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                if (latitude != EvaLocationUpdater.NO_LOCATION) {
                    evatureUrl += ("&longitude=" + longitude + "&latitude=" + latitude);
                }
            }
        } catch (Exception e1) {
            DLog.e(TAG, "Exception setting location", e1);
        }


        try {
            evatureUrl += "&time_zone="+ URLEncoder.encode(("" + TimeZone.getDefault().getRawOffset() / 3600000.0).replaceFirst("\\.0+$", ""), "UTF-8");
            evatureUrl += "&android_ver="+URLEncoder.encode(String.valueOf(android.os.Build.VERSION.RELEASE), "UTF-8");
            evatureUrl += "&device="+URLEncoder.encode(android.os.Build.MODEL, "UTF-8");

            HashMap<String, String> extraParams = mEva.getExtraParams();
            for (String key : extraParams.keySet()) {
                String val = extraParams.get(key);
                if (val != null)
                    evatureUrl += "&"+key+"="+URLEncoder.encode(val, "UTF-8");
            }
        } catch (UnsupportedEncodingException e) {
            DLog.e(TAG, "UnsupportedEncodingException", e);
        }


		
		if (config.vrService != null && !"none".equals(config.vrService)) {
            evatureUrl += "&vr_service="+ URLEncoder.encode(config.vrService, "UTF-8");
		}


        if (editLastUtterance) {
            evatureUrl += "&edit_last_utterance=true";
        }

		String host = config.vproxyHost.toLowerCase();
		if (host.startsWith("http") == false) {
            evatureUrl = "https://"+evatureUrl;
		}
        URI uri = new URI(evatureUrl);
		return uri.toURL();
	}

	/*
	 * This is a simpler helper function to setup the Header parameters
	 */
	private HttpURLConnection getConnection(URL url) throws IOException
	{
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		try {
			conn.setRequestMethod("POST");
		} catch (ProtocolException e) {
			e.printStackTrace();
		}
		conn.setRequestProperty("Content-Type", CODEC);
		conn.setRequestProperty("Content-Language", LANGUAGE);
		conn.setRequestProperty("Accept-Language", LANGUAGE);
		conn.setRequestProperty("Accept", RESULTS_FORMAT);
//		conn.setRequestProperty("Accept-Encoding","gzip");
//		conn.setRequestProperty("Transfer-Encoding","chunked");

		conn.setReadTimeout(READ_TIMEOUT);
		conn.setConnectTimeout(CONNECT_TIMEOUT);
		conn.setDoOutput(true);
		conn.setDoInput(true);
		conn.setChunkedStreamingMode(512);
		conn.setUseCaches(false);
		return conn;
	}

	/***
	 * Start the recording and return it as 
	 * @return
	 * @throws NumberFormatException
	 * @throws Exception
	 */
	private void setAudioContent(HttpURLConnection connection) throws Exception
	{
		String filepath = null;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		boolean saveEncoded = prefs.getBoolean("eva_save_encoded", false);
		OutputStream uploadStream = connection.getOutputStream();
		if (saveEncoded) {
			filepath = Environment.getExternalStorageDirectory().getPath() + "/recording.flac";
			mUploadStream = new DebugOutputStream(uploadStream, saveEncoded, filepath);
		}
		else {
			mUploadStream = uploadStream;
		}
	}


	private StringBuilder inputStreamToString(InputStream is) throws UnsupportedEncodingException {
		String line = "";
		StringBuilder total = new StringBuilder();

		// Wrap a BufferedReader around the InputStream
		BufferedReader rd = new BufferedReader(new InputStreamReader(is, "UTF-8"));

		// Read response until the end
		try {
			while ((line = rd.readLine()) != null && mInTransaction) { 
				total.append(line); 
			}
		} catch (IOException e) {
			DLog.e(TAG, "IOError reading inputStream", e);
		}

		// Return full string
		return total;
	}

	private void processResponse(HttpURLConnection connection) throws IllegalStateException, IOException
	{
		DLog.i(TAG,"<<< Getting response");
		long t0 = System.nanoTime();

//		InputStream is = resEntity.getContent();
//		Header contentEncoding = response.getFirstHeader("Content-Encoding");
//		if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
//			Log.d(TAG, "gzip encoded vproxy result");
//			is = new GZIPInputStream(is);
//		}
//
		DLog.d(TAG, "status: "+connection.getResponseCode()+"  "+connection.getResponseMessage());
//		for ( Map.Entry<String,List<String>> header : connection.getHeaderFields().entrySet()) {
//		    System.out.println(header.getKey() + "=" + header.getValue());
//		}

		if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) { 
			mEvaResponse = inputStreamToString(connection.getInputStream()).toString();
		}
		else {
			mEvaResponse = null;
			DLog.w(TAG, "Error from server: "+inputStreamToString(connection.getErrorStream()).toString());
		}

		
//		resEntity.consumeContent();
		
		timeSpentReadingResponse = (System.nanoTime() - t0) / 1000000;
		DLog.i(TAG, "<<< Got Response");//: \n"+mEvaResponse);

	}
	
	public String getEvaResponse()
	{
		return mEvaResponse;
	}

	public void startVoiceRequest() {
		mInTransaction = true;
		hadError = false;
		try {
			URL url = getURL();
			DLog.i(TAG,"<<< Sending post request to URL: "+url);
			long t0 = System.nanoTime();
			mConnection = getConnection(url);
			DLog.d(TAG, "<<< Connection opened in "+((System.nanoTime() - t0)/1000)+"ms");
			t0 = System.nanoTime();
			setAudioContent(mConnection);
			// wait until write is complete
			while(true) {
				byte[] buffer = mSpeechBufferQueue.poll(MAX_WAIT_FOR_BUFFER, TimeUnit.SECONDS);
				if (buffer == null) {
					DLog.w(TAG, "Waited for "+MAX_WAIT_FOR_BUFFER+" seconds for data from encoder");
					break;
				}
				if (buffer.length == 0) {
					// end of encoded stream
					break;
				}
				if (mInTransaction) { // if not canceled already
					mUploadStream.write(buffer);
				}
			}
			
			long t1 = System.nanoTime();
			timeSpentUploading = (t1 - t0) / 1000000;
			if (mInTransaction) {
				t0 = System.nanoTime();
				processResponse(mConnection);
			}
		}
		catch (IOException e) {
			if ("Connection already shutdown".equals(e.getMessage())) {
				DLog.w(TAG, "Connection already shutdown");
			}
			if ("Request aborted".equals(e.getMessage())) {
				DLog.i(TAG, "Request aborted");
			}
			else {
				DLog.e(TAG, "Exception sending voice request", e);
				hadError = true;
			}
		}
		catch(Exception e)	{
			DLog.e(TAG, "Exception sending voice request", e);
			hadError = true;
		}
		finally {
			mConnection = null;
			mInTransaction = false;
		}
	}
	
	/*
	public static void sendAudioFile(Context context, EvaComponent.EvaConfig config, Uri fileuri, String filekey) {
		HttpClient httpclient = null;
		try {
			httpclient = getHttpClient();

			String host = config.vproxyHost.toLowerCase();
			if (host.startsWith("http") == false) {
				host = "https://"+host;
			}
			
			String url = host+"/save_to_s3/"+filekey;
			Log.i(TAG,"<<< Sending audio file to URI: "+url);

		    HttpPost httppost = new HttpPost(url);
		    ContentResolver contentResolver = context.getContentResolver();
		    InputStreamEntity reqEntity =  new InputStreamEntity( contentResolver.openInputStream(fileuri), -1); //new FileInputStream(file), -1);
		    reqEntity.setContentType("audio/amr");
//		    reqEntity.setChunked(true);
		    httppost.setEntity(reqEntity);
			long t0 = System.nanoTime();
			HttpResponse response = httpclient.execute(httppost);
			long t1 = System.nanoTime();
			Log.d(TAG, "Response: code="+ response.getStatusLine().getStatusCode()+ ";   Time spent uploading audio file = "+ ((t1 - t0) / 1000000));
			if( httpclient != null ) {
				httpclient.getConnectionManager().shutdown();
				httpclient = null;
			}
		}
		catch (IOException e) {
			if (e.getMessage().equals("Connection already shutdown")) {
				Log.i(TAG, "Connection already shutdown");
			}
			else {
				Log.e(TAG, "Exception sending audio file", e);
			}
		}
		catch(Exception e)	{
			Log.e(TAG, "Exception sending audio file", e);
		}
		finally {
			// When HttpClient instance is no longer needed, 
			// shut down the connection manager to ensure
			// immediate deallocation of all system resources
			if( httpclient != null ) 
				httpclient.getConnectionManager().shutdown();
		}
	}
	*/

	public void cancelTransfer()
	{
		mInTransaction=false;
	}

	public boolean getInTransaction()
	{
		return mInTransaction;
	}


}

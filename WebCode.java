package com.sukohi.lib;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

/**
 * ver 4.11 (15 Apr, 2014)
 * @author Sukohi Kuhoh
 */
public class WebCode extends AsyncTask<String, Void, String> {

	private final String MODE_GET = "get";
	private final String MODE_POST = "post";
	private String code;
	private String encoding = "UTF-8";
	private int statusCode;
	private boolean responseResult = false;
	private boolean imageFlag = false;
	private byte[] byteArrayCode;
	private List<NameValuePair> postParams;
	private HttpEntity entityCode;
	private BitmapFactory.Options bitmapOptions = null;
	private WebCodeCallback callback;
	
	public WebCode() {}
	
	public void setEncoding(String encoding) {
	
		this.encoding = encoding;
	
	}
	
	public void setImageFlag(Boolean bool) {
		
		imageFlag = bool;
		
	}
	
	public void setBitmapOptions(BitmapFactory.Options options) {
		
		bitmapOptions = options;
		
	}
	
	public void get(String url) {
		
		execute(url, MODE_GET);
		
	}
	
	public void post(String url, Map<String, String> paramsMap) {
		
		postParams = new ArrayList<NameValuePair>();
		
		for (String key : paramsMap.keySet()) {

			postParams.add(new BasicNameValuePair(key, paramsMap.get(key)));
			
		}
		
		execute(url, MODE_POST);
		
	}

	@Override
	protected String doInBackground(String... params) {

		String url = params[0];
		String mode = params[1];
		HttpClient client = new DefaultHttpClient();
		
		code = "";
		statusCode = 0;

		try {
			
			HttpResponse response;
			
			if(mode.equals(MODE_GET)) {
				
				HttpGet httpGet = new HttpGet(url);
				response = client.execute(httpGet);
				
			} else {
				
				HttpPost httpPost = new HttpPost(url);
				httpPost.setEntity(new UrlEncodedFormEntity(postParams, encoding));
				response = client.execute(httpPost);
				
			}
			
			StatusLine statusLine = response.getStatusLine();
			statusCode = statusLine.getStatusCode();
			
			if(statusCode == HttpURLConnection.HTTP_OK){
			
				entityCode = response.getEntity();
				byteArrayCode = EntityUtils.toByteArray(entityCode);
				code = new String(byteArrayCode, encoding);
				responseResult = true;
				
			}
			
		} catch(Exception e){}
		
		return null;
		
	}
	
	@Override
	protected void onPostExecute(String result) {
		
		WebCodeResult webCodeResult = new WebCodeResult();
		webCodeResult.setResult(responseResult);
		webCodeResult.setCode(code);
		webCodeResult.setStatusCode(statusCode);
		webCodeResult.setByteArrayCode(byteArrayCode);
		
		if(imageFlag) {
			
			Bitmap bitmap = null;
			
			if(byteArrayCode != null) {

				if(bitmapOptions != null) {
					
					BitmapFactory.decodeByteArray(byteArrayCode, 0, byteArrayCode.length, bitmapOptions); 
					
				} else {
					
					bitmap = BitmapFactory.decodeByteArray(byteArrayCode, 0, byteArrayCode.length);
					
				}
				
			}
			 
			webCodeResult.setBitmapCode(bitmap);
			
		}
		
		if(callback != null) {
			
			callback.getResult(webCodeResult);
			
		}

	}
	
	public void setCallback(WebCodeCallback webCodeCallback) {
		
		callback = webCodeCallback;
		
	}
	
	public static class WebCodeCallback {
		
		public void getResult(WebCodeResult result) {}
		
	}

	public class WebCodeResult {
		
		private Boolean responseResult;
		private String code;
		private int statusCode;
		private byte[] byteArrayCode;
		private Bitmap bitmap;
		
		public void setResult(Boolean result) {
			
			responseResult = result;
			
		}
		
		public Boolean getResult() {
			
			return responseResult;
			
		}
		
		public void setCode(String resultCode) {
			
			code = resultCode; 
			
		}
		
		public String getCode() {
			
			return code;
			
		}
		
		public void setStatusCode(int resultStatusCode) {
			
			statusCode = resultStatusCode;
			
		}
		
		public int getStatusCode() {
			
			return statusCode;
			
		}
		
		public void setByteArrayCode(byte[] resultByteArray) {
			
			byteArrayCode = resultByteArray;
			
		}
		
		public byte[] getByteArrayCode() {
			
			return byteArrayCode;
			
		}
		
		public void setBitmapCode(Bitmap resultBitmap) {
			
			bitmap = resultBitmap;
			
		}
		
		public Bitmap getBitmapCode() {
			
			return bitmap;
			
		}
		
	}
	
}
/*** Example

	WebCode webCode = new WebCode();
	
	webCode.setEncoding("UTF-8");		// Skippable (Default: UTF-8)
	webCode.setImageFlag(true);			// Required only for getBitmapCode()
	webCode.setBitmapOptions(options);	// Skippable
	
	webCode.setCallback(new WebCodeCallback(){
		
		@Override
		public void getResult(WebCodeResult webCodeResult) {
			
			if(webCodeResult.getResult()) {
				
				int statusCode = webCodeResult.getStatusCode();
				String code = webCodeResult.getCode();
				byte[] byteArrayCode = webCodeResult.getByteArrayCode();
				Bitmap bitmap = webCodeResult.getBitmapCode();
				
			}
			
		}
		
	});
	webCode.get("http://example.com/");
	
	// or
	
	webCode.post("http://example.com/", paramsMap);
	
***/

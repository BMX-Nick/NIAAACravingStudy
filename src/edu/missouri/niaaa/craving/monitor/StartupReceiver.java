package edu.missouri.niaaa.craving.monitor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import edu.missouri.niaaa.craving.Utilities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

public class StartupReceiver extends BroadcastReceiver {
	private static final String TAG = "Startup Receiver";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "received");
		
		if(MonitorUtilities.ID == null){
			MonitorUtilities.ID = Utilities.getSP(context, Utilities.SP_LOGIN).getString(Utilities.SP_KEY_LOGIN_USERID, "0000");
			Log.d(TAG, "MonitorUtilities.ID was null. Now it is: "+MonitorUtilities.ID);
		}
		
		// Recording
		String fileName = MonitorUtilities.RECORDING_CATEGORY + "." + MonitorUtilities.ID + "." + MonitorUtilities.getFileDate();
		Log.d(TAG, "filename: "+fileName);
		String toWrite = MonitorUtilities.getCurrentTimeStamp() + MonitorUtilities.LINEBREAK + "Device was TURNED ON by user! And just finished starting up."
				+ MonitorUtilities.LINEBREAK + MonitorUtilities.SPLIT;
		
		try {
			Utilities.writeToFile(fileName + ".txt", toWrite);
			Log.d(TAG, "write to file");
		} catch (IOException e) {
			Log.d(TAG, "not write to file!!");
			e.printStackTrace();	
		}
		String fileHead = getFileHead(fileName);
		// Log.d("RecordingReceiver", fileHead);
		String toSend = fileHead + toWrite;
		String enformattedData = null;
		try {
			enformattedData = Utilities.monitorEncryption(toSend, context);
		} catch (Exception e) {
			Log.d(TAG, "utilities monitorEncryption failed!!");
			e.printStackTrace();
		}

		TransmitData transmitData = new TransmitData();
		if (MonitorUtilities.checkNetwork(context)) {
			transmitData.execute(enformattedData);
			/*try {
				boolean result = transmitData.execute(enformattedData).get();
				Log.d(TAG, "send to server: "+result);
			} catch (InterruptedException e) {
				Log.d(TAG, "send to server interrupted exception");
				e.printStackTrace();
			} catch (ExecutionException e) {
				Log.d(TAG, "send to server interrupted exception");
				e.printStackTrace();
			}*/
		}	
	}
	
// Recording
	private String getFileHead(String fileName) {
		StringBuilder prefix_sb = new StringBuilder(Utilities.PREFIX_LEN);
		prefix_sb.append(fileName);

		for (int i = fileName.length(); i <= Utilities.PREFIX_LEN; i++) {
			prefix_sb.append(" ");
		}
		return prefix_sb.toString();
	}

	private class TransmitData extends AsyncTask<String, Void, Boolean> {
		@Override
		protected Boolean doInBackground(String... strings) {
			String data = strings[0];
			// String fileName = strings[0];
			// String dataToSend = strings[1];

			HttpPost request = new HttpPost(Utilities.UPLOAD_ADDRESS);
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("data", data));
			// // file_name
			// params.add(new BasicNameValuePair("file_name", fileName));
			// // data
			// params.add(new BasicNameValuePair("data", dataToSend));
			try {
				request.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
				HttpResponse response = new DefaultHttpClient().execute(request);
				Log.d("Sensor Data Point Info", String.valueOf(response.getStatusLine().getStatusCode()));
				if(response.getStatusLine().getStatusCode() == 200){
					Log.d(TAG, "send to server: true");	
				}
				return true;
			} catch (Exception e){
				Log.d(TAG, "not send to server!!");
				e.printStackTrace();
				return false;
			}
		}
	}
}

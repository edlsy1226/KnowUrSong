package com.acrcloud.rec.demo;

import java.io.File;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.acrcloud.rec.sdk.ACRCloudConfig;
import com.acrcloud.rec.sdk.ACRCloudClient;
import com.acrcloud.rec.sdk.IACRCloudListener;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements IACRCloudListener {
    //NOTE: You can also implement IACRCloudResultWithAudioListener, replace "onResult(String result)" with "onResult(ACRCloudResult result)"

	private ACRCloudClient mClient;
	private ACRCloudConfig mConfig;
	
	private TextView mVolume, mResult, tv_time;
	
	private boolean mProcessing = false;
	private boolean initState = false;
	
	private String path = "";

	private long startTime = 0;
	private long stopTime = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		if (Build.VERSION.SDK_INT < 16) {
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
		setContentView(R.layout.activity_main);
		
		path = Environment.getExternalStorageDirectory().toString()
				+ "/acrcloud/model";
		
		File file = new File(path);
		if(!file.exists()){
			file.mkdirs();
		}		
			
		mVolume = (TextView) findViewById(R.id.volume);
		mResult = (TextView) findViewById(R.id.result);
		tv_time = (TextView) findViewById(R.id.time);
		
		Button startBtn = (Button) findViewById(R.id.start);
		startBtn.setText(getResources().getString(R.string.start));

		Button stopBtn = (Button) findViewById(R.id.stop);
		stopBtn.setText(getResources().getString(R.string.stop));

		findViewById(R.id.stop).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						stop();
					}
				});
		
		Button cancelBtn = (Button) findViewById(R.id.cancel);
		cancelBtn.setText(getResources().getString(R.string.cancel));
		
		findViewById(R.id.start).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				start();
			}
		});
		
		findViewById(R.id.cancel).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						cancel();
					}
				});


        this.mConfig = new ACRCloudConfig();
        this.mConfig.acrcloudListener = this;
        
        // If you implement IACRCloudResultWithAudioListener and override "onResult(ACRCloudResult result)", you can get the Audio data.
        //this.mConfig.acrcloudResultWithAudioListener = this;
        
        this.mConfig.context = this;
        this.mConfig.host = "identify-us-west-2.acrcloud.com";
        this.mConfig.dbPath = path; // offline db path, you can change it with other path which this app can access.
        this.mConfig.accessKey = "8eaa2f015bf5bcca44d642067c59cc17";
        this.mConfig.accessSecret = "f54kWgM1N8wRz8rtk9Zzoln0Uk82dXmIsPGYY55X";
        this.mConfig.protocol = ACRCloudConfig.ACRCloudNetworkProtocol.PROTOCOL_HTTP; // PROTOCOL_HTTPS
        this.mConfig.reqMode = ACRCloudConfig.ACRCloudRecMode.REC_MODE_REMOTE;
        //this.mConfig.reqMode = ACRCloudConfig.ACRCloudRecMode.REC_MODE_LOCAL;
        //this.mConfig.reqMode = ACRCloudConfig.ACRCloudRecMode.REC_MODE_BOTH;

        this.mClient = new ACRCloudClient();
        // If reqMode is REC_MODE_LOCAL or REC_MODE_BOTH,
        // the function initWithConfig is used to load offline db, and it may cost long time.
        this.initState = this.mClient.initWithConfig(this.mConfig);
        if (this.initState) {
            this.mClient.startPreRecord(3000); //start prerecord, you can call "this.mClient.stopPreRecord()" to stop prerecord.
        }
	}

	
	public void start() {
        if (!this.initState) {
            Toast.makeText(this, "init error", Toast.LENGTH_SHORT).show();
            return;
        }
		Toast.makeText(this, "Checking!", Toast.LENGTH_SHORT).show();
		if (!mProcessing) {
			mProcessing = true;
			mVolume.setText("");
			mResult.setText("");
			if (this.mClient == null || !this.mClient.startRecognize()) {
				mProcessing = false;
				mResult.setText("start error!");
			}
            startTime = System.currentTimeMillis();
		}
	}

	protected void stop() {
		if (mProcessing && this.mClient != null) {
			this.mClient.stopRecordToRecognize();
		}
		mProcessing = false;
		stopTime = System.currentTimeMillis();
	}
	
	protected void cancel() {
		if (mProcessing && this.mClient != null) {
			mProcessing = false;
			this.mClient.cancel();
			tv_time.setText("");
			mResult.setText("");
		} 		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void watchYoutubeVideo(String id) {
		Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + id));
		Intent webIntent = new Intent(Intent.ACTION_VIEW,
				Uri.parse("http://www.youtube.com/watch?v=" + id));
		try {
			startActivity(appIntent);
		} catch (ActivityNotFoundException ex) {
			startActivity(webIntent);
		}
	}

    // Old api
	@Override
	public void onResult(String result) {	
		if (this.mClient != null) {
			this.mClient.cancel();
			mProcessing = false;
		} 
		
		String tres = "\n";
		
		try {
		    JSONObject j = new JSONObject(result);
		    JSONObject j1 = j.getJSONObject("status");
		    int j2 = j1.getInt("code");
		    if(j2 == 0){
		    	JSONObject metadata = j.getJSONObject("metadata");
		    	//
		    	if (metadata.has("humming")) {
		    		JSONArray hummings = metadata.getJSONArray("humming");
		    		for(int i=0; i<hummings.length(); i++) {
		    			JSONObject tt = (JSONObject) hummings.get(i);
		    			String title = tt.getString("title");
		    			JSONArray artistt = tt.getJSONArray("artists");
		    			JSONObject art = (JSONObject) artistt.get(0);
		    			String artist = art.getString("name");
		    			tres = title + "\n";
		    		}
		    	}
		    	if (metadata.has("music")) {
		    		JSONArray musics = metadata.getJSONArray("music");
		    		for(int i=0; i<musics.length(); i++) {
		    			JSONObject tt = (JSONObject) musics.get(i); 
		    			String title = tt.getString("title");
		    			JSONArray artistt = tt.getJSONArray("artists");
		    			JSONObject art = (JSONObject) artistt.get(0);
		    			String artist = art.getString("name");
		    			if (artist.equals("Rick Astley")) {
		    				watchYoutubeVideo("dQw4w9WgXcQ");
						}
		    			tres = "\n" +"Ur Song: " + title +"\n\n"+  "Ur Singer: " + artist;
		    		}
		    	}
		    	if (metadata.has("streams")) {
		    		JSONArray musics = metadata.getJSONArray("streams");
		    		for(int i=0; i<musics.length(); i++) {
		    			JSONObject tt = (JSONObject) musics.get(i);
		    			String title = tt.getString("title");
		    			String channelId = tt.getString("channel_id");
		    			tres = "Title: " + title + "    Channel Id: " + channelId + "\n";
		    		}
		    	}
		    	if (metadata.has("custom_files")) {
		    		JSONArray musics = metadata.getJSONArray("custom_files");
		    		for(int i=0; i<musics.length(); i++) {
		    			JSONObject tt = (JSONObject) musics.get(i);
		    			String title = tt.getString("title");
		    			tres = "Title: " + "\n" + title + "\n";
		    		}
		    	}
		    	tres = tres;
				Toast.makeText(this, "Congrats! U Know Ur Song!", Toast.LENGTH_SHORT).show();
		    }else{
		    	tres = "Can't find Ur song :-(";
		    }
		} catch (JSONException e) {
			tres = result;
		    e.printStackTrace();
		}
		mResult.setText(tres);	
	}

	@Override
	public void onVolumeChanged(double volume) {
		long time = (System.currentTimeMillis() - startTime);
		mVolume.setText("");
	}
	
	@Override  
    protected void onDestroy() {  
        super.onDestroy();  
        Log.e("MainActivity", "release");
        if (this.mClient != null) {
        	this.mClient.release();
        	this.initState = false;
        	this.mClient = null;
        }
    } 
}

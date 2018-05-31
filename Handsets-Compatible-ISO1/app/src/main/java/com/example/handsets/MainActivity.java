package com.example.handsets;

import com.fgtit.data.Conversions;
import com.fgtit.device.Constants;
import com.fgtit.device.FPModule;
import com.fgtit.fpcore.FPMatch;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.os.SystemClock;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Build;

public class MainActivity extends ActionBarActivity {

	private FPModule fpm=new FPModule();
    
    private byte bmpdata[]=new byte[Constants.RESBMP_SIZE];
    private int bmpsize=0;
    
    private byte refdata[]=new byte[Constants.TEMPLATESIZE];
    private int refsize=0;    
    private byte matdata[]=new byte[Constants.TEMPLATESIZE];
    private int matsize=0;
    
    private String refstring,matstring;
    
    private int worktype=0;
    
	private TextView	tvDevStatu,tvFpStatu,tvFpData;
	private ImageView 	ivFpImage=null;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().detectNetwork().penaltyLog().build());
		
        initView();
        tvDevStatu.setText(String.valueOf(fpm.getDeviceType()));
      
        fpm.InitMatch();
        fpm.SetContextHandler(this, mHandler);
        fpm.SetTimeOut(Constants.TIMEOUT_LONG);
        fpm.SetLastCheckLift(true);
    }
    
    private Handler mHandler = new Handler(){
        @Override
    	public void handleMessage(Message msg){
    		switch (msg.what){
    			case Constants.FPM_DEVICE:
    				switch(msg.arg1){
    				case Constants.DEV_OK:
    					tvFpStatu.setText("Open Device OK");
    					break;
    				case Constants.DEV_FAIL:
    					tvFpStatu.setText("Open Device Fail");
    					break;
    				case Constants.DEV_ATTACHED:
    					tvFpStatu.setText("USB Device Attached");
    					break;
    				case Constants.DEV_DETACHED:
    					tvFpStatu.setText("USB Device Detached");
    					break;
    				case Constants.DEV_CLOSE:
    					tvFpStatu.setText("Device Close");
    					break;
    				}
    				break;
    			case Constants.FPM_PLACE:
    				tvFpStatu.setText("Place Finger");
    				break;
    			case Constants.FPM_LIFT:
    				tvFpStatu.setText("Lift Finger");
    				break;
           	 	case Constants.FPM_GENCHAR:{
       	 			if(msg.arg1==1){
       	 				if(worktype==0){
       	 					tvFpStatu.setText("Generate Template OK");
       	 					matsize=fpm.GetTemplateByGen(matdata);       	 	
       	 					
       	 					byte[] tmpstd=new byte[512];
       	 					byte[] tmpiso=new byte[512];
       	 					Conversions.getInstance().StdChangeCoord(matdata, Constants.TEMPLATESIZE, tmpstd, 1);
   	 						Conversions.getInstance().StdToIso(2, tmpstd, tmpiso);
   	 						matstring=Base64.encodeToString(tmpiso,0,378,Base64.DEFAULT);
   	 						tvFpData.setText(matstring);
       	 					
   	 						if(MatchIsoTemplateStr(refstring,matstring,80))
       	 						tvFpStatu.setText(String.format("Match OK"));
       	 					else
       	 						tvFpStatu.setText(String.format("Match Fail"));
   	 						
       	 				}else{
       	 					tvFpStatu.setText("Enrol Template OK");
       	 					refsize=fpm.GetTemplateByGen(refdata);
       	 					
       	 					byte[] tmpstd=new byte[512];
       	 					byte[] tmpiso=new byte[512];
       	 					Conversions.getInstance().StdChangeCoord(refdata, Constants.TEMPLATESIZE, tmpstd, 1);
       	 					Conversions.getInstance().StdToIso(2, tmpstd, tmpiso);
       	 					refstring=Base64.encodeToString(tmpiso,0,378,Base64.DEFAULT);
       	 					tvFpData.setText(refstring);
       	 				}
       	 			}else{
       	 				tvFpStatu.setText("Generate Template Fail");
       	 			}
       	 			}
       	 			break;
           	 	case Constants.FPM_NEWIMAGE:{
           	 		bmpsize=fpm.GetBmpImage(bmpdata);
       	 			Bitmap bm1=BitmapFactory.decodeByteArray(bmpdata, 0, bmpsize);
       	 			ivFpImage.setImageBitmap(bm1);
       	 			}
       	 			break; 
           	 	case Constants.FPM_TIMEOUT:
           	 		tvFpStatu.setText("Time Out");
           	 		break;
    		}
        }  
    };
    
	public boolean MatchIsoTemplateByte(byte[] piFeatureA, byte[] piFeatureB,int score){		
		int at=Conversions.getInstance().GetDataType(piFeatureA);
		int bt=Conversions.getInstance().GetDataType(piFeatureB);
		if((at==1)&&(bt==1)){
			int sc=FPMatch.getInstance().MatchTemplate(piFeatureA,piFeatureB);
			if(sc>score)
				return true;
			else
				return false;
		}else{
			byte adat[]=new byte[512];
			byte bdat[]=new byte[512];	
			Conversions.getInstance().IsoToStd(2,piFeatureA,adat);
			Conversions.getInstance().IsoToStd(2,piFeatureB,bdat);			
			for(int i=0;i<4;i++){
				byte tmpdat[]=new byte[512];
				Conversions.getInstance().StdChangeCoord(bdat, 256, tmpdat,i);
				int sc=FPMatch.getInstance().MatchTemplate(adat,tmpdat);
				if(sc>=score)
					return true;
			}
		}
		return false;
	}
	
	public boolean MatchIsoTemplateStr(String strFeatureA, String strFeatureB,int score){
		byte piFeatureA[]=Base64.decode(strFeatureA, Base64.DEFAULT);
		byte piFeatureB[]=Base64.decode(strFeatureB, Base64.DEFAULT);
		int at=Conversions.getInstance().GetDataType(piFeatureA);
		int bt=Conversions.getInstance().GetDataType(piFeatureB);
		if((at==1)&&(bt==1)){
			int sc=FPMatch.getInstance().MatchTemplate(piFeatureA,piFeatureB);
			if(sc>score)
				return true;
			else
				return false;
		}else{
			byte adat[]=new byte[512];
			byte bdat[]=new byte[512];	
			Conversions.getInstance().IsoToStd(2,piFeatureA,adat);
			Conversions.getInstance().IsoToStd(2,piFeatureB,bdat);			
			for(int i=0;i<4;i++){
				byte tmpdat[]=new byte[512];
				Conversions.getInstance().StdChangeCoord(bdat, 256, tmpdat,i);
				int sc=FPMatch.getInstance().MatchTemplate(adat,tmpdat);
				if(sc>=score)
					return true;
			}
		}
		return false;
	}
	
	@Override
	protected void onDestroy(){
		super.onDestroy();
	}
	
    @Override
	protected void onResume() {
		super.onResume();		
		fpm.ResumeRegister();
		fpm.OpenDevice();
    }
    
    /*
	@Override
	protected void onPause() {		
		super.onPause();
		fpm.PauseUnRegister();
		fpm.CloseDevice();
	}
	*/

	@Override
	protected void onStop() {		
		super.onStop();
		fpm.PauseUnRegister();
		fpm.CloseDevice();
	}

	private void initView(){
		
		tvDevStatu=(TextView)findViewById(R.id.textView1);
		tvFpStatu=(TextView)findViewById(R.id.textView2);
		tvFpData=(TextView)findViewById(R.id.textView3);
		ivFpImage=(ImageView)findViewById(R.id.imageView1);
		
		final Button btn_enrol=(Button)findViewById(R.id.button1);
		final Button btn_capture=(Button)findViewById(R.id.button2);
				
		btn_enrol.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				if(fpm.GenerateTemplate(1)){
					worktype=1;
				}else{
					Toast.makeText(MainActivity.this, "Busy", Toast.LENGTH_SHORT).show();
				}
			}
		});
		
		btn_capture.setOnClickListener(new View.OnClickListener(){
			@Override			
			public void onClick(View v) {
				if(fpm.GenerateTemplate(1)){
					worktype=0;
				}else{
					Toast.makeText(MainActivity.this, "Busy", Toast.LENGTH_SHORT).show();
				}
			}
		});
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}

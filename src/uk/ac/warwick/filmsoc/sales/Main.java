package uk.ac.warwick.filmsoc.sales;

import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.CheckBox;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

/** Called when the activity is first created. */

public class Main extends Activity {
	String username;
	String password;
	
	int adsTrailersSeconds = 1200;
	int runtime = 0;
	
	int capRefreshes = 360;
	
	Calendar scrTime;
	Calendar finTime;
	
	Timer refreshTimer = new Timer();
	Timer countdownTimer = new Timer();
	
	 /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);    
	    setContentView(R.layout.main);
	    
	    Bundle extras = getIntent().getExtras();
	    username = extras.getString("username");
	    password = extras.getString("password");
	    
	    ((Button)findViewById(R.id.btnRefresh)).setOnClickListener(btnRefreshClick);
	    ((Button)findViewById(R.id.btnLogout)).setOnClickListener(btnLogoutClick);
	    ((CheckBox)findViewById(R.id.chkAutoRefresh)).setOnClickListener(chkAutoRefreshClick);
	    
	    updateFilmInfo();
	    updateInfo();	
	    updateCountdown();
	    
	    countdownTimer = new Timer();
	    countdownTimer.schedule(new RefreshCountdowns(), 500, 1000);
    }
    
    public void updateFilmInfo() {
    	String loginTryURL = "http://www.filmsoc.warwick.ac.uk/upages/csujba/othercontent/HEAD/www/html/content/android/filminfo.php?";
    	
    	loginTryURL += "username=" + username;
    	loginTryURL += "&password=" + password;
    	
    	String response = downloadString(loginTryURL);
    	String[] processedResponse = response.split("\n");
    	
    	Timestamp ts = new Timestamp(Long.parseLong(processedResponse[1]) * 1000);
    	scrTime = Calendar.getInstance();
    	scrTime.setTimeInMillis(ts.getTime());
    	
    	SimpleDateFormat sd = new SimpleDateFormat("E d MMM, HH:mm");
    	
    	((TextView)findViewById(R.id.txtFilmTitle)).setText(processedResponse[0]);
    	((TextView)findViewById(R.id.txtScreeningTime)).setText(sd.format(scrTime.getTime()));
    	
    	String cert = processedResponse[3];
    	cert = cert.toLowerCase();
    	
    	((ImageView)findViewById(R.id.imgCert)).setImageDrawable(this.getResources().getDrawable(this.getResources().getIdentifier("bbfc_" + cert, "drawable", this.getPackageName())));
    	
    	runtime = Integer.parseInt(processedResponse[2]);
    	finTime = (Calendar) scrTime.clone();
    	finTime.add(Calendar.MINUTE, runtime);
    	finTime.add(Calendar.SECOND, adsTrailersSeconds);
    	
    	sd = new SimpleDateFormat("HH:mm");
    	
    	((TextView)findViewById(R.id.txtScrEnd)).setText(sd.format(finTime.getTime()));    	
    	
    }
    
    public void updateInfo() {
    	String loginTryURL = "http://www.filmsoc.warwick.ac.uk/upages/csujba/othercontent/HEAD/www/html/content/android/attendance.php?";
    	
    	loginTryURL += "username=" + username;
    	loginTryURL += "&password=" + password;
    	
    	String response = downloadString(loginTryURL);
    	String[] processedResponse = response.split("\n");
    	
    	((TextView)findViewById(R.id.txtAttendanceTotal)).setText(processedResponse[0]);
    	
    	TicketSaleSummary[] sales = new TicketSaleSummary[((processedResponse.length - 2) / 2)];
    	int j = 0;
    	for(int i=2;i<processedResponse.length;i=i+2) {
    		sales[j] = new TicketSaleSummary(processedResponse[i],Integer.parseInt(processedResponse[i+1]));
    		j++;
    	}
    	
    	TableLayout tl = (TableLayout)findViewById(R.id.tblTickets);
    	tl.removeAllViews();
    	for(TicketSaleSummary tss : sales) {
    		// Build Table Row
    		TableRow tr = generateRow(tss.Description,String.valueOf(tss.Quantity));
    		
    		tl.addView(tr,new TableLayout.LayoutParams(
                    LayoutParams.FILL_PARENT,
                    LayoutParams.WRAP_CONTENT));
    	}
    	
    	TableRow trt = generateRow("Total: ", "£" + processedResponse[1], true);
		
		tl.addView(trt,new TableLayout.LayoutParams(
                LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT));
    	
    }    
    
    public TableRow generateRow(String t1, String t2) {
    	return generateRow(t1,t2,false);
    }
    
    public TableRow generateRow(String t1, String t2, boolean boldRow) {
    	TableRow tr = new TableRow(this);
		tr.setLayoutParams(new LayoutParams(
                LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT));
		
		TextView tv1 = new TextView(this);  
		tr.setLayoutParams(new LayoutParams(
                LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT));
		tv1.setText(t1);
		if(boldRow) tv1.setTypeface(null, Typeface.BOLD);
		tv1.setTextSize(TypedValue.COMPLEX_UNIT_DIP,15);
		tr.addView(tv1);
		
		TextView tv2 = new TextView(this);
		tv2.setText(t2);
		if(boldRow) tv2.setTypeface(null, Typeface.BOLD);
		tv2.setTextSize(TypedValue.COMPLEX_UNIT_DIP,15);
		tv2.setGravity(5);
		tr.addView(tv2);
		
		return tr;
    }
    
    public void updateCountdown() {
    	String helpText;
    	Calendar adsStart = (Calendar) scrTime.clone();
    	adsStart.add(Calendar.SECOND, adsTrailersSeconds);
    	Calendar nextEvent;
    	Calendar now = Calendar.getInstance();
    	
    	// Step One: Work out which time we are counting from
    	if(scrTime.compareTo(now) == 1) {
    		helpText = "Ads Start";
    		nextEvent = (Calendar) scrTime.clone();
    	} else if (adsStart.compareTo(now) == 1) {
    		helpText = "Film Starts";
    		nextEvent = (Calendar) adsStart.clone();    	
    	} else if (finTime.compareTo(now) == 1) {
    		helpText = "Film Ends";
    		nextEvent = (Calendar) finTime.clone();
    	} else {    		
    		helpText = "Finished ago";
    		nextEvent = (Calendar) finTime.clone();
    	}
    	
    	long difference = nextEvent.getTimeInMillis() - now.getTimeInMillis();
    	difference /= 1000;
    	if(difference < 0) difference *= -1;
    	
    	DecimalFormat df = new DecimalFormat("#");
    	
    	((TextView)findViewById(R.id.txtCountdownHelp)).setText(helpText);
    	
    	double secondsLeft = (difference - (Math.floor(difference/60) * 60));
    	
    	if(Math.floor(difference/60) < 59) {
    		((TextView)findViewById(R.id.txtCountdown)).setText(df.format(Math.floor(difference/60)) + ":" + (secondsLeft < 10 ? "0" : "") + df.format(secondsLeft));
    	} else {    		
    		((TextView)findViewById(R.id.txtCountdown)).setText("Ages");
    		countdownTimer.cancel();
    		countdownTimer.purge();    		
    	}
    }
    
    Button.OnClickListener btnRefreshClick = new View.OnClickListener() {
		public void onClick(View v) {
			updateInfo();
			countdownTimer = new Timer();
			countdownTimer.schedule(new RefreshCountdowns(), 500, 1000);
		}
    };
    
    Button.OnClickListener btnLogoutClick = new View.OnClickListener() {
		public void onClick(View v) {
			SharedPreferences.Editor settings = getPreferences(0).edit();
			settings.clear();
    		settings.commit();
    		
    		Intent i = new Intent(Main.this,Login.class);
    		startActivity(i);
    		finish();
		}
    };
    
    CheckBox.OnClickListener chkAutoRefreshClick = new View.OnClickListener() {
		public void onClick(View v) {
			if(((CheckBox)v).isChecked() == true) {
				refreshTimer = new Timer();
				refreshTimer.schedule(new RefreshData(), 500, 10000);
			} else {
				refreshTimer.cancel();
				refreshTimer.purge();
			}
		}
    };

    public String downloadString(String url) {
    	HttpClient client = new DefaultHttpClient();
		HttpGet getMethod = new HttpGet(url);
		String responseBody = "-1";
		
		try {
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			responseBody=client.execute(getMethod, responseHandler);
			
		}
		catch (Throwable t) {
			Toast
				.makeText(this, "Request failed: "+t.toString(), 25000)
				.show();
		}
		return responseBody;
    }
    
    class TicketSaleSummary {
    	public String Description;
    	public int Quantity;    
    	
    	public TicketSaleSummary(String d, int q) {    		
    		this.Description = d;
    		this.Quantity = q;
    	}
    }
    

    class RefreshData extends TimerTask {
    	public void run() {
    		
    		Main.this.runOnUiThread(new Runnable() {
    			
    			public void run() {    		
		    		Main.this.updateInfo();
		    		capRefreshes--;
		    		if(capRefreshes <= 0) {		    			
		    			refreshTimer.cancel();
		    			refreshTimer.purge();
		    			((CheckBox)findViewById(R.id.chkAutoRefresh)).setChecked(false);
		    		}
		    	}
    		});
    	}	
    }
    
    class RefreshCountdowns extends TimerTask {
    	public void run() {
    		
    		Main.this.runOnUiThread(new Runnable() {
    			
    			public void run() {    		
		    		Main.this.updateCountdown();
    			}
    		});
    	}	
    }


}
package uk.ac.warwick.filmsoc.sales;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class Login extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.login);
        
        SharedPreferences settings = getPreferences(0);
        if(settings.getString("username", null) != null && settings.getString("password", null) != null) {
        	login(settings.getString("username", null), settings.getString("password",null));        
        }
        

        Button btnLogin = (Button)findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(btnLoginClick);
        
    }
    
    Button.OnClickListener btnLoginClick = new View.OnClickListener() {
		public void onClick(View v) {

	    	
	    	String username = ((EditText)findViewById(R.id.txtUsername)).getText().toString();
	    	String password = ((EditText)findViewById(R.id.txtPassword)).getText().toString();	    	
	    	
	    	login(username, password);
	    	
		}
    };    
    
    public void login(String username, String password) {
    	String loginTryURL = "http://www.filmsoc.warwick.ac.uk/upages/csujba/othercontent/HEAD/www/html/content/android/trylogin.php?";
    	
    	loginTryURL += "username=" + username;
    	loginTryURL += "&password=" + password;
    	
    	String response = downloadString(loginTryURL);
    	
    	try {
	    	if(Integer.parseInt(response) > 0) {	
	    		SharedPreferences.Editor settings = getPreferences(0).edit();
	    		settings.putString("username",username);
	    		settings.putString("password",password);
	    		settings.commit();
	    		
	    		Intent i = new Intent(Login.this,Main.class);
	    		i.putExtra("username",username);
	    		i.putExtra("password",password);
	    		startActivity(i);
	    		finish();
	    	} else {
	    		// Error off
	    		Toast
				.makeText(Login.this, "Login Failed!", 25000)
				.show();		    		
	    	}
    	} catch (Exception e) {
    		// Error off
    		Toast
			.makeText(Login.this, "Login Failed - " + response, 25000)
			.show();
    	}     
    }
    
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
    
}
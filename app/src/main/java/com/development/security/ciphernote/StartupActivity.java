package com.development.security.ciphernote;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.development.security.ciphernote.model.DatabaseManager;
import com.development.security.ciphernote.model.UserConfiguration;

import org.json.JSONException;

public class StartupActivity extends AppCompatActivity {
    Context applicationContext;
    WebView browser;
    final SecurityManager securityManager = SecurityManager.getInstance();

    String firstPassword = null;
    String secondPassword = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startup);

        applicationContext = getApplicationContext();


        browser=(WebView)findViewById(R.id.webkit);
        browser.getSettings().setJavaScriptEnabled(true);
        browser.addJavascriptInterface(new StartupActivity.WebAppInterface(this), "Android");
        browser.loadUrl("file:///android_asset/StartupPage.html");




    }


    private class AsyncAccountCreation extends AsyncTask<String, String, Boolean> {
        @Override
        protected Boolean doInBackground(String... strings) {
            try {
                //Boolean passwordVaidate = validatePassword(passwordOneValue);
                int score = securityManager.calculatePasswordStrength(firstPassword);

                if (firstPassword.equals(secondPassword) && score > 0) {
                    long start_time = System.nanoTime();
                    int iterations = 100000;
//                if(levelValue.equals("high")){
//                    iterations = 250000;
//                }else if(levelValue.equals("medium")){
//                    iterations = 75000;
//                }else{
//                    iterations = 10000;
//                }


                    String salt = securityManager.generateSalt();
                    Log.d("help", "StartupActivity salt: " + salt);


                    DataStructures.UserConfiguration userConfiguration = new DataStructures.UserConfiguration();
                    userConfiguration.setPasswordHash("");
                    userConfiguration.setSalt(salt);
                    userConfiguration.setIterations(iterations);
                    DatabaseManager databaseManager = new DatabaseManager(applicationContext);
                    databaseManager.addUserConfiguration(new UserConfiguration(userConfiguration.getIterations(), userConfiguration.getPasswordHash(), userConfiguration.getSalt()));

//                writeUserConfig(context);

//                fileManager.saveHashInfo(applicationContext, "", Base64.encodeToString(salt.getBytes(), Base64.DEFAULT), iterations);

                    String saltFromFile = databaseManager.getUserConfiguration().getSalt();

                    byte[] newHash = securityManager.hashPassword(firstPassword, saltFromFile.getBytes(), iterations);

                    Log.d("help", "Startup ran");

                    databaseManager.checkConfigDirectory(applicationContext);
                    databaseManager.writeToDataFile(applicationContext, "started".getBytes(), "startup", true);


                    UserConfiguration currentUserConfig = databaseManager.getUserConfiguration();
                    String hash = Base64.encodeToString(newHash, Base64.DEFAULT);
                    currentUserConfig.setPassword_hash(hash);
                    databaseManager.addUserConfiguration(currentUserConfig);

//                fileManager.writeToFirstRunFile(applicationContext);
//
//                fileManager.saveHashInfo(applicationContext, Base64.encodeToString(newHash, Base64.DEFAULT), Base64.encodeToString(salt.getBytes(), Base64.DEFAULT), iterations);

                    long end_time = System.nanoTime();
                    double difference = (end_time - start_time) / 1e6;
                    int loginTime = (int) difference;
                    writeLoginTime(loginTime);


                    return true;
                }
            }catch(Exception e){
                e.printStackTrace();
            }
            return false;

        }

        protected void onProgressUpdate(Integer... progress) {
        }

        protected void onPostExecute(Boolean status) {
            if(status){
                Intent loginIntent = new Intent(applicationContext, LoginActivity.class);
                startActivity(loginIntent);
                finish();
            }else{
                if(firstPassword.equals(secondPassword)){
                    CharSequence failedAuthenticationString = getString(R.string.passwordTooShort);

                    Toast toast = Toast.makeText(applicationContext, failedAuthenticationString, Toast.LENGTH_LONG);
                    toast.show();
                }else{
                    CharSequence failedAuthenticationString = getString(R.string.passwordsDoNotMatch);

                    Toast toast = Toast.makeText(applicationContext, failedAuthenticationString, Toast.LENGTH_LONG);
                    toast.show();
                }
            }
            browser.post(new Runnable() {
                @Override
                public void run() {
                    browser.loadUrl("javascript:clearFields()");
                }
            });
        }
    }




    protected void androidCreatePassword(String passwordOne, String passwordTwo){
       firstPassword = passwordOne;
       secondPassword = passwordTwo;

        new StartupActivity.AsyncAccountCreation().execute("");
    }

    private int androidCheckPasswordStrength(String password){
        return securityManager.calculatePasswordStrength(password);
    }

       private void writeLoginTime(int time){
        SharedPreferences sp = getSharedPreferences("digital_safe", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt("login_time", time);
        editor.commit();
    }


    public class WebAppInterface {
        Context mContext;
        WebAppInterface(Context c) {
            mContext = c;
        }
        @JavascriptInterface
        public void createPassword(String passwordOne, String passwordTwo) {
            androidCreatePassword(passwordOne, passwordTwo);
        }

        @JavascriptInterface
        public int checkPasswordStrength(String password){
            return androidCheckPasswordStrength(password);
        }
    }
}

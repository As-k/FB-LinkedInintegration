package com.cioc.fbintegration;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.support.annotation.RestrictTo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.linkedin.platform.APIHelper;
import com.linkedin.platform.LISessionManager;
import com.linkedin.platform.errors.LIApiError;
import com.linkedin.platform.errors.LIAuthError;
import com.linkedin.platform.listeners.ApiListener;
import com.linkedin.platform.listeners.ApiResponse;
import com.linkedin.platform.listeners.AuthListener;
import com.linkedin.platform.utils.Scope;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private final String TAG = MainActivity.this.getClass().getName();

    private CallbackManager callbackManager;
    private AccessToken accessToken;

    private LoginButton loginButton;
    private RelativeLayout rlProfileArea;
    private TextView tvName, textView;
    private static final String topCardUrl = "https://api.linkedin.com/v1/people/~:(first-name,last-name,email-address,formatted-name,phone-numbers,public-profile-url,picture-url,picture-urls::(original))";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initParameters();
        initViews();
        computePakageHash();
        AccessTokenTracker accessTokenTracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(
                    AccessToken oldAccessToken,
                    AccessToken currentAccessToken) {
                if (currentAccessToken == null) {
                    Log.d(TAG, "User logged out successfully");
                    rlProfileArea.setVisibility(View.GONE);
                }
            }
        };
    }

    public void initParameters() {
        accessToken = AccessToken.getCurrentAccessToken();
        callbackManager = CallbackManager.Factory.create();
    }

    public void initViews() {
        loginButton = findViewById(R.id.activity_main_btn_login);
        rlProfileArea = findViewById(R.id.activity_main_rl_profile_area);
        tvName = findViewById(R.id.activity_main_tv_name);
        textView = findViewById(R.id.text);


        loginButton.setReadPermissions(Arrays.asList(new String[]{"email", "user_birthday", "user_hometown"}));

        if (accessToken != null) {
            getProfileData();
        } else {
            rlProfileArea.setVisibility(View.GONE);
        }

    // Callback registration
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d(TAG, "User login successfully");
                getProfileData();
            }

            @Override
            public void onCancel() {
                // App code
                Log.d(TAG, "User cancel login");
            }

            @Override
            public void onError(FacebookException exception) {
                // App code
                Log.d(TAG, "Problem for login");
            }
        });
    }

//    @Override
//    protected void onActivityResult ( int requestCode, int resultCode, Intent data){
//        super.onActivityResult(requestCode, resultCode, data);
//        callbackManager.onActivityResult(requestCode, resultCode, data);
//    }

    public void getProfileData() {
        try {
            accessToken = AccessToken.getCurrentAccessToken();
            rlProfileArea.setVisibility(View.VISIBLE);
            GraphRequest request = GraphRequest.newMeRequest(
                    accessToken,
                    new GraphRequest.GraphJSONObjectCallback() {
                        @Override
                        public void onCompleted(
                                JSONObject object,
                                GraphResponse response) {
                            Log.d(TAG, "Graph Object :" + object);
                            try {
                                String name = object.getString("name");
                                tvName.setText("Welcome,  "+ name);

                                Log.d(TAG, "Name : "+ name);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
            Bundle parameters = new Bundle();
            parameters.putString("fields", "id,name,link,birthday,gender,email");
            request.setParameters(parameters);
            request.executeAsync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void computePakageHash() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(
                    "com.cioc.fbintegration",
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (Exception e) {
            Log.e("TAG",e.getMessage());
        }
    }

    public void loginLinkedin(View view){
        LISessionManager.getInstance(getApplicationContext())
                .init(this, buildScope(), new AuthListener() {
                    @Override
                    public void onAuthSuccess() {
                        Toast.makeText(getApplicationContext(), "success" +
                                        LISessionManager.getInstance(getApplicationContext())
                                                .getSession().getAccessToken().toString(),
                                Toast.LENGTH_LONG).show();
                        APIHelper apiHelper = APIHelper.getInstance(getApplicationContext());
                        apiHelper.getRequest(MainActivity.this, topCardUrl, new ApiListener() {
                            @Override
                            public void onApiSuccess(ApiResponse s) {
                                textView.setVisibility(View.VISIBLE);
                                textView.setText("Connected");
                            }

                            @Override
                            public void onApiError(LIApiError error) {
                            }
                        });
                    }

                    @Override
                    public void onAuthError(LIAuthError error) {
                        Toast.makeText(getApplicationContext(), "failed "
                                        + error.toString(),
                                Toast.LENGTH_LONG).show();
                    }
                }, true);
    }

    // handle the respone by calling LISessionManager and start new activity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(callbackManager!=null) {
            callbackManager.onActivityResult(requestCode, resultCode, data);
        }else {
            LISessionManager.getInstance(getApplicationContext())
                    .onActivityResult(this,
                            requestCode, resultCode, data);

            Intent intent = new Intent(MainActivity.this, HomePageActivity.class);
            startActivity(intent);
        }
    }

    // set the permission to retrieve basic -
    //information of User's linkedIn account
    private static Scope buildScope() {
        return Scope.build(Scope.R_BASICPROFILE,
                Scope.R_EMAILADDRESS);
    }
}
package com.poorjar.location.oauth2;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.IOException;

import com.google.api.client.auth.oauth2.draft10.AccessTokenResponse;
import com.google.api.client.googleapis.auth.oauth2.draft10.GoogleAccessTokenRequest.GoogleAuthorizationCodeGrant;
import com.google.api.client.googleapis.auth.oauth2.draft10.GoogleAuthorizationRequestUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.poorjar.location.oauth2.store.CredentialStore;
import com.poorjar.location.oauth2.store.SharedPreferencesCredentialStore;

/**
 * Execute the OAuthRequestTokenTask to retrieve the request, and authorize the request. After the request is authorized by the user, the
 * callback URL will be intercepted here.
 */
public class OAuthAccessTokenActivity extends Activity
{
    final String TAG = getClass().getName();
    private SharedPreferences prefs;

    @Override
    public void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Starting task to retrieve request token.");
        this.prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // new OAuthRequestTokenTask(this).execute();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        final WebView webview = new WebView(this);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.setVisibility(View.VISIBLE);
        setContentView(webview);
        final String authorizationUrl = new GoogleAuthorizationRequestUrl(OAuth2ClientCredentials.CLIENT_ID,
                OAuth2ClientCredentials.REDIRECT_URI, OAuth2ClientCredentials.SCOPE).build();

        /* WebViewClient must be set BEFORE calling loadUrl! */
        webview.setWebViewClient(new WebViewClient()
        {
            @Override
            public void onPageStarted(final WebView view, final String url, final Bitmap bitmap)
            {
                System.out.println("onPageStarted : " + url);
            }

            @Override
            public void onPageFinished(final WebView view, final String url)
            {
                if (url.startsWith(OAuth2ClientCredentials.REDIRECT_URI))
                {
                    try
                    {
                        if (url.indexOf("code=") != -1)
                        {
                            final String code = extractCodeFromUrl(url);
                            System.out.println("Code : " + code);
                            final AccessTokenResponse accessTokenResponse = new GoogleAuthorizationCodeGrant(new NetHttpTransport(),
                                    new JacksonFactory(), OAuth2ClientCredentials.CLIENT_ID, OAuth2ClientCredentials.CLIENT_SECRET, code,
                                    OAuth2ClientCredentials.REDIRECT_URI).execute();

                            final CredentialStore credentialStore = new SharedPreferencesCredentialStore(prefs);
                            credentialStore.write(accessTokenResponse);
                            view.setVisibility(View.INVISIBLE);
                            startActivity(new Intent(OAuthAccessTokenActivity.this, LatitudeApiSample.class));
                        }
                        else if (url.indexOf("error=") != -1)
                        {
                            view.setVisibility(View.INVISIBLE);
                            new SharedPreferencesCredentialStore(prefs).clearCredentials();
                            startActivity(new Intent(OAuthAccessTokenActivity.this, LatitudeApiSample.class));
                        }
                    }
                    catch (final IOException e)
                    {
                        e.printStackTrace();
                    }
                }
                System.out.println("onPageFinished : " + url);
            }

            private String extractCodeFromUrl(final String url)
            {
                return url.substring(OAuth2ClientCredentials.REDIRECT_URI.length() + 7, url.length());
            }
        });
        webview.loadUrl(authorizationUrl);
    }
}

/**
 * Copyright 2018 Iron City Software LLC
 *
 * This file is part of TrueCrew.
 *
 * TrueCrew is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TrueCrew is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with TrueCrew.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.truecrew.flica;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;

import org.joda.time.DateTime;

import crewtools.flica.FlicaConnection;

import static com.truecrew.flica.Constants.ACCOUNT_TYPE;

public class AuthenticatorActivity extends AccountAuthenticatorActivity {
  public final static String PARAM_USERNAME = "username";
  //public final static String PARAM_PASSWORD = "password";

  private final String TAG = this.getClass().getName();

  private AccountManager accountManager;
  private UserLoginTask userLoginTask = null;
  private ProgressDialog progressDialog = null;
  private EditText usernameField;
  private EditText passwordField;
  private TextView messageField;
  private boolean isNewAccount;

  // TODO does screen reorienting or backgrounding preserve username?
  // ie do we need to deal with savedInstanceState?

  @Override
  public void onCreate(Bundle savedInstanceState) {
    Log.i(TAG, "onCreate");
    super.onCreate(savedInstanceState);

    requestWindowFeature(Window.FEATURE_LEFT_ICON);
    setContentView(R.layout.login_activity);
    getWindow().setFeatureDrawableResource(
        Window.FEATURE_LEFT_ICON, android.R.drawable.ic_dialog_alert);

    accountManager = AccountManager.get(this);
    usernameField = (EditText) findViewById(R.id.username_field);
    passwordField = (EditText) findViewById(R.id.password_field);
    messageField = (TextView) findViewById(R.id.message);

    String username = getIntent().getStringExtra(PARAM_USERNAME);
    if (username != null) {
      usernameField.setText(username);
    } else {
      isNewAccount = true;
    }
    findViewById(R.id.ok_button).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Log.i(TAG, "onClick");
        String username = usernameField.getText().toString();
        String password = passwordField.getText().toString();
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
          messageField.setText("type something...");
        } else {
          showProgressDialog();
          // TODO Precondition not null
          userLoginTask = new UserLoginTask(username, password);
          userLoginTask.execute();
        }
      }
    });
  }

  void showProgressDialog() {
    final ProgressDialog dialog = new ProgressDialog(this);
    dialog.setMessage("hold on a sec");
    dialog.setIndeterminate(true);
    dialog.setCancelable(true);
    dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
      public void onCancel(DialogInterface dialog) {
        Log.i(TAG, "user cancelling authentication");
        if (userLoginTask != null) {
          userLoginTask.cancel(true);
        }
      }
    });
    progressDialog = dialog;
  }

  public void onAuthenticationResult(String authToken) {
    // Does anything call this but UserLoginTask?

    boolean success = ((authToken != null) && (authToken.length() > 0));
    Log.i(TAG, "onAuthenticationResult(" + success + ")");

    hideProgress();  // should this be done from UserLoginTask?

    if (success) {
      final Account account = new Account(userLoginTask.getUsername(), ACCOUNT_TYPE);
      if (isNewAccount) {
        // TODO encrypt password
        accountManager.addAccountExplicitly(account, userLoginTask.getPassword(), null /* user data bundle */);
      }
      accountManager.setPassword(account, userLoginTask.getPassword());
      final Intent intent = new Intent();
      intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, userLoginTask.getUsername());
      intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, account.type);
      intent.putExtra(AccountManager.KEY_AUTHTOKEN, authToken);
      intent.putExtra(FlicaAuthenticator.KEY_CUSTOM_TOKEN_EXPIRY,
          new DateTime().plus(FlicaAuthenticator.SESSION_TIMEOUT).getMillis());
      setAccountAuthenticatorResult(intent.getExtras());
      setResult(RESULT_OK, intent);
      finish();
    } else {
      Log.e(TAG, "onAuthenticationResult: failed to authenticate");
      messageField.setText("invalid password");
    }
    userLoginTask = null;
  }

  // This is dumb, UserLoginCalls this to null itself.  Must be a better way...
  public void onAuthenticationCancel() {
    // Move to UserLoginTask...
    Log.i(TAG, "onAuthenticationCancel()");
    // Our task is complete, so clear it out
    userLoginTask = null;
    // Hide the progress dialog
    hideProgress();
  }

  /**
   * Hides the progress UI for a lengthy operation.
   */
  private void hideProgress() {
    if (progressDialog != null) {
      progressDialog.dismiss();
      progressDialog = null;
    }
  }

  /**
   * Represents an asynchronous task used to authenticate a user against the
   * SampleSync Service
   */
  public class UserLoginTask extends AsyncTask<Void, Void, String> {
    private final String username;
    private final String password;

    public UserLoginTask(String username, String password) {
      super();
      this.username = username;
      this.password = password;
    }

    String getUsername() {
      return username;
    }

    String getPassword() {
      return password;
    }

    @Override
    protected String doInBackground(Void... params) {
      Log.e(TAG, "UserLoginTask.doInBackground");
      try {
        FlicaConnection connection = new FlicaConnection(username, password);
        connection.connect();
        Log.i(TAG, "Result: " + connection.getSession());
        return connection.getSession();
      } catch (Exception ex) {
        Log.e(TAG, "UserLoginTask.doInBackground: failed to authenticate");
        Log.i(TAG, ex.toString());
        return null;
      }
    }

    /* Invoked on the UI thread.  */
    @Override
    protected void onPostExecute(final String authToken) {
      Log.i(TAG, "UserLoginTask.onPostExecute");
      onAuthenticationResult(authToken);
    }

    @Override
    protected void onCancelled() {
      // Invoked on the UI Thread
      Log.i(TAG, "UserLoginTask.onCancelled");
      onAuthenticationCancel();
    }
  }
}

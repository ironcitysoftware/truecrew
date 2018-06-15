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

import android.accounts.*;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.Minutes;

import java.io.IOException;
import java.util.Arrays;

import crewtools.flica.FlicaConnection;

import static android.accounts.AccountManager.KEY_BOOLEAN_RESULT;

public class FlicaAuthenticator extends AbstractAccountAuthenticator {
  private final String TAG = this.getClass().getName();
  private final Context context;

  /** Figure out the right value. */
  public static final Minutes SESSION_TIMEOUT = Minutes.minutes(15);

  public FlicaAuthenticator(Context context) {
    super(context);
    this.context = context;
  }

  @Override
  public Bundle addAccount(AccountAuthenticatorResponse response, String accountType,
      String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
    Log.i(TAG, "addAccount(response=" + response + " accountType=" + accountType
        + " authTokenType=" + authTokenType + " requiredFeatures="
        + (requiredFeatures == null ? "" : Arrays.asList(requiredFeatures))
        + " options=" + options + ")");

    final Intent intent = new Intent(context, AuthenticatorActivity.class);
//    intent.putExtra(AuthenticatorActivity.ARG_ACCOUNT_TYPE, accountType);
//    intent.putExtra(AuthenticatorActivity.ARG_AUTH_TYPE, authTokenType);
//    intent.putExtra(AuthenticatorActivity.ARG_IS_ADDING_NEW_ACCOUNT, true);
    intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

    final Bundle bundle = new Bundle();
    bundle.putParcelable(AccountManager.KEY_INTENT, intent);
    return bundle;
  }

  @Override
  public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account,
      String authTokenType, Bundle options) throws NetworkErrorException {
    Log.i(TAG, "getAuthToken(response=" + response + " account=" + account
        + " authTokenType=" + authTokenType + " options=" + options + ")");

    if (!authTokenType.equals(Constants.AUTH_TOKEN_TYPE)) {
      final Bundle result = new Bundle();
      result.putString(AccountManager.KEY_ERROR_MESSAGE, "invalid authTokenType");
      return result;
    }

    final AccountManager accountManager = AccountManager.get(context);
    String authToken = accountManager.peekAuthToken(account, authTokenType);
    Log.i(TAG, "peekAuthToken returned - " + authToken);

    if (authToken == null || authToken.isEmpty()) {
      final String password = accountManager.getPassword(account);
      if (password != null) {
        Log.i(TAG, "Authenticating with the existing password");
        FlicaConnection flicaConnection = new FlicaConnection(account.name, password);
        try {
          if (flicaConnection.connect()) {
            authToken = flicaConnection.getSession();
            Log.i(TAG, "Success! Token is [" + authToken + "]");
          } else {
            Log.i(TAG, "Failed to authenticate - bad credentials");
          }
        } catch (IOException ioe) {
          Log.i(TAG, "Failed to authenticate", ioe);
        }
      }
    }

    if (authToken != null && !authToken.isEmpty()) {
      final Bundle result = new Bundle();
      result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
      result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
      result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
      result.putLong(KEY_CUSTOM_TOKEN_EXPIRY, new DateTime().plus(SESSION_TIMEOUT).getMillis());
      return result;
    }

    final Intent intent = new Intent(context, AuthenticatorActivity.class);
    intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
    intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, account.type);
    intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, account.name);
    intent.putExtra(AuthenticatorActivity.PARAM_USERNAME, account.name);
//    intent.putExtra(AuthenticatorActivity.PARAM_AUTHTOKEN_TYPE, authTokenType);
    final Bundle bundle = new Bundle();
    bundle.putParcelable(AccountManager.KEY_INTENT, intent);
    return bundle;
  }

  @Override
  public Bundle hasFeatures(
          AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
    Log.i(TAG, "hasFeatures(response=" + response
            + " account=" + account + " features=" + Arrays.asList(features) + ")");
    final Bundle result = new Bundle();
    result.putBoolean(KEY_BOOLEAN_RESULT, false);
    return result;
  }


  @Override
  public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) throws NetworkErrorException {
    Log.i(TAG, "confirmCredentials(response=" + response
            + " account=" + account + " options=" + options + ")");
    throw new UnsupportedOperationException();
  }

  @Override
  public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
    Log.i(TAG, "editProperties(response=" + response
            + " accountType=" + accountType + ")");
    throw new UnsupportedOperationException();
  }

  @Override
  public String getAuthTokenLabel(String authTokenType) {
    Log.i(TAG, "getAuthTokenLabel(authTokenType=" + authTokenType + ")");
    throw new UnsupportedOperationException();
  }

  @Override
  public Bundle updateCredentials(
      AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
    Log.i(TAG, "updateCredentials(response=" + response
        + " account=" + account + " authTokenType=" + authTokenType + " options=" + options + ")");
    throw new UnsupportedOperationException();
  }
}

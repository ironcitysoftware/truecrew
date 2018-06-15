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

package com.truecrew;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.google.common.base.Preconditions;
import com.truecrew.flica.Constants;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import crewtools.aa.FlightStatusService;
import crewtools.dashboard.Dashboard;
import crewtools.dashboard.DashboardService;
import crewtools.flica.FlicaConnection;
import crewtools.flica.FlicaService;
import crewtools.util.Clock;
import crewtools.util.SystemClock;

public class DashboardActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {
  private AccountManager accountManager;
  private SwipeRefreshLayout swipeRefreshLayout;
  private ListView listView;
  private DashboardAdapter dashboardAdapter;
  private DashboardUpdater dashboardUpdater;

  private final String TAG = this.getClass().getName();

  /**
   * Note: screen rotation will restart the activity.
   */
  @Override
  protected void onStart() {
    super.onStart();

    setContentView(R.layout.activity_main);
    accountManager = AccountManager.get(this);

    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    swipeRefreshLayout = findViewById(R.id.swiperefresh);

    dashboardAdapter = new DashboardAdapter(this);
    dashboardUpdater = new DashboardUpdater(dashboardAdapter);

    listView = findViewById(R.id.dashboard_list);
    listView.setAdapter(dashboardAdapter);
  }

  private static final int ACTIVITY_RESULT_FOR_ADD_ACCOUNT = 41;

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    // TODO: getting resultCode = 0 (CANCELED) even though the activity sets OK.
    Log.i(TAG, "onActivityResult " + requestCode + " " + resultCode);
    if (requestCode == ACTIVITY_RESULT_FOR_ADD_ACCOUNT) {
      if (resultCode == RESULT_OK) {
        Log.i(TAG, "Data: " + data);
        String authToken = data.getStringExtra(AccountManager.KEY_AUTHTOKEN);
        Log.i(TAG, "Authtoken: " + authToken);
        dashboardUpdater.new UpdateFromTokenTask().execute(authToken);
      }
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onRefresh() {
    updateDashboard();
    swipeRefreshLayout.setRefreshing(false);
  }

  @Override
  public void onResume() {
    super.onResume();
    updateDashboard();
  }

  private void updateDashboard() {
    Log.i(TAG, "updateDashboard");
    if (isNetworkAvailable()) {
      Log.i(TAG, "network is available");
      Account availableAccounts[] = accountManager.getAccountsByType(Constants.ACCOUNT_TYPE);
      if (availableAccounts.length == 0) {
        Log.i(TAG, "No Flica account; add one.");

        Intent addAccountIntent = new Intent(android.provider.Settings.ACTION_ADD_ACCOUNT)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        addAccountIntent.putExtra(Settings.EXTRA_ACCOUNT_TYPES, new String[] {Constants.ACCOUNT_TYPE});
        startActivityForResult(addAccountIntent, ACTIVITY_RESULT_FOR_ADD_ACCOUNT);
        return;
      }

      AccountManagerFuture<Bundle> bundle = accountManager.getAuthToken(
          availableAccounts[0],
          Constants.AUTH_TOKEN_TYPE,
          null,
          this,
          null,
          null);
      dashboardUpdater.new UpdateFromBundleTask().execute(bundle);
      return;
    }

    Log.i(TAG, "Notify data set changed");
    dashboardAdapter.notifyDataSetChanged();
  }

  private boolean isNetworkAvailable() {
    android.net.ConnectivityManager connectivityManager
          = (android.net.ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
  }
}


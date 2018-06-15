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

import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.os.Bundle;
import android.util.Log;

import com.google.common.base.Preconditions;

import java.io.IOException;

import crewtools.aa.FlightStatusService;
import crewtools.dashboard.Dashboard;
import crewtools.dashboard.DashboardService;
import crewtools.flica.FlicaConnection;
import crewtools.flica.FlicaService;
import crewtools.flica.parser.ParseException;
import crewtools.util.Clock;
import crewtools.util.SystemClock;

public class DashboardUpdater {
  private final String TAG = this.getClass().getName();

  private final FlicaConnection flicaConnection;
  private final FlicaService flicaService;
  private final FlightStatusService flightStatusService;
  private final DashboardService dashboardService;
  private final DashboardAdapter dashboardAdapter;
  private final Clock clock;

  DashboardUpdater(DashboardAdapter dashboardAdapter) {
    this.flicaConnection = new FlicaConnection(null, null);
    this.flicaService = new FlicaService(flicaConnection);
    this.flightStatusService = new FlightStatusService();
    this.dashboardService = new DashboardService(flicaService, flightStatusService);
    this.clock = new SystemClock();
    this.dashboardAdapter = dashboardAdapter;
  }

  public class UpdateFromBundleTask extends android.os.AsyncTask<AccountManagerFuture<Bundle>, Void, Void> {
    protected Void doInBackground(AccountManagerFuture<Bundle>... accounts) {
      Log.i(TAG, "UpdateFromBundleTask");
      Preconditions.checkState(accounts.length == 1);
      try {
        Bundle bundle = accounts[0].getResult();  // blocks
        String authToken = (String) bundle.get(AccountManager.KEY_AUTHTOKEN);
        flicaConnection.setSession(authToken);
        Dashboard dashboard = dashboardService.getDashboard(clock);
        dashboardAdapter.updateDashboard(dashboard);
      } catch (Exception e) {
        Log.i(TAG, "Error updating", e);
      }
      return null;
    }
  }

  public class UpdateFromTokenTask extends android.os.AsyncTask<String, Void, Void> {
    protected Void doInBackground(String... authTokens) {
      Log.i(TAG, "UpdateFromTokenTask");
      Preconditions.checkState(authTokens.length == 1);
      try {
        flicaConnection.setSession(authTokens[0]);
        Dashboard dashboard = dashboardService.getDashboard(clock);
        dashboardAdapter.updateDashboard(dashboard);
      } catch (Exception e) {
        Log.i(TAG, "Error updating", e);
      }
      return null;
    }
  }
}

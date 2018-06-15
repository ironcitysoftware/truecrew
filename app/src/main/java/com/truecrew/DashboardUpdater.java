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
  private final Clock clock;
  private final DashboardAdapter dashboardAdapter;

  DashboardUpdater(DashboardAdapter dashboardAdapter) {
    this.flicaConnection = new FlicaConnection(null, null);
    this.flicaService = new FlicaService(flicaConnection);
    this.flightStatusService = new FlightStatusService();
    this.dashboardService = new DashboardService(flicaService, flightStatusService);
    this.clock = new SystemClock();
    this.dashboardAdapter = dashboardAdapter;
  }

  private void updateInternal(String authToken) throws IOException, ParseException {
    flicaConnection.setSession(authToken);
    Dashboard dashboard = dashboardService.getDashboard(clock);
    dashboardAdapter.updateDashboard(dashboard);
  }

  public class UpdateFromBundleTask extends android.os.AsyncTask<AccountManagerFuture<Bundle>, Void, Void> {
    protected Void doInBackground(AccountManagerFuture<Bundle>... accounts) {
      Preconditions.checkState(accounts.length == 1);
      try {
        Bundle bundle = accounts[0].getResult();  // blocks
        String authToken = (String) bundle.get(AccountManager.KEY_AUTHTOKEN);
        updateInternal(authToken);
      } catch (Exception e) {
        Log.i(TAG, "Error updating", e);
      }
      return null;
    }
  }

  public class UpdateFromTokenTask extends android.os.AsyncTask<String, Void, Void> {
    protected Void doInBackground(String... authTokens) {
      Preconditions.checkState(authTokens.length == 1);
      try {
        updateInternal(authTokens[0]);
      } catch (Exception e) {
        Log.i(TAG, "Error updating", e);
      }
      return null;
    }
  }
}

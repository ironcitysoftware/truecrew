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

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Ints;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import crewtools.dashboard.Dashboard;
import crewtools.dashboard.FlightInfo;
import crewtools.dashboard.TimeInfo;

public class DashboardAdapter extends BaseAdapter {
  private final Activity activity;
  private final LayoutInflater inflater;
  private Dashboard dashboard;

  public DashboardAdapter(Activity activity) {
    this.activity = activity;
    this.inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
  }

  public void updateDashboard(Dashboard dashboard) {
    this.dashboard = dashboard;
    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        notifyDataSetChanged();
      }
    });
  }

  @Override
  public int getCount() {
    if (dashboard == null) {
      return 0;
    }
    return dashboard.getFlights().size();
  }

  @Override
  public FlightInfo getItem(int position) {
    Preconditions.checkState(dashboard != null);
    return dashboard.getFlights().get(position);
  }

  @Override
  public long getItemId(int position) {
    return dashboard.getFlights().get(position).hashCode();
  }

  @Override
  public View getView(int position, View view, ViewGroup container) {
    if (view == null) {
      view = inflater.inflate(R.layout.dashboard, container, false);
    }
    DashboardView dv = new DashboardView(view);
    FlightInfo info = dashboard.getFlights().get(position);
    dv.set(R.id.retrieved_time, dashboard.getPrettyRetrievedTime());
    dv.set(R.id.flight_number, info.getFlightNumber());
    if (info.isCanceled()) {
      dv.set(R.id.flight_status, "CANCELLED");
    } else {
      dv.set(R.id.flight_status, "");
    }
    dv.set(R.id.origin_airport, info.getOriginAirport());
    dv.set(R.id.origin_address, info.getOriginGate());
    dv.set(R.id.destination_airport, info.getDestinationAirport());
    dv.set(R.id.destination_address, info.getDestinationGate());
    dv.set(R.id.arrow, "->");
    dv.set(R.id.equipment, info.getAircraftType());
    TimeInfo timeInfo = info.getTimeInfo();
    if (position == 0 && dashboard.getCurrentFlight() != null) {
      // current flight
      if (timeInfo.hasDeparture()) {
        dv.set(R.id.departure_time, String.format("Depart %s %s",
            timeInfo.getDepartureOffset(), timeInfo.getDepartureZulu()));
      }
      if (timeInfo.hasArrival()) {
        dv.set(R.id.arrival_time, String.format("Arrive %s %s",
            timeInfo.getArrivalOffset(), timeInfo.getArrivalZulu()));
      }
    } else {
      // next flight
      dv.set(R.id.company_show, String.format("AA show %s %s",
          timeInfo.getCompanyShowOffset(), timeInfo.getCompanyShowZulu()));
      if (timeInfo.hasEstimatedShow()) {
        dv.set(R.id.estimated_show, String.format("Est. show %s %s",
            timeInfo.getEstimatedShowOffset(), timeInfo.getEstimatedShowZulu()));
      }
      if (timeInfo.hasDeparture()) {
        dv.set(R.id.departure_time, String.format("Departed %s %s",
            timeInfo.getDepartureOffset(), timeInfo.getDepartureZulu()));
      } else {
        dv.set(R.id.departure_time, String.format("Departs %s %s",
            timeInfo.getScheduledDepartureOffset(), timeInfo.getScheduledDepartureZulu()));
      }
    }
    return view;
  }

  private class DashboardView {
    private final View view;

    public DashboardView(View view) {
      this.view = view;
    }

    public void set(int id, String text) {
      ((TextView) view.findViewById(id)).setText(text);
    }

    public void clear(int id) {
      ((TextView) view.findViewById(id)).setText("");
    }
  }
}

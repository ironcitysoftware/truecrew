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

import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Ints;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import crewtools.dashboard.Dashboard;

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
    return dashboard == null ? 0 : 1;
  }

  @Override
  public Dashboard getItem(int position) {
    return dashboard;
  }

  @Override
  public long getItemId(int position) {
    return dashboard.hashCode();
  }

  @Override
  public View getView(int position, View view, ViewGroup container) {
    if (view == null) {
      view = inflater.inflate(R.layout.dashboard, container, false);
    }
    set(view, R.id.retrieved_time, dashboard.getPrettyRetrievedTime());
    set(view, R.id.origin_airport, dashboard.getCurrentFlight().getOriginAirport());
    set(view, R.id.origin_address, dashboard.getCurrentFlight().getOriginGate());
    set(view, R.id.destination_airport, dashboard.getCurrentFlight().getDestinationAirport());
    set(view, R.id.destination_address, dashboard.getCurrentFlight().getDestinationGate());
    return view;
  }

  private void set(View view, int id, String text) {
    ((TextView) view.findViewById(id)).setText(text);
  }
}

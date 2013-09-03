package com.evature.search.views.adapters;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.evature.search.R;
import com.evature.search.models.vayant.BookingSolution;
import com.evature.search.models.vayant.Flight;
import com.evature.search.models.vayant.Segment;
import com.evature.search.models.vayant.VayantJourneys;
import com.evature.search.views.fragments.FlightsFragment;

public class FlightListAdapter extends BaseAdapter {
	// This is the VAYANT version of the flight list adapter.
	// It is currently not in use

	private static final String TAG = "FlightListAdapter";

	private LayoutInflater mInflater;
	// private FlightsFragment mParent;
	private VayantJourneys journeys = null;
	private SimpleDateFormat dateFormatter;
	private HashMap<String, Integer> airlineLogos = new HashMap<String, Integer>();

	public FlightListAdapter(FlightsFragment parent, VayantJourneys journeys) {
		Log.d(TAG, "CTOR");
		mInflater = LayoutInflater.from(parent.getActivity());
		// mParent = parent;
		this.journeys = journeys;
		
		dateFormatter = new SimpleDateFormat("yyyy-MM-dd\n HH:mm");  // TODO: base on chosen Locale
		
		
		airlineLogos.put("DL", R.drawable.logo_dl);
		airlineLogos.put("KL", R.drawable.logo_kl);
		airlineLogos.put("US", R.drawable.logo_us); // 
		airlineLogos.put("VS", R.drawable.logo_vs); // Virgin
		airlineLogos.put("AF", R.drawable.logo_af); // AirFrance
		airlineLogos.put("AZ", R.drawable.logo_az); // Alitalia
		airlineLogos.put("AY", R.drawable.logo_ay); // Finnair Oyj
		airlineLogos.put("UA", R.drawable.logo_ua); // United Airlines
		airlineLogos.put("AC", R.drawable.logo_ac);	// Air Canada
		airlineLogos.put("AA", R.drawable.logo_aa);	// American Airlines
		airlineLogos.put("TK", R.drawable.logo_tk);	// Turkish Airlines
		airlineLogos.put("LH", R.drawable.logo_lh);	// Deutsche Lufthansa 
		airlineLogos.put("TN", R.drawable.logo_tn);	// Air Tahiti Nui
		airlineLogos.put("SU", R.drawable.logo_su);	// Aeroflot Russian Airlines
		airlineLogos.put("BA", R.drawable.logo_ba);	// British Airways
		airlineLogos.put("SN", R.drawable.logo_sn);	// Brussels Airlines
		airlineLogos.put("LX", R.drawable.logo_lx);	// SWISS International Air Lines
	}

	public void setJourneys(VayantJourneys journeys) {
		Log.d(TAG, "setJourneys()");
		this.journeys = journeys;
	}

	@Override
	public int getCount() {
		if (journeys != null) {
			return journeys.mOneWayJourneys.size();
		}
		return 0;
	}

	@Override
	public Object getItem(int position) {
		return journeys.mJourneys[position];
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override   // TODO: reuse views
	public View getView(int position, View convertView, ViewGroup parent) {
		
		BookingSolution solution = journeys.mOneWayJourneys.get(position).mBookingSolutions[0];
		ViewHolder holder;		
		if (convertView == null) 
		{
			convertView  = mInflater.inflate(R.layout.flight_list_item_vayant, parent, false);
			holder = new ViewHolder();
			 
			holder.logo = (ImageView) convertView.findViewById(R.id.airline_logo);
			holder.airline =  (TextView) convertView.findViewById(R.id.airline_text);
			holder.departure = (TextView) convertView.findViewById(R.id.itinerary_view_departure_time);
			holder.arrival = (TextView) convertView.findViewById(R.id.itinerary_view_arrival_time);
			holder.rate = (TextView) convertView.findViewById(R.id.itinerary_view_price);
			convertView.setTag(holder);				 						 				 				 
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		

		// TextView label = (TextView) row.findViewById(R.id.price);
		// label.setText("$" + String.valueOf(solution.mPrice) + " ");

		int segmentCount = 1;
		//for (Segment segment : solution.mSegments) {
		Segment segment = solution.mSegments.get(0);
		
		// TextView stops_count = (TextView) child.findViewById(R.id.itinerary_view_stops_count);
		// stops_count.setText(segment.isDirect() ? "Direct" : "Not Direct");

		Date departureDateTime = segment.flights.get(0).departureDateTime;
		String departureTime = dateFormatter.format(departureDateTime);
//			String departureTime = String.format("%1$d:%2$02d", departureDateTime.getHours(), departureDateTime.getMinutes());
		holder.departure.setText(departureTime + " " + segment.flights.get(0).origin);
		
		Flight flight = segment.flights.get(segment.flights.size() - 1);
		Date arrivalDateTime = flight.arrivalDateTime;
		String ArrivalTime = dateFormatter.format(arrivalDateTime); //String.format("%1$d:%2$02d", arrivalDateTime.getHours(), arrivalDateTime.getMinutes());
		holder.arrival.setText(ArrivalTime + " " + flight.destination);

		String airlineCode = segment.flights.get(0).marketingCarrier.toUpperCase(Locale.US);
		if (airlineLogos.containsKey(airlineCode)) {
			holder.airline.setVisibility(View.GONE);
			holder.logo.setImageResource(airlineLogos.get(airlineCode));
			holder.logo.setVisibility(View.VISIBLE);
		}
		else {
			holder.logo.setVisibility(View.GONE);
			holder.airline.setText(airlineCode);
			holder.airline.setVisibility(View.VISIBLE);
		}
		
		holder.rate.setText( String.format("%1$,.2f %2$s", solution.mOutboundPrice, solution.mCurrency.equals("USD") ? "$" : solution.mCurrency));
		
//		holder.layout.addView(child);
		//Log.d(TAG, "getView position "+String.valueOf(position)+" adding view for segment "+(segmentCount++));
		//}
		return (convertView);

	}

	public static class ViewHolder {
		public LinearLayout layout;
		ImageView logo;
		TextView airline;
		TextView departure;
		TextView arrival;
		TextView rate;
	}

}

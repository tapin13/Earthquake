package com.example.earthquake;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.location.Location;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.support.v4.app.NavUtils;
import android.widget.AdapterView.OnItemClickListener;

public class Earthquake extends Activity {

	ListView earthquakeListView;
	ArrayAdapter<Quake> aa;
	
	ArrayList<Quake> earthquakes = new ArrayList<Quake>();
	
	static final private int MENU_UPDATE = Menu.FIRST;
	static final private int QUAKE_DIALOG = 1;
	Quake selectedQuake;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_earthquake);
        
        earthquakeListView = (ListView)this.findViewById(R.id.earthquakeListView);
        
        earthquakeListView.setOnItemClickListener(new OnItemClickListener() {
        	@Override
        	public void onItemClick(AdapterView _av, View _v, int _index, long arg3) {
        		selectedQuake = earthquakes.get(_index);
        		showDialog(QUAKE_DIALOG);
        	}
		});
        
        int layoutID = android.R.layout.simple_list_item_1;
        aa = new ArrayAdapter<Quake>(this, layoutID, earthquakes);
        earthquakeListView.setAdapter(aa);
        
		refrashEarthquakes();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.activity_earthquake, menu);
    	
    	super.onCreateOptionsMenu(menu);
    	menu.add(0, MENU_UPDATE, Menu.NONE, R.string.menu_update);
    	
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	super.onOptionsItemSelected(item);
    	
    	switch(item.getItemId()) {
    		case (MENU_UPDATE) : {
    			refrashEarthquakes();
    			return true;
    		}
    	}
    	
		return false;
    }
    
    public void refrashEarthquakes() {
    	URL url;
    	try {
    		String quakeFeed = getString(R.string.quake_feed);
    		url = new URL(quakeFeed);
    		
    		URLConnection connection;
    		connection = url.openConnection();
    		
    		HttpURLConnection httpConnection = (HttpURLConnection)connection;
    		int responseCode = httpConnection.getResponseCode();
    		
    		if(responseCode == HttpURLConnection.HTTP_OK) {
    			InputStream in = httpConnection.getInputStream();
    			
    			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    			DocumentBuilder db = dbf.newDocumentBuilder();
    			
    			Document dom = db.parse(in);
    			Element docEle = dom.getDocumentElement();
    			
    			earthquakes.clear();
    			
    			NodeList nl = docEle.getElementsByTagName("entry");
    			if(nl != null && nl.getLength() > 0) {
    				for(int i = 0; i < nl.getLength(); i++) {
    					Element entry = (Element)nl.item(i);
    					Element title = (Element)entry.getElementsByTagName("title").item(0);
    					Element g = (Element)entry.getElementsByTagName("georss:point").item(0);
    					Element when = (Element)entry.getElementsByTagName("updated").item(0);
    					Element link = (Element)entry.getElementsByTagName("link").item(0);
    					
    					String details = title.getFirstChild().getNodeValue();
    					String hostname = ""; //"http://earthquake.usgs.gov";
    					String linkString = hostname + link.getAttribute("href");
    					
    					String point = g.getFirstChild().getNodeValue();
    					String dt = when.getFirstChild().getNodeValue();
    					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss'Z'");
    					Date qdate = new GregorianCalendar(0,0,0).getTime();
    					
    					try {
    						qdate = sdf.parse(dt);
    					} catch (ParseException e) {
    						e.printStackTrace();
    					}
    					
    					String[] location = point.split(" ");
    					Location l = new Location("dummyGPS");
    					l.setLatitude(Double.parseDouble(location[0]));
    					l.setLongitude(Double.parseDouble(location[1]));
    					
    					String magniString = details.split(" ")[1];
    					int end = magniString.length() - 1;
    					double magnitude = Double.parseDouble(magniString.substring(0, end));
    					details = details.split(",")[1].trim();
    					
    					Quake quake = new Quake(qdate, details, l, magnitude, linkString);
    					addNewQuake(quake);
    				}
    			}
    		}
    		
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
		finally {
		}
	}

	private void addNewQuake(Quake _quake) {
		earthquakes.add(_quake);
		
		aa.notifyDataSetChanged();
	}
    
	@Override
	public Dialog onCreateDialog(int id) {
		switch(id) {
			case(QUAKE_DIALOG) : 
				LayoutInflater li = LayoutInflater.from(this);
				View quakeDetailsView = li.inflate(R.layout.quake_details, null);
				
				AlertDialog.Builder quakeDialog = new AlertDialog.Builder(this);
				quakeDialog.setTitle("Quake Time");
				quakeDialog.setView(quakeDetailsView);
				
				return quakeDialog.create();
		}
		return null;
	}
	
	@Override
	public void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
		case (QUAKE_DIALOG):
			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
			String dateString = sdf.format(selectedQuake.getDate());
			String quakeText = "Magnitude " + selectedQuake.getMagnitude() + 
				"\n" + selectedQuake.getLink();
			
			AlertDialog quakeDialog = (AlertDialog)dialog;
			quakeDialog.setTitle(dateString);
			TextView tv = (TextView)quakeDialog.findViewById(R.id.quakeDetailsTextView);
			tv.setText(quakeText);
			break;
		}
	}
	
}
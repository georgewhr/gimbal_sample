/**
 * Copyright (C) 2014 Gimbal, Inc. All rights reserved.
 *
 * This software is the confidential and proprietary information of Gimbal, Inc.
 *
 * The following sample code illustrates various aspects of the Gimbal SDK.
 *
 * The sample code herein is provided for your convenience, and has not been
 * tested or designed to work on any particular system configuration. It is
 * provided AS IS and your use of this sample code, whether as provided or
 * with any modification, is at your own risk. Neither Gimbal, Inc.
 * nor any affiliate takes any liability nor responsibility with respect
 * to the sample code, and disclaims all warranties, express and
 * implied, including without limitation warranties on merchantability,
 * fitness for a specified purpose, and against infringement.
 */
package com.example.gwang.gimbalsample;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.gimbal.android.Communication;
import com.gimbal.android.CommunicationListener;
import com.gimbal.android.CommunicationManager;
import com.gimbal.android.BeaconEventListener;
import com.gimbal.android.BeaconManager;
import com.gimbal.android.BeaconSighting;
import com.gimbal.android.Gimbal;
import com.gimbal.android.PlaceEventListener;
import com.gimbal.android.PlaceManager;
import com.gimbal.android.Push;
import com.gimbal.android.Push.PushType;
import com.gimbal.android.Visit;

import com.example.gwang.gimbalsample.GimbalEvent.TYPE;


public class AppService extends Service {
    private static final int MAX_NUM_EVENTS = 100;
    private LinkedList<GimbalEvent> events;
    private PlaceEventListener placeEventListener;
    private CommunicationListener communicationListener;
    private static final String TAG = "MyActivity";

    private BeaconEventListener beaconSightingListener;
    private BeaconManager beaconManager;


    @Override
    public void onCreate() {
        events = new LinkedList<GimbalEvent>(GimbalDAO.getEvents(getApplicationContext()));

        Gimbal.setApiKey(this.getApplication(), "549120f7-558f-47fc-8b0b-d282f2a4b2f8");
        Log.i(TAG, "georgewhr, after get spi key");

        // Setup PlaceEventListener
        placeEventListener = new PlaceEventListener() {

            @Override
            public void onVisitStart(Visit visit) {
                Log.i("Info:", "Enter: " + visit.getPlace().getName() + ", at: " + new Date(visit.getArrivalTimeInMillis()));
                addEvent(new GimbalEvent(TYPE.PLACE_ENTER, visit.getPlace().getName(), new Date(visit.getArrivalTimeInMillis())));
            }

            @Override
            public void onVisitEnd(Visit visit) {
                Log.i("Info:", "Exit: " + visit.getPlace().getName() + ", at: " + new Date(visit.getDepartureTimeInMillis()));
                addEvent(new GimbalEvent(TYPE.PLACE_EXIT, visit.getPlace().getName(), new Date(visit.getDepartureTimeInMillis())));
            }
        };
        PlaceManager.getInstance().addListener(placeEventListener);






        beaconSightingListener = new BeaconEventListener() {
            @Override
            public void onBeaconSighting(BeaconSighting sighting) {
                Log.i("INFO", sighting.toString());
                Log.i(TAG, "georgewhr, get beacon sighting");
                Log.i("INFO", "RSSI value = " + sighting.getRSSI());
                Log.i("INFO", "beacon name is " + sighting.getBeacon().getName().toString());
            }
        };
        beaconManager = new BeaconManager();
        beaconManager.addListener(beaconSightingListener);

        beaconManager.startListening();




        // Setup CommunicationListener.
        communicationListener = new CommunicationListener() {
            @Override
            public Collection<Communication> presentNotificationForCommunications(Collection<Communication> communications, Visit visit) {
                for (Communication comm : communications) {
                    if (visit.getDepartureTimeInMillis() == 0L) {
                        addEvent(new GimbalEvent(TYPE.COMMUNICATION_ENTER, comm.getTitle(), new Date(visit.getArrivalTimeInMillis())));
                    }
                    else {
                        addEvent(new GimbalEvent(TYPE.COMMUNICATION_EXIT, comm.getTitle(), new Date(visit.getDepartureTimeInMillis())));
                    }
                    Log.i("INFO", "Place Communication: " + visit.getPlace().getName() + ", message: " + comm.getTitle());
                }

                // let the SDK post notifications for the communicates

                return communications;
            }

            @Override
            public Collection<Communication> presentNotificationForCommunications(Collection<Communication> communications, Push push) {
                for (Communication communication : communications) {


                    if (push.getPushType() == PushType.INSTANT) {
                        Log.i("INFO", "georgewhr, Received an Instant PushComm message: " + communication.getTitle());
                        addEvent(new GimbalEvent(TYPE.COMMUNICATION_INSTANT_PUSH, communication.getTitle(), new Date()));
                    }
                    else {

                        Log.i("INFO", "georgewhr, Received an Non-Instant PushComm message: " + communication.getTitle());
                        addEvent(new GimbalEvent(TYPE.COMMUNICATION_TIME_PUSH, communication.getTitle(), new Date()));
                    }

                }

                // let the SDK post notifications for the communicates
                return communications;
            }

            @Override
            public void onNotificationClicked(List<Communication> communications) {
                for (Communication communication : communications) {
                    Log.i("INFO", "Notification was clicked on, in loop");
                    addEvent(new GimbalEvent(TYPE.NOTIFICATION_CLICKED, communication.getTitle(), new Date()));
                }
                Log.i("INFO", "Notification was clicked on, out of for loop");
            }
        };
        CommunicationManager.getInstance().addListener(communicationListener);
    }

    private void addEvent(GimbalEvent event) {
        while (events.size() >= MAX_NUM_EVENTS) {
            events.removeLast();
        }
        events.add(0, event);
        GimbalDAO.setEvents(getApplicationContext(), events);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        PlaceManager.getInstance().removeListener(placeEventListener);
        CommunicationManager.getInstance().removeListener(communicationListener);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}

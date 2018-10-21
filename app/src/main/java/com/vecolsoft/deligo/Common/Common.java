package com.vecolsoft.deligo.Common;

import android.location.Location;

import com.vecolsoft.deligo.Modelo.Rider;
import com.vecolsoft.deligo.Remote.FCMClient;
import com.vecolsoft.deligo.Remote.IFCMService;

public class Common {

    public static final String driver_tbl = "Drivers";
    public static final String user_driver_tbl = "DriversInformation";
    public static final String user_rider_tbl  = "RidersInformation";
    public static final String pickup_request_tbl = "PickupResquest";
    public static final String token_tbl = "Tokens";

    public static Rider CurrentUser;

    public static Location MyLocation =  null;

    public static boolean isDriverFound = false;
    public static boolean CuandoEncuentra = false;
    public static boolean onService = false;

    public static final int PICK_IMAGE_REQUEST = 9999;


    public static final String fcmURL = "https://fcm.googleapis.com/";
    public static IFCMService getFCMService()
    {
        return FCMClient.getClient(fcmURL).create(IFCMService.class);
    }



}

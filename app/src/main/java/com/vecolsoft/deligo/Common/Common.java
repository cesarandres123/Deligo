package com.vecolsoft.deligo.Common;

import com.vecolsoft.deligo.Modelo.Rider;
import com.vecolsoft.deligo.Remote.FCMClient;
import com.vecolsoft.deligo.Remote.GetGson;
import com.vecolsoft.deligo.Remote.IFCMService;
import com.vecolsoft.deligo.Remote.RetrofitClient;

public class Common {

    public static final String driver_tbl = "Drivers";
    public static final String user_driver_tbl = "DriversInformation";
    public static final String user_rider_tbl  = "RidersInformation";
    public static final String pickup_request_tbl = "PickupResquest";
    public static final String token_tbl = "Tokens";

    public static Rider CurrentUser;

    private static final String BASE_URL = "https://api.mapbox.com";
    public static GetGson getGson()
    {
        return RetrofitClient.getClient(BASE_URL).create(GetGson.class);
    }

    public static final String fcmURL = "https://fcm.googleapis.com/";
    public static IFCMService getFCMService()
    {
        return FCMClient.getClient(fcmURL).create(IFCMService.class);
    }



}

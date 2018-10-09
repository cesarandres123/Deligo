package com.vecolsoft.deligo.Remote;

import com.vecolsoft.deligo.Modelo.FCMResponse;
import com.vecolsoft.deligo.Modelo.Sender;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface IFCMService {

    @Headers({
            "Content-Type:application/json",
            "Authorization:key=AAAA0lwwqlg:APA91bEgynXr9-pwyXGw8TQIdzr4yLDxxQhsE3iMRH4DQn7ZLn5RIT-1BAhKlIt7_cbpmrRxke1_K3WUrn9ZcnggAWLkvOvGuBDT2m4o09bCymud-pZra3KhxgxuVAElov7I_NfXaGOWGHIPBaH1laNAVGUqWe64tg"
    })
    @POST("fcm/send")
    Call<FCMResponse> sendMessage(@Body Sender body);
}

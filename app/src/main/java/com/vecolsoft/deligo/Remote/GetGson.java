package com.vecolsoft.deligo.Remote;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

public interface GetGson {

    @GET
    Call<String> getPath(@Url String url);
}

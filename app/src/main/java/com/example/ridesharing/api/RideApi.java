package com.example.ridesharing.api;

import com.example.ridesharing.dto.RideRequestDTO;
import com.example.ridesharing.dto.BaseResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface RideApi {
    @POST("rides") // Replace with the actual backend endpoint
    Call<BaseResponse<Void>> createRide(@Body RideRequestDTO rideRequest);
}

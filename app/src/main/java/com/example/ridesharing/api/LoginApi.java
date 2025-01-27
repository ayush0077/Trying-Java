package com.example.ridesharing.api;

import com.example.ridesharing.LoginRequest;
import com.example.ridesharing.dto.BaseResponse;
import com.example.ridesharing.dto.LoginResponse;
import com.example.ridesharing.dto.RideRequestDTO;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface LoginApi {

    @POST("auth/login")
    Call<BaseResponse<LoginResponse>> login(@Body LoginRequest loginRequest);


    @POST("api/requests/create")
    Call<BaseResponse<RideRequestDTO>> create(@Body RideRequestDTO rideRequestDTO);


}

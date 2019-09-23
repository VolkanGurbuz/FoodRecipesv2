package com.codingwithmitch.foodrecipes.requests.responses;

import java.io.IOException;

import retrofit2.Response;

public class ApiResponse<T> {

  public ApiResponse<T> create(Throwable error) {
    return new ApiErrorResponse<>(
        !error.getMessage().equals("") ? error.getMessage() : "Unknown error ! ");
  }

  public ApiResponse<T> create(Response<T> response) {
    if (response.isSuccessful()) {
      T body = response.body();

      if (body == null || response.code() == 204) { // 204 empty response code
        return new ApiEmptyResponse<>();

      } else {

        return new ApiSuccessResponse<>(body);
      }
    } else {
      String errMsg = "";

      try {
        errMsg = response.errorBody().string();
      } catch (IOException e) {
        e.printStackTrace();
        errMsg = e.getMessage();
      }

      return new ApiErrorResponse<>(errMsg);
    }
  }

  public class ApiSuccessResponse<T> extends ApiResponse<T> {
    private T body;

    ApiSuccessResponse(T body) {
      this.body = body;
    }

    public T getBody() {
      return body;
    }
  }

  public class ApiErrorResponse<T> extends ApiResponse {

    private String errorMessage;

    public ApiErrorResponse(String errorMessage) {
      this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
      return errorMessage;
    }
  }

  public class ApiEmptyResponse<T> extends ApiResponse<T> {}
}

package com.codingwithmitch.foodrecipes.util;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.lifecycle.Observer;
import android.nfc.Tag;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.codingwithmitch.foodrecipes.AppExecutors;
import com.codingwithmitch.foodrecipes.models.Recipe;
import com.codingwithmitch.foodrecipes.requests.responses.ApiResponse;

import java.util.List;

// CacheObject: Type for the Resource data. (database cache)
// RequestObject: Type for the API response. (network request)
public abstract class NetworkBoundResource<CacheObject, RequestObject> {
  private AppExecutors appExecutors;
  private MediatorLiveData<Resource<CacheObject>> results = new MediatorLiveData<>();
  public static final String TAG = "NetworkBoundResource";

  public NetworkBoundResource(AppExecutors appExecutors) {
    this.appExecutors = appExecutors;
    init();
  }

  private void init() {
    // update livedate for loading status
    results.setValue((Resource<CacheObject>) Resource.loading(null));
    // observe livedata source from local database
    /*
    * obsoerve localdb

    * stp observinf the local d
    */
    final LiveData<CacheObject> dbSource = loadFromDb();

    // if data from dbsource we run this observe
    results.addSource(
        dbSource,
        new Observer<CacheObject>() {
          @Override
          public void onChanged(@Nullable CacheObject cacheObject) {

            results.removeSource(dbSource);

            // decide whether or not catch source

            // if query  the netwoek
            if (shouldFetch(cacheObject)) {
              // get data from network
              fetchFromNetwork(dbSource);

            } else {

              results.addSource(
                  dbSource,
                  new Observer<CacheObject>() {
                    @Override
                    public void onChanged(@Nullable CacheObject cacheObject) {

                      setValue(Resource.success(cacheObject));
                    }
                  });
            }
          }
        });
  }

  /*
  * obsoerve localdb
  * if query  the netwoek
  * stp observinf the local db
  * insert new data into local db
  * begin observing local db again to see the refreshed data from network
  @param dbSource
   */

  private void fetchFromNetwork(final LiveData<CacheObject> dbSource) {

    // UPDATE LIVEDATA FOR LOADING STATUS

    results.addSource(
        dbSource,
        new Observer<CacheObject>() {
          @Override
          public void onChanged(@Nullable CacheObject cacheObject) {
            setValue(Resource.loading(cacheObject));
          }
        });

    final LiveData<ApiResponse<RequestObject>> apiResponse = createCall();

    results.addSource(
        apiResponse,
        new Observer<ApiResponse<RequestObject>>() {
          @Override
          public void onChanged(
              @Nullable final ApiResponse<RequestObject> requestObjectApiResponse) {
            results.removeSource(dbSource);
            results.removeSource(apiResponse);

            // three cases
            // api 1 success, 2 error , 3 empty

            if (requestObjectApiResponse instanceof ApiResponse.ApiSuccessResponse) {
              Log.d(TAG, "API SUCCESS");
              appExecutors.mDiskIO.execute(
                  new Runnable() {
                    @Override
                    public void run() {
                      // save the response to the local db
                      saveCallResult(
                          (RequestObject)
                              processResponse(
                                  (ApiResponse.ApiSuccessResponse.ApiSuccessResponse)
                                      requestObjectApiResponse));
                      appExecutors.mMainThreadExecutor.execute(
                          new Runnable() {
                            @Override
                            public void run() {
                              results.addSource(
                                  loadFromDb(),
                                  new Observer<CacheObject>() {
                                    @Override
                                    public void onChanged(@Nullable CacheObject cacheObject) {
                                      setValue(Resource.success(cacheObject));
                                    }
                                  });
                            }
                          });
                    }
                  });
            } else if (requestObjectApiResponse instanceof ApiResponse.ApiEmptyResponse) {
              Log.d(TAG, "API ApiEmptyResponse");

              appExecutors.mMainThreadExecutor.execute(
                  new Runnable() {
                    @Override
                    public void run() {
                      results.addSource(
                          loadFromDb(),
                          new Observer<CacheObject>() {
                            @Override
                            public void onChanged(@Nullable CacheObject cacheObject) {
                              setValue(Resource.success(cacheObject));
                            }
                          });
                    }
                  });

            } else if (requestObjectApiResponse instanceof ApiResponse.ApiErrorResponse) {
              Log.d(TAG, "onChanged: ApiErrorResponse.");
              results.addSource(
                  dbSource,
                  new Observer<CacheObject>() {
                    @Override
                    public void onChanged(@Nullable CacheObject cacheObject) {
                      setValue(
                          Resource.error(
                              ((ApiResponse.ApiErrorResponse) requestObjectApiResponse)
                                  .getErrorMessage(),
                              cacheObject));
                    }
                  });
            }
          }
        });
  }

  private CacheObject processResponse(ApiResponse.ApiSuccessResponse response) {
    return (CacheObject) response.getBody();
  }

  private void setValue(Resource<CacheObject> newValue) {
    if (results.getValue() != newValue) {
      results.setValue(newValue);
    }
  }

  // Called to save the result of the API response into the database.
  @WorkerThread
  protected abstract void saveCallResult(@NonNull RequestObject item);

  // Called with the data in the database to decide whether to fetch
  // potentially updated data from the network.
  @MainThread
  protected abstract boolean shouldFetch(@Nullable CacheObject data);

  // Called to get the cached data from the database.
  @NonNull
  @MainThread
  protected abstract LiveData<CacheObject> loadFromDb();

  // Called to create the API call.
  @NonNull
  @MainThread
  protected abstract LiveData<ApiResponse<RequestObject>> createCall();

  // Returns a LiveData object that represents the resource that's implemented
  // in the base class.
  public final LiveData<Resource<CacheObject>> getAsLiveData() {
    return results;
  };
}

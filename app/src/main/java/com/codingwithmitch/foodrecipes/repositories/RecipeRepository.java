package com.codingwithmitch.foodrecipes.repositories;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.codingwithmitch.foodrecipes.AppExecutors;
import com.codingwithmitch.foodrecipes.models.Recipe;
import com.codingwithmitch.foodrecipes.persistence.RecipeDao;
import com.codingwithmitch.foodrecipes.persistence.RecipeDatabase;
import com.codingwithmitch.foodrecipes.requests.ServiceGenerator;
import com.codingwithmitch.foodrecipes.requests.responses.ApiResponse;
import com.codingwithmitch.foodrecipes.requests.responses.RecipeSearchResponse;
import com.codingwithmitch.foodrecipes.util.Constants;
import com.codingwithmitch.foodrecipes.util.NetworkBoundResource;
import com.codingwithmitch.foodrecipes.util.Resource;

import java.util.List;

public class RecipeRepository {

  private static RecipeRepository instance;
  private RecipeDao recipeDao;

  public static RecipeRepository getInstance(Context context) {
    if (instance == null) {
      instance = new RecipeRepository(context);
    }

    return instance;
  }

  public RecipeRepository(Context context) {
    recipeDao = RecipeDatabase.getInstance(context).getRecipeDao();
  }

  public LiveData<Resource<List<Recipe>>> searchRecipesApi(
      final String query, final int pageNumber) {
    return new NetworkBoundResource<List<Recipe>, RecipeSearchResponse>(
        AppExecutors.getInstance()) {
      // look at the cache it will decide it is network request or not, and the nget the data update
      // the cache
      @Override
      public void saveCallResult(@NonNull RecipeSearchResponse item) {}

      // decide whether or net fetch the cache
      @Override
      public boolean shouldFetch(@Nullable List<Recipe> data) {
        return true; // always query the network since the queries can be anything
      }

      // retrieving data from local cache call searchRecipes from db
      @NonNull
      @Override
      public LiveData<List<Recipe>> loadFromDb() {
        return recipeDao.searchRecipes(query, pageNumber);
      }
      // creating retrofit call object and creating call from api
      @NonNull
      @Override
      public LiveData<ApiResponse<RecipeSearchResponse>> createCall() {
        return ServiceGenerator.getRecipeApi()
            .searchRecipe(Constants.API_KEY, query, String.valueOf(pageNumber));
      }
    }.getAsLiveData();
  }
}

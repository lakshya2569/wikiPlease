package com.example.wikinow.data

import android.view.textclassifier.TextLanguage
import com.example.wikinow.data.model.Article
import com.example.wikinow.data.model.FeaturedResponse
import com.example.wikinow.data.model.SearchResponse
import com.example.wikinow.data.model.Tfa
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface Api
{
    @GET("feed/v1/wikipedia/{language}/featured/{year}/{month}/{day}")
    suspend fun getFeaturedArticle(
        @Path("year") year: String,
        @Path("month") month: String,
        @Path("day") day: String,
        @Path("language") language: String
    ): FeaturedResponse


    companion object{
        const val BASE_URL = "https://api.wikimedia.org/"
    }


        // Add the new Core API search endpoint
        @GET("core/v1/wikipedia/{language}/search/page")
        suspend fun searchPages(
            @Path("language") language: String,
            @Query("q") query: String,
            @Query("limit") limit: Int = 10
        ): SearchResponse

    }



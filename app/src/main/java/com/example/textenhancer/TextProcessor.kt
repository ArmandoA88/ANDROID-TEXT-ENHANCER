package com.example.textenhancer

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

data class OpenAiRequest(val model: String, val messages: List<Message>)
data class Message(val role: String, val content: String)
data class OpenAiResponse(val choices: List<Choice>)
data class Choice(val message: Message)

interface OpenAiApi {
    @Headers("Content-Type: application/json")
    @POST("v1/chat/completions")
    suspend fun getCompletion(@Body request: OpenAiRequest): OpenAiResponse
}

class TextProcessor(private val apiKey: String) {
    private val api: OpenAiApi

    init {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $apiKey")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(OpenAiApi::class.java)
    }

    suspend fun enhance(text: String, tone: String, length: Int, language: String = "Auto"): String {
        return withContext(Dispatchers.IO) {
            try {
                // length is now the target word count (e.g. 10, 20, 50, etc)
                var languageInstruction = ""
                if (language != "Auto" && language != "Auto (Same as Input)") {
                    languageInstruction = "Translate the text to $language. "
                } else {
                    languageInstruction = "Keep the same language as the input. "
                }

                val systemPrompt = "You are a helpful assistant that rewrites text. " +
                        "Tone: $tone. " +
                        "Target Length: Approximately $length words. " +
                        languageInstruction +
                        "Return ONLY the rewritten text, nothing else."

                val messages = listOf(
                    Message("system", systemPrompt),
                    Message("user", text)
                )
                val response = api.getCompletion(OpenAiRequest("gpt-3.5-turbo", messages))
                response.choices.firstOrNull()?.message?.content?.trim() ?: text
            } catch (e: Exception) {
                Log.e("TextProcessor", "Error enhancing text", e)
                throw e // Propagate to Service for Toast
            }
        }
    }
}

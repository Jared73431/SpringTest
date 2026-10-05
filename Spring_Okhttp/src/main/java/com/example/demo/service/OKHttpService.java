package com.example.demo.service;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Service
public class OKHttpService {

    @Autowired
    private OkHttpClient okHttpClient;

    @Autowired
    private Gson gson;

    @Value("${jsonplaceholder.base-url:https://jsonplaceholder.typicode.com}")
    private String baseUrl;

    // JSON 數據模型類
    static class Post {
        private int userId;
        private int id;
        private String title;
        private String body;

        // 建構子
        public Post() {}

        public Post(int userId, String title, String body) {
            this.userId = userId;
            this.title = title;
            this.body = body;
        }

        // Getter 和 Setter 方法
        public int getUserId() { return userId; }
        public void setUserId(int userId) { this.userId = userId; }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }

        @Override
        public String toString() {
            return "Post{" +
                    "userId=" + userId +
                    ", id=" + id +
                    ", title='" + title + '\'' +
                    ", body='" + body + '\'' +
                    '}';
        }
    }

    /**
     * GET 請求範例
     */
    public void performGetRequest() {
        System.out.println("📥 執行 GET 請求...");
        System.out.println("使用 Spring Boot 管理的 OkHttpClient\n");

        String url = baseUrl + "/posts/1";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", "Spring-Boot-OKHttp-App/1.0")
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String responseBody = response.body().string();

                System.out.println("✅ GET 請求成功！");
                System.out.println("狀態碼: " + response.code());
                System.out.println("響應標頭數量: " + response.headers().size());

                // 使用 Gson 解析 JSON
                Post post = gson.fromJson(responseBody, Post.class);
                System.out.println("\n📄 解析後的文章對象:");
                System.out.println(post);

            } else {
                System.out.println("❌ GET 請求失敗！");
                System.out.println("狀態碼: " + response.code());
                System.out.println("錯誤信息: " + response.message());
            }

        } catch (IOException e) {
            System.out.println("❌ 網路錯誤: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * POST 請求範例
     */
    public void performPostRequest() {
        System.out.println("📤 執行 POST 請求...");
        System.out.println("使用 Spring Boot + LoggingInterceptor\n");

        String url = baseUrl + "/posts";

        // 創建要發送的數據
        Post newPost = new Post(1, "Spring Boot + OKHttp 測試", "這是在 Spring Boot 中使用 OKHttp 發送的 POST 請求");
        String jsonData = gson.toJson(newPost);

        System.out.println("📝 要發送的數據: " + jsonData + "\n");

        // 創建請求體
        RequestBody body = RequestBody.create(
                jsonData,
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", "Spring-Boot-OKHttp-App/1.0")
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String responseBody = response.body().string();

                System.out.println("\n✅ POST 請求成功！");
                System.out.println("狀態碼: " + response.code());
                System.out.println("響應標頭數量: " + response.headers().size());

                // 解析響應
                Post createdPost = gson.fromJson(responseBody, Post.class);
                System.out.println("\n📄 伺服器創建的文章:");
                System.out.println(createdPost);

            } else {
                System.out.println("❌ POST 請求失敗！");
                System.out.println("狀態碼: " + response.code());
                System.out.println("錯誤信息: " + response.message());
            }

        } catch (IOException e) {
            System.out.println("❌ 網路錯誤: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 異步請求範例
     */
    public void performAsyncRequest() {
        System.out.println("🔄 執行異步請求...");

        Request request = new Request.Builder()
                .url(baseUrl + "/posts/1")
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                System.out.println("❌ 異步請求失敗: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    System.out.println("✅ 異步請求成功！");
                    System.out.println("響應: " + response.body().string());
                }
                response.close();
            }
        });
    }
}

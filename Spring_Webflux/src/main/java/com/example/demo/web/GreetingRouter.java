package com.example.demo.web;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * 寫法二：Functional Endpoint。路由是一般的程式碼（RouterFunction），處理函式接收 ServerRequest、回傳 Mono&lt;ServerResponse&gt;。
 *
 * <p>
 * 與 Annotated Controller 功能相同。優點是路由集中、可以用程式組合，不靠反射與註解；
 * 缺點是參數取得、驗證都要自己寫。團隊熟悉 MVC 的話，用 Annotated Controller 通常比較容易維護。
 */
@Configuration
public class GreetingRouter {

	@Bean
	RouterFunction<ServerResponse> greetingRoutes(GreetingService greetingService) {
		return route(GET("/api/fn/greetings/{name}"),
				request -> greetingService.greet(request.pathVariable("name"))
						.flatMap(greeting -> ServerResponse.ok().bodyValue(greeting)));
	}
}

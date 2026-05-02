package com.queroassistir.backend.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("QueroAssistir API")
                        .version("1.0.0")
                        .description("API oficial do projeto QueroAssistir, responsável por orquestrar a recomendação de filmes utilizando Inteligência Artificial (Google Gemini) e TMDB.")
                        .contact(new Contact()
                                .name("Suporte QueroAssistir")
                                .email("suporte@queroassistir.com"))
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")));
    }
}

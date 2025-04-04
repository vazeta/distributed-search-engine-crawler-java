package com.example.googol;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;

import javax.servlet.Servlet;

// Assuming Example is a custom servlet, make sure it implements javax.servlet.Servlet
class Example implements Servlet {
    // Implement required methods for Servlet
    @Override
    public void init(javax.servlet.ServletConfig config) throws javax.servlet.ServletException {
        // Initialization logic
    }

    @Override
    public void service(javax.servlet.ServletRequest req, javax.servlet.ServletResponse res) throws javax.servlet.ServletException, java.io.IOException {
        // Service logic
    }

    @Override
    public void destroy() {
        // Cleanup logic
    }

    @Override
    public javax.servlet.ServletConfig getServletConfig() {
        return null;
    }

    @Override
    public String getServletInfo() {
        return null;
    }
}

@SpringBootApplication
public class ServingWebContentApplication {

    @Bean
    public ServletRegistrationBean<Example> exampleServletBean() {
        ServletRegistrationBean<Example> bean = new ServletRegistrationBean<>(new Example(), "/exampleServlet/*");
        bean.setLoadOnStartup(1);
        return bean;
    }

    public static void main(String[] args) {
        SpringApplication.run(ServingWebContentApplication.class, args);
    }
}

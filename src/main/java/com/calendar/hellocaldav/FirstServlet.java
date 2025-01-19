package com.calendar.hellocaldav;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(name = "FirstServlet", urlPatterns = "/first")
public class FirstServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("hello first servlet");
        super.doGet(req, resp);
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        System.out.println("hello init");
        super.init(config);
    }
}
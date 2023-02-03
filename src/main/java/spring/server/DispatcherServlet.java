package spring.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;

public class DispatcherServlet extends HttpServlet {
    public Map<String, Map.Entry<Method, Object>> mappings = new HashMap<>();
    public Map<Pattern, Map.Entry<Method, Object>> starMappings = new HashMap<>(); // posts/*/comments, posts/*

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        PrintWriter writer = resp.getWriter();
        String method = req.getMethod();
        if (!mappings.containsKey(method + pathInfo))
            return;

        Map.Entry<Method, Object> methodObjectEntry = mappings.get(method + pathInfo);
        processRequest(methodObjectEntry, writer);
    }

    private void processRequest(Map.Entry<Method, Object> methodObjectEntry, PrintWriter writer) throws ServletException {
        Object instance = methodObjectEntry.getValue();
        Method method = methodObjectEntry.getKey();
        try {

            Object invokedMethod = method.invoke(instance);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String s = gson.toJson(invokedMethod);
            writer.println(s);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new ServletException(e);
        }
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException {

    }

    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException {

    }

    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException {

    }
}

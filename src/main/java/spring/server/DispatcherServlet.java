package spring.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import spring.MethodHandler;
import spring.annotation.PathVariable;
import spring.annotation.RequestBody;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.regex.Pattern;

public class DispatcherServlet extends HttpServlet {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public Map<String, MethodHandler> mappings = new HashMap<>();
    public Map<Pattern, MethodHandler> starMappings = new HashMap<>(); // posts/*/comments, posts/*

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = buildPath(req);
        PrintWriter writer = resp.getWriter();
        processRequest(path, writer, req);
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = buildPath(req);
        PrintWriter writer = resp.getWriter();
        processRequest(path, writer, req);
    }

    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = buildPath(req);
        PrintWriter writer = resp.getWriter();
        processRequest(path, writer, req);
    }

    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = buildPath(req);
        PrintWriter writer = resp.getWriter();
        processRequest(path, writer, req);
    }

    private String buildPath(HttpServletRequest req) {
        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();
        String queryString = req.getQueryString();
        StringBuilder path = new StringBuilder();
        if (servletPath != null)
            path.append(servletPath);

        if (pathInfo != null)
            path.append(pathInfo);

        if (queryString != null)
            path.append("?").append(queryString);

        return path.toString();
    }

    private void processRequest(String path, PrintWriter writer, HttpServletRequest req) throws ServletException, IOException {
        String mapping = req.getMethod() + path;
        if (!mappings.containsKey(mapping)) {
            processSpecialRequest(path, writer, req);
            return;
        }

        MethodHandler handler = mappings.get(mapping);
        Object arg = null;
        for (Parameter parameter : handler.parameters) {
            if (parameter.getDeclaredAnnotation(RequestBody.class) != null) {
                Class<?> type = parameter.getType();
                arg = gson.fromJson(new InputStreamReader(req.getInputStream()), type);
            }
        }

        Method method = handler.method;
        Object instance = handler.instance;
        try {
            Object invokedMethod = arg == null ? method.invoke(instance) : method.invoke(instance, arg);
            String methodResult = gson.toJson(invokedMethod);
            writer.println(methodResult);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new ServletException(e);
        }
    }

    private void processSpecialRequest(String path, PrintWriter writer, HttpServletRequest req) throws IOException, ServletException {
        String method = req.getMethod();
        for (Map.Entry<Pattern, MethodHandler> entry : starMappings.entrySet()) {
            Pattern pattern = entry.getKey();
            if (pattern.matcher(method + path).matches()) {
                MethodHandler methodHandler = entry.getValue();
                processReq(path, writer, pattern, methodHandler, req);
                return;
            }
        }
    }

    private void processReq(String path, PrintWriter writer, Pattern pattern, MethodHandler methodHandler, HttpServletRequest req) throws IOException, ServletException {
        String[] splitPath = path.split("/");
        String queryString = req.getQueryString();
        if (queryString != null) {
            splitPath = queryString.split("=");
            System.out.println(Arrays.toString(splitPath));
            String[] curSplitPath = new String[splitPath.length / 2];
            int counter = 0;
            for (int i = 1; i <= curSplitPath.length; i += 2) {
                curSplitPath[counter++] = splitPath[i];
            }

            splitPath = curSplitPath;
        }

        String[] patternSplit = pattern.toString().substring(req.getMethod().length()).split("/");
        Object[] methodArgs = getMethodArgs(splitPath, patternSplit, methodHandler, req);
        Object invokedMethod;
        try {
            invokedMethod = methodHandler.invoke(methodArgs);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new ServletException(e);
        }

        String printResponse = gson.toJson(invokedMethod);
        writer.println(printResponse);
    }


    private Object[] getMethodArgs(String[] splitPath, String[] patternSplit, MethodHandler methodHandler, HttpServletRequest req) throws IOException {
        List<Object> methodArgs = new ArrayList<>();
        List<Object> pathArgs = new ArrayList<>();
        for (int i = 0; i < splitPath.length; i++) {
            if (!Objects.equals(splitPath[i], patternSplit[i]))
                pathArgs.add(splitPath[i]);
        }

        Parameter[] parameters = methodHandler.parameters;
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            PathVariable pathAnnotation = parameter.getDeclaredAnnotation(PathVariable.class);
            if (pathAnnotation != null) {
                Class<?> type = parameter.getType();
                Object arg = pathArgs.get(i);
                addParameters(arg.toString(), methodArgs, type);
            }

            RequestBody requestBodyAnnotation = parameter.getDeclaredAnnotation(RequestBody.class);
            if (requestBodyAnnotation != null) {
                Class<?> type = parameter.getType();
                Object object = gson.fromJson(new InputStreamReader(req.getInputStream()), type);
                methodArgs.add(object);
            }
        }

        return methodArgs.toArray();
    }

    private static void addParameters(String value, List<Object> methodArgs, Class<?> parameterType) {
        System.out.println(value);
        if (parameterType.equals(Byte.class) || parameterType.equals(byte.class)) {
            byte parsedByte = Byte.parseByte(value);
            methodArgs.add(parsedByte);
        } else if (parameterType.equals(Short.class) || parameterType.equals(short.class)) {
            short parsedShort = Short.parseShort(value);
            methodArgs.add(parsedShort);
        } else if (parameterType.equals(Integer.class) || parameterType.equals(int.class)) {
            int parsedInt = Integer.parseInt(value);
            methodArgs.add(parsedInt);
        } else if (parameterType.equals(Long.class) || parameterType.equals(long.class)) {
            long parsedLong = Long.parseLong(value);
            methodArgs.add(parsedLong);
        } else if (parameterType.equals(Float.class) || parameterType.equals(float.class)) {
            float parsedFloat = Float.parseFloat(value);
            methodArgs.add(parsedFloat);
        } else if (parameterType.equals(Double.class) || parameterType.equals(double.class)) {
            double parsedDouble = Double.parseDouble(value);
            methodArgs.add(parsedDouble);
        } else if (parameterType.equals(Character.class) || parameterType.equals(char.class)) {
            char parsedChar = value.charAt(0);
            methodArgs.add(parsedChar);
        } else if (parameterType.equals(Boolean.class) || parameterType.equals(boolean.class)) {
            boolean parsedBoolean = Boolean.getBoolean(value);
            methodArgs.add(parsedBoolean);
        } else if (parameterType.equals(String.class))
            methodArgs.add(value);
    }
}

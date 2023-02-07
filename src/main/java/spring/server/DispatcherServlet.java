package spring.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import spring.annotation.*;
import spring.config.MethodHandler;
import spring.dic.ApplicationContext;
import spring.exception.BeanCreationException;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DispatcherServlet extends HttpServlet {
    private static final Pattern PATH_PARAMS_PATTERN = Pattern.compile("\\{\\w+}");
    private static final String REPLACEMENT_PATTERN_STR = "[\\\\w]+";

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


    public void addController(Class<?> c, Object classInstance, ApplicationContext context) throws BeanCreationException, IllegalAccessException {
        RequestMapping requestMapping = c.getDeclaredAnnotation(RequestMapping.class);
        String mapping = requestMapping.value() != null ? requestMapping.value()[0] : "";
        processFields(c, classInstance, context);
        processMethods(c, classInstance, mapping);
    }

    private void processFields(Class<?> c, Object instance, ApplicationContext applicationContext) throws IllegalAccessException {
        Field[] fields = c.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            Autowired autowired = field.getDeclaredAnnotation(Autowired.class);
            if (autowired == null)
                continue;

            Object o = applicationContext.getInstance(field.getType());
            field.set(instance, o);
        }
    }

    private void processMethods(Class<?> c, Object instance, String mapping) throws BeanCreationException {
        Method[] declaredMethods = c.getDeclaredMethods();
        for (Method method : declaredMethods) {
            method.setAccessible(true);
            Annotation[] methodAnnotations = method.getDeclaredAnnotations();
            processMethodAnnotations(instance, mapping, method, methodAnnotations);
        }
    }

    private void processMethodAnnotations(Object instance, String mapping, Method method, Annotation[] annotations) throws BeanCreationException {
        for (Annotation annotation : annotations) {
            String methodStr = null;
            String[] value = switch (annotation) {
                case GetMapping getMapping -> {
                    methodStr = "GET";
                    yield getMapping.value();
                }
                case PostMapping postMapping -> {
                    methodStr = "POST";
                    yield postMapping.value();
                }
                case PutMapping putMapping -> {
                    methodStr = "PUT";
                    yield putMapping.value();
                }
                case DeleteMapping delMapping -> {
                    methodStr = "DELETE";
                    yield delMapping.value();
                }
                default -> null;
            };

            if (methodStr == null)
                continue;

            putMappings(methodStr, instance, mapping, method, value);
        }
    }

    private void putMappings(String requestMethod, Object instance, String mapping, Method method, String[] paths) throws
            BeanCreationException {
        String methodPath = requestMethod + mapping;
        if (paths == null || paths.length == 0) {
            validateMapping(methodPath, method);
            addMapping(methodPath, instance, method);
            return;
        }

        for (String path : paths) {
            String combinedPath = methodPath + path;
            validateMapping(combinedPath, method);
            addMapping(combinedPath, instance, method);
        }
    }

    private void addMapping(String combinedPath, Object instance, Method method) {
        Matcher matcher = PATH_PARAMS_PATTERN.matcher(combinedPath);
        MethodHandler methodHandler = new MethodHandler(method, instance);
        if (!matcher.find()) {
            mappings.put(combinedPath, methodHandler);
            return;
        }

        String pattern = matcher.replaceAll(REPLACEMENT_PATTERN_STR).replaceAll("\\?", "\\\\?");
        Pattern compile = Pattern.compile(pattern);
        starMappings.put(compile, methodHandler);
    }

    private void validateMapping(String mapping, Method method) throws BeanCreationException {
        if (mappings.containsKey(mapping)) {
            String message = String.format("Ambiguous mapping. Cannot map '%s' method.", method.getName());
            throw new BeanCreationException(message);
        }
    }
}

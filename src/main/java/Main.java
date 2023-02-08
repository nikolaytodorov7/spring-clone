import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class Main {
    public static void main(String[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassNotFoundException, MalformedURLException, InstantiationException {
        File file = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        URL url = file.toURI().toURL();

        URLClassLoader child = new URLClassLoader(new URL[]{url}, ClassLoader.getSystemClassLoader());
        Class<?> classToLoad = Class.forName("restapi.SpringRestApiApplication", true, child);
        Method method = classToLoad.getDeclaredMethods()[0];
        Object instance = classToLoad.getDeclaredConstructor().newInstance();
        method.invoke(instance, (Object) args);
    }
}

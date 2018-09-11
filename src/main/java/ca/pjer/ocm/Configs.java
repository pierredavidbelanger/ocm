package ca.pjer.ocm;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsSchema;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Configs {

    public static Properties read(ClassLoader classLoader, String... locations) throws Exception {
        Properties properties = new Properties();
        for (String location : locations) {
            load(classLoader, URI.create(location), properties);
        }
        return properties;
    }

    public static <T> T read(Class<T> type, String... locations) throws Exception {
        return read(type.getClassLoader(), type, locations);
    }

    public static <T> T read(ClassLoader classLoader, Class<T> type, String... locations) throws Exception {
        Properties properties = read(classLoader, locations);
        JavaPropsMapper javaPropsMapper = new JavaPropsMapper();
        javaPropsMapper.setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CASE);
        javaPropsMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JavaPropsSchema javaPropsSchema = new JavaPropsSchema();
        return javaPropsMapper.readPropertiesAs(properties, javaPropsSchema, type);
    }

    private static void load(ClassLoader classLoader, URI uri, Properties properties) throws Exception {
        if (uri.getScheme().equalsIgnoreCase("env")) {
            loadFromEnv(classLoader, uri, properties);
        } else {
            loadFromFile(classLoader, uri, properties);
        }
    }

    private static final Pattern pattern = Pattern.compile("(_)\\d+(_)");

    private static void loadFromEnv(ClassLoader classLoader, URI uri, Properties properties) throws Exception {
        for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
            StringBuilder key = new StringBuilder(entry.getKey());
            Matcher matcher = pattern.matcher(key);
            while (matcher.find()) {
                key.replace(matcher.start(1), matcher.end(1), "[");
                key.replace(matcher.start(2), matcher.end(2), "].");
            }
            int idx;
            while ((idx = key.indexOf("_")) != -1) {
                key.replace(idx, idx + 1, ".");
            }
            properties.setProperty(key.toString().toLowerCase(), entry.getValue());
        }
    }

    private static void loadFromFile(ClassLoader classLoader, URI uri, Properties properties) throws Exception {
        InputStream inputStream = stream(classLoader, uri);
        if (inputStream != null) {
            Properties tmp = new Properties();
            tmp.load(inputStream);
            for (Map.Entry<Object, Object> entry : tmp.entrySet()) {
                properties.setProperty(String.valueOf(entry.getKey()).toLowerCase(), String.valueOf(entry.getValue()));
            }
        }
    }

    private static InputStream stream(ClassLoader classLoader, URI uri) throws Exception {
        if (uri.getScheme().equalsIgnoreCase("classpath")) {
            return classLoader.getResourceAsStream(uri.getSchemeSpecificPart());
        } else if (uri.getScheme().equalsIgnoreCase("file")) {
            File file = new File(uri.getSchemeSpecificPart());
            if (!file.exists()) {
                return null;
            }
            return new FileInputStream(file);
        }
        return null;
    }
}

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.*;

class Launcher {
    private static String mainClassName = "Main";
    private static String mainMethodName = "main";
    private static Class mainClass = null;
    private static Method mainMethod = null;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void augmentEnv(Map<String, String> newEnv) {
        try {
            for (Class cl : Collections.class.getDeclaredClasses()) {
                if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                    Field field = cl.getDeclaredField("m");
                    field.setAccessible(true);
                    Object obj = field.get(System.getenv());
                    Map<String, String> map = (Map<String, String>) obj;
                    map.putAll(newEnv);
                }
            }
        } catch (Exception e) {}
    }

    private static void parseArgs(String[] args) throws Exception {
        if(args.length > 0)
            mainClassName = args[0];
        int pos = mainClassName.indexOf("#");
        if(pos != -1) {
            mainMethodName = args[0].substring(pos+1);
            mainClassName = args[0].substring(0,pos);
        }
        mainClass = Class.forName(mainClassName);
        Method m = mainClass.getMethod(mainMethodName, new Class[] { JsonObject.class });
        m.setAccessible(true);
        int modifiers = m.getModifiers();
        if (m.getReturnType() != JsonObject.class || !Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers)) {
            throw new NoSuchMethodException(mainMethodName);
        }
        mainMethod = m;
    }

    private static JsonObject invokeMain(JsonObject arg, Map<String, String> env) throws Exception {
        augmentEnv(env);
        return (JsonObject) mainMethod.invoke(null, arg);
    }

    public static void main(String[] args) throws Exception {

        parseArgs(args);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(System.in, "UTF-8"));
        PrintWriter out = new PrintWriter(
                new OutputStreamWriter(
                        new FileOutputStream("/dev/fd/3"), "UTF-8"));
        JsonParser json = new JsonParser();
        JsonObject error = json.parse(
                "{\"error\":\"not an object\"}").getAsJsonObject();
        JsonObject empty = json.parse("{}").getAsJsonObject();
        String input = "";
        JsonElement output = error;
        while (true) {
            try {
                input = in.readLine();
                if (input == null)
                    break;
                JsonElement element = json.parse(input);
                JsonObject payload = empty.deepCopy();
                HashMap<String, String> env = new HashMap<String, String>();
                if (element.isJsonObject()) {
                    // collect payload and environment
                    for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                        if (entry.getKey().equals("value")) {
                            if (entry.getValue().isJsonObject())
                                payload = entry.getValue().getAsJsonObject();
                        } else {
                            env.put(String.format("__OW_%s", entry.getKey().toUpperCase()),
                                    entry.getValue().getAsString());
                        }
                    }
                    augmentEnv(env);
                }
                output =invokeMain(payload, env);
                out.println(output.toString());
                out.flush();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
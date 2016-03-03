package dk.aakb.itk.brilleappen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

abstract class Util {
    static Map getValues(String json) {
        Type type = new TypeToken<Map>(){}.getType();
        return new Gson().fromJson(json, type);
    }

    static Object getDrupalValue(Map values, String key) {
        try {
            List list = (List)values.get(key);
            Map map = (Map)list.get(0);
            return map.get("value");
        } catch (Exception e) {}
        return null;
    }

    static String toJson(Object value) {
        Gson gson = new GsonBuilder().serializeNulls().create();
        return gson.toJson(value);
    }
}

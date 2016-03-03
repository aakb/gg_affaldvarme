package dk.aakb.itk.brilleappen;

import com.google.gson.internal.LinkedTreeMap;

import java.util.Map;

public class ContactPerson {
    public final String name;
    public final String phone;
    public final String email;

    ContactPerson(String json) {
        Map values = Util.getValues(json);
        this.name = (String)values.get("name");
        this.phone = (String)values.get("telephone");
        this.email = (String)values.get("email");
    }

    ContactPerson(LinkedTreeMap values) {
        this.name = (String)values.get("name");
        this.phone = (String)values.get("telephone");
        this.email = (String)values.get("email");
    }
}

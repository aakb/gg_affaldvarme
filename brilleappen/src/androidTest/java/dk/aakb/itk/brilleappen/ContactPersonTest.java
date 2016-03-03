package dk.aakb.itk.brilleappen;

import junit.framework.TestCase;

import java.util.Map;

public class ContactPersonTest extends TestCase {
    public void testConstruct() {
        String json = "{\"name\":\"Anders And\",\"email\":\"aand@andeby.dk\",\"telephone\":\"12345678\"}";

        ContactPerson contactPerson = new ContactPerson(json);
        assertNotNull(contactPerson);
        assertEquals("Anders And", contactPerson.name);
        assertEquals("aand@andeby.dk", contactPerson.email);
        assertEquals("12345678", contactPerson.phone);

        Map values = Util.getValues(json);
        contactPerson = new ContactPerson(json);
        assertNotNull(contactPerson);
        assertEquals("Anders And", contactPerson.name);
        assertEquals("aand@andeby.dk", contactPerson.email);
        assertEquals("12345678", contactPerson.phone);
    }
}
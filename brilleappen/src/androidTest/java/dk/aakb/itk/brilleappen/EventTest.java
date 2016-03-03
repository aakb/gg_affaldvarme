package dk.aakb.itk.brilleappen;

import junit.framework.TestCase;

public class EventTest extends TestCase {
    public void testConstructWithContactPeople() {
        String json = "{\"nid\":[{\"value\":\"1\"}],\"uuid\":[{\"value\":\"21d59ce2-fa69-426a-bd06-6e6c9edee086\"}],\"vid\":[{\"value\":\"1\"}],\"type\":[{\"target_id\":\"gg_event\"}],\"langcode\":[{\"value\":\"en\"}],\"title\":[{\"value\":\"Frokost\"}],\"uid\":[{\"target_id\":\"1\",\"url\":\"/user/1\"}],\"status\":[{\"value\":\"1\"}],\"created\":[{\"value\":\"1454409338\"}],\"changed\":[{\"value\":\"1456832561\"}],\"promote\":[{\"value\":\"1\"}],\"sticky\":[{\"value\":\"0\"}],\"revision_timestamp\":[{\"value\":\"1454409358\"}],\"revision_uid\":[{\"target_id\":\"1\",\"url\":\"/user/1\"}],\"revision_log\":[],\"revision_translation_affected\":[{\"value\":\"1\"}],\"default_langcode\":[{\"value\":\"1\"}],\"body\":[{\"value\":\"Vi skal spise. Selvsmurt er velsmurt!\",\"format\":\"plain_text\",\"summary\":\"\"}],\"field_gg_contact_people\":[{\"name\":\"Mikkel Ricky\",\"email\":\"rimi@aarhus.dk\",\"telephone\":\"+45 41 85 80 69\"},{\"name\":\"Anders And\",\"email\":\"aand@andeby.dk\",\"telephone\":\"12345678\"}],\"field_gg_email_push\":[{\"value\":\"0\"}],\"field_gg_email_recipients\":[{\"value\":\"rimi@aarhus.dk, nsp@aarhus.dk\"}],\"field_gg_geolocation\":[],\"field_gg_instagram_caption\":[{\"value\":\"Hmm \u2026 #frokost #mad\"}],\"field_gg_instagram_password\":[{\"value\":\"metallica\"}],\"field_gg_instagram_push\":[{\"value\":\"0\"}],\"field_gg_instagram_username\":[{\"value\":\"rimi.itk\"}],\"field_gg_parent\":[],\"field_gg_twitter_access_secret\":[{\"value\":\"Sh7XktEU8nv8cIkV0J1mvyu4RMEp2YkL0e4u9IWjDG3dy\"}],\"field_gg_twitter_access_token\":[{\"value\":\"4854483055-3RLryZXJwab3rWuWtQxnRDwg6wChacqpPOpDuPa\"}],\"field_gg_twitter_caption\":[{\"value\":\"#test #frokost\"}],\"field_gg_twitter_consumer_key\":[{\"value\":\"ItZIlHoLgAIs9E77A0v367KnQ\"}],\"field_gg_twitter_consumer_secret\":[{\"value\":\"WGVyZuqd1HTTbNAomtfq9yC4EDHlNyJRnMswq0WXD03WT44kL3\"}],\"field_gg_twitter_push\":[{\"value\":\"0\"}],\"field_gg_type\":[],\"add_file_url\":\"http://brilleappen.hulk.aakb.dk/brilleappen/event/21d59ce2-fa69-426a-bd06-6e6c9edee086/file\"}";

        Event event = new Event(json);
        assertNotNull(event);
        assertNotNull(event.contactPersons);
        assertEquals(2, event.contactPersons.size());
    }
}
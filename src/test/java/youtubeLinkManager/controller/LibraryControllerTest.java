package controller;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;
import youtubeLinkManager.InvalidListNameException;
import youtubeLinkManager.LibraryController;
import youtubeLinkManager.ListAlreadyExistsException;

import javax.swing.*;

class LibraryControllerTest {

    private static String apiKey;
    private static LibraryController libc;

    @BeforeAll
    static void setup() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch(Exception e) {
            System.out.println(e.toString());
            e.printStackTrace();
        }
        libc = new LibraryController();
        apiKey = "";
        while(apiKey.equals(""))
            apiKey = JOptionPane.showInputDialog("Enter an API key for testing:", JOptionPane.PLAIN_MESSAGE);
    }

    @BeforeEach
    void reset() {
        //make a new library for each test, never serialize it.
        libc.clearAll();
    }
    @AfterAll
    static void teardown() {
        //I don't think anything needs to be done here.
    }

    @Test
    void hasAPIKey() {
        assertEquals(false, libc.hasAPIKey());
        libc.setAPIKey(apiKey);
        assertEquals(true, libc.hasAPIKey());
        libc.setAPIKey("");
        assertEquals(false, libc.hasAPIKey());
    }

    @Test
    void setKey() {
        assertEquals("", libc.getAPIKey());
        libc.setAPIKey(apiKey);
        assertEquals(apiKey, libc.getAPIKey());
        libc.setAPIKey("");
        assertEquals("", libc.getAPIKey());
    }

    //Do everything that should make hasUnsavedChanges() true, and make sure they all do?
    @Test
    void hasUnsavedChanges() throws InvalidListNameException, ListAlreadyExistsException {
        libc.setUnsavedChanges(false);
        libc.setAPIKey(apiKey);
        assertEquals(true, libc.hasUnsavedChanges());

        libc.setUnsavedChanges(false);
        libc.addNewList("foo");
        assertEquals(true, libc.hasUnsavedChanges());

        libc.setUnsavedChanges(false);
        libc.addLink("foo", "https://www.youtube.com/watch?v=_YUugB4IUl4");
        assertEquals(true, libc.hasUnsavedChanges());

        libc.setUnsavedChanges(false);
        libc.addLink("foo", "https://www.youtube.com/watch?v=_YUugB4IUl4", "bar", "0:00", 0);
        assertEquals(true, libc.hasUnsavedChanges());

        libc.setUnsavedChanges(false);
        libc.removeLink("foo", 0);
        assertEquals(true, libc.hasUnsavedChanges());

        libc.setUnsavedChanges(false);
        libc.removeList("foo");
        assertEquals(true, libc.hasUnsavedChanges());

        libc.setUnsavedChanges(false);
        libc.clearAll();
        assertEquals(true, libc.hasUnsavedChanges());
    }

    @Test
    void setUnsavedChanges() {
        libc.setUnsavedChanges(false);
        assertEquals(false, libc.hasUnsavedChanges());
        libc.setUnsavedChanges(true);
        assertEquals(true, libc.hasUnsavedChanges());
    }

    @Test
    void hasList() throws InvalidListNameException, ListAlreadyExistsException {
        libc.addNewList("foo");
        assertEquals(true, libc.hasList("foo"));
        assertEquals(false, libc.hasList("bar"));
        libc.removeList("foo");
        assertEquals(false, libc.hasList("foo"));

    }

    @Test
    void getLink() throws InvalidListNameException, ListAlreadyExistsException {
        libc.setAPIKey(apiKey);
        libc.addNewList("foo");
        assertThrows(IndexOutOfBoundsException.class, () -> libc.getLink("foo", 0));
        assertThrows(IndexOutOfBoundsException.class, () -> libc.getLink("foo", 12));
        libc.addLink("foo", "https://www.youtube.com/watch?v=_YUugB4IUl4", "bar", "0:00", 0);
        assertAll("Link",
                () -> assertEquals("https://www.youtube.com/watch?v=_YUugB4IUl4",
                        libc.getLink("foo", 0).getUrl()),
                () -> assertEquals("bar", libc.getLink("foo", 0).getTitle()),
                () -> assertEquals("0:00", libc.getLink("foo", 0).getLength()));
        libc.addLink("foo", "bar", "bar", "0:00", 1);
        libc.addLink("foo", "https://www.youtube.com/watch?v=rdwz7QiG0lk");
        assertAll("Link",
                () -> assertEquals("bar", libc.getLink("foo", 1).getUrl()),
                () -> assertEquals("bar", libc.getLink("foo", 1).getTitle()),
                () -> assertEquals("https://www.youtube.com/watch?v=rdwz7QiG0lk",
                        libc.getLink("foo", 2).getUrl()),
                () -> assertEquals("YouTube on the tube!", libc.getLink("foo", 2).getTitle()),
                () -> assertEquals("7:04", libc.getLink("foo", 2).getLength()));
        assertThrows(NullPointerException.class, () -> libc.getLink("bar", 0));
    }

    @Test
    void getListNames() throws ListAlreadyExistsException, InvalidListNameException {
        assertArrayEquals(new String[]{}, libc.getListNames());
        libc.addNewList("foo");
        assertArrayEquals(new String[]{"foo"}, libc.getListNames());
        libc.addNewList("z");
        libc.addNewList("a");
        assertArrayEquals(new String[]{"a", "foo", "z"}, libc.getListNames());
    }

    @Test
    void getLinksFromCollection() throws InvalidListNameException, ListAlreadyExistsException {
        libc.addNewList("foo");
        assertEquals(0, libc.getLinksFromList("foo").size());
        libc.addLink("foo", "https://www.youtube.com/watch?v=_YUugB4IUl4", "bar", "0:00", 0);
        assertAll( "Links",
                () -> assertEquals("https://www.youtube.com/watch?v=_YUugB4IUl4",
                    libc.getLinksFromList("foo").get(0).getUrl()),
                () -> assertEquals("bar", libc.getLinksFromList("foo").get(0).getTitle()),
                () -> assertEquals("0:00", libc.getLinksFromList("foo").get(0).getLength())
        );
        libc.addLink("foo", "https://www.youtube.com/watch?v=_YUugB4IUl4", "bar", "0:00", 0);
        libc.addLink("foo", "https://www.youtube.com/watch?v=_YUugB4IUl4", "bar", "0:00", 0);
        libc.addLink("foo", "https://www.youtube.com/watch?v=_YUugB4IUl4", "bar", "0:00", 0);
        libc.addLink("foo", "url", "title", "1:00:00", 2);
        assertAll("Links",
                () -> assertEquals("url", libc.getLinksFromList("foo").get(2).getUrl()),
                () -> assertEquals("title", libc.getLinksFromList("foo").get(2).getTitle()),
                () -> assertEquals("1:00:00", libc.getLinksFromList("foo").get(2).getLength()));
        libc.removeLink("foo", 1);
        assertAll("Links",
                () -> assertEquals("url", libc.getLinksFromList("foo").get(1).getUrl()),
                () -> assertEquals("title", libc.getLinksFromList("foo").get(1).getTitle()),
                () -> assertEquals("1:00:00", libc.getLinksFromList("foo").get(1).getLength()));
        assertThrows(NullPointerException.class, () -> libc.getLinksFromList("bar"));
    }

    @Test
    void getAllCollections() throws InvalidListNameException, ListAlreadyExistsException {
        libc.setAPIKey(apiKey);
        assertEquals(0, libc.getAllLists().length);
        libc.addNewList("foo");
        assertEquals("foo", libc.getAllLists()[0].getName());
        libc.addLink("foo", "https://www.youtube.com/watch?v=rdwz7QiG0lk");
        assertEquals("YouTube on the tube!", libc.getAllLists()[0].getLink(0).getTitle());
        libc.addNewList("a");
        libc.addNewList("z");
        assertAll("Collections",
                () -> assertEquals("a", libc.getAllLists()[0].getName()),
                () -> assertEquals("foo", libc.getAllLists()[1].getName()),
                () -> assertEquals("z", libc.getAllLists()[2].getName()));
        libc.removeList("z");
        assertAll("Collections",
                () -> assertEquals("a", libc.getAllLists()[0].getName()),
                () -> assertEquals("foo", libc.getAllLists()[1].getName()));
    }

    @Test
    void getNumberOfLists() throws InvalidListNameException, ListAlreadyExistsException {
        assertEquals(0, libc.getNumberOfLists());
        libc.addNewList("foo");
        assertEquals(1, libc.getNumberOfLists());
        libc.addNewList("bar");
        libc.addNewList("foobar");
        assertEquals(3, libc.getNumberOfLists());
        libc.removeList("foobar");
        assertEquals(2, libc.getNumberOfLists());
    }

    @Test
    void getListSize() throws InvalidListNameException, ListAlreadyExistsException {
        libc.setAPIKey(apiKey);
        libc.addNewList("foo");
        assertEquals(0, libc.getListSize("foo"));
        libc.addLink("foo", "https://www.youtube.com/watch?v=rdwz7QiG0lk");
        assertEquals(1, libc.getListSize("foo"));
        libc.addLink("foo", "https://www.youtube.com/watch?v=rdwz7QiG0lk");
        libc.addLink("foo", "https://www.youtube.com/watch?v=rdwz7QiG0lk");
        assertEquals(3, libc.getListSize("foo"));
        libc.removeLink("foo", 1);
        assertEquals(2, libc.getListSize("foo"));
        assertThrows(NullPointerException.class, () -> libc.getListSize("bar"));
    }

    @Test
    void hasLink() throws InvalidListNameException, ListAlreadyExistsException {
        libc.setAPIKey(apiKey);
        libc.addNewList("foo");
        assertEquals(false, libc.hasLink("foo", "http://www.example.com"));
        libc.addLink("foo", "https://www.youtube.com/watch?v=rdwz7QiG0lk");
        assertEquals(true, libc.hasLink("foo", "https://www.youtube.com/watch?v=rdwz7QiG0lk"));
        //Don't ignore case differences, video ids are case sensitive.
        assertEquals(false, libc.hasLink("foo", "https://www.YOUTUBE.com/watch?v=rdwz7QiG0lk"));
        assertThrows(NullPointerException.class, () -> libc.hasLink("bar", "bar"));
    }

    @Test
    void addNewCollection() throws InvalidListNameException, ListAlreadyExistsException {
        //make sure it throws the exceptions when its supposed to
        libc.addNewList("foo");
        assertEquals(true, libc.hasList("foo"));
        assertEquals(false, libc.hasList("bar"));
        assertThrows(ListAlreadyExistsException.class, () -> libc.addNewList("foo"));
        assertThrows(InvalidListNameException.class, () -> libc.addNewList(""));
        assertThrows(InvalidListNameException.class, () -> libc.addNewList("@#$%^"));
    }

    @Test //addLink(String collectionName, String url)
    void addLink() throws InvalidListNameException, ListAlreadyExistsException {
        libc.setAPIKey(apiKey);
        libc.addNewList("foo");
        libc.addLink("foo","https://www.youtube.com/watch?v=rdwz7QiG0lk");
        assertThrows(NullPointerException.class,
                () -> libc.addLink("bar", "https://www.youtube.com/watch?v=_YUugB4IUl4"));
        assertAll("Link",
                () -> assertEquals("https://www.youtube.com/watch?v=rdwz7QiG0lk",
                        libc.getLink("foo", 0).getUrl()),
                () -> assertEquals("YouTube on the tube!", libc.getLink("foo", 0).getTitle()),
                () -> assertEquals("7:04", libc.getLink("foo", 0).getLength()));
        libc.addLink("foo", "https://www.youtube.com/watch?v=bRmicYiGoQ4");
        libc.addLink("foo", "https://www.youtube.com/watch?v=BPsfbV9t8Vw");
        assertAll("Link",
                () -> assertEquals("https://www.youtube.com/watch?v=BPsfbV9t8Vw",
                        libc.getLink("foo", 2).getUrl()),
                () -> assertEquals("YouTube Happy New Year", libc.getLink("foo", 2).getTitle()),
                () -> assertEquals("1:02", libc.getLink("foo", 2).getLength()));
        libc.addLink("foo", "http://www.example.com");
        assertAll("Link",
                () -> assertEquals("http://www.example.com",
                        libc.getLink("foo", 3).getUrl()),
                () -> assertEquals("Could not find video id in URL.",
                        libc.getLink("foo", 3).getTitle()),
                () -> assertEquals("Invalid time.", libc.getLink("foo", 3).getLength()));
        //test result when Google API doesn't return expected results.
        libc.setAPIKey("foobar");
        libc.addLink("foo", "https://www.youtube.com/watch?v=Y7dpJ0oseIA");
        assertAll("Link",
                () -> assertEquals("https://www.youtube.com/watch?v=Y7dpJ0oseIA",
                        libc.getLink("foo", 4).getUrl()),
                () -> assertEquals("Connection error.", libc.getLink("foo", 4).getTitle()),
                () -> assertEquals("Invalid time.", libc.getLink("foo", 4).getLength()));
        assertThrows(NullPointerException.class, () -> libc.addLink("bar", "http://www.example.com"));
        //Can't reach JSONException result.
    }

    @Test //addLink(String collectionName, String url, String title, String length, int index)
    void addLink1() throws InvalidListNameException, ListAlreadyExistsException {
        //adding to collection that doesn't exist.
        libc.addNewList("foo");
        libc.addLink("foo", "https://www.youtube.com/watch?v=_YUugB4IUl4", "bar", "0:00", 0);
        assertThrows(NullPointerException.class,
                () -> libc.addLink("bar", "https://www.youtube.com/watch?v=_YUugB4IUl4", "bar", "0:00", 0));
        assertThrows(IndexOutOfBoundsException.class,
                () -> libc.addLink("foo", "http://www.example.com", "bar", "foobar", 5));
        libc.addLink("foo", "http://www.example.com", "bar", "foobar", 1);
        libc.addLink("foo", "http://www.example.com", "foobar", "foobar", 1);
        assertAll("AddLink",
                () -> assertEquals("foobar", libc.getLink("foo", 1).getTitle()),
                () -> assertEquals("bar", libc.getLink("foo", 2).getTitle()),
                () -> assertEquals("Invalid time.", libc.getLink("foo", 2).getLength()));
        assertThrows(NullPointerException.class,
                () -> libc.addLink("bar", "http://www.example.com", "bar", "foobar", 5));
    }

    @Test
    void removeList() throws InvalidListNameException, ListAlreadyExistsException {
        libc.addNewList("foo");
        assertEquals(true, libc.hasList("foo"));
        libc.removeList("foo");
        assertEquals(false, libc.hasList("foo"));
        assertEquals(false, libc.hasList("bar"));
        libc.removeList("bar");
        assertEquals(false, libc.hasList("bar"));
    }

    @Test
    void removeLink() throws InvalidListNameException, ListAlreadyExistsException {
        libc.setAPIKey(apiKey);
        assertThrows(NullPointerException.class, () -> libc.removeLink("foo", 0));
        libc.addNewList("foo");
        assertThrows(IndexOutOfBoundsException.class, () -> libc.removeLink("foo", 0));
        libc.addLink("foo", "https://www.youtube.com/watch?v=_YUugB4IUl4");
        assertEquals(true, libc.hasLink("foo", "https://www.youtube.com/watch?v=_YUugB4IUl4"));
        libc.addLink("foo", "https://www.youtube.com/watch?v=Y7dpJ0oseIA");
        libc.addLink("foo", "https://www.youtube.com/watch?v=bRmicYiGoQ4");
        assertEquals(true, libc.hasLink("foo", "https://www.youtube.com/watch?v=Y7dpJ0oseIA"));
        assertEquals(true, libc.hasLink("foo", "https://www.youtube.com/watch?v=bRmicYiGoQ4"));
        assertThrows(IndexOutOfBoundsException.class, () -> libc.removeLink("foo", 4));
        libc.removeLink("foo", 1);
        assertEquals(false, libc.hasLink("foo", "https://www.youtube.com/watch?v=Y7dpJ0oseIA"));
        libc.removeLink("foo", 0);
        assertEquals(false, libc.hasLink("foo", "https://www.youtube.com/watch?v=_YUugB4IUl4"));
        assertEquals(true, libc.hasLink("foo", "https://www.youtube.com/watch?v=bRmicYiGoQ4"));
    }

    @Test
    void searchForTitle() throws InvalidListNameException, ListAlreadyExistsException {
        //create sample library to search
        libc.setAPIKey(apiKey);
        libc.addNewList("foo");
        libc.addNewList("bar");
        libc.addLink("foo", "https://www.youtube.com/watch?v=_YUugB4IUl4"); //Gmail Theater Act 1
        libc.addLink("foo", "https://www.youtube.com/watch?v=Y7dpJ0oseIA"); //YouTube
        libc.addLink("foo", "https://www.youtube.com/watch?v=bRmicYiGoQ4"); //Supervote How-To
        assertAll( "Title Search",
                () -> assertEquals(0, libc.searchForTitle("").length),
                () -> assertEquals(0, libc.searchForTitle("foobar").length),
                () -> assertEquals(1, libc.searchForTitle("YouTube").length),
                () -> assertEquals(3, libc.searchForTitle("e").length),
                () -> assertEquals("YouTube", libc.searchForTitle("YouTube")[0].getTitle()));
        libc.addLink("bar", "https://www.youtube.com/watch?v=Y7dpJ0oseIA"); //YouTube
        assertEquals(2, libc.searchForTitle("YouTube").length);
        assertAll("Title Search",
                () -> assertEquals("YouTube", libc.searchForTitle("YouTube")[0].getTitle()),
                () -> assertEquals("YouTube", libc.searchForTitle("YouTube")[1].getTitle()),
                () -> assertEquals("bar", libc.searchForTitle("YouTube")[0].getListName()),
                () -> assertEquals("foo", libc.searchForTitle("YouTube")[1].getListName()));
    }

    @Test
    void searchForURL() throws InvalidListNameException, ListAlreadyExistsException {
        libc.setAPIKey(apiKey);
        libc.addNewList("foo");
        libc.addNewList("bar");
        libc.addLink("foo", "https://www.youtube.com/watch?v=_YUugB4IUl4"); //Gmail Theater Act 1
        libc.addLink("foo", "https://www.youtube.com/watch?v=Y7dpJ0oseIA"); //YouTube
        libc.addLink("foo", "https://www.youtube.com/watch?v=bRmicYiGoQ4"); //Supervote How-To
        assertAll( "URL Search",
                () -> assertEquals(0, libc.searchForTitle("").length),
                () -> assertEquals(0, libc.searchForTitle("foobar").length),
                () -> assertEquals(1,
                        libc.searchForURL("https://www.youtube.com/watch?v=Y7dpJ0oseIA").length),
                () -> assertEquals(3, libc.searchForURL("e").length),
                () -> assertEquals("YouTube", libc.searchForURL("Y7dpJ0oseIA")[0].getTitle()));
        libc.addLink("bar", "https://www.youtube.com/watch?v=Y7dpJ0oseIA"); //YouTube
        assertEquals(2, libc.searchForTitle("YouTube").length);
        assertAll("URL Search",
                () -> assertEquals("YouTube",
                        libc.searchForURL("https://www.youtube.com/watch?v=Y7dpJ0oseIA")[0].getTitle()),
                () -> assertEquals("YouTube",
                        libc.searchForURL("https://www.youtube.com/watch?v=Y7dpJ0oseIA")[1].getTitle()),
                () -> assertEquals("bar", libc.searchForURL("Y7dpJ0oseIA")[0].getListName()),
                () -> assertEquals("foo", libc.searchForURL("Y7dpJ0oseIA")[1].getListName()));
    }

}
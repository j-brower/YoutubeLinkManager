package youtubeLinkManager.models;

import java.util.*;
import java.io.Serializable;

public class Library implements Serializable {

    private TreeMap<String, LinkList> lists = new TreeMap<String, LinkList>(String.CASE_INSENSITIVE_ORDER);
    private String apiKey = "";

    public boolean hasAPIKey() { return !apiKey.equals(""); }
    public boolean containsList(String name) { return lists.containsKey(name); }

    public int size() { return lists.size(); }
    public String getAPIKey() { return apiKey; }
    public LinkList getList(String listName) { return lists.get(listName); }
    public ArrayList<Link> GetLinksFrom(String listName) { return lists.get(listName).getLinks(); }
    public Link getLink(String listName, int index) { return lists.get(listName).getLink(index); }
    public Set<String> getKeySet() { return lists.keySet(); }

    public void setAPIKey(String s) { apiKey = (s == null) ? "" : s; }

    public void addList(String name) { lists.put(name, new LinkList(name)); }

    public void removeList(String name) { lists.remove(name); }
}

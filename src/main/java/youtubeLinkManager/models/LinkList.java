package youtubeLinkManager.models;

import java.util.*;
import java.io.Serializable;

public class LinkList implements Serializable {

    private String name;
    private ArrayList<Link> links = new ArrayList<Link>();

    public LinkList(String n) {
        name = n;
    }

    public boolean contains(String url) {
        for(Link l: links)
            if(l.getUrl().equals(url))
                return true;
        return false;
    }

    public void add(String url, String title, String length, int index) {
        links.add(index, new Link(url, title, length));
    }
    public void append(String url, String title, String length) {
        links.add(new Link(url, title, length));
    }
    public void remove(int index) { links.remove(index); }

    public int getSize() { return links.size(); }
    public String getName() { return name; }
    public Link getLink(int index) { return links.get(index); }
    public ArrayList<Link> getLinks() { return links; }

}

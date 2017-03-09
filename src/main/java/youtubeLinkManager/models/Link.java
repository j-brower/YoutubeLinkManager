package youtubeLinkManager.models;

import java.io.Serializable;


public class Link implements Serializable, Comparable<Link> {
    private String url;
    private String title;
    private String length;

    public Link(String u, String t, String l) {
        url = u;
        title = t;
        length = (l.matches("[0-9]+:[0-9][0-9]+:[0-9][0-9]") ||
                l.matches("[0-9]+:[0-9][0-9]") || l.matches("[0-9][0-9]")) ? l : "Invalid time.";
    }

    public String getUrl() { return url; }
    public String getTitle() { return title; }
    public String getLength() { return length; }

    public int compareTo(Link l) { return title.compareTo(l.getTitle()); }

    public String toString() { return title + " (" + length + ")"; }
}

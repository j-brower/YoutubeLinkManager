package youtubeLinkManager.models;

public class SearchResult {

    private String listName;
    private int index;
    private String title;
    private String url;
    private String length;

    public SearchResult(String lst, int i, String t, String u, String l) {
        listName = lst;
        index = i;
        title = t;
        url = u;
        length = l;
    }

    public String getListName() { return listName; }
    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public String getLength() { return length; }
    public int getIndex() { return index; }

    public String toString() {
        return listName + ": " + index + " - " + title + " (" + length + ")";
    }
}

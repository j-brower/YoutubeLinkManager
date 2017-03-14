package youtubeLinkManager;

import au.com.bytecode.opencsv.CSVWriter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import youtubeLinkManager.models.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;

public class LibraryController {

    //TODO: import

    public final String INVALIDCHAR =
            "List name contains an invalid character. Only a-z, A-Z, 0-9, space and _ are allowed.";
    public final String NOLIBRARYFILE =
            "\"library.ser\" not found in current directory, creating a new empty link library.";

    public final int INORDER = 0;
    public final int ALPHABETICAL = 1;
    public final int RANDOMIZED = 2;
    public final int CSVFILE = 0;
    public final int JSONFILE = 1;
    public final int XMLFILE = 2;

    private Library lib = null;

    private boolean notifyNew = false;
    private boolean unsavedChanges = false;

    public LibraryController() {
        try {
            FileInputStream fis = new FileInputStream("library.ser");
            ObjectInputStream ois = new ObjectInputStream(fis);
            lib = (Library) ois.readObject();
            ois.close();
            fis.close();
        } catch(FileNotFoundException fnfe) {
            System.out.println(NOLIBRARYFILE);
            lib = new Library();
            notifyNew = true;
        } catch(Exception e) {
            System.out.println(e.toString());
            e.printStackTrace();
        }
    }
    public boolean notifyNew(){ return notifyNew; }
    public boolean hasAPIKey() { return lib.hasAPIKey(); }
    public void setAPIKey(String s) {
        lib.setAPIKey(s);
        unsavedChanges = true;
    }
    public void setNotifyNew(boolean b) { notifyNew = b; }
    public boolean hasUnsavedChanges() { return unsavedChanges; }
    public void setUnsavedChanges(boolean b) { unsavedChanges = b; }

    /*
    * Getters.
    */
    public String getAPIKey() { return lib.getAPIKey(); }
    public boolean hasList(String name) { return lib.containsList(name); }
    public Link getLink(String listName, int pos) { return lib.getLink(listName, pos); }
    public String[] getListNames() {
        String[] arr = new String[lib.size()];
        Set<String> keys = lib.getKeySet();
        int i = 0;
        for(String key: keys) {
            arr[i] = key;
            i++;
        }
        return arr;
    }
    public ArrayList<Link> getLinksFromList(String listName) {
        LinkList lc = lib.getList(listName);
        return lc.getLinks();
    }
    public LinkList[] getAllLists() {
        LinkList[] result = new LinkList[lib.size()];
        Set<String> keys = lib.getKeySet();
        int i = 0;
        for(String key: keys) {
            result[i] = lib.getList(key);
            i++;
        }
        return result;
    }
    public int getNumberOfLists() { return lib.size(); }
    public int getListSize(String name) { return lib.getList(name).getSize(); }
    public boolean hasLink(String listName, String url) { return lib.getList(listName).contains(url); }

    /*
    * Add methods.
    */
    public void addNewList(String name) throws InvalidListNameException, ListAlreadyExistsException {
        if(lib.containsList(name)) {
            throw new ListAlreadyExistsException("List name \"" + name + "\" already in use.");
        } else if(!name.matches("[a-zA-Z0-9_ ]+")) {
            throw new InvalidListNameException(INVALIDCHAR);
        } else {
            lib.addList(name);
            unsavedChanges = true;
        }
    }
    public void addLink(String listName, String url) {
        String[] info = getTitleAndDuration(getIDFromURL(url));
        String title = info[0];
        String length = info[1];
        lib.getList(listName).append(url, title, length);
        unsavedChanges = true;
    }
    public void addLink(String listName, String url, String title, String length, int index) {
        lib.getList(listName).add(url, title, length, index);
        unsavedChanges = true;
    }

    /*
    * Remove methods.
    */
    public void removeList(String name) {
        if(lib.containsList(name)) {
            lib.removeList(name);
            unsavedChanges = true;
        }
    }
    public void removeLink(String listName, int index) {
        lib.getList(listName).remove(index);
        unsavedChanges = true;
    }
    public void clearAll() {
        String apiKey = lib.getAPIKey();
        lib = new Library();
        lib.setAPIKey(apiKey);
        unsavedChanges = true;
    }

    /*
    * Search methods.
    */
    public SearchResult[] searchForTitle(String query) {
        //TODO: make into a RecursiveTask
        if(query.equals(""))
            return new SearchResult[]{};
        ArrayList<SearchResult> result = new ArrayList<SearchResult>();
        LinkList[] lists = getAllLists();
        for(LinkList l: lists) {
            ArrayList<Link> ls = getLinksFromList(l.getName());
            for(int i = 0; i < ls.size(); i++) {
                if(Pattern.compile(Pattern.quote(query), Pattern.CASE_INSENSITIVE).matcher(ls.get(i).getTitle()).find())
                    result.add(new SearchResult(l.getName(), i,
                            ls.get(i).getTitle(), ls.get(i).getUrl(), ls.get(i).getLength()));
            }
        }
        return result.toArray(new SearchResult[result.size()]);
    }
    public SearchResult[] searchForURL(String query) {
        //TODO: make into a RecursiveTask
        if(query.equals(""))
            return new SearchResult[]{};
        ArrayList<SearchResult> result = new ArrayList<SearchResult>();
        LinkList[] lists = getAllLists();
        for(LinkList l: lists) {
            ArrayList<Link> ls = getLinksFromList(l.getName());
            for(int i = 0; i < ls.size(); i++) {
                if(Pattern.compile(Pattern.quote(query), Pattern.CASE_INSENSITIVE).matcher(ls.get(i).getUrl()).find())
                    result.add(new SearchResult(l.getName(), i,
                            ls.get(i).getTitle(), ls.get(i).getUrl(), ls.get(i).getLength()));
            }
        }
        return result.toArray(new SearchResult[result.size()]);
    }

    /*
    * API calls and helpers.
    */
    private String[] getTitleAndDuration(String id) {
        if(id == null)
            return new String[]{"Could not find video id in URL.", ""};
        String req = "https://www.googleapis.com/youtube/v3/videos?id="+id+
                "&key="+lib.getAPIKey()+"&part=snippet,contentDetails";
        String res = getJSON(req);
        if(res == null)
            return new String[] {"Connection error.", "Connection error."};
        try {
            JSONObject j = new JSONObject(res);
            String title = j.getJSONArray("items").getJSONObject(0).getJSONObject("snippet").getString("title");
            String duration = prettyDuration(j.getJSONArray("items")
                    .getJSONObject(0).getJSONObject("contentDetails").getString("duration"));
            return new String[]{title, duration};
        } catch(JSONException jse) {
            //possibly unreachable
            jse.printStackTrace();
            return new String[]{"Could not process Google API response.", ""};
        }
    }
    private String getJSON(String req) {
        String s = null;
        try {
            URL url = new URL(req);
            URLConnection connection = url.openConnection();
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder res = new StringBuilder();
            String line;
            while((line = br.readLine()) != null)
                res.append(line);
            br.close();
            s = res.toString();
        } catch(Exception e) {
            System.out.println(e.toString());
            e.printStackTrace();
        }
        return s;
    }
    private String getIDFromURL(String s) {
        String result = "";
        String[] split = s.split("\\?v=");
        if(split.length > 1) {
            if(split[1].length() < 11)
                return split[1];
            for (int i = 0; i < 11; i++) {
                //video ids are 11 chars long.
                result += split[1].charAt(i);
            }
            return result;
        }
        return null;
    }
    private String prettyDuration(String s) {
        //TODO: figure out ISO 6801 compliance
        //http://support.sas.com/documentation/cdl/en/lrdict/64316/HTML/default/viewer.htm#a003169814.htm
        //parse right-left because time libraries are more complex than the problem
        Long hrs = 0L;
        Long mins = 0L;
        Long secs = 0L;
        int i = s.length() - 1;
        while(i >= 0) {
            if(s.charAt(i) == 'S') {
                String tmp = "";
                while(i > 0) {
                    i--;
                    if(Character.isDigit(s.charAt(i)))
                        tmp = s.charAt(i) + tmp;
                    else
                        break;
                }
                secs += Long.parseLong(tmp);
            } else if(s.charAt(i) == 'M') {
                String tmp = "";
                while(i > 0) {
                    i--;
                    if (Character.isDigit(s.charAt(i)))
                        tmp = s.charAt(i) + tmp;
                    else
                        break;
                }
                mins += Long.parseLong(tmp);
            } else if(s.charAt(i) == 'H') {
                String tmp = "";
                while(i > 0) {
                    i--;
                    if(Character.isDigit(s.charAt(i)))
                        tmp = s.charAt(i) + tmp;
                    else
                        break;
                }
                hrs += Long.parseLong(tmp);
            } else if(s.charAt(i) == 'D') {
                String tmp = "";
                while(i > 0) {
                    i--;
                    if(Character.isDigit(s.charAt(i)))
                        tmp = s.charAt(i) + tmp;
                    else
                        break;
                }
                hrs += Long.parseLong(tmp) * 24;
            } else if(s.charAt(i) == 'W') {
                String tmp = "";
                while(i > 0) {
                    i--;
                    if(Character.isDigit(s.charAt(i)))
                        tmp = s.charAt(i) + tmp;
                    else
                        break;
                }
                hrs += Long.parseLong(tmp) * 168;
            } else {
                //ignores parts of the standard I haven't seen returned by the API, and the 'T' before 'D'.
                i--;
            }
        }

        String t = "";
        if (hrs > 0)
            t += hrs + ":";
        if (hrs > 0 && mins < 10) {
            t += "0" + mins + ":";
        } else if (mins > 0) {
            t += mins + ":";
        } else {
            t += "0:";
        }
        if (secs > 9)
            t += + secs;
        else
            t += "0" + secs;
        return t;
    }

    /*
    * Output methods.
    */
    public void export(ArrayList<String> linkNames, int order, int fileType) throws IOException {
        //I'm not going to parallelize this because it would lock the user out of doing anything meaningful
        if(linkNames.size() == 0)
            return;
        String fileString = "";
        String orderString = "";
        ArrayList<Link> links = new ArrayList<Link>();
        if(order == INORDER) {
            //sort an arraylist of links at the same index with util.collections, then put them in order into entries
            LinkList largest = lib.getList(linkNames.get(0));
            for(String n: linkNames)
                if(lib.getList(n).getSize() >= largest.getSize())
                    largest = lib.getList(n);
            for(int i = 0; i < largest.getSize(); i++) {
                ArrayList<Link> tmp = new ArrayList<Link>();
                for(String n: linkNames)
                    if(i < lib.getList(n).getSize())
                        tmp.add(lib.getList(n).getLink(i));
                Collections.sort(tmp);
                for(Link l: tmp)
                    links.add(l);
            }
            orderString = " in order (if multiple lists were selected, " +
                    "links that shared an index were alphabetically sorted).";
        } else if(order == ALPHABETICAL) {
            //merge an arraylist of links, sort with util.collections, put into entries
            for(String n: linkNames) {
                ArrayList<Link> ls = getLinksFromList(n);
                for (Link l: ls)
                    links.add(l);
            }
            Collections.sort(links);
            orderString = " in alphabetical order by title.";
        } else if(order == RANDOMIZED) {
            //put into entries, then shuffle
            for(String n: linkNames) {
                ArrayList<Link> ls = getLinksFromList(n);;
                for(Link l: ls)
                    links.add(l);
            }
            Collections.shuffle(links, new Random(System.nanoTime()));
            orderString = " in a pseudorandom order.";
        }
        if(fileType == CSVFILE) {
            fileString = "Exported to \"out.csv\"";
            ArrayList<String[]> entries = new ArrayList<String[]>();
            for(Link l: links)
                entries.add(new String[]{l.getUrl(), l.getTitle(), l.getLength()});
            CSVWriter writer = new CSVWriter(new FileWriter("out.csv"));
            writer.writeAll(entries);
            writer.close();
        } else if(fileType == JSONFILE) {
            fileString = "Exported to \"out.json\"";
            JSONArray entries = new JSONArray();
            for(Link l: links) {
                JSONObject obj = new JSONObject();
                obj.put("url", l.getUrl());
                obj.put("title", l.getTitle());
                obj.put("length", l.getLength());
                entries.put(obj);
            }
            JSONObject out = new JSONObject();
            out.put("items", entries);
            FileWriter writer = new FileWriter("out.json");
            out.write(writer, 1, 4);
            writer.close();
        } else if(fileType == XMLFILE) {
            try {
                fileString = "Exported to \"out.xml\"";
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document doc = db.newDocument();
                Element rootElement = doc.createElement("items");
                doc.appendChild(rootElement);
                for(Link l: links) {
                    Element item = doc.createElement("item");
                    Element url = doc.createElement("url");
                    url.appendChild(doc.createTextNode(l.getUrl()));
                    Element title = doc.createElement("title");
                    title.appendChild(doc.createTextNode(l.getTitle()));
                    Element length = doc.createElement("length");
                    length.appendChild(doc.createTextNode(l.getLength()));
                    item.appendChild(url);
                    item.appendChild(title);
                    item.appendChild(length);
                    rootElement.appendChild(item);
                }
                TransformerFactory tf = TransformerFactory.newInstance();
                Transformer transformer = tf.newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
                DOMSource source = new DOMSource(doc);
                StreamResult result = new StreamResult(new File("out.xml"));
                transformer.transform(source, result);
            } catch(Exception e) {
                e.printStackTrace();
                return;
            }
        }
        System.out.println(fileString + orderString);
    }
    public void writeFile() throws IOException {
        FileOutputStream fos = new FileOutputStream("library.ser");
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(lib);
        oos.close();
        fos.close();
        System.out.println("Saved library to current directory as \"library.ser\"");
        unsavedChanges = false;
    }
}

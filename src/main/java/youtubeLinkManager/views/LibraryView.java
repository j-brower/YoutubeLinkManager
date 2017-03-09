package youtubeLinkManager.views;

import youtubeLinkManager.*;
import youtubeLinkManager.models.*;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

public class LibraryView implements Runnable {

    /*
    * I should separate this into different files.
    */

    private final String TITLE = "Youtube Link Manager";
    private final String OK = "OK";
    private final String CANCEL = "Cancel";
    private final String SAVETEXT = "Save Changes to Disk";
    private final String UNSAVEDCHANGES = "Unsaved changes exist, would you like to close anyway?";
    private final String DUPLICATELINK = "URL is already in list. Create anyway?";
    private final String NOAPIKEY = "Google API key not found, please enter one:";
    private final String NEWAPIKEY = "Enter a new Google API key below:";
    private final String NOTHINGTOEXPORT = "There are no lists to export.";
    private final String NOEXPORTSELECTION = "At least one list must be selected for export.";

    private LibraryController libc;
    private JFrame listsFrame;
    private JList<String> listsList;
    private DefaultListModel<String> listModel;
    private DefaultListModel<String> linkListModel;
    private DefaultListModel<SearchResult> resultListModel;

    public LibraryView(LibraryController l) { libc = l; }

    /*
     *  The main dialog, for managing lists.
     */
    public void createAndShowListsView() {
        listsFrame = new JFrame(TITLE);
        java.net.URL imgURL = this.getClass().getResource("/icon.png");
        listsFrame.setIconImage(new ImageIcon(imgURL).getImage());
        listsFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        listsFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if(libc.hasUnsavedChanges()) {
                    if(JOptionPane.showConfirmDialog(listsFrame, UNSAVEDCHANGES, TITLE,
                            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                        System.exit(0);
                    }
                } else {
                    System.exit(0);
                }
            }
        });

        JPanel listsPanel = new JPanel(new BorderLayout());
        listsPanel.setBorder(new EmptyBorder(2, 2, 2, 2));

        //make a single selection scrollable list of the lists
        listModel = new DefaultListModel<String>();
        String[] names = libc.getListNames();
        for(String n: names)
            listModel.addElement(n);
        listsList = new JList<String>(listModel);
        listsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane listsScrollPane = new JScrollPane(listsList);

        //make the buttons on the bottom
        JPanel listsButtonPanel = new JPanel(new GridLayout(2, 4));
        listsButtonPanel.setBorder(new EmptyBorder(2, 0, 0, 0));

        JButton newListButton = new JButton("Create a New List");
        newListButton.addActionListener(e -> {
            createAndShowNewListDialog();
            listModel.clear();
            String[] newNames = libc.getListNames();
            for(String n: newNames)
                listModel.addElement(n);
        });
        listsButtonPanel.add(newListButton);

        JButton viewListButton = new JButton("View and Modify Selected List");
        viewListButton.addActionListener(e ->
                createAndShowListViewDialog(listsList.getSelectedValue()));
        listsButtonPanel.add(viewListButton);

        JButton deleteListButton = new JButton("Delete Selected List");
        deleteListButton.addActionListener(e -> {
            String name = listsList.getSelectedValue();
            if(name == null)
                return;
            String msg = "Delete list \""+name+"\"? This will delete all links in the list.";
            if(JOptionPane.showConfirmDialog(listsFrame, msg,"Confirm Deletion",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION)
                libc.removeList(name);
            listModel.clear();
            String[] newNames = libc.getListNames();
            for(String n: newNames)
                listModel.addElement(n);
        });
        listsButtonPanel.add(deleteListButton);

        JButton searchButton = new JButton("Search for Links");
        searchButton.addActionListener(e -> createAndShowSearchDialog());
        listsButtonPanel.add(searchButton);

        JButton saveButton = new JButton(SAVETEXT);
        saveButton.addActionListener(e -> tryToSave(listsFrame));
        listsButtonPanel.add(saveButton);

        JButton updateKeyButton = new JButton("Update API Key");
        updateKeyButton.addActionListener(e ->
            libc.setAPIKey(JOptionPane.showInputDialog(listsFrame, NEWAPIKEY, TITLE, JOptionPane.PLAIN_MESSAGE)));
        listsButtonPanel.add(updateKeyButton);

        JButton exportButton = new JButton("Export Links");
        exportButton.addActionListener(e -> {
            createAndShowExportSelectionDialog();
        });
        listsButtonPanel.add(exportButton);

        listsPanel.add(new JLabel("Link Lists:"), BorderLayout.NORTH);
        listsPanel.add(listsScrollPane, BorderLayout.CENTER);
        listsPanel.add(listsButtonPanel, BorderLayout.SOUTH);

        listsFrame.getContentPane().add(listsPanel, BorderLayout.CENTER);
        listsFrame.pack();
        listsFrame.setLocationRelativeTo(null);
        listsFrame.setVisible(true);
        if(libc.notifyNew()) {
            JOptionPane.showMessageDialog(listsFrame, libc.NOLIBRARYFILE, TITLE, JOptionPane.PLAIN_MESSAGE);
        }
        if(!libc.hasAPIKey()) {
            libc.setAPIKey(JOptionPane.showInputDialog(listsFrame, NOAPIKEY, TITLE, JOptionPane.PLAIN_MESSAGE));
        }
    }
    /*
     *  Dialog for making new lists.
     */
    private void createAndShowNewListDialog() {
        JDialog newListDialog = new JDialog(listsFrame, "Create a New List");
        newListDialog.setModal(true);
        newListDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        JPanel newListPanel = new JPanel(new BorderLayout());
        newListPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel newListLabel = new JLabel("Enter a name for your new list:");
        JTextField newNameField = new JTextField(32);

        JPanel newListButtonPanel = new JPanel(new GridLayout(1, 2));
        JButton createButton = new JButton("Create");
        createButton.addActionListener(e -> {
            String name = newNameField.getText();
            if(name.length() == 0) {
                JOptionPane.showMessageDialog(newListDialog,
                        "Please enter a name.", TITLE, JOptionPane.PLAIN_MESSAGE);
                return;
            }
            try {
                libc.addNewList(name);
            } catch(Exception ex) {
                JOptionPane.showMessageDialog(newListDialog, ex.getMessage(), TITLE, JOptionPane.PLAIN_MESSAGE);
                return;
            }
            newListDialog.dispose();
        });
        JButton cancelButton = new JButton(CANCEL);
        cancelButton.addActionListener(e -> newListDialog.dispose());
        newListButtonPanel.add(createButton);
        newListButtonPanel.add(cancelButton);

        newListPanel.add(newListLabel, BorderLayout.NORTH);
        newListPanel.add(newNameField, BorderLayout.CENTER);
        newListPanel.add(newListButtonPanel, BorderLayout.SOUTH);

        newListDialog.getContentPane().add(newListPanel, BorderLayout.CENTER);
        newListDialog.pack();
        newListDialog.setLocationRelativeTo(null);
        newListDialog.getRootPane().setDefaultButton(createButton);
        newListDialog.setVisible(true);
    }
    /*
     *  The dialog for viewing a list's contents.
     */
    private void createAndShowListViewDialog(String listName) {
        if(listName == null)
            return;
        JDialog listViewDialog = new JDialog(listsFrame, listName+" - "+TITLE);
        listViewDialog.setModal(true);
        listViewDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        JPanel listViewPanel = new JPanel(new BorderLayout());
        listViewPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        JLabel listNameLabel = new JLabel("Links in "+listName+":");

        linkListModel = new DefaultListModel<>();
        JList<String> linkList = new JList<>(linkListModel);
        linkList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        refreshLinkList(listName);
        JScrollPane listScrollPane = new JScrollPane(linkList);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 4));
        JButton addLinkButton = new JButton("Add a Link");
        addLinkButton.addActionListener(e -> {
            createAndShowNewLinkDialog(listName, listViewDialog);
        });
        JButton viewLinkButton = new JButton("View/Modify Selected Link");
        viewLinkButton.addActionListener(e -> {
            int index = linkList.getSelectedIndex();
            if(index == -1)
                return;
            createAndShowViewLinkDialog(listName, index, listViewDialog);
        });
        JButton deleteLinkButton = new JButton("Delete Selected Link");
        deleteLinkButton.addActionListener(e -> {
            int linkIndex = linkList.getSelectedIndex();
            if(linkIndex == -1)
                return;
            libc.removeLink(listName, linkIndex);
            refreshLinkList(listName);
        });
        JButton saveButton = new JButton(SAVETEXT);
        saveButton.addActionListener(e -> tryToSave(listViewDialog));

        JButton cancelButton = new JButton("Close List View");
        cancelButton.addActionListener(e -> listViewDialog.dispose());

        buttonPanel.add(addLinkButton);
        buttonPanel.add(viewLinkButton);
        buttonPanel.add(deleteLinkButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        listViewDialog.add(listNameLabel, BorderLayout.NORTH);
        listViewDialog.add(listScrollPane, BorderLayout.CENTER);
        listViewDialog.add(buttonPanel, BorderLayout.SOUTH);
        listViewDialog.pack();
        listViewDialog.setLocationRelativeTo(null);
        listViewDialog.setVisible(true);
    }



    /*
     *  The dialog for creating new links inside of a list.
     */
    private void createAndShowNewLinkDialog(String listName, JDialog parent) {
        JDialog newLinkDialog = new JDialog(parent, "Add a Link to "+listName);
        newLinkDialog.setModal(true);
        newLinkDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        JPanel newLinkPanel = new JPanel(new BorderLayout());
        newLinkPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel fieldLabel = new JLabel("URL:");
        JTextField urlField = new JTextField(64);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        JButton okButton = new JButton("Create");
        okButton.addActionListener(e -> {
            String url = urlField.getText();
            if(url.length() == 0) {
                JOptionPane.showMessageDialog(newLinkDialog, "Please enter some text.",
                        TITLE, JOptionPane.PLAIN_MESSAGE);
            } else if(libc.hasLink(listName, url)) {
                if(JOptionPane.showConfirmDialog(newLinkDialog, DUPLICATELINK, TITLE,
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION) {
                    newLinkDialog.dispose();
                    return;
                }
            }
            libc.addLink(listName, url);
            newLinkDialog.dispose();
            createAndShowViewLinkDialog(listName, libc.getListSize(listName) - 1, parent);
            refreshLinkList(listName);

        });
        JButton cancelButton = new JButton(CANCEL);
        cancelButton.addActionListener(e -> newLinkDialog.dispose());
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        newLinkDialog.getRootPane().setDefaultButton(okButton);

        newLinkPanel.add(fieldLabel, BorderLayout.WEST);
        newLinkPanel.add(urlField, BorderLayout.EAST);
        newLinkPanel.add(buttonPanel, BorderLayout.SOUTH);
        newLinkDialog.add(newLinkPanel, BorderLayout.CENTER);

        newLinkDialog.pack();
        newLinkDialog.setLocationRelativeTo(null);
        newLinkDialog.setVisible(true);
    }
    /*
     *  The dialog for modifying a link inside list results.
     */
    private void createAndShowViewLinkDialog(String listName, int index, JDialog parent) {
        Link link = libc.getLink(listName, index);
        JDialog viewLinkDialog = new JDialog(parent, "View/Modify a Link - "+TITLE);
        viewLinkDialog.setModal(true);
        viewLinkDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        JPanel viewPanel = new JPanel(new BorderLayout());
        viewPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel labelPanel = new JPanel(new GridLayout(4, 1));
        JPanel textFieldPanel = new JPanel(new GridLayout(4, 1));
        labelPanel.add(new JLabel("URL:"));
        JTextField urlField = new JTextField(64);
        urlField.setText(link.getUrl());
        textFieldPanel.add(urlField);
        labelPanel.add(new JLabel("Title:"));
        JTextField titleField = new JTextField(64);
        titleField.setText(link.getTitle());
        textFieldPanel.add(titleField);
        labelPanel.add(new JLabel("Length:"));
        JTextField lengthField = new JTextField(8);
        lengthField.setText(link.getLength());
        textFieldPanel.add(lengthField);
        labelPanel.add(new JLabel("Position:"));
        SpinnerNumberModel spinnerModel =
                new SpinnerNumberModel(index, 0, libc.getListSize(listName)-1, 1);
        JSpinner indexSpinner = new JSpinner(spinnerModel);
        textFieldPanel.add(indexSpinner);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 3));
        JButton okButton = new JButton(OK);
        okButton.addActionListener(e -> {
            String newUrl = urlField.getText();
            String newTitle = titleField.getText();
            String newLength = lengthField.getText();
            int newIndex = (int) indexSpinner.getValue();
            libc.removeLink(listName, index);
            libc.addLink(listName, newUrl, newTitle, newLength, newIndex);
            viewLinkDialog.dispose();
            refreshLinkList(listName);
        });
        buttonPanel.add(okButton);

        JButton deleteButton = new JButton("Delete");
        deleteButton.addActionListener(e -> {
            String msg = "Delete link: " + libc.getLink(listName, index).toString() + " ?";
            if(JOptionPane.showConfirmDialog(viewLinkDialog, msg,"Confirm Deletion",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                libc.removeLink(listName, index);
                viewLinkDialog.dispose();
                refreshLinkList(listName);
            }
        });
        buttonPanel.add(deleteButton);

        JButton cancelButton = new JButton(CANCEL);
        cancelButton.addActionListener(e -> viewLinkDialog.dispose());
        buttonPanel.add(cancelButton);

        viewPanel.add(labelPanel, BorderLayout.WEST);
        viewPanel.add(textFieldPanel, BorderLayout.EAST);
        viewLinkDialog.add(viewPanel, BorderLayout.CENTER);
        viewLinkDialog.add(buttonPanel, BorderLayout.SOUTH);
        viewLinkDialog.pack();
        viewLinkDialog.getRootPane().setDefaultButton(okButton);
        viewLinkDialog.setLocationRelativeTo(null);
        viewLinkDialog.setVisible(true);
    }



    /*
     *  The dialog for searching.
     */
    private void createAndShowSearchDialog() {
        JDialog searchDialog = new JDialog(listsFrame, "Search");
        searchDialog.setModal(true);
        searchDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        searchDialog.getRootPane().setBorder(new EmptyBorder(10, 10, 10, 10));

        JTextField queryField = new JTextField(64);
        JComboBox<String> searchOptions = new JComboBox<String>(new String[]{"Title", "URL"});

        JPanel buttonsPanel = new JPanel(new GridLayout(1, 2));
        JButton okButton = new JButton("Search");
        okButton.addActionListener(e -> {
            if(queryField.getText().length() == 0) {
                JOptionPane.showMessageDialog(searchDialog,
                        "Please enter text to search for.", TITLE, JOptionPane.PLAIN_MESSAGE);
            } else {
                createAndShowSearchResults(searchDialog, queryField.getText(),
                        searchOptions.getSelectedItem().toString());
            }
        });
        JButton cancelButton = new JButton(CANCEL);
        cancelButton.addActionListener(e -> searchDialog.dispose());
        buttonsPanel.add(okButton);
        buttonsPanel.add(cancelButton);

        searchDialog.getRootPane().setDefaultButton(okButton);
        searchDialog.add(queryField, BorderLayout.CENTER);
        searchDialog.add(new JLabel("Search for:"), BorderLayout.WEST);
        searchDialog.add(searchOptions, BorderLayout.EAST);
        searchDialog.add(buttonsPanel, BorderLayout.SOUTH);
        searchDialog.pack();
        searchDialog.setLocationRelativeTo(null);
        searchDialog.setVisible(true);

    }
    /*
     *  The dialog for looking at search results.
     */
    private void createAndShowSearchResults(JDialog parent, String text, String option) {
        JDialog resultsDialog = new JDialog(parent, "Search Results");
        resultsDialog.setModal(true);
        resultsDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        resultsDialog.getRootPane().setBorder(new EmptyBorder(10, 10, 10, 10));

        resultListModel = new DefaultListModel<SearchResult>();
        runSearch(text, option);
        JList<SearchResult> resultJList = new JList<SearchResult>(resultListModel);
        resultJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane resultsScrollPane = new JScrollPane(resultJList);

        JPanel buttonsPanel = new JPanel(new GridLayout(1, 3));
        JButton viewButton = new JButton("View/Modify Selected Link");
        viewButton.addActionListener(e ->{
            SearchResult s = resultJList.getSelectedValue();
            if(s == null)
                return;
            createAndShowViewSearchedLinkDialog(s.getListName(), s.getIndex(), resultsDialog, text, option);
        });
        buttonsPanel.add(viewButton);
        JButton deleteButton = new JButton("Delete Selected Link");
        //TODO: add action listener

        buttonsPanel.add(deleteButton);
        //TODO: confirm and delete
        JButton cancelButton = new JButton(CANCEL);
        cancelButton.addActionListener(e -> resultsDialog.dispose());
        buttonsPanel.add(cancelButton);

        resultsDialog.add(new JLabel("Results:"), BorderLayout.NORTH);
        resultsDialog.add(resultsScrollPane, BorderLayout.CENTER);
        resultsDialog.add(buttonsPanel, BorderLayout.SOUTH);
        resultsDialog.pack();
        resultsDialog.setLocationRelativeTo(null);
        resultsDialog.setVisible(true);
    }
    /*
     *  The dialog for modifying a search result.
     */
    private void createAndShowViewSearchedLinkDialog(String listName, int index, JDialog parent,
                                                     String searchText, String searchOption) {
        //similar to createAndShowViewLinkDialog, but has to refresh the search instead.
        Link link = libc.getLink(listName, index);
        JDialog viewLinkDialog = new JDialog(parent, "View/Modify a Link - "+TITLE);
        viewLinkDialog.setModal(true);
        viewLinkDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        JPanel viewPanel = new JPanel(new BorderLayout());
        viewPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel labelPanel = new JPanel(new GridLayout(4, 1));
        JPanel textFieldPanel = new JPanel(new GridLayout(4, 1));
        labelPanel.add(new JLabel("URL:"));
        JTextField urlField = new JTextField(64);
        urlField.setText(link.getUrl());
        textFieldPanel.add(urlField);
        labelPanel.add(new JLabel("Title:"));
        JTextField titleField = new JTextField(64);
        titleField.setText(link.getTitle());
        textFieldPanel.add(titleField);
        labelPanel.add(new JLabel("Length:"));
        JTextField lengthField = new JTextField(8);
        lengthField.setText(link.getLength());
        textFieldPanel.add(lengthField);
        labelPanel.add(new JLabel("Position:"));
        SpinnerNumberModel spinnerModel =
                new SpinnerNumberModel(index, 0, libc.getListSize(listName)-1, 1);
        JSpinner indexSpinner = new JSpinner(spinnerModel);
        textFieldPanel.add(indexSpinner);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 3));
        JButton okButton = new JButton(OK);
        okButton.addActionListener(e -> {
            String newUrl = urlField.getText();
            String newTitle = titleField.getText();
            String newLength = lengthField.getText();
            int newIndex = (int) indexSpinner.getValue();
            libc.removeLink(listName, index);
            libc.addLink(listName, newUrl, newTitle, newLength, newIndex);
            viewLinkDialog.dispose();
            runSearch(searchText, searchOption);
        });
        buttonPanel.add(okButton);
        JButton deleteButton = new JButton("Delete");
        deleteButton.addActionListener(e -> {
            libc.removeLink(listName, index);
            viewLinkDialog.dispose();
            //TODO: confirm delete
            runSearch(searchText, searchOption);
        });
        buttonPanel.add(deleteButton);

        JButton cancelButton = new JButton(CANCEL);
        cancelButton.addActionListener(e -> viewLinkDialog.dispose());
        buttonPanel.add(cancelButton);

        viewPanel.add(labelPanel, BorderLayout.WEST);
        viewPanel.add(textFieldPanel, BorderLayout.EAST);
        viewLinkDialog.add(viewPanel, BorderLayout.CENTER);
        viewLinkDialog.add(buttonPanel, BorderLayout.SOUTH);
        viewLinkDialog.pack();
        viewLinkDialog.getRootPane().setDefaultButton(okButton);
        viewLinkDialog.setLocationRelativeTo(null);
        viewLinkDialog.setVisible(true);

    }



    /*
     *  The dialog for picking lists to export.
     */
    private void createAndShowExportSelectionDialog() {
        if(libc.getListNames().length == 0) {
            JOptionPane.showMessageDialog(listsFrame, NOTHINGTOEXPORT, TITLE, JOptionPane.PLAIN_MESSAGE);
            return;
        }
        JDialog exportDialog = new JDialog(listsFrame, "Export Youtube Links");
        exportDialog.setModal(true);
        exportDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        JPanel exportPanel = new JPanel(new BorderLayout());
        exportPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        JCheckBox[] boxes = new JCheckBox[libc.getNumberOfLists()];
        String[] boxNames = libc.getListNames();
        for(int i = 0; i < boxNames.length; i++)
            boxes[i] = new JCheckBox(boxNames[i]);
        JPanel boxPanel = new JPanel(new GridLayout(boxes.length+1, 1));
        for(JCheckBox b: boxes)
            boxPanel.add(b);

        JButton selectAllButton = new JButton("Select All");
        selectAllButton.addActionListener(e -> {
            for(JCheckBox b: boxes)
                if(!b.isSelected())
                    b.setSelected(true);
        });
        boxPanel.add(selectAllButton);

        JPanel buttonsPanel = new JPanel(new GridLayout(1, 2));
        JButton okButton = new JButton("Continue");
        okButton.addActionListener(e -> {
            ArrayList<String> toExport = new ArrayList<>();
            for(JCheckBox b: boxes)
                if(b.isSelected())
                    toExport.add(b.getText());
            if(toExport.size() == 0) {
                JOptionPane.showMessageDialog(listsFrame, NOEXPORTSELECTION, TITLE, JOptionPane.PLAIN_MESSAGE);
            } else {
                exportDialog.dispose();
                createAndShowExportOptionsDialog(toExport);
            }
        });

        JButton cancelButton = new JButton(CANCEL);
        cancelButton.addActionListener(e -> exportDialog.dispose());
        buttonsPanel.add(okButton);
        buttonsPanel.add(cancelButton);

        exportPanel.add(new JLabel("Select lists to export:"), BorderLayout.NORTH);
        exportPanel.add(boxPanel, BorderLayout.CENTER);
        exportPanel.add(buttonsPanel, BorderLayout.SOUTH);
        exportDialog.add(exportPanel, BorderLayout.CENTER);
        exportDialog.pack();
        exportDialog.setLocationRelativeTo(null);
        exportDialog.setVisible(true);
    }
    /*
     *  The dialog for export options.
     */
    private void createAndShowExportOptionsDialog(ArrayList<String> names) {
        if(names.size() == 0)
            return;
        JDialog exportDialog = new JDialog(listsFrame, "Export Options");
        exportDialog.getRootPane().setBorder(new EmptyBorder(10, 10, 10, 10));
        exportDialog.setModal(true);
        exportDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        //options: col1: in order, alphabetical by title, shuffle   col2: xml, json, csv
        JPanel orderPanel = new JPanel(new GridLayout(4, 1));
        orderPanel.add(new JLabel("Ordering:"));
        ButtonGroup orderButtons = new ButtonGroup();
        JRadioButton inOrderRadio = new JRadioButton("In Order");
        inOrderRadio.setSelected(true);
        JRadioButton alphaRadio = new JRadioButton("Alphabetical (By Title)");
        JRadioButton randomizedRadio = new JRadioButton("Randomized");
        orderButtons.add(inOrderRadio);
        orderPanel.add(inOrderRadio);
        orderButtons.add(alphaRadio);
        orderPanel.add(alphaRadio);
        orderButtons.add(randomizedRadio);
        orderPanel.add(randomizedRadio);

        JPanel formatPanel = new JPanel(new GridLayout(4, 1));
        formatPanel.add(new JLabel("Format:"));
        ButtonGroup formatButtons = new ButtonGroup();
        JRadioButton csvRadio = new JRadioButton(".csv");
        csvRadio.setSelected(true);
        JRadioButton jsonRadio = new JRadioButton(".json");
        JRadioButton xmlRadio = new JRadioButton(".xml");
        formatButtons.add(csvRadio);
        formatPanel.add(csvRadio);
        formatButtons.add(jsonRadio);
        formatPanel.add(jsonRadio);
        formatButtons.add(xmlRadio);
        formatPanel.add(xmlRadio);

        JPanel buttonsPanel = new JPanel(new GridLayout(1, 2));
        JButton okButton = new JButton(OK);
        okButton.addActionListener(e -> {
            try {
                if (csvRadio.isSelected()) {
                    if (inOrderRadio.isSelected())
                        libc.export(names, libc.INORDER, libc.CSVFILE);
                    else if (alphaRadio.isSelected())
                        libc.export(names, libc.ALPHABETICAL, libc.CSVFILE);
                    else  //random is selected
                        libc.export(names, libc.RANDOMIZED, libc.CSVFILE);
                } else if (jsonRadio.isSelected()) {
                    if (inOrderRadio.isSelected())
                        libc.export(names, libc.INORDER, libc.JSONFILE);
                    else if (alphaRadio.isSelected())
                        libc.export(names, libc.ALPHABETICAL, libc.JSONFILE);
                    else //random is selected
                        libc.export(names, libc.RANDOMIZED, libc.JSONFILE);
                } else { //xml is selected
                    if(inOrderRadio.isSelected())
                        libc.export(names, libc.INORDER, libc.XMLFILE);
                    else if(alphaRadio.isSelected())
                        libc.export(names, libc.ALPHABETICAL, libc.XMLFILE);
                    else //random is selected
                        libc.export(names, libc.RANDOMIZED, libc.XMLFILE);
                }
                exportDialog.dispose();
            } catch(IOException ioe) {
                ioe.printStackTrace();
                JOptionPane.showMessageDialog(listsFrame, "Could not export, IO exception.",
                        TITLE, JOptionPane.PLAIN_MESSAGE);
            }
        });
        JButton cancelButton = new JButton(CANCEL);
        cancelButton.addActionListener(e -> exportDialog.dispose());
        buttonsPanel.add(okButton);
        buttonsPanel.add(cancelButton);

        exportDialog.add(orderPanel, BorderLayout.WEST);
        exportDialog.add(formatPanel, BorderLayout.EAST);
        exportDialog.add(buttonsPanel, BorderLayout.SOUTH);
        exportDialog.pack();
        exportDialog.setLocationRelativeTo(null);
        exportDialog.setVisible(true);
    }


    /*
     *  Helpers that refresh lists in main dialogs.
     */
    private void refreshLinkList(String listName) {
        ArrayList<Link> tmpLinks = libc.getLinksFromList(listName);
        linkListModel.clear();
        int i = 0;
        for(Link l: tmpLinks) {
            linkListModel.addElement(i + " - " + l.toString());
            i++;
        }
    }
    private void runSearch(String text, String option) {
        SearchResult[] results;
        if(option.equalsIgnoreCase("URL"))
            results = libc.searchForURL(text);
        else
            results = libc.searchForTitle(text);
        resultListModel.clear();
        for(SearchResult s: results)
            resultListModel.addElement(s);
    }



    /*
     *  Helper for serialization try block.
     */
    private void tryToSave(Component parent) {
        try {
            libc.writeFile();
        } catch(IOException ioe) {
            ioe.printStackTrace();
            libc.setUnsavedChanges(true);
            JOptionPane.showMessageDialog(parent, "IO exception, could not save to disk.",
                    TITLE, JOptionPane.PLAIN_MESSAGE);
        }
    }

    /*
     * Run method to use Swing thread.
     */
    public void run() {
        createAndShowListsView();
    }
}

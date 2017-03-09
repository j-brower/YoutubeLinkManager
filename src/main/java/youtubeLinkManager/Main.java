package youtubeLinkManager;

import javax.swing.*;
import youtubeLinkManager.views.*;

public class Main {

    public static void main(String[] args) {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch(Exception e) {
            System.out.println(e.toString());
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(new LibraryView(new LibraryController()));
    }

}

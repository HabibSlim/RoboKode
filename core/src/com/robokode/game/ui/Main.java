package com.robokode.game.ui;

import com.sun.javafx.css.StyleManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Main {

    public final static int WIDTH = 1250;
    public final static int HEIGHT = 653;

    private LevelInfo levelInfo;
    private JFrame jFrame;
    private JButton lol;
    static int lvl = 0;

    public static void main(String[] args) {
        Main main = new Main();
        main.testFrame();
    }

    private void testFrame() {
        /** Contenu SWING **/
        jFrame = new JFrame("RoboKode Test");

        JLabel imgL = null;
        jFrame.setLayout(null);
        try {
            BufferedImage img = ImageIO.read(new File("frame.png"));
            imgL = new JLabel(new ImageIcon(img));

            imgL.setBounds(0, 0, WIDTH, HEIGHT-241);
            jFrame.add(imgL);
        } catch (IOException e) {
            e.printStackTrace();
        }
        lol = new JButton("Clique");
        lol.setBounds((Main.WIDTH-200)/2, Main.HEIGHT-200, 200, 200);
        lol.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                levelInfo.loadLevelInfo(lvl);
                lvl++;
            }
        });
        lol.setVisible(true);

        jFrame.add(lol);

        jFrame.setSize(WIDTH, HEIGHT);
        jFrame.setResizable(false);
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // On centre le JFrame
        jFrame.setLocationRelativeTo(null);

        jFrame.setVisible(true);

        /** Création du panneau d'info **/
        new JFXPanel();
        Platform.setImplicitExit(false); // Le thread JavaFX ne doit pas mourrir quand le stage disparait
        Platform.runLater(() -> {
            levelInfo = new LevelInfo(jFrame);

            Application.setUserAgentStylesheet(Application.STYLESHEET_MODENA);
            StyleManager.getInstance().addUserAgentStylesheet("file:uistyle.css");
        });
    }
}

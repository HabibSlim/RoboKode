package com.robokode.utils.maps;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader;
import com.badlogic.gdx.utils.XmlReader.Element;
import com.robokode.utils.ConsoleLog;
import com.robokode.utils.Ressources;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;

public class MapInfo {

    /** Attributs des cartes **/
    // Identifiant
    private int id;

    // Type de carte de jeu
    public MapType typeMap;
    public enum MapType {
        TUTO_MOVE, TUTO_SHOOT, TUTO_MULTI, MULTI
    }

    // Source du fichier tmx
    public String tmxSrc;

    // Source du fichier rkmp
    private String rkmpSrc;

    // Caractéristiques de la carte
    public ArrayList<int[]> obstacles;
    public ArrayList<int[]> barrieres;
    public ArrayList<int[]> reliefs;

    // Coordonnées de départ\d'arrivée
    public int[] startPos;
    public int[] ennemyPos;
    public int[] endPos;

    // Positions d'éléments de la carte
    public int[] towerPos;

    /** Constructeur principal **/
    public MapInfo(int id) {
        this.obstacles = new ArrayList<int[]>();
        this.barrieres = new ArrayList<int[]>();
        this.reliefs = new ArrayList<int[]>();

        this.id = id;
        this.tmxSrc = Ressources.getMap(id);
        this.rkmpSrc = Ressources.getMapInfo(id);

        // On récupère l'info de la map
        parseXML();
    }

    private void parseXML() {
        int[] coord;  Array<Element> items;

        XmlReader reader = new XmlReader();
        Element root = reader.parse(readFile(rkmpSrc));

        // On lit le type de carte => on le transforme en énuméré
        Element mapGoal = root.getChildByName("mapGoal");
        String mapType = mapGoal.getAttribute("type");

        switch (mapType) {
            case "TUTO_MOVE":
                typeMap = MapType.TUTO_MOVE;
                // On lit la cellule d'arrivée du joueur
                Element endCell = mapGoal.getChildByName("endCell");
                endPos = text2Coord(endCell.getText());
            break;
            case "TUTO_SHOOT":
                typeMap = MapType.TUTO_SHOOT;
                // On récupère les coordonnées de la tourelle à positionner
                Element towerCell = mapGoal.getChildByName("tower");
                towerPos = text2Coord(towerCell.getText());
            break;
            case "TUTO_MULTI":
                typeMap = MapType.TUTO_MULTI;
                // On récupère les coordonnées de départ du robot ennemi
                Element startCell = mapGoal.getChildByName("ennemyPos");
                ennemyPos = text2Coord(startCell.getText());
            break;
            case "MULTI":
                typeMap = MapType.MULTI;
                // => des trucs à faire
            break;
            default:
                ConsoleLog.put("MapInfo : Type de map inconnu : " + mapType, true);
            break;
        }

        // On lit la coordonnée de départ du personnage
        Element startCell = root.getChildByName("startCell");
        startPos = text2Coord(startCell.getText());

        // On lit les objets en relief
        Element relief = root.getChildByName("relief");
        if (relief != null) {
            items = relief.getChildrenByName("coord");

            // On lit le contenu de chaque noeud <coord>
            for (Element child : items)
            {
                coord = text2Coord(child.getText());
                // On ajoute les coordonnées de l'obstacle à la map
                reliefs.add(coord);
            }

            items.clear();
        }

        // On lit les barrières de la map
        Element barrieresNode = root.getChildByName("barrieres");
        if (barrieresNode != null) {
            items = barrieresNode.getChildrenByName("barriere");

            // On lit le contenu de chaque noeud <barriere>
            for (Element child : items)
            {
                // Pour chaque barriere, on ajoute un array de taille 4 [x1;y1,x2;y2] à la liste
                int[] wallCoord = new int[4]; int id = 0;
                Array<Element> coordList = child.getChildrenByName("coord");

                for (Element dCoord : coordList) {
                    coord = text2Coord(dCoord.getText());

                    wallCoord[id] = coord[0];
                    wallCoord[id+1] = coord[1];
                    id+=2;
                }

                // On ajoute la nouvelle barrière à la liste
                barrieres.add(wallCoord);
            }

            items.clear();
        }

        // On lit les obstacles de la carte
        Element obst = root.getChildByName("obstacles");
        items = obst.getChildrenByName("coord");

        // On lit le contenu de chaque noeud <coord>
        for (Element child : items)
        {
            coord = text2Coord(child.getText());
            // On ajoute les coordonnées de l'obstacle à la map
            obstacles.add(coord);
        }

        // => après, à voir
    }

    /**
     * Renvoie vrai si la map possède des objets en relief
     */
    public boolean hasRelief() { return this.reliefs.size() > 0; }

    /**
     * Convertit une entrée XML au format : x;y en array de coordonnées entières
     */
    private int[] text2Coord(String xmlVal) {
        String[] coordS = xmlVal.split(";");
        return new int[] {Integer.valueOf(coordS[0]), Integer.valueOf(coordS[1])};
    }

    /**
     * Lit le contenu d'un fichier et en renvoie le contenu en une seule chaîne
     * de caractères.
     */
    private String readFile(String path) {
        try {
            FileInputStream stream = new FileInputStream(path);

            try {
                InputStreamReader input = new InputStreamReader(stream,
                        Charset.defaultCharset());
                Reader reader = new BufferedReader(input);

                StringBuilder builder = new StringBuilder();
                char[] buffer = new char[8192];
                int read;

                while ((read = reader.read(buffer, 0, buffer.length)) > 0) {
                    builder.append(buffer, 0, read);
                }

                return builder.toString();
            } finally {
                stream.close();
            }
        } catch (IOException ex) {
            ConsoleLog.put("MapInfo : Impossible de charge les infos de la carte : "+rkmpSrc, true);
        }

        return "";
    }
}

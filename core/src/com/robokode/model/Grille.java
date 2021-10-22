package com.robokode.model;

import com.robokode.utils.Direction;

import java.util.ArrayList;

/**
 * Grille de la map contenant tous les objets du jeu
 */
public class Grille {
    // Dimensions de la grille
    public static int DIM_X = 10;
    public static int DIM_Y = 10;

    // Attributs de la grille
    private Tuile[][] tuiles;
    private ArrayList<Barriere> barrieres;

    public Grille() {
        // On initialise la grille
        initGrille();

        // On initialise les listes
        barrieres = new ArrayList<Barriere>();
    }

    // Supprime tous les robots de la grille
    public void resetGrille() {
        for (int j = 0; j < DIM_Y; j++) {
            for (int i = 0; i < DIM_X; i++) {
                this.tuiles[j][i].rmvRobot();
            }
        }
    }

    // Recrée toutes les tuiles de la grille
    private void initGrille() {
        this.tuiles = new Tuile[DIM_Y][DIM_X];
        for (int j = 0; j < DIM_Y; j++) {
            for (int i = 0; i < DIM_X; i++) {
                tuiles[j][i] = new Tuile(j, i);
            }
        }
    }

    /* Getters
    * =================================== */

    public Tuile getCase (int lig, int col) {
        if ((lig < DIM_X) && (col < DIM_Y))
            return this.tuiles[lig][col];
        else
            return null;
    }

    // Renvoie les tuiles de la grille où se trouvent un robot
    public ArrayList<Tuile> getTuilesJoueurs() {
        ArrayList<Tuile> at = new ArrayList();
        for (Tuile t : getTuilesArray()) {
            if (t.getRobot() != null) { //si la tuile contient un robot
                at.add(t);
            }
        }
        return at;
    }

    // Renvoie les tuiles de la grille où se trouvent un obstacle
    public ArrayList<Tuile> getTuilesObstacle() {
        ArrayList<Tuile> at = new ArrayList();
        for (Tuile t : getTuilesArray()) {
            if (!t.aucunObstacle()) { //si la tuile contient un obstacle ou un robot
                at.add(t);
            }
        }
        for (Tuile t : getTuilesJoueurs()) //on retire les tuiles où se trouvent un robot
            at.remove(t);
        return at;
    }

    // Renvoie les tuiles de la grille sous forme d'arraylist
    public ArrayList<Tuile> getTuilesArray() {
        ArrayList<Tuile> at = new ArrayList<Tuile>();
        for (int j = 0; j < 10; j++) {
            for (int i = 0; i < 10; i++) {
                at.add(tuiles[j][i]);
            }
        }
        return at;
    }

    // Renvoie les coordonnées du premier obstacle dans une direction, depuis la tuile t
    // Si strict est à true : Renvoie une case derrière la map si aucun obstacle n'a été trouvé sur la ligne de tir
    public int[] getFirstObstacle(Tuile t, Direction dir, boolean strict) {
        int[] coord = null;

        if (dir == Direction.HG) {
            for (int i = t.getLigne()-1; i > -1; i--) {
                if (!tuiles[i][t.getColonne()].aucunObstacle()) { // On rencontre un obstacle : banco
                    coord = new int[]{i, t.getColonne()};
                    break;
                }
            }

            if (coord==null) {
                if (strict)
                    coord = new int[]{-2, t.getColonne()};
                else
                    coord = new int[]{0, t.getColonne()};
            }
        }
        else if (dir == Direction.BG) {
            for (int i = t.getColonne()-1; i > -1; i--) {
                if (!tuiles[i][t.getLigne()].aucunObstacle()) { // On rencontre un obstacle : banco
                    coord = new int[]{t.getLigne(), i};
                    break;
                }
            }
            if (coord==null) {
                if (strict)
                    coord = new int[]{t.getLigne(), -2};
                else
                    coord = new int[]{t.getLigne(), 0};
            }
        }
        else if (dir == Direction.BD) {
            for (int i = t.getLigne()+1; i < DIM_X; i++) {
                if (!tuiles[i][t.getColonne()].aucunObstacle()) { // On rencontre un obstacle : banco
                    coord = new int[]{i, t.getColonne()};
                    break;
                }
            }
            if (coord==null) {
                if (strict)
                    coord = new int[]{DIM_X+1, t.getColonne()};
                else
                    coord = new int[]{DIM_X-1, t.getColonne()};
            }
        }
        else if (dir == Direction.HD) {
            for (int i = t.getColonne()+1; i < DIM_Y; i++) {
                if (!tuiles[t.getLigne()][i].aucunObstacle()) { // On rencontre un obstacle : banco
                    coord = new int[]{t.getLigne(), i};
                    break;
                }
            }
            if (coord==null) {
                if (strict)
                    coord = new int[]{t.getLigne(), DIM_Y+1};
                else
                    coord = new int[]{t.getLigne(), DIM_Y-1};
            }
        }

        return coord;
    }

    public Tuile getFirstObstacleT(Tuile t, Direction dir, boolean strict) {
        int[] coord = getFirstObstacle(t, dir, strict);

        return getCase(coord[0], coord[1]);
    }

    // Renvoie le robot présent dans la direction dir s'il y en a un, ou null sinon
    public Robot getFirstRobot(Tuile t, Direction dir) {
        int[] coord = getFirstObstacle(t, dir, false);

        if (getFirstObstacle(t, dir, false) != null) {
            if (!getCase(coord[0], coord[1]).estLibre())
                return getCase(coord[0], coord[1]).getRobot();
        }

        return null;
    }

    /* Setters
    * =================================== */

    public void setBarrieres(ArrayList<int[]> listeB) {
        // On ajoute chaque barrière à la liste locale
        Barriere temp;
        for (int[] coords : listeB) {
            temp = new Barriere(new Tuile(coords[0], coords[1]), new Tuile(coords[2], coords[3]));
            barrieres.add(temp);
        }
    }

    // Méthode définissant les obstacles de la grille
    public void setObstacles(ArrayList<int[]> obstacles) {
        for(int[] coord : obstacles) {
            // On définit l'obstacle sur la tuile
            getCase(coord[0], coord[1]).setObstacle(true);
        }
    }

    /* Autres méthodes d'ajout\modification
    * =================================== */

    // Retourne true s'il y a une barrière entre les deux tuiles en paramètre
    public boolean verifBarrieresTuile(Tuile t1, Tuile t2) {

        for (Barriere b : barrieres) {
            if (b.equals(new Barriere(t1,t2)))
                return true;
        }
        return false;
    }

    // Utilitaire d'affichage de la grille
    public void affiche() { //affiche chaque tuile de la grille plus détaille les tuiles qui contiennent un robot ou un obstacle
        System.out.println("\nGrille : ");
        for (int j = 0; j < 10; j++) {
            for (int i = 0; i < 10; i++) {
                tuiles[j][i].affiche();
                if (i != 9)
                    System.out.print(" | ");
            }
            if (j != 9)
                System.out.println("\n----------- ----------- ----------- ----------- ----------- ----------- ----------- ----------- ----------- ----------");
            else
                System.out.print("\n");
        }
        System.out.println("\nTuiles où se trouvent un robot : ");
        for (Tuile t : getTuilesJoueurs()) {
            t.afficheComplet();
            System.out.println("");
        }
        System.out.println ("===========================================");
    }
}

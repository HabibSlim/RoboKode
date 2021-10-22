package com.robokode.utils;

// LibGDX
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.math.Vector3;


// Classe pour les calculs géométriques
// destinée à alléger le code ailleurs
public class Math {

    // Marges hautes et latérales d'affichage
    public final static int MARGE_X = 7;
    public final static int MARGE_Y = 5;

    // Configuration de la taille des tiles (par défaut : 128 x 64)
    public static int TILE_WIDTH = 128;
    public static int TILE_HEIGHT = 64;

    // Renvoie les dimensions en pixels d'une map tmx passée en paramètres
    public static int[] getTmxDim(String path) {
        TiledMap tiledMap = new TmxMapLoader().load(path);
        MapProperties prop = tiledMap.getProperties();

        int mapWidth = prop.get("width", Integer.class);
        int mapHeight = prop.get("height", Integer.class);
        int tilePixelWidth = prop.get("tilewidth", Integer.class);
        int tilePixelHeight = prop.get("tileheight", Integer.class);

        // Affectation des attributs statiques
        TILE_HEIGHT = tilePixelHeight;
        TILE_WIDTH = tilePixelWidth;

        int height = mapHeight * tilePixelHeight;
        int width = mapWidth * tilePixelWidth;

        // On ajoute une marge 1/MARGE_X en hauteur et 1/MARGE_Y en largeur (objets\calques\décalage)
        if (MARGE_X != 0) {
            width += width/MARGE_X;
            height += height/MARGE_Y;
        }

        return new int[] {width, height};
    }

    // Fait la transformation coordonnées cartésiennes => isométriques
    public static Vector3 worldToIso(Vector3 point) {
        Vector3 tempPt = new Vector3();

        tempPt.x = point.x/TILE_WIDTH;
        tempPt.y = (point.y - TILE_HEIGHT / 2) / TILE_HEIGHT + tempPt.x;
        tempPt.x -= tempPt.y - tempPt.x;

        return tempPt;
    }

    // Fait la transformation coordonnées isométriques => cartésiennes
    public static Vector3 isoToWorld(Vector3 Tpoint) {
        Vector3 tempPt = new Vector3();

        tempPt.x = (TILE_WIDTH*(Tpoint.x + Tpoint.y))/2;
        tempPt.y = (2*TILE_HEIGHT*(Tpoint.y - tempPt.x/TILE_WIDTH) + TILE_HEIGHT)/2;
//
//      // Correction
        //tempPt.x -= TILE_WIDTH/15;
        tempPt.y += TILE_HEIGHT/4;

        return tempPt;
    }

    // Fait la transformation coordonnées isométriques => cartésiennes
    // => Pour les sprites joueur
    public static Vector3 isoToWorldPSP(Vector3 Tpoint) {
        Vector3 tempPt = new Vector3();

        tempPt.x = (TILE_WIDTH*(Tpoint.x + Tpoint.y))/2;
        tempPt.y = (2*TILE_HEIGHT*(Tpoint.y - tempPt.x/TILE_WIDTH) + TILE_HEIGHT)/2;
//
//      // Correction
        //tempPt.x -= TILE_WIDTH/15;
        tempPt.y -= TILE_HEIGHT/6;

        return tempPt;
    }

    // Corrige la destination en pixels d'un sprite laser
    public static void correctLaser(Vector3 init, Vector3 dest) {
        float ratio = 0.15f;

        dest.x = dest.x - ratio * (dest.x - init.x);
        dest.y = dest.y - ratio * (dest.y - init.y);
    }

    // Renvoie la valeur absolue d'un nombre
    public static float abs (float nb) {
        return (nb < 0) ? -nb : nb;
    }
    public static int abs (int nb) {
        return (nb < 0) ? -nb : nb;
    }

    // Renvoie la valeur y interpolée entre deux points (x1,y1) et (x0,y0) connus
    public static int interpolate(int x0, int y0, int x1, int y1, int x) {
        return (y0*(x1-x) + y1*(x-x0))/(x1 - x0);
    }

    // Renvoie la distance entre deux coordonnées alignées selon les vecteurs du repère isométrique
    public static float distance(Vector3 pA, Vector3 pB) {
        if (pA.x == pB.x)
            return abs(pA.y - pB.y);
        else
            return abs(pA.x - pB.x);
    }
}

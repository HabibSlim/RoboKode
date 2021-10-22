package com.robokode.utils.maps;

import com.robokode.utils.Ressources;

import java.util.HashMap;

/** Classe singleton servant à charger les informations d'une map **/
public class MapLoader {

    // HashMap faisant le lien ID => Map
    private HashMap<Integer,MapInfo> mapMap; // => BONNE VANNE :-)

    // Constructeur principal
    private MapLoader() {
        // On crée toutes les infos concernant les maps => pour l'instant en dur
        mapMap = new HashMap<Integer, MapInfo>();

        // => On charge toutes les maps
        MapInfo mapTemp;
        for (int i=0; i< Ressources.NB_LEVELS; i++) {
            mapTemp = new MapInfo(i);
            mapMap.put(i, mapTemp);
        }
    }

    // Récupération des infos d'une map indexée
    public MapInfo getInfo(int index) {
        return this.mapMap.get(index);
    }

    // Instance unique
    private static MapLoader INSTANCE = null;

    // Point d'accès
    public static MapLoader Instance()
    {
        if (INSTANCE == null)
        { 	INSTANCE = new MapLoader();
        }
        return INSTANCE;
    }
}

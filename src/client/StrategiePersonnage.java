package client;


import java.awt.Point;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import client.controle.Console;
import logger.LoggerProjet;
import serveur.Arene;
import serveur.IArene;
import serveur.element.Caracteristique;
import serveur.element.Personnage;
import utilitaires.Calculs;
import utilitaires.Constantes;

/**
 * Strategie d'un personnage. 
 */
public class StrategiePersonnage {
	
	/**
	 * Console permettant d'ajouter une phrase et de recuperer le serveur 
	 * (l'arene).
	 */
	protected Console console;
	private int nbDepCible;
	private int dist2en;// distance entre deux ennemis pour prevoir si il vont se battre
	private int distMoi2en;
	private int reffEn1;// ref de l'ennemi 1
	private int reffEn2;// ref de l'ennemi 2
	
	
	protected StrategiePersonnage(LoggerProjet logger){
		logger.info("Lanceur", "Creation de la console...");
	}

	/**
	 * Cree un personnage, la console associe et sa strategie.
	 * @param ipArene ip de communication avec l'arene
	 * @param port port de communication avec l'arene
	 * @param ipConsole ip de la console du personnage
	 * @param nom nom du personnage
	 * @param groupe groupe d'etudiants du personnage
	 * @param nbTours nombre de tours pour ce personnage (si negatif, illimite)
	 * @param position position initiale du personnage dans l'arene
	 * @param logger gestionnaire de log
	 */
	public StrategiePersonnage(String ipArene, int port, String ipConsole, 
			String nom, String groupe, HashMap<Caracteristique, Integer> caracts,
			int nbTours, Point position, LoggerProjet logger) {
		
		this(logger);
		try {
			console = new Console(ipArene, port, ipConsole, this, 
					new Personnage(nom, groupe, caracts), 
					nbTours, position, logger);
			logger.info("Lanceur", "Creation de la console reussie");
			
		} catch (Exception e) {
			logger.info("Personnage", "Erreur lors de la creation de la console : \n" + e.toString());
			e.printStackTrace();
		}
		nbDepCible = -1;
		dist2en = -1;
		reffEn1 = -1;
		reffEn2 = -1;
		distMoi2en = -1;
	}

	// TODO etablir une strategie afin d'evoluer dans l'arene de combat
	// une proposition de strategie (simple) est donnee ci-dessous
	/** 
	 * Decrit la strategie.
	 * Les methodes pour evoluer dans le jeu doivent etre les methodes RMI
	 * de Arene et de ConsolePersonnage. 
	 * @param voisins element voisins de cet element (elements qu'il voit)
	 * @throws RemoteException
	 */
	public void executeStrategie(HashMap<Integer, Point> voisins) throws RemoteException {
		// arene
		IArene arene = console.getArene();
		// reference RMI de l'element courant
		int refRMI = 0;
		// position de l'element courant
		Point position = null;			
		try {
			refRMI = console.getRefRMI();
			position = arene.getPosition(refRMI);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		
		if (voisins.isEmpty()) { // je n'ai pas de voisins, j'erre
			errerOuSoigner(refRMI); 			
		} else {
			ArrayList<Integer> persoVoisins = getPersonnageVoisins(arene,voisins);
			switch(persoVoisins.size()){
			case 0:// aucun personnage en vue
					
			case 1:// 1ennemi en vue
					duel1v1(refRMI,persoVoisins.get(0),arene);
					break;
			case 2:// 2 ennemis en vue
					duel1v2(refRMI,arene,persoVoisins);
					break;
			}	
		}
	}
	
	private void duel1v2(int refAtt,IArene arene,ArrayList<Integer> persoVoisins) throws RemoteException{
		Point position = arene.getPosition(refAtt);
		int dist2enTemp = nbDep(arene.getPosition((int)persoVoisins.get(0)),arene.getPosition((int)persoVoisins.get(1)));// on enregistre la distance entre les deux ennemis
		int reffEn1Temp = (int)persoVoisins.get(0);
		int reffEn2Temp = (int)persoVoisins.get(1);
		int distMoi2enTemp = nbDep(position,arene.getPosition((int)persoVoisins.get(0))) + 
				 			 nbDep(position,arene.getPosition((int)persoVoisins.get(1)));
		if (dist2en < 0){
			dist2en = dist2enTemp;
			distMoi2en = distMoi2enTemp;
			reffEn1 = reffEn1Temp;
			reffEn2 = reffEn2Temp;
		}else{
			if (reffEn1Temp == reffEn1 && reffEn2Temp == reffEn2){// le deux ennemis sont bien toujours les deux memes
				if (distMoi2en > distMoi2enTemp){
					distMoi2en = distMoi2enTemp;
					arene.deplace(refAtt, 0);
				}else{
					if(dist2en < dist2enTemp){
						dist2en = dist2enTemp;
						distMoi2en = distMoi2enTemp;
						reffEn1 = reffEn1Temp;
						reffEn2 = reffEn2Temp;
						duel1v1(refAtt,reffEn1Temp,arene);
					}else{
						arene.deplace(refAtt, 0);
					}
				}
			}else{
				dist2en = dist2enTemp;
				distMoi2en = distMoi2enTemp;
				reffEn1 = reffEn1Temp;
				reffEn2 = reffEn2Temp;
				arene.deplace(refAtt, 0);
			}
		}
		
	}
	
	private void duel1v1(int refAtt, int refCible, IArene arene) throws RemoteException{
		Point posCible = arene.getPosition(refCible);
		Point posAtt = arene.getPosition(refAtt);
		if (arene.estPersonnageFromRef(refCible)){// on verifie bien que l'on se bat avec un personnage
			nbDepCible = nbDep(posAtt, posCible);
			switch(nbDepCible){
			case 0: // on est au corp a corp avec l'adversaire
					arene.lanceAttaque(refAtt, refCible);
					break;
			case 1: // on est a un deplacement de l'adversaire
					int x = (int)posCible.getX();
					int y = (int)posCible.getY();
					ArrayList<Point> posAttaqueViable = posAttaqueValide(arene, posCible);
					if (!posAttaqueViable.isEmpty()){
						arene.deplace(refAtt, posAttaqueViable.get(0));
						arene.lanceAttaque(refAtt, refCible);
					}else{
						arene.deplace(refAtt, 0);
					}					
					break;
			case 2: // on est a deux deplacements de l'adversaire
					arene.lanceClairvoyance(refAtt, refCible);
			default:// on est au moins a deux deplacements de l'adversaire
				if(console.getPersonnage().getCaract(Caracteristique.VIE) <100){ //Si la vie n'est pas a son maximum on se soigne
					console.setPhrase("Je panse donc je suis.");
					arene.lanceAutoSoin(refAtt);
				}else{
					arene.deplace(refAtt, refCible);
				}
				break;
			}
		}
	}
	
	private int nbDep (Point moi, Point posCible){
		int xA = (int)moi.getX();//A = Attaquant
		int yA = (int)moi.getY();
		int xC = (int)posCible.getX();// C = Cible
		int yC = (int)posCible.getY();
		int nbDep;
		boolean memeLigneColonne = xA==xC || yA==yC;		
		if (xA > xC){
			if (yA>yC){
				nbDep = xA-xC+yA-yC;
			}else{
				nbDep = xA-xC+yC-yA;
			}
		}else{
			if (yA>yC){
				nbDep = xC-xA+yA-yC;
			}else{
				nbDep = xC-xA+yC-yA;
			}
		}
		if (memeLigneColonne){
			return nbDep-1;
		}else{
			return nbDep-2;
		}
	}
	
	private int projection(int force){
		int max = Caracteristique.FORCE.getMax();		
		int quart = (int) (4 * ((float) (force - 1) / max)); // -1 pour le cas force = 100
		return Constantes.DISTANCE_PROJECTION[quart];
	}
	
	private ArrayList<Point> posAttaqueValide(IArene arene, Point posCible){
		
		ArrayList<Point> posPotentielles = new ArrayList<Point>();
		int x = (int)posCible.getX();
		int y = (int)posCible.getY();
		posPotentielles.add(new Point(x-1,y+1));
		posPotentielles.add(new Point(x,y+1));
		posPotentielles.add(new Point(x+1,y+1));
		posPotentielles.add(new Point(x-1,y));
		posPotentielles.add(new Point(x+1,y));
		posPotentielles.add(new Point(x-1,y-1));
		posPotentielles.add(new Point(x,y-1));
		posPotentielles.add(new Point(x-1,y-1));
		
		Iterator<Point> it = posPotentielles.iterator();
		while(it.hasNext()){
			Point pointATest = it.next();
			if(!verifDansBorne(pointATest)){
				int diffX = (int)pointATest.getX()-x;
				int diffY = (int)pointATest.getY()-y;
				Point ptSymetrique = new Point(-diffX,-diffY);
				posPotentielles.remove(ptSymetrique);
				posPotentielles.remove(pointATest);				
			}
		}
		return posPotentielles;
	}

	private boolean verifDansBorne(Point p){
		return
			(p.x > Constantes.XMIN_ARENE + Calculs.getOffset()) &&
			(p.x < Constantes.XMAX_ARENE - Calculs.getOffset()) &&
			(p.y > Constantes.YMIN_ARENE + Calculs.getOffset()) &&
			(p.y < Constantes.YMAX_ARENE - Calculs.getOffset());
	}
	
	/**
	 * Methode qui permet de soit se soigner, soit d'errer si la vie est deja a son maximum
	 * @param refRMI la reference RMI du personnage
	 * @param arene l'arene dans laquelle on se bat
	 * @throws RemoteException
	 */
	private void errerOuSoigner(int refRMI) throws RemoteException{
		IArene arene = console.getArene();
		if(console.getPersonnage().getCaract(Caracteristique.VIE) <100){ //Si la vie n'est pas a son maximum on se soigne
			console.setPhrase("Je panse donc je suis.");
			arene.lanceAutoSoin(refRMI);
		}
		else{
			console.setPhrase("Dans quel etat j'erre ?");
			arene.deplace(refRMI, 0); 
		}
	}
	
	/**
	 * methode Qui se deplace vers un element non joueur,
	 * le tue si c'est un monstre
	 * @param refRMI la reference RMI du personnage
	 * @param arene l'arene dans laquelle on se bat
	 * @throws RemoteException
	 */
//	private void actionENJ(int refRMI, HashMap<Integer, Point> voisins, int refCible) throws RemoteException{
//		IArene arene = console.getArene();
//		Point origine = arene.getPosition(refRMI);
//
//		if(	Calculs.chercheElementProche(origine, voisins) == refCible && 
//				Calculs.distanceChebyshev(origine, arene.getPosition(refCible) )<= Constantes.DISTANCE_MIN_INTERACTION ){
//			if(arene.estMonstreFromRef(refRMI)){
//				console.setPhrase("Meurs creature demoniaque !");
//				arene.lanceAttaque(refRMI, refCible);
//			}
//			else if(arene.estPotionFromRef(refRMI)){
//				console.setPhrase("Je bois un verre de " + arene.nomFromRef(refCible));
//				arene.ramassePotion(refRMI, refCible);
//			}
//		}
//		else
//			arene.deplace(refRMI, refCible);
//	}
	
	private void recupPotion(int refRMI, int refPotion, IArene arene) throws RemoteException{
		if(arene.estPotionFromRef(refRMI) && Calculs.distanceChebyshev(arene.getPosition(refRMI),arene.getPosition(refPotion)) <= Constantes.DISTANCE_MIN_INTERACTION){
			console.setPhrase("Je bois un verre de " + arene.nomFromRef(refPotion));
			arene.ramassePotion(refRMI, refPotion);
		}else{
			arene.deplace(refRMI, refPotion);
		}		
	}
		

	
	/**
	 * @param voisins element voisins de cet element (elements qu'il voit)
	 * @return une Hashmap qui contient uniquement les personnages visibles
	 * @throws RemoteException
	 */
	private HashMap<Integer, Point> getPersonnagesVoisins(HashMap<Integer,Point>  voisins) throws RemoteException{
		HashMap<Integer, Point> personnages = voisins;
		IArene arene = console.getArene();
		Iterator<Integer> it = personnages.keySet().iterator();
		while(it.hasNext()){
			int reference = (int)it.next();
			if(!arene.estPersonnageFromRef(reference)){
				personnages.remove(reference);
			}
		}
		return personnages;	
	}

	/**
	 * @param voisins element voisins de cet element (elements qu'il voit)
	 * @return une Hashmap qui contient uniquement les monstres visibles
	 * @throws RemoteException
	 */
	private HashMap<Integer, Point> getMonstresVoisins(HashMap<Integer,Point>  voisins) throws RemoteException{
		HashMap<Integer, Point> monstres = voisins;
		IArene arene = console.getArene();
		Iterator<Integer> it = monstres.keySet().iterator();
		while(it.hasNext()){
			int reference = (int)it.next();
			if(!arene.estMonstreFromRef(reference)){
				monstres.remove(reference);
			}
		}
		return monstres;	
	}


	/**
	 * @param voisins element voisins de cet element (elements qu'il voit)
	 * @return une Hashmap qui contient uniquement les potions utiles visibles
	 * @throws RemoteException
	 */
	private HashMap<Integer, Point> getPotionsVoisins(HashMap<Integer,Point>  voisins) throws RemoteException{
		HashMap<Integer, Point> potions = voisins;
		IArene arene = console.getArene();
		Iterator<Integer> it = potions.keySet().iterator();
		while(it.hasNext()){
			int reference = (int)it.next();
			if(!arene.estPotionFromRef(reference))
				potions.remove(reference);
			else{
				if(!estPotionUtile(reference))
					potions.remove(reference);					
			}
		}
		return potions;	
	}
	
	private ArrayList<Integer> getPotionsVoisins(IArene arene, HashMap<Integer,Point>  voisins) throws RemoteException{
		ArrayList<Integer> potions = new ArrayList<Integer>();
		Iterator<Integer> it = voisins.keySet().iterator();
		while(it.hasNext()){
			int reference = (int)it.next();
			if(!arene.estPotionFromRef(reference) && estPotionUtile(reference)){
					potions.add(reference);					
			}
		}
		return potions;
	}
	
	private ArrayList<Integer> getMonstreVoisins(IArene arene, HashMap<Integer,Point>  voisins) throws RemoteException{
		ArrayList<Integer> monstres = new ArrayList<Integer>();
		Iterator<Integer> it = voisins.keySet().iterator();
		while(it.hasNext()){
			int reference = (int)it.next();
			if(!arene.estMonstreFromRef(reference)){
					monstres.add(reference);					
			}
		}
		return monstres;
	}
	
	private ArrayList<Integer> getPersonnageVoisins(IArene arene, HashMap<Integer,Point>  voisins) throws RemoteException{
		ArrayList<Integer> personnages = new ArrayList<Integer>();
		Iterator<Integer> it = voisins.keySet().iterator();
		while(it.hasNext()){
			int reference = (int)it.next();
			if(!arene.estPersonnageFromRef(reference) && estPotionUtile(reference)){
					personnages.add(reference);					
			}
		}
		return personnages;
	}

	/**
	 * verifie qu'une potion est utile :
	 * le cumul des caracteristiques apportees est superieur a 0
	 * elle ne nous fait pas passer en dessous de 15 HP ni perdre 10 de force ou d'armure
	 * @param refPotion la reference de la potion a tester
	 * @return si la potion est consideree utile
	 * @throws RemoteException
	 */
	private boolean estPotionUtile( int refPotion) throws RemoteException{
		boolean estPotionUtile = false;
		IArene arene = console.getArene();
		//Si le cumul des caracteristiques obtenus est strictement positif, on envisage de prendre la potion
		if(getCumulCaracPotion(refPotion)>0){
			//Si prendre la potion ne nous fait pas descendre en dessous de 15 HP
			if(console.getPersonnage().getCaract(Caracteristique.VIE) >=  -(arene.caractFromRef(refPotion, Caracteristique.VIE)+ 15)){ 
				//Si prendre la potion ne nous fait pas perdre 10 ou plus de force
				if(arene.caractFromRef(refPotion, Caracteristique.FORCE) > -10){
					//Si prendre la potion ne nous fait pas perre 10 ou plus de defense
					if(arene.caractFromRef(refPotion, Caracteristique.DEFENSE) > -10){
						estPotionUtile = true;
					}
				}
			}
		}
		return estPotionUtile;
	}

	/**
	 * @param refPotion la reference de la potion qui nous interesse
	 * @return la somme de toute les caracteristique qu'apporte la potion
	 * @throws RemoteException
	 */
	private int getCumulCaracPotion(int refPotion) throws RemoteException{
		IArene arene = console.getArene();
		int bonusTotalPotion = -666;
		if(arene.estPotionFromRef(refPotion)){
			bonusTotalPotion = 0;
			Iterator<Caracteristique> it = console.getPersonnage().getCaracts().keySet().iterator();
			while(it.hasNext()){
				bonusTotalPotion+= arene.caractFromRef(refPotion, it.next());
			}
		}
		return bonusTotalPotion;
	}

	/**
	 * @param refRMI la reference RMI du personnage
	 * @param cibles une HashMap d'Element representes par leur reference RMI et leur position
	 * @return la reference de l'element de cibles le plus proche du personnage
	 * @throws RemoteException
	 */
	private int getRefElemenPlusProche(int refRMI, HashMap<Integer, Point> cibles) throws RemoteException{
		int refPlusProche = -1;
		int distPlusProche = 666;
		Point pointPerso = console.getArene().getPosition(refRMI);
		Iterator<Integer> it = cibles.keySet().iterator();
		while(it.hasNext()){
			int ref = it.next();
			int distance = Calculs.distanceChebyshev(pointPerso, cibles.get(ref));
			if(distance<distPlusProche){
				distPlusProche = distance;
				refPlusProche = ref;
			}
		}
		return refPlusProche;
	}
}
	

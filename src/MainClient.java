import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class MainClient {

    private static JDBC database;
    private static Client c;
    private static List<Panier> panierArchiver;
    private static String separator = "------------------------------------------------------------------------------------------------------------";


    public static void main(String[] args) {

        database = new JDBC(ProjectConfig.getURL(), ProjectConfig.getUsername(), ProjectConfig.getPassword());
        panierArchiver = new ArrayList<>();
        Scanner sc = new Scanner(System.in);

        System.out.println("Bienvenue sur notre application de courses en ligne !");
        System.out.println("Veuillez vous connecter pour continuer: (votre identifiant)");

        int id = sc.nextInt();

        if (id == 999) {
            // admin
            MainAdmin.main(args);
        } else if (id == 998) {
            // preparateur
            MainPreparateur.main(args);
        }

        int idVille = 0;

        while (idVille == 0) {
            idVille = choixVille();
        }

        c = new Client(id, idVille, new Profil(id, database), database);


        System.out.println();

        char cas = ' ';

        while (cas != 'q') {
            System.out.println(separator);
            System.out.println("a. Consulter le catalogue de produit");
            System.out.println("c. Consulter son panier");
            System.out.println("p. Consulter son profil");
            System.out.println("o. /!\\ Voir les produit du moment /!\\");
            System.out.println("u. Changer d'utilisateur");
            System.out.println("q. Quitter l'application");
            System.out.println(separator);

            cas = sc.next().charAt(0);

            switch (cas) {
                case 'a':
                    AffCatalogue();
                    break;
                case 'c':
                    AffPanier();
                    break;
                case 'p':
                    AffProfil();
                    break;
                case 'o':
                    ProdMoment();
                    break;
                case 'u':
                    main(args);
                case 'q':
                    exitApp();
                    break;
                default:
                    System.out.println("Cas non reconnu");
            }
        }
        database.disconnect();
    }

    public static void AffCatalogue() {
        String s = "";
        Scanner sc = new Scanner(System.in);

        while (!s.equals("q")) {
            System.out.println(separator);
            System.out.println("m. Rechercher un produit par mot clé");
            System.out.println("c. Rechercher un produit par catégorie");
            System.out.println("q. Revenir au menu principal");
            System.out.println(separator);

            s = sc.next();
            switch (s) {
                case "m":
                    System.out.println("Entrez le mot clé : ");
                    List<Produit> prodKey = Research.searchByKeyword(database, sc.next());
                    if (prodKey.isEmpty()) System.out.println("Aucun produit trouvé");
                    else {
                        AffProduitList(prodKey);
                        Produit p = AjouterAuPanier(prodKey);
                        if (p != null) ajouterRecommandation(p);
                    }
                    break;
                case "c":
                    System.out.println("Entrez la catégorie : ");
                    List<Produit> prodCat = Research.listCategory(database, sc.next());
                    if (prodCat.isEmpty()) System.out.println("Aucun produit trouvé");
                    else {
                        AffProduitList(prodCat);
                        Produit p = AjouterAuPanier(prodCat);
                        if (p != null) ajouterRecommandation(p);
                    }
                    break;
                default:
                    System.out.println("Cas non reconnu");
            }
        }
    }

    public static void AffPanier() {
        String s = "";
        Scanner sc = new Scanner(System.in);
        while (!s.equals("q")) {
            System.out.println(separator);
            System.out.println("a. Afficher les produits dans mon panier");
            System.out.println("v. Valider mon panier");
            System.out.println("c. Archiver mon panier");
            System.out.println("r. Restorer mon panier");
            System.out.println("d. Vider mon panier");
            System.out.println("q. Revenir au menu principal");
            System.out.println(separator);

            s = sc.next();
            switch (s) {
                case "a":
                    afficherProduitPanier(c.getPanier().getProduits());
                    break;
                case "v":
                    validerPanier();
                    break;
                case "c":
                    archiverPanier();
                    break;
                case "r":
                    restorerPanier();
                    break;
                case "d":
                    viderPanier();
                    break;
                default:
                    System.out.println("Cas non reconnu");
            }
        }
    }

    public static void ProdMoment() {
        List<List<String>> prods = database.executeQuery(
                """
                        SELECT DISTINCT
                    Produit.ProduitId,
                    NomProd,
                    PrixAuKg,
                    PrixUnitaire,
                    Poids,
                    cs.NomCat AS "Sou-Categorie",
                    cp.NomCat AS "Categorie Principale",
                    Marque,
                    Nutriscore,
                    bio
                FROM Produit
                JOIN Categorie cs ON Produit.CategorieId = cs.CategorieId
                JOIN Etre ON cs.CategorieId = Etre.CategorieId_SousCategorie
                JOIN Categorie cp ON Etre.CategorieId_Principale = cp.CategorieId
                JOIN CategoriePhare On cs.CategorieId = CategoriePhare.CategorieId
                        """
        );



        System.out.println("Produits du moment : ");

        List<Produit> listProd = new ArrayList<>();
        for (List<String> row : prods) {
            Produit p = new Produit(row);
            listProd.add(p);
        }

        listProd.addAll(Recommandation.RecommanderPeriode(getPeriodeId(), database));

        AffProduitList(listProd);
        AjouterAuPanier(listProd);

    }

    public static void exitApp() {
        System.out.println("Merci d'avoir utilisé notre application");
        System.exit(0);
    }


    public static void afficherProduitPanier(HashMap<Produit, Integer> ListProduit) {
        System.out.println("Panier : ");
        String header = String.format("%8s | %-30s | %-15s | %-15s | %-15s",
                "Quantite", "Nom Produit", "Marque", "Prix", "Categorie");

        System.out.println(separator);
        System.out.println(header);
        System.out.println(separator);

        for (Produit produit : ListProduit.keySet()) {
            String prix;
            if (produit.getPrixAuKg() == 0) prix = produit.getPrixUnitaire() + "€/u";
            else prix = produit.getPrixAuKg() + "€/kg";
            System.out.printf("%8s | %-30s | %-15s | %-15s| %-15s%n",
                    ListProduit.get(produit),
                    produit.getLibelle(),
                    produit.getMarque(),
                    prix,
                    produit.getSouCategorie());

        }
        System.out.println(separator);

        System.out.println();
    }

    public static void validerPanier() {
        c.validatePanier();
        System.out.println("Panier validé");
        System.out.println("Merci d'avoir effectué vos courses dans nos magasins !");
        System.exit(0);
    }

    public static void archiverPanier() {
        System.out.println("Panier archivé");
        panierArchiver.add(c.getPanier().archivePanier());
    }

    public static void restorerPanier() {
        System.out.println("Paniers archivés : ");
        for (Panier p : panierArchiver) {
            System.out.println("Panier n°" + panierArchiver.indexOf(p) + 1 + " : ");
            afficherProduitPanier(p.getProduits());
        }
        System.out.println("Entrez le numéro du panier à restaurer : ");
        Scanner sc = new Scanner(System.in);

        int num = sc.nextInt() - 1;
        if (num < 0 || num >= panierArchiver.size()) {
            System.out.println("Panier non trouvé");
        } else {
            c.getPanier().restorePanier(panierArchiver.get(num));
            System.out.println("Panier restauré");
        }
    }

    public static void viderPanier() {
        System.out.println("Etes-vous sûr de vouloir vider votre panier ? (o : oui, autre caractère : non)");
        Scanner sc = new Scanner(System.in);
        String s = sc.next();
        if (s.equals("o")) {
            c.getPanier().clear();
            System.out.println("Panier vidé");
        } else System.out.println("Opération annulée");
    }


    public static void AffProfil() {
        String s = "";
        while (!s.equals("q")) {
            System.out.println(separator);
            System.out.println("a. Consulter les produits préférés");
            System.out.println("b. Consulter son historique des commandes");
            System.out.println("c. Consulter ses préférences de consomation");
            System.out.println("q. Revenir au menu principal");
            System.out.println(separator);

            Scanner sc = new Scanner(System.in);

            s = sc.next();

            switch (s) {
                case "a":
                    AffProduitList(c.getProfil().getArticlesPref());
                    break;
                case "b":
                    AffDerniereCommandes();
                    break;
                case "c":
                    AffHabitudesConsommation();
                    break;
                case "q":
                    break;
                default:
                    System.out.println("Cas non reconnu");
            }
        }

    }

    public static void AffHabitudesConsommation() {
        System.out.println("Votre profil de consommation : ");
        for (String s : c.getProfil().getNomProfils()) {
            System.out.print("Type de profil : "+s+" ");
        }
        System.out.println(".");
        ClientPreferences.getClientPreferences(c.getIdClient(), database);
    }

    //Retourne l'id de la periode si il le trouve et -1 sinon
    public static int getPeriodeId(){
        List<List<String>> periode = database.executeQuery("SELECT periodeId\n" +
                "FROM Periode\n" +
                "WHERE (TO_CHAR(debutPeriode, 'MM-DD') <= TO_CHAR(SYSDATE, 'MM-DD')\n" +
                "       AND TO_CHAR(finPeriode, 'MM-DD') >= TO_CHAR(SYSDATE, 'MM-DD'))\n" +
                "   OR (TO_CHAR(debutPeriode, 'MM-DD') > TO_CHAR(finPeriode, 'MM-DD')\n" +
                "       AND (TO_CHAR(SYSDATE, 'MM-DD') >= TO_CHAR(debutPeriode, 'MM-DD')\n" +
                "            OR TO_CHAR(SYSDATE, 'MM-DD') <= TO_CHAR(finPeriode, 'MM-DD')))");
        if (!periode.isEmpty()) {
            return Integer.parseInt(periode.get(0).get(0));
        }
        else return -1;
    }


    public static void ajouterRecommandation(Produit p) {
        List<Produit> listProd = Recommandation.Recommander(p, database);
        int idPeriode = getPeriodeId();

        if (idPeriode != -1) listProd.addAll(Recommandation.RecommanderPeriode(idPeriode, database));

        System.out.println();
        if (listProd.isEmpty()) return;

        Statistique.NbProposeRecom++;
        System.out.println("Produit recomandée et produit de saison :");
        AffProduitList(listProd);


        System.out.println("Ajouter un produit de recomandation au panier ? (o : oui, n : non)");
        Scanner sc = new Scanner(System.in);
        String s = sc.next();


        if (s.equals("o")) {
            System.out.println("Entrez le numéro du produit à ajouter : ");
            int num = sc.nextInt() - 1;
            if (num < 0 || num >= listProd.size()) {
                System.out.println("Produit non trouvé");
            } else {
                System.out.println("Entrez la quantité : ");
                int qte = sc.nextInt();
                Produit p1 = listProd.get(num);
                c.addProduct(p1, qte);
                Statistique.NbChoisiRecom++;
            }
        }


    }

    public static int choixVille() {
        Scanner sc = new Scanner(System.in);
        System.out.println("Dans quelle ville souhaitez-vous faire vos courses ?");
        String ville = sc.next();

        List<List<String>> listeVille = database.executeQuery("SELECT * FROM Magasin WHERE VilleMag = '" + ville + "'");

        if (listeVille == null || listeVille.isEmpty()) {
            System.out.println("Aucun magasin dans cette ville.");
            return 0;
        }

        System.out.println("Liste des magasins :");
        String header = String.format("%2s | %-40s | %-40s | %-10s",
                "n°", "Nom Magasin", "Adresse du magasin", "Code postal");

        System.out.println(separator);
        System.out.println(header);
        System.out.println(separator);
        for (List<String> row : listeVille) {
            System.out.printf("%2s | %-40s | %-40s | %-10s%n",
                    listeVille.indexOf(row) + 1,
                    row.get(1),
                    row.get(2),
                    row.get(3));
        }
        System.out.println(separator);

        System.out.println("Entrez le numéro du magasin choisi : ");
        int numMag = sc.nextInt() - 1;

        if (numMag < 0 || numMag >= listeVille.size()) {
            System.out.println("Magasin non trouvé");
            return 0;
        }
        return Integer.parseInt(listeVille.get(numMag).get(0));

    }

    public static void AffProduitList(List<Produit> listProd) {
        String header = String.format("%2s | %-30s | %-15s | %-15s | %-15s",
                "n°", "Nom Produit", "Marque", "Prix", "Categorie");

        System.out.println(separator);
        System.out.println(header);
        System.out.println(separator);

        for (Produit produit : listProd) {
            String prix;
            if (produit.getPrixAuKg() == 0) prix = produit.getPrixUnitaire() + "€/u";
            else prix = produit.getPrixAuKg() + "€/kg";
            System.out.printf("%2s | %-30s | %-15s | %-15s | %-15s%n",
                    listProd.indexOf(produit) + 1,
                    produit.getLibelle(),
                    produit.getMarque(),
                    prix,
                    produit.getSouCategorie());
        }
        System.out.println(separator);
        Scanner sc = new Scanner(System.in);
        System.out.println("Trier les resultats ? o : oui, n : non");
        String s = sc.next();
        if (s.equals("o")) {
            System.out.println("Entrez le critère de tri : (NomProd, PrixUnitaire, PrixAuKg, Poids, Nutriscore, Marque, Sous-Categorie,Categorie Principale)");
            String critere = sc.next();
            System.out.println("Ordre croissant ? o : oui, n : non");
            boolean croissant = sc.next().equals("o");
            listProd = Research.orderList(listProd, critere, croissant);
            AffProduitList(listProd);
        }
    }

    public static void AffProduitBd(List<List<String>> listProd) {


        String header = String.format("%2s | %-30s | %-15s | %-15s | %-15s",
                "n°", "Nom Produit", "Marque", "Prix", "Categorie");

        System.out.println(separator);
        System.out.println(header);
        System.out.println(separator);

        for (List<String> produit : listProd) {
            String prix;
            if (produit.get(2) == null) prix = produit.get(3) + "€/u";
            else prix = produit.get(2) + "€/kg";
            System.out.printf("%2s | %-30s | %-15s | %-15s | %-15s%n",
                    listProd.indexOf(produit) + 1,
                    produit.get(1),
                    produit.get(7),
                    prix,
                    produit.get(6));

        }
        System.out.println(separator);
    }

    public static void AffDerniereCommandes() {
        String requete = "SELECT \n" +
                "    TO_CHAR(c.HeureDebut, 'YYYY-MM-DD HH24:MI:SS') AS DateCommandeFormattee,\n" +
                "    c.CommandeId,\n" +
                "    SUM(\n" +
                "        CASE\n" +
                "            WHEN p.PrixUnitaire IS NOT NULL\n" +
                "            THEN comp.QteCom * p.PrixUnitaire\n" +
                "            ELSE comp.QteCom * p.PrixAuKg * p.Poids / 1000\n" +
                "        END\n" +
                "    ) AS MontantTotalCommande,\n" +
                "    c.etatcom\n" +
                "FROM \n" +
                "    Commande c, Composer comp, Produit p\n" +
                "WHERE c.CommandeId = comp.CommandeId\n" +
                "AND comp.ProduitId = p.ProduitId\n" +
                "AND c.clientid = " + c.getIdClient() + "\n" +
                "GROUP BY c.HeureDebut, c.etatcom, c.CommandeId\n" +
                "ORDER BY c.HeureDebut DESC";

        List<List<String>> result = database.executeQuery(requete);
        if (result.isEmpty()) System.out.println("Aucune commande trouvée");
        else {
            System.out.println("Historique des commandes : ");
            String header = String.format("%2s | %-30s | %-15s | %-15s",
                    "n°", "Date", "Montant", "Statut");

            System.out.println(separator);
            System.out.println(header);
            System.out.println(separator);

            for (List<String> row : result) {
                System.out.printf("%2s | %-30s | %-15s | %-15s%n",
                        result.indexOf(row) + 1,
                        row.get(0),
                        row.get(2) + "€",
                        row.get(3));
            }
            System.out.println(separator);
        }
        System.out.println("Afficher une commande en détails ? (o : oui, n : non)");
        Scanner sc = new Scanner(System.in);
        String s = sc.next();
        if (s.equals("o")) {
            System.out.println("Entrez le numéro de la commande à afficher : ");
            int num = sc.nextInt() - 1;
            if (num < 0 || num >= result.size()) {
                System.out.println("Commande non trouvée");
            } else {
                List<List<String>> listProd = database.executeQuery(
                        "SELECT DISTINCT\n" +
                                "        Produit.ProduitId,\n" +
                                "        NomProd,\n" +
                                "        PrixAuKg,\n" +
                                "        PrixUnitaire,\n" +
                                "        Poids,\n" +
                                "        cs.NomCat AS \"Sou-Categorie\",\n" +
                                "        cp.NomCat AS \"Categorie Principale\",\n" +
                                "        Marque,\n" +
                                "        Nutriscore,\n" +
                                "        bio\n" +
                                "    FROM Produit\n" +
                                "    JOIN Categorie cs ON Produit.CategorieId = cs.CategorieId\n" +
                                "    JOIN Etre ON cs.CategorieId = Etre.CategorieId_SousCategorie\n" +
                                "    JOIN Categorie cp ON Etre.CategorieId_Principale = cp.CategorieId\n" +
                                "    JOIN Preferer pr ON pr.ProduitId = Produit.ProduitId\n" +
                                "    JOIN Composer c ON Produit.ProduitId = c.ProduitId\n" +
                                "    WHERE c.CommandeId = " + result.get(num).get(1));
                AffProduitBd(listProd);
            }
        }
    }

    public static Produit AjouterAuPanier(List<Produit> prods) {
        System.out.println("Ajouter un produit au panier ? (o : oui, n : non)");
        Scanner sc = new Scanner(System.in);
        String s = sc.next();
        if (s.equals("o")) {
            System.out.println("Entrez le numéro du produit à ajouter : ");
            int num = sc.nextInt() - 1;
            if (num < 0 || num >= prods.size()) {
                System.out.println("Produit non trouvé");
            } else {
                System.out.println("Entrez la quantité : ");
                int qte = sc.nextInt();
                Produit p1 = prods.get(num);
                c.addProduct(p1, qte);
                return p1;
            }
        }
        return null;
    }
}

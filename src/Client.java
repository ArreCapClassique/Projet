public class Client {
    private int idClient;
    private String Nom;
    private String Prenom;
    private int Age;
    private String Adresse;
    private Panier panier;
    private Profil profil;


    public Client(int idClient, String Nom, String Prenom, int Age, String Adresse, Profil profil) {
        this.idClient = idClient;
        this.Nom = Nom;
        this.Prenom = Prenom;
        this.Age = Age;
        this.Adresse = Adresse;
        this.panier = new Panier();
        this.profil = profil;
    }

    public Client(int idClient){
        JDBC database = new JDBC("jdbc:oracle:thin:@localhost:1521:orclcdb", "C##ADMINMIAGE", "adminmiage");
    }



    public void validate(){
        JDBC database = new JDBC("jdbc:oracle:thin:@localhost:1521:orclcdb", "C##ADMINMIAGE", "adminmiage");

        database.execute("INSERT INTO Commande (commandeId, DateCommande, MagId, ClientId) " +
                "VALUES (commandeId_seq.nextval, 1, SYSDATE, " + 0 + ")");
    }




    public int getIdClient() {
        return idClient;
    }

    public void setIdClient(int idClient) {
        this.idClient = idClient;
    }

    public String getNom() {
        return Nom;
    }

    public void setNom(String nom) {
        Nom = nom;
    }

    public String getPrenom() {
        return Prenom;
    }

    public void setPrenom(String prenom) {
        Prenom = prenom;
    }

    public int getAge() {
        return Age;
    }

    public void setAge(int age) {
        Age = age;
    }

    public String getAdresse() {
        return Adresse;
    }

    public void setAdresse(String adresse) {
        Adresse = adresse;
    }

    public Panier getPanier() {
        return panier;
    }

    public void setPanier(Panier panier) {
        this.panier = panier;
    }

    public Profil getProfil() {
        return profil;
    }

    public void setProfil(Profil profil) {
        this.profil = profil;
    }
}

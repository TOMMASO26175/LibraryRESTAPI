import io.github.cdimascio.dotenv.Dotenv;

public class Database {
    public static String[] getData(){
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch ( ClassNotFoundException e ) {
            e.printStackTrace();
        }
        Dotenv dotenv = Dotenv.load();
        final String DB_URL = "jdbc:mysql://rds-mysql-main.ck4bf8ke7zt9.eu-central-1.rds.amazonaws.com/School?allowMultiQueries=true";
        final String USER = "admin";
        final String PASS = dotenv.get("DB_PASS");
        String[] data = new String[3];
        data[0] = DB_URL;
        data[1] = USER;
        data[2] = PASS;
        return data;
    }
}

import java.sql.*;


public class Database {
    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch ( ClassNotFoundException e ) {
            e.printStackTrace();
        }
        String isbn = "FJALSS";
        final String DB_URL = "jdbc:mysql://rds-mysql-main.ck4bf8ke7zt9.eu-central-1.rds.amazonaws.com/School";
        final String USER = "admin";
        final String PASS = "Ym4&lrSyAGv";
        final String QUERY = "SELECT Autore,Titolo FROM Libri WHERE ISBN = ?";
        String username = null;

        try (
                Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                PreparedStatement pstmt = conn.prepareStatement( QUERY )
        )
        {
            Library library = new Library();
            System.out.println(library.read(conn));
        } catch ( SQLException e ) {
            e.printStackTrace();
        }


    }
}
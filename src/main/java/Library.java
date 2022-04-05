import com.google.gson.Gson;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/book")
public class Library {
    private final String error = "Server error, contact administrators";
    private boolean checkParams(String isbn,String autore, String titolo){
        return (isbn == null || isbn.trim().length() == 0) || (titolo == null || titolo.trim().length() == 0) || (autore == null || autore.trim().length() == 0);
    }

    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    public Response read(){
        final String QUERY = "SELECT * FROM Libri";
        final List<Book> books = new ArrayList<>();
        final String[] data = Database.getData();
        try(

                Connection conn = DriverManager.getConnection(data[0], data[1], data[2]);
                PreparedStatement pstmt = conn.prepareStatement( QUERY )
        ) {
            ResultSet results =  pstmt.executeQuery();
            while (results.next()){
                Book book = new Book();
                book.setTitolo(results.getString("Titolo"));
                book.setAutore(results.getString("Autore"));
                book.setISBN(results.getString("ISBN"));
                book.setQuantity(results.getString("Quantity"));
                books.add(book);

            }
        }catch (SQLException e){
            e.printStackTrace();
            String obj = new Gson().toJson(error);
            return Response.serverError().entity(obj).build();
        }
        String obj = new Gson().toJson(books);
        return Response.status(200).entity(obj).build();
    }

    @PUT
    @Path("/update")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response update(@FormParam("ISBN") String isbn,
                           @FormParam("Titolo")String titolo,
                           @FormParam("Autore") String autore){
        if(checkParams(isbn, titolo, autore)) {
            String obj = new Gson().toJson("Parameters must be valid");
            return Response.serverError().entity(obj).build();
        }
        final String QUERY = "UPDATE Libri SET Titolo = ?, Autore = ? WHERE ISBN = ?";
        final String[] data = Database.getData();
        try(

                Connection conn = DriverManager.getConnection(data[0], data[1], data[2]);
                PreparedStatement pstmt = conn.prepareStatement( QUERY )
        ) {
            pstmt.setString(1,titolo);
            pstmt.setString(2,autore);
            pstmt.setString(3,isbn);
            pstmt.execute();
        }catch (SQLException e){
            e.printStackTrace();
            String obj = new Gson().toJson(error);
            return Response.serverError().entity(obj).build();
        }
        String obj = new Gson().toJson("Libro con ISBN:" + isbn + " modificato con successo");
        return Response.ok(obj,MediaType.APPLICATION_JSON).build();
    }

    @POST
    @Path("/add")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response create(@FormParam("ISBN") String isbn,
                           @FormParam("Titolo")String titolo,
                           @FormParam("Autore") String autore){
        if(checkParams(isbn, titolo, autore)) {
            String obj = new Gson().toJson("Parameters must be valid");
            return Response.serverError().entity(obj).build();
        }
        final String QUERY = "INSERT INTO Libri(ISBN,Titolo,Autore) VALUES(?,?,?)";
        final String[] data = Database.getData();
        try(

                Connection conn = DriverManager.getConnection(data[0], data[1], data[2]);
                PreparedStatement pstmt = conn.prepareStatement( QUERY )
        ) {
            pstmt.setString(1,isbn);
            pstmt.setString(2,autore);
            pstmt.setString(3,titolo);
            pstmt.execute();
        }catch (SQLException e){
            e.printStackTrace();
            String obj = new Gson().toJson(error);
            return Response.serverError().entity(obj).build();
        }
        String obj = new Gson().toJson("Libro con ISBN:" + isbn + " aggiunto con successo");
        return Response.ok(obj,MediaType.APPLICATION_JSON).build();
    }

    @DELETE
    @Path("/delete")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response update(@FormParam("ISBN") String isbn){
        if(isbn == null || isbn.trim().length() == 0){
            String obj = new Gson().toJson("ISBN must be valid");
            return Response.serverError().entity(obj).build();
        }
        final String QUERY = "DELETE FROM Libri WHERE ISBN = ?";
        final String[] data = Database.getData();
        try(

                Connection conn = DriverManager.getConnection(data[0], data[1], data[2]);
                PreparedStatement pstmt = conn.prepareStatement( QUERY )
        ) {
            pstmt.setString(1,isbn);
            pstmt.execute();
        }catch (SQLException e){
            e.printStackTrace();
            String obj = new Gson().toJson(error);
            return Response.serverError().entity(obj).build();
        }
        String obj = new Gson().toJson("Libro con ISBN:" + isbn + " eliminato con successo");
        return Response.ok(obj,MediaType.APPLICATION_JSON).build();
    }

    @POST
    @Path("/rent")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response rent(@FormParam("ISBN") String isbn){
        if(isbn == null || isbn.trim().length() == 0){
            String obj = new Gson().toJson("ISBN must be valid");
            return Response.serverError().entity(obj).build();
        }
        final String QUERY = "SELECT Quantity FROM Libri WHERE ISBN = ?";
        int quantity = 0;
        final String[] data = Database.getData();
        try(

                Connection conn = DriverManager.getConnection(data[0], data[1], data[2]);
                PreparedStatement pstmt = conn.prepareStatement( QUERY )
        ) {
            pstmt.setString(1,isbn);
            ResultSet results =  pstmt.executeQuery();
            while(results.next()) {
                quantity = results.getInt("Quantity");
            }
                if(quantity > 0){
                    final LocalDateTime dateNow = LocalDateTime.now();
                    final LocalDateTime rentExpiration = dateNow.plusMinutes(10);
                    final String QUERY2 = "INSERT INTO Prestiti(Inizio,Scadenza,ISBN) VALUES(?,?,?)";
                    final String QUERY3 = "UPDATE Libri SET Quantity = ? WHERE ISBN = ?";

                    try (
                            PreparedStatement stmt = conn.prepareStatement( QUERY2 )
                    )
                    {
                        stmt.setString(1,String.valueOf(dateNow));
                        stmt.setString(2,String.valueOf(rentExpiration));
                        stmt.setString(3,isbn);
                        stmt.execute();
                    }catch ( SQLException e ){
                        e.printStackTrace();
                        return Response.serverError().entity(error).build();
                    }

                    try (
                            PreparedStatement stmt = conn.prepareStatement( QUERY3 )
                    )
                    {
                        stmt.setInt(1,quantity-1);
                        stmt.setString(2,isbn);
                        stmt.execute();
                    }catch ( SQLException e ){
                        e.printStackTrace();
                        return Response.serverError().entity(error).build();
                    }

                }else {
                    String obj = new Gson().toJson("All the books with this ISBN are already rented");
                    return Response.serverError().entity(obj).build();
                }
        }catch (SQLException e){
            e.printStackTrace();
            String obj = new Gson().toJson(error);
            return Response.serverError().entity(obj).build();
        }
        String obj = new Gson().toJson("Book with ISBN:" + isbn + " Rented");
        return Response.ok(obj,MediaType.APPLICATION_JSON).build();
    }


    @POST
    @Path("/return")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response forceReturn(@FormParam("ID") String id){
        if(id == null || id.trim().length() == 0){
            String obj = new Gson().toJson("ID must be valid");
            return Response.serverError().entity(obj).build();
        }
        final String QUERY = "SELECT ISBN FROM Prestiti WHERE ID = ?";
        final String[] data = Database.getData();
        String isbn = "";
        try(

                Connection conn = DriverManager.getConnection(data[0], data[1], data[2]);
                PreparedStatement pstmt = conn.prepareStatement( QUERY )
        ) {
            pstmt.setString(1,id);
            ResultSet results =  pstmt.executeQuery();
            while(results.next()) {
                isbn = results.getString("ISBN");
            }
            if(!isbn.isEmpty()){
                final String QUERY2 = "DELETE FROM Prestiti WHERE ID = ?";
                final String QUERY3 = "UPDATE Libri SET Quantity = Quantity + 1 WHERE ISBN = ?";
                try (
                        PreparedStatement stmt = conn.prepareStatement( QUERY2 )
                )
                {
                    stmt.setString(1,id);
                    stmt.execute();
                }catch ( SQLException e ){
                    e.printStackTrace();
                    return Response.serverError().entity(error).build();
                }

                try (
                        PreparedStatement stmt = conn.prepareStatement( QUERY3 )
                )
                {
                    stmt.setString(1,isbn);
                    stmt.execute();
                }catch ( SQLException e ){
                    e.printStackTrace();
                    return Response.serverError().entity(error).build();
                }

            }else {
                String obj = new Gson().toJson("Unable to find the rented book");
                return Response.serverError().entity(obj).build();
            }
        }catch (SQLException e){
            e.printStackTrace();
            String obj = new Gson().toJson(error);
            return Response.serverError().entity(obj).build();
        }
        String obj = new Gson().toJson("Closed the rent with id: " + id + "that had the book with ISBN:" + isbn);
        return Response.ok(obj,MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/nearexpiry")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response getExpiries(){
        final String QUERY = "SELECT * FROM Prestiti WHERE Scadenza BETWEEN ? AND ?";
        final List<Rentals> rentals = new ArrayList<>();
        final LocalDateTime dateNow = LocalDateTime.now();
        final String[] data = Database.getData();
        try(
                Connection conn = DriverManager.getConnection(data[0], data[1], data[2]);
                PreparedStatement pstmt = conn.prepareStatement( QUERY )
        ) {
            pstmt.setString(1, String.valueOf(dateNow.minusDays(3)));
            pstmt.setString(2, String.valueOf(dateNow.plusDays(3)));
            ResultSet results =  pstmt.executeQuery();
            while (results.next()){
                Rentals rent = new Rentals();
                rent.setID(results.getString("ID"));
                rent.setStart(results.getString("Inizio"));
                rent.setExpiry(results.getString("Scadenza"));
                rent.setISBN(results.getString("ISBN"));
                rentals.add(rent);
            }
        }catch (SQLException e){
            e.printStackTrace();
            String obj = new Gson().toJson(error);
            return Response.serverError().entity(obj).build();
        }
        String obj = new Gson().toJson(rentals);
        return Response.status(200).entity(obj).build();
    }
}

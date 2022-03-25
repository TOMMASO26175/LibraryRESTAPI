import com.google.gson.Gson;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.*;
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
            return Response.serverError().entity("ISBN must be valid").build();
        }
        final String QUERY = "SELECT Quantity FROM Libri WHERE ISBN = ?";
        final String[] data = Database.getData();
        try(

                Connection conn = DriverManager.getConnection(data[0], data[1], data[2]);
                PreparedStatement pstmt = conn.prepareStatement( QUERY )
        ) {
            pstmt.setString(1,isbn);
            ResultSet results =  pstmt.executeQuery();
            while(results.next())
                if(results.getInt("Quantity") > 0){
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
                        stmt.setInt(1,results.getInt("Quantity"));
                        stmt.setString(2,isbn);
                        stmt.execute();
                    }catch ( SQLException e ){
                        e.printStackTrace();
                        return Response.serverError().entity(error).build();
                    }

                }else {
                    return Response.serverError().entity("All the books with this ISBN are already rented").build();
                }
        }catch (SQLException e){
            e.printStackTrace();
            String obj = new Gson().toJson(error);
            return Response.serverError().entity(obj).build();
        }
        String obj = new Gson().toJson("Libro con ISBN:" + isbn + " eliminato con successo");
        return Response.ok(obj,MediaType.APPLICATION_JSON).build();
    }
}

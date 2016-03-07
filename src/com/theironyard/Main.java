package com.theironyard;

import jodd.json.JsonSerializer;
import spark.Session;
import spark.Spark;

import java.sql.*;
import java.util.ArrayList;

public class Main {

    public static void main(String[] args) throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:h2:./main");
        createTables(conn);

        Spark.externalStaticFileLocation("public");
        Spark.init();
        Spark.get(
                "/allSightings",
                ((request, response) -> {
                    JsonSerializer serializer = new JsonSerializer();

                    ArrayList<Sighting> allSightings = selectSightings(conn);

                    return serializer.serialize(allSightings);

                })
        );

        Spark.get(
                "/allUsers",
                ((request1, response1) -> {
                    JsonSerializer serializer = new JsonSerializer();
                    ArrayList<User> allUsers = selectUsers(conn);
                    return serializer.serialize(allUsers);
                })
        );

        Spark.post(
                "/login",
                ((request, response) ->  {
                    String userName = request.queryParams("userName");
                    String userPass = request.queryParams("userPass");
                    if (userName.equals("") || userPass.equals("")) {
                        throw new Exception("Login name not found");
                    }

                    User user = selectUser(conn, userName);
                    if (user == null) {
                        throw new Exception("Please create an account.");
                    }
                    if (!user.userPass.equals(userPass)){
                        throw new Exception("Password is incorrect!");
                    }


                    Session session = request.session();
                    session.attribute("userName", userName);
                    return userName + " " + user.id;
                })
        );

        Spark.post(
                "/create-user",
                ((request, response) -> {
                    String userName = request.queryParams("userName");
                    String userPass = request.queryParams("userPass");
                    Session session = request.session();
                    String user = session.attribute(userName);
                    if (user != null) {
                        Spark.halt(403);
                        System.out.println("Username already exists");

                    }
                    insertUser(conn, userName, userPass);
                    return "Success!";
                })
        );

        Spark.post(
                "/delete-sighting",
                (request, response) -> {
                    String deleteById = request.queryParams("id");
                    int id = Integer.valueOf(deleteById);
                    deleteSighting(conn, id);
                    return "";
                }
        );
        Spark.post(
                "/create-sighting",
                (request, response) -> {
                    String lat = request.queryParams("lat");
                    String lon = request.queryParams("lon");
                    String text = request.queryParams("text");
                    String timestamp = request.queryParams("timestamp");
                    String url = request.queryParams("url");
                    Session session = request.session();
                    String userName = session.attribute("userName");
                    User user = selectUser(conn, userName);
                    if (user == null) {
                        throw new Exception("User not logged in.");
                    }
                    insertSighting(conn, lat, lon, text, timestamp, url, user.id);
                    return "Success!";
                }
        );
        Spark.put(
                "/update-sighting",
                (request, response) -> {
                    String lat = request.queryParams("lat");
                    String lon = request.queryParams("lon");
                    String text = request.queryParams("text");
                    String timestamp = request.queryParams("timestamp");
                    String url = request.queryParams("url");
                    int id = Integer.valueOf(request.queryParams("id"));
                    Sighting sighting = selectSighting(conn, id);

                    if(!lat.equals("")){
                        sighting.lat = lat;
                    }
                    if(!lon.equals("")){
                        sighting.lon = lon;
                    }
                    if(!text.equals("")){
                        sighting.text = text;
                    }
                    if(!timestamp.equals("")){
                        sighting.timestamp = timestamp;
                    }
                    if(!url.equals("")){
                        sighting.url = url;
                    }

                    updateSighting(conn, sighting.lat, sighting.lon, sighting.text, sighting.timestamp, sighting.url, id);
                    return "";

                }
                //only passing what is updated.
        );
        Spark.post(
                "/logout",
                (request, response) -> {
                    Session session = request.session();
                    session.invalidate();
                    return "Success!";
                }
        );



    }

    public static void createTables(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS users (id IDENTITY, user_name VARCHAR, user_password VARCHAR)");
        // Some of this may change.
        stmt.execute("CREATE TABLE IF NOT EXISTS sightings (id IDENTITY, lat VARCHAR, lon VARCHAR, text VARCHAR, timestamp VARCHAR," + //WHY IS THERE A + HERE?
                "url VARCHAR, user_id INT)");
        // We may need to add additional information here
        // so that we can INNER JOIN them. I need some clarification on this. NOTHING ELSE NEEDED HERE
    }

    public static void insertUser(Connection conn, String userName, String userPass) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO users VALUES (NULL, ?, ?)");
        stmt.setString(1, userName);
        stmt.setString(2, userPass);
        stmt.execute();
    }

    public static User selectUser(Connection conn, String userName) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE user_name = ?");
        stmt.setString(1, userName);
        ResultSet results = stmt.executeQuery();
        if (results.next()) {
            int id = results.getInt("id");
            String password = results.getString("user_password");
            return new User(id, userName, password);
        }
        return null;

        // I think this method will work for what we need. AGREED
    }

    public static ArrayList<User> selectUsers(Connection conn) throws SQLException {
        ArrayList<User> users = new ArrayList<>();
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users");
        ResultSet results = stmt.executeQuery();
        while (results.next()) {
            int id = results.getInt("id");
            String userName = results.getString("user_name");
            String userPass = results.getString("user_password");
            User user = new User(id, userName, userPass);
            users.add(user);

        }
        return users;
    }

    public static void insertSighting(Connection conn, String lat, String lon, String text, String timestamp, String url, int userId) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO sightings VALUES (NULL, ?, ?, ?, ?, ?, ?)");
        stmt.setString(1, lat);
        stmt.setString(2, lon);
        stmt.setString(3, text);
        stmt.setString(4, timestamp);
        stmt.setString(5, url);
        stmt.setInt(6, userId);
        stmt.execute();

        //I think this is good too.
    }

    public static Sighting selectSighting(Connection conn, int id) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM sightings INNER JOIN users ON " +
                "sightings.user_id = users.id WHERE sightings.id = ?");
        stmt.setInt(1, id);
        ResultSet results = stmt.executeQuery();
        if (results.next()) {
            String lat = results.getString("sightings.lat");
            String lon = results.getString("sightings.lon");
            String text = results.getString("sightings.text");
            String timestamp = results.getString("sightings.timestamp");
            String url = results.getString("sightings.url");
            String userName = results.getString("users.user_name");
            //int userId = results.getInt("user_id"); I THINK USERS.USERNAME
            return new Sighting(id, lat, lon, text, timestamp, url,userName);



        }
        return null;
    }

    public static ArrayList<Sighting> selectSightings(Connection conn) throws SQLException {
        ArrayList<Sighting> sightings = new ArrayList<>();
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM sightings INNER JOIN users ON sightings.user_id = users.id"); //DONT NEED INNER JOIN
        ResultSet results = stmt.executeQuery();
        while (results.next()) {
            int id = results.getInt("sightings.id");
            String lat = results.getString("sightings.lat");
            String lon = results.getString("sightings.lon");
            String text = results.getString("text");
            String timestamp = results.getString("timestamp");
            String url = results.getString("url");
            String name = results.getString("users.user_name");
            Sighting sighting = new Sighting(id, lat, lon, text, timestamp, url, name);
            sightings.add(sighting);
        }
        return sightings;
    }

    static void deleteSighting(Connection conn, int id) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("DELETE FROM sightings WHERE id = ?");
        stmt.setInt(1, id);
        stmt.execute();
    }

    static void updateSighting(Connection conn, String lat, String lon, String text, String timestamp, String url, int id) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("UPDATE sightings SET lat = ?, lon = ?, text = ?, timestamp = ?, url = ? WHERE id = ?");
        stmt.setString(1, lat);
        stmt.setString(2, lon);
        stmt.setString(3, text);
        stmt.setString(4, timestamp);
        stmt.setString(5, url);
        stmt.setInt(6, id);
        stmt.execute();
    }
}
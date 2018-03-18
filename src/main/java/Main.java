import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;
import spark.ModelAndView;
import spark.template.freemarker.FreeMarkerEngine;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static spark.Spark.*;

public class Main
{
    private BoneCP connectionPool;
    private FreeMarkerEngine fme;

    public static void main(String[] args)
    {
        port(1337);
        Main main = new Main();

        main.setupConnectionPool();
        main.registerGetRoutes();
        main.registerPostRoutes();
        main.registerPutRoutes();
    }

    public Main()
    {
        fme = new FreeMarkerEngine();
    }

    public void registerGetRoutes()
    {
        get("/", (req, res) ->
        {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("message", "Welcome to Morten G's web application.");
            return new ModelAndView(map, "register.ftl");
        }, fme);

        get("/test", (req, res) ->
        {
            return test();
        });
    }

    public void registerPostRoutes()
    {
        //201 created
        //400 bad request
        post("/adduser", (req, res) ->
        {
            String steamID, playerName = "Unnamed";
            steamID = req.queryParams("SteamID");
            playerName = req.queryParams("PlayerName");

            System.out.println("Detected a AddUser call with user = " + playerName);
            if(steamID == null || steamID == "")
            {
                res.status(400); // Bad request
            }
            else
            {
                if(insertUser(Long.parseLong(steamID), playerName))
                {
                    res.status(201); // User added
                }
                else
                {
                    res.status(409); // Conflict, user might exist
                }
            }
            return res;
        });

        post("/registerstats", (req, res) ->
        {
            boolean bDidWin = false;
            String steamID, hostSteamID, roleType, didWin, theDate, amountOfPlayers, withAbilities, amountLibsPlayed, amountFascPlayed;

            steamID = req.queryParams("SteamID");
            hostSteamID = req.queryParams("HostSteamID");
            roleType = req.queryParams("RoleType");
            didWin = req.queryParams("DidWin");
            theDate = req.queryParams("TheDate");
            amountOfPlayers = req.queryParams("AmountOfPlayers");
            withAbilities = req.queryParams("WithAbilities");
            amountLibsPlayed = req.queryParams("AmountLibsPlayed");
            amountFascPlayed = req.queryParams("AmountLibsPlayed");

            for(String line: req.queryParams())
            {
                System.out.println(line);
            }
            if(steamID == null || hostSteamID == null || roleType == null || didWin == null)
            {
                res.status(400);
                return res;
            }

            System.out.println(steamID + " was a " + roleType + " and " + didWin + " the game.");

            if(Integer.parseInt(roleType) > 2)
            {
                res.status(400);
                res.body("<p>Invalid roleType. 0 = lib, 1 = fasc, 2 = hitler</p>");
                return res;
            }

            if(didWin == "1")
            {
                bDidWin = true;
            }
            else if(didWin == "0")
            {
                bDidWin = false;
            }

            if(theDate == null)
            {
                java.util.Date utilDate = new java.util.Date();
                java.sql.Date sqlDate = new java.sql.Date(utilDate.getTime());
                theDate = sqlDate.toString();
            }

            if(amountOfPlayers == null)
            {
                amountOfPlayers = "8";
            }

            if(withAbilities == null)
            {
                withAbilities = "0";
            }

            if(amountLibsPlayed == null)
            {
                amountLibsPlayed = "0";
            }

            if(amountFascPlayed == null)
            {
                amountFascPlayed = "0";
            }

            if(insertStat(
                    Long.parseLong(steamID), Long.parseLong(hostSteamID), Integer.parseInt(roleType),
                    bDidWin, Date.valueOf(theDate), Integer.parseInt(amountOfPlayers),
                    Boolean.parseBoolean(withAbilities), Integer.parseInt(amountLibsPlayed), Integer.parseInt(amountFascPlayed)))
            {
                res.status(200);
            }
            else
            {
                res.status(400);
                res.body("<p> Something went wrong when trying to add valeus into database. <br> make sure you parsed the values correctly</p>");
            }
            return res;
        });
    }

    public void registerPutRoutes()
    {

    }

    public String test()
    {
        Gson gson = new Gson();
        JsonObject jo = new JsonObject();
        jo.addProperty("SteamID", "76561198014861477");
        jo.addProperty("PlayerName", "Morten G");
        return gson.toJson(jo);
    }

    private void setupConnectionPool()
    {
        BoneCPConfig config = new BoneCPConfig();
        config.setJdbcUrl("jdbc:sqlserver://mortensserver.database.windows.net:1433;database=mortensdb;user=morten@mortensserver;password=Gerdes@70;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;");
        config.setMinConnectionsPerPartition(5);
        config.setMaxConnectionsPerPartition(10);
        config.setPartitionCount(1);
        try {
            this.connectionPool = new BoneCP(config);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean insertUser(long steamID, String name) throws SQLException {
        Connection conn = null;
        int rowsAffected = 0;
        String query = "INSERT INTO Players (SteamID, PlayerName) VALUES (?, ?)";
        try {
            conn = connectionPool.getConnection();
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setLong(1, steamID);
            ps.setString(2, name);
            rowsAffected = ps.executeUpdate();
            conn.close();
        } catch (SQLException e)
        {
            if(conn != null)
            {
                conn.close();
            }
        }
        return (rowsAffected == 1);
    }

    private boolean insertStat(long steamID, long hostSteamID, int roleType, boolean didWin, Date theDate, int amountOfPlayers, boolean withAbilitites, int amountLibsPlayed, int amountFascPlayed)
    {
        int rowsAffected = 0;
        String query = "INSERT INTO Games (SteamID, HostSteamID, RoleType, DidWin, TheDate, AmountOfPlayers, WithAbilities, AmountLibsPlayed, AmountFascPlayed) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            Connection conn = connectionPool.getConnection();
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setLong(1, steamID);
            ps.setLong(2, hostSteamID);
            ps.setInt(3, roleType);
            ps.setBoolean(4, didWin);
            ps.setDate(5, theDate);
            ps.setInt(6, amountOfPlayers);
            ps.setBoolean(7, withAbilitites);
            ps.setInt(8, amountLibsPlayed);
            ps.setInt(9, amountFascPlayed);

            rowsAffected = ps.executeUpdate();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return (rowsAffected == 1);
    }
}

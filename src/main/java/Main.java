import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;
import spark.ModelAndView;
import spark.template.freemarker.FreeMarkerEngine;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

import static spark.Spark.*;

public class Main
{
    private int annID = 1;
    private String annMessage = "";
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

        get("/am", (req, res) ->
        {
            if(annMessage.equals(""))
            {
                return "{}";
            }
            JsonObject jo = new JsonObject();
            Gson gson = new Gson();
            jo.addProperty("ID", annID);
            jo.addProperty("Message", annMessage);
            return gson.toJson(jo);
        });
    }

    public void registerPostRoutes()
    {
        post("/setam", (req, res) ->
        {
            String id = req.queryParams("id");
            String pass = req.queryParams("pass");
            String message = req.queryParams("message");
           if(!req.queryParams("pass").equals("MortenG"))
           {
               res.status(401);
               res.body("Access Denied!");
               return res.body();
           }
           annID = Integer.parseInt(id);
           annMessage = message;
           res.status(200);
           res.body("Done");
           return res.body();
        });

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
            boolean bDidWin = false, bWithAbilities = false;
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
            System.out.println("Detected a RegisterStats call for userID = " + steamID);
            if(steamID == null || hostSteamID == null || roleType == null || didWin == null)
            {
                res.status(400);
                return res;
            }

            if(Integer.parseInt(roleType) > 2)
            {
                res.status(400);
                res.body("<p>Invalid roleType. 0 = lib, 1 = fasc, 2 = hitler</p>");
                return res;
            }

            if(withAbilities == null)
            {
                withAbilities = "0";
            }

            if(Integer.parseInt(didWin) == 1)
            {
                bDidWin = true;
            }
            else if(Integer.parseInt(didWin) == 0)
            {
                bDidWin = false;
            }

            if(Integer.parseInt(withAbilities) == 1)
            {
                bWithAbilities = true;
            }
            else if(Integer.parseInt(withAbilities) == 0)
            {
                bWithAbilities = false;
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
                    bWithAbilities, Integer.parseInt(amountLibsPlayed), Integer.parseInt(amountFascPlayed)))
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

        post("/retrievestats", (req, res) ->
        {
            Map<String, String[]> map = req.queryMap().toMap();
            if(validMap(map))
            {
                res.status(200);
                return getUserStats(map, false);
            }
            else
            {
                res.status(400);
                System.out.println("Invalid Map!");
            }
            return res;
        });

        post("/retrievehoststats", (req, res) ->
        {
            Map<String, String[]> map = req.queryMap().toMap();
            if(validMap(map))
            {
                res.status(200);
                return getUserStats(map, true);
            }
            else
            {
                res.status(400);
                System.out.println("Invalid Map!");
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

    private boolean insertStat(long steamID, long hostSteamID, int roleType, boolean didWin, Date theDate, int amountOfPlayers, boolean withAbilitites, int amountLibsPlayed, int amountFascPlayed) throws SQLException {
        int rowsAffected = 0;
        String query = "INSERT INTO Games (SteamID, HostSteamID, RoleType, DidWin, TheDate, AmountOfPlayers, WithAbilities, AmountLibsPlayed, AmountFascPlayed) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try(Connection conn = connectionPool.getConnection()) {

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
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
        return (rowsAffected == 1);
    }

    private String generateQuery(Map<String, String[]> map)
    {
        int loopIndex = 1;

        StringBuilder sb = new StringBuilder();
        sb.append(


                "select g.SteamID,\n" +
                        "Count(case when RoleType = 0 then 1 else null end) as LibGames,\n" +
                        "Count(case when RoleType = 0 and DidWin = 1 then 1 else null end) as LibWins,\n" +
                        "Count(case when RoleType = 1 then 1 else null end) as FascGames,\n" +
                        "Count(case when RoleType = 1 and DidWin = 1 then 1 else null end) as FascWins,\n" +
                        "Count(case when RoleType = 2 then 1 else null end) as HitlerGames,\n" +
                        "Count(case when RoleType = 2 and DidWin = 1 then 1 else null end) as HitlerWins,\n" +
                        "Count(*) as TotalGames\n" +
                        "from Games as g where "
        );

        for(String index: map.keySet())
        {
            sb.append("g.SteamID = ?");
            if(loopIndex < map.keySet().size())
            {
                sb.append(" or ");
            }
            loopIndex++;
        }
        sb.append(" group by g.SteamID;");

        return sb.toString();
    }

    private String generateQueryWithHost(Map<String, String[]> map)
    {
        int loopIndex = 1;

        StringBuilder sb = new StringBuilder();
        sb.append(
                        "select g.SteamID,\n" +
                        "Count(case when RoleType = 0 then 1 else null end) as LibGames,\n" +
                        "Count(case when RoleType = 0 and DidWin = 1 then 1 else null end) as LibWins,\n" +
                        "Count(case when RoleType = 1 then 1 else null end) as FascGames,\n" +
                        "Count(case when RoleType = 1 and DidWin = 1 then 1 else null end) as FascWins,\n" +
                        "Count(case when RoleType = 2 then 1 else null end) as HitlerGames,\n" +
                        "Count(case when RoleType = 2 and DidWin = 1 then 1 else null end) as HitlerWins,\n" +
                        "Count(*) as TotalGames\n" +
                        "from Games as g where (g.HostSteamID = " + map.get("host")[0] + ") and ("
        );

        for(String index: map.keySet())
        {
            sb.append("g.SteamID = ?");
            if(loopIndex < map.keySet().size())
            {
                sb.append(" or ");
            }
            loopIndex++;
        }
        sb.append(") group by g.SteamID;");

        //System.out.println(sb.toString());
        return sb.toString();
    }

    private String getUserStats(Map<String, String[]> queryMap, boolean hostSpecific) {
        int index = 1;
        Gson gson = new Gson();
        String query = "";
        if(hostSpecific)
        {
            query = generateQueryWithHost(queryMap);
        }
        else
        {
           query = generateQuery(queryMap);
        }
        try(Connection conn = connectionPool.getConnection())
        {
            PreparedStatement ps = conn.prepareStatement(query);
            for(String key: queryMap.keySet())
            {
                ps.setLong(index, Long.parseLong(queryMap.get(key)[0]));
                index++;
            }
            ResultSet rs = ps.executeQuery();
            HashMap<Long, HashMap<String, Integer>> map = new HashMap<>();
            while(rs.next())
            {
                HashMap<String, Integer> innerMap = new HashMap<>();
                innerMap.put("LibGames", rs.getInt("LibGames"));
                innerMap.put("LibWins", rs.getInt("LibWins"));
                innerMap.put("FascGames", rs.getInt("FascGames"));
                innerMap.put("FascWins", rs.getInt("FascWins"));
                innerMap.put("HitlerGames", rs.getInt("HitlerGames"));
                innerMap.put("HitlerWins", rs.getInt("HitlerWins"));
                innerMap.put("TotalGames", rs.getInt("TotalGames"));
                map.put(rs.getLong("SteamID"), innerMap);
            }
            conn.close();
            return gson.toJson(map);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        return "Something went wrong";
    }

    private boolean validMap(Map<String, String[]> map)
    {
        if(map == null)
        {
            return false;
        }

        if(map.isEmpty())
        {
            return false;
        }

        for(String index: map.keySet())
        {
            try
            {
                if(Long.parseLong(map.get(index)[0]) != 0)
                {
                    continue;
                }
            }
            catch (NumberFormatException e)
            {
                return false;
            }
        }
        return true;
    }
}

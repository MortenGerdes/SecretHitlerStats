import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;
import spark.ModelAndView;
import spark.template.freemarker.FreeMarkerEngine;

import java.sql.Connection;
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

    }

    public void registerPutRoutes()
    {
        //201 created
        //400 bad request
        put("/adduser", (req, res) ->
        {
            String steamID, playerName = "Unnamed";
            steamID = req.queryParams("SteamID");
            playerName = req.queryParams("PlayerName");
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

    private boolean insertUser(long steamID, String name)
    {
        int rowsAffected = 0;
        String query = "INSERT INTO Players (SteamID, PlayerName) VALUES (?, ?)";
        try {
            Connection conn = connectionPool.getConnection();
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setLong(1, steamID);
            ps.setString(2, name);
            rowsAffected = ps.executeUpdate();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return (rowsAffected == 1);
    }
}

import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;
import spark.ModelAndView;
import spark.template.freemarker.FreeMarkerEngine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static spark.Spark.get;
import static spark.Spark.port;

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
    }

    public Main()
    {
        fme = new FreeMarkerEngine();
    }

    public void registerGetRoutes()
    {
        get("/testinsert", (req, res) ->
        {
            Map<String, Object> map = new HashMap<String, Object>();
            if(testInsert())
            {
                map.put("message", "Added Morten G to the database, yay");
            }
            else
            {
                map.put("message", "Failed to add Morten G to Database :(");
            }
            // map.put("message", "Yay, server works");
            return new ModelAndView(map, "register.ftl");
        }, fme);
    }

    public void registerPostRoutes()
    {

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

    private boolean testInsert()
    {
        int rowsAffected = 0;
        String query = "INSERT INTO Players (SteamID, PlayerName) VALUES (?, ?)";
        try {
            Connection conn = connectionPool.getConnection();
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setLong(1, 76561198014861477L);
            ps.setString(2, "Morten G");
            rowsAffected = ps.executeUpdate();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return (rowsAffected == 1);
    }
}

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

public class DbCheck {
    public static void main(String[] args) {
        System.out.println("=== CHECKING EASY-IMMO DATABASES ===");
        
        checkDb("jdbc:postgresql://localhost:5433/easy_immo_tenant", "easy_user", "pgpass123", 
                "SELECT id, name, country FROM agencies",
                "SELECT id, email, first_name, last_name, agency_id FROM collaborators");
                
        checkDb("jdbc:postgresql://localhost:5434/easy_immo_property", "easy_user", "pgpass123", 
                "SELECT id, agency_id, first_name, last_name, phone, email, share_percentage FROM owners",
                "SELECT id, reference, type, city, current_rent, owner_id FROM properties");
    }
    
    private static void checkDb(String url, String user, String password, String... queries) {
        System.out.println("\nDatabase: " + url);
        try {
            Class.forName("org.postgresql.Driver");
            try (Connection conn = DriverManager.getConnection(url, user, password)) {
                System.out.println("Status: CONNECTED successfully");
                for (String query : queries) {
                    System.out.println("\nQuery: " + query);
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery(query)) {
                        ResultSetMetaData meta = rs.getMetaData();
                        int cols = meta.getColumnCount();
                        for (int i = 1; i <= cols; i++) {
                            System.out.print(meta.getColumnName(i) + "\t");
                        }
                        System.out.println();
                        int count = 0;
                        while (rs.next()) {
                            for (int i = 1; i <= cols; i++) {
                                System.out.print(rs.getObject(i) + "\t");
                            }
                            System.out.println();
                            count++;
                        }
                        System.out.println("Total rows: " + count);
                    } catch (Exception e) {
                        System.err.println("Query Error: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Connection Error: " + e.getMessage());
        }
    }
}

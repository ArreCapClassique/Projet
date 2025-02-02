import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class JDBC {
    private String url;
    private String username;
    private String password;
    private Connection connection;

    public JDBC(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.connection = connect();
    }

    private Connection connect() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(url, username, password);
                System.out.println("Connected to the database");
            }
        } catch (SQLException e) {
            System.err.format("SQL State: %s\n%s\n", e.getSQLState(), e.getMessage());
        }
        return connection;
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Disconnected from the database");
            }
        } catch (SQLException e) {
            System.err.format("SQL State: %s\n%s\n", e.getSQLState(), e.getMessage());
        }
    }

    public void execute(String command) {
        try {
            PreparedStatement statement = connection.prepareStatement(command);
            statement.execute();
        } catch (SQLException e) {
            System.err.format("SQL State: %s\n%s\n", e.getSQLState(), e.getMessage());
            System.err.println(command);
        }
    }

    public List<List<String>> executeQuery(String command) {
        List<List<String>> result = new ArrayList<>();
        try {
            PreparedStatement statement = connection.prepareStatement(command);
            ResultSet resultSet = statement.executeQuery();
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
            int columnCount = resultSetMetaData.getColumnCount();

            while (resultSet.next()) {
                List<String> line = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    line.add(resultSet.getString(i));
                }
                result.add(line);
            }
            resultSet.close();

            return result;
        } catch (SQLException e) {
            System.err.format("SQL State: %s\n%s\n", e.getSQLState(), e.getMessage());
            System.err.println(command);
        }
        return null;
    }

    public List<List<String>> executeQueryMetaData(String command) {
        List<List<String>> result = new ArrayList<>();
        try {
            PreparedStatement statement = connection.prepareStatement(command);
            ResultSet resultSet = statement.executeQuery();
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
            int columnCount = resultSetMetaData.getColumnCount();

            List<String> columeName = new ArrayList<>();
            List<String> columeType = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                columeName.add(resultSetMetaData.getColumnName(i));
                columeType.add(resultSetMetaData.getColumnTypeName(i));
            }
            result.add(columeName);
            result.add(columeType);
            resultSet.close();

            return result;
        } catch (SQLException e) {
            System.err.format("SQL State: %s\n%s\n", e.getSQLState(), e.getMessage());
            System.err.println(command);
        }
        return null;
    }

    public void executeUpdate(String command) {
        try {
            PreparedStatement statement = connection.prepareStatement(command);
            int line = statement.executeUpdate();
            System.out.println(String.format("%s lines updated", line));
        } catch (SQLException e) {
            System.err.format("SQL State: %s\n%s\n", e.getSQLState(), e.getMessage());
            System.err.println(command);
        }
    }

    public String executeUpdateAutoGenKey(String command, String tableName) {
        try {
            PreparedStatement statement = connection.prepareStatement(command, 1);
            int line = statement.executeUpdate();
            System.out.println(String.format("%s lines updated", line));
            ResultSet resultSet = statement.getGeneratedKeys();
            resultSet.next();
            String generatedKey = resultSet.getString(1);
            return executeQuery(String.format("""
                    SELECT *
                    FROM %s
                    WHERE ROWID = '%s'
                    """, tableName, generatedKey)).get(0).get(0);
        } catch (SQLException e) {
            System.err.format("SQL State: %s\n%s\n", e.getSQLState(), e.getMessage());
            System.err.println(command);
        }
        return null;
    }

    public void executeScript(String path) {
        try (Scanner scanner = new Scanner(
                new FileInputStream(path))) {
            StringBuilder command = new StringBuilder();
            while (scanner.hasNextLine()) {
                String ch = scanner.nextLine().trim();
                if (ch.isEmpty() || ch.startsWith("--")) {
                    continue;
                }
                command.append(ch + " ");
                if (ch.endsWith(";")) {
                    String sql = command.toString().trim();
                    sql = sql.substring(0, sql.length() - 1);
                    if (sql.startsWith("INSERT")) {
                        executeUpdate(sql);
                    } else if (sql.startsWith("SELECT")) {
                        executeQuery(sql).forEach(System.out::println);
                    } else {
                        execute(sql);
                    }
                    command.setLength(0);
                }
            }
        } catch (FileNotFoundException fne) {
            System.err.println(path + " est absent ! ");
        }
    }
}
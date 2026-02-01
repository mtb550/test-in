package testGit.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

public class sql {
    private final static ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final Connection connection;
    public ArrayList<HashMap<String, Object>> dbResult;

    @SneakyThrows
    public sql() {

        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

        if (isWindows)
            connection = DriverManager.getConnection("jdbc:sqlite:C:/Users/mmoghyiri/Documents/Repo/iintellij-plugin-tc/autofy.db");
        else
            connection = DriverManager.getConnection("jdbc:sqlite:/home/mtb/IdeaProjects/demo/autofy.db");

    }

    public void close() {
        try {
            if (connection != null) connection.close();
        } catch (SQLException e) {
            e.printStackTrace(System.out);
        }
    }

    /**
     * Execute a SELECT query and return a list of rows (each row as a map of column → value)
     */
    @SneakyThrows
    public sql get(String query, Object... params) {
        dbResult = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            // Bind parameters
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();

                while (rs.next()) {
                    HashMap<String, Object> object = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String colName = meta.getColumnName(i);
                        Object value = rs.getObject(i);
                        object.put(colName, value);
                    }
                    dbResult.add(object);
                }
            }
        }

        return this;
    }

    /**
     * Execute an INSERT/UPDATE/DELETE
     */
    @SneakyThrows
    public int execute(String query, Object... params) {
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            return stmt.executeUpdate();
        }
    }

    @SneakyThrows(JsonProcessingException.class)
    public <T> T as(final Class<T> clazz) {
        if (dbResult == null)
            return null;

        String data = clazz.isArray() ? mapper.writeValueAsString(dbResult) : mapper.writeValueAsString(dbResult.get(0));
        return mapper.readValue(data, clazz);
    }

    public <T> T asType(final Class<T> clazz) {
        if (dbResult == null || dbResult.isEmpty() || dbResult.get(0).isEmpty())
            return null;

        Object data = dbResult.get(0).values().iterator().next();
        return mapper.convertValue(data, clazz);
    }
}


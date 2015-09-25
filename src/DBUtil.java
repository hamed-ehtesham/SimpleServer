import java.sql.*;
import java.util.*;
import java.util.Date;

/**
 * Created by Hamed on 9/23/2015.
 */
public class DBUtil {
    private Connection connection;
    private Statement statement = null;
    private ResultSet resultSet = null;

    public Connection getConnection() {
        return connection;
    }

    public void connectToDB(String username, String password, String databaseName) {
        connectToDB("localhost", username, password, databaseName);
    }

    public void connectToDB(String dbHost, String username, String password, String databaseName) {
        try {
            // This will load the MySQL driver, each DB has its own driver
            Class.forName("com.mysql.jdbc.Driver");
            // Setup the connection with the DB
            connection = DriverManager.getConnection("jdbc:mysql://" + dbHost + "/" + databaseName + "?" + "user=" + username + "&password=" + password);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void query(String sqlQuery) throws SQLException {
        // Statements allow to issue SQL queries to the database
        statement = connection.createStatement();
        // Result set get the result of the SQL query
        try {
            resultSet = statement.executeQuery(sqlQuery);
            writeMetaData(resultSet);
            writeResultSet(resultSet);
//            writeResultSet(resultSet,new Class[]{});
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void query(PreparedStatement sqlQuery) throws SQLException {
        resultSet = sqlQuery.executeQuery();
        writeResultSet(resultSet);
    }

    public ArrayList<Object> selectQuery(PreparedStatement sqlQuery) throws SQLException {
        resultSet = sqlQuery.executeQuery();
        ArrayList<Object> table = new ArrayList<Object>();
        ArrayList<Class<?>> columnTypes = getColumnTypes(resultSet);
        ArrayList<String> columnNames = new ArrayList<String>(columnTypes.size());
        for (int i = 1; i <= columnTypes.size(); i++) {
            String columnName = resultSet.getMetaData().getColumnName(i);
            columnNames.add(columnName);
        }
        table.add(columnNames);
        table.add(columnTypes);
        while (resultSet.next()) {
            ArrayList<Object> row = new ArrayList<Object>(columnTypes.size());
            for (int i = 1; i <= columnTypes.size(); i++) {
                Object data = null;
                if (columnTypes.get(i - 1).equals(String.class)) {
                    data = resultSet.getString(i);
                } else if (columnTypes.get(i - 1).equals(Integer.class)) {
                    data = resultSet.getInt(i);
                } else if (columnTypes.get(i - 1).equals(Long.class)) {
                    data = resultSet.getLong(i);
                } else if (columnTypes.get(i - 1).equals(Boolean.class)) {
                    data = resultSet.getBoolean(i);
                } else if (columnTypes.get(i - 1).equals(java.util.Date.class)) {
                    data = resultSet.getDate(i);
                } else if (columnTypes.get(i - 1).equals(Timestamp.class)) {
                    data = resultSet.getTimestamp(i);
                }
                row.add(data);
            }
            table.add(row);
        }
        return table;
    }

    public ArrayList<Object> getRow(ArrayList<Object> table, int index) {
        ArrayList<Object> row = (ArrayList<Object>) table.get(index + 2);
        return row;
    }

    public <T> T getElement(ArrayList<Object> table, int row, int column, Class<T> outputClass) {
        ArrayList<Object> rowData = getRow(table, row);
        ArrayList<Class<?>> columnTypes = (ArrayList<Class<?>>) table.get(1);
        Class<?> columnType = columnTypes.get(column);
        if (outputClass != columnType)
            throw new IllegalArgumentException("output class is not correct expect: " + columnType);
        return (T)rowData.get(column);
    }

    public void printTable(ArrayList<Object> table) {
        ArrayList<String> columnNames = (ArrayList<String>) table.get(0);
        int columnCount = columnNames.size();
        for (int i = 0; i < columnCount; i++) {
            System.out.print(columnNames.get(i) + "\t");
        }
        System.out.println();
        for (int i = 2; i < table.size(); i++) {
            ArrayList<Object> row = (ArrayList<Object>) table.get(i);
            for (int j = 0; j < columnCount; j++) {
                System.out.print(row.get(j) + "\t");
            }
            System.out.println();
        }
    }

    private ArrayList<Class<?>> getColumnTypes(ResultSet resultSet) throws SQLException {
        ArrayList<Class<?>> columnTypes = new ArrayList<Class<?>>();

        for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
            String className = resultSet.getMetaData().getColumnClassName(i);
            try {
                Class<?> aClass = Class.forName(className);
                columnTypes.add(aClass);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        return columnTypes;
    }

    private void writeMetaData(ResultSet resultSet) throws SQLException {
        //   Now get some metadata from the database
        // Result set get the result of the SQL query

        System.out.println("The columns in the table are: ");

        System.out.println("Table: " + resultSet.getMetaData().getTableName(1));
        for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
            System.out.println("Column " + i + " " + resultSet.getMetaData().getColumnName(i) + ":" + resultSet.getMetaData().getColumnClassName(i));
        }
    }

    private void writeResultSet(ResultSet resultSet) throws SQLException {
        // ResultSet is initially before the first data set
        ArrayList<Class<?>> columnTypes = getColumnTypes(resultSet);
        while (resultSet.next()) {
            for (int i = 1; i <= columnTypes.size(); i++) {
                String columnName = resultSet.getMetaData().getColumnName(i);
                if (columnTypes.get(i - 1).equals(String.class)) {
                    String data = resultSet.getString(i);
                    System.out.println(columnName + ": " + data);
                } else if (columnTypes.get(i - 1).equals(Integer.class)) {
                    int data = resultSet.getInt(i);
                    System.out.println(columnName + ": " + data);
                } else if (columnTypes.get(i - 1).equals(Long.class)) {
                    long data = resultSet.getLong(i);
                    System.out.println(columnName + ": " + data);
                } else if (columnTypes.get(i - 1).equals(Boolean.class)) {
                    boolean data = resultSet.getBoolean(i);
                    System.out.println(columnName + ": " + data);
                } else if (columnTypes.get(i - 1).equals(java.util.Date.class)) {
                    Date data = resultSet.getDate(i);
                    System.out.println(columnName + ": " + data);
                } else if (columnTypes.get(i - 1).equals(Timestamp.class)) {
                    Timestamp data = resultSet.getTimestamp(i);
                    System.out.println(columnName + ": " + data);
                }
            }
        }
    }

    // You need to close the resultSet
    public void close() {
        try {
            if (resultSet != null) {
                resultSet.close();
            }

            if (statement != null) {
                statement.close();
            }

            if (connection != null) {
                connection.close();
            }
        } catch (Exception e) {

        }
    }

    public static void main(String[] args) {
        DBUtil dbUtil = new DBUtil();
        dbUtil.connectToDB("server_admin", "sdfcldkd", "chat_server");
        try {
            dbUtil.query("SELECT\n" +
                    "person.person_email,\n" +
                    "person.person_first_name,\n" +
                    "person.person_last_name,\n" +
                    "person.person_nickname,\n" +
                    "`group`.group_name\n" +
                    "FROM\n" +
                    "person\n" +
                    "INNER JOIN person_group ON person_group.person_email = person.person_email\n" +
                    "INNER JOIN `group` ON person_group.group_id = `group`.group_id\n");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

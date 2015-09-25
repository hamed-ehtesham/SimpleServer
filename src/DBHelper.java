import java.net.SocketAddress;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;

/**
 * Created by Hamed on 9/23/2015.
 */
public class DBHelper {
    public static RespondInfo register(RegistrationRequestInfo requestInfo, String symmetricKey) {
        RespondInfo respondInfo = new RespondInfo();
        //email validation
        if (!ValidationUtil.validateEmail(requestInfo.getEmail())) {
            respondInfo.setSucceed(false);
            respondInfo.setMessage("email not valid");
            return respondInfo;
        } else {
            DBUtil dbUtil = new DBUtil();
            dbUtil.connectToDB("server_admin", "sdfcldkd", "chat_server");
            PreparedStatement preparedStatement = null;
            try {
                preparedStatement = dbUtil.getConnection().prepareStatement("INSERT INTO person (person_email,\n" +
                        "person_password,\n" +
                        "person_first_name,\n" +
                        "person_last_name,\n" +
                        "person_nickname,\n" +
                        "person_salt)\n" +
                        "VALUES (?, ?, ?, ?, ?, ?);");
                preparedStatement.setString(1, requestInfo.getEmail());
                preparedStatement.setString(2, requestInfo.getPassword());
                preparedStatement.setString(3, requestInfo.getFirstName());
                preparedStatement.setString(4, requestInfo.getLastName());
                preparedStatement.setString(5, requestInfo.getNickname());
                preparedStatement.setString(6, symmetricKey);
                int executeUpdate = 0;
                try {
                    executeUpdate = preparedStatement.executeUpdate();
                } catch (SQLException e) {
                    System.err.println(e.getMessage());
                    respondInfo.setSucceed(false);
                    respondInfo.setMessage(e.getMessage());
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                    respondInfo.setSucceed(false);
                    respondInfo.setMessage(e.getMessage());
                }
                if (executeUpdate == 1) {
                    respondInfo.setSucceed(true);
                    respondInfo.setMessage("User with email address:" + requestInfo.getEmail() + " has registered successfully!");
                }
            } catch (SQLException e) {
                respondInfo.setSucceed(false);
                respondInfo.setMessage("Prepared Statement Failed");
                e.printStackTrace();
            }
            dbUtil.close();
        }
        return respondInfo;
    }

    public static LoginRespondInfo login(LoginRequestInfo requestInfo, String symmetricKey, SocketAddress ip) {
        LoginRespondInfo respondInfo = new LoginRespondInfo();
        DBUtil dbUtil = new DBUtil();
        dbUtil.connectToDB("server_admin", "sdfcldkd", "chat_server");

        try {
            Connection con = dbUtil.getConnection();
            ResultSet rs;

            PreparedStatement prs = con.prepareStatement("SELECT person_password, "
                    + "person_salt, "
                    + "person_isremove "
                    + "FROM person "
                    + "WHERE ( person_email = ?)");
            prs.setString(1, requestInfo.getEmail());
            rs = prs.executeQuery();
            try {
                while (rs.next()) {
                    // decrypt password by person_salt
                    AESEncryptionUtil aesEncryptionUtil = new AESEncryptionUtil(rs.getString("person_salt"));
                    String password = aesEncryptionUtil.decrypt(rs.getString("person_password"));
                    //check password and isremove
                    if (password.equals(requestInfo.getPassword()) && rs.getInt("person_isremove") == 0) {
                        respondInfo.setMessage("Login  successful");

                        //set session
                        try {
                            String sessionID = SymmetricUtil.getSessionID(requestInfo.email, ip, symmetricKey);
                            PreparedStatement preparedStatement = dbUtil.getConnection().prepareStatement("INSERT INTO sessions (session_id,\n" +
                                    "person_email,\n" +
                                    "session_ip,\n" +
                                    "session_salt)\n" +
                                    "VALUES (?, ?, ?, ?);");
                            preparedStatement.setString(1, sessionID);
                            preparedStatement.setString(2, requestInfo.getEmail());
                            preparedStatement.setString(3, ip.toString());
                            preparedStatement.setString(4, symmetricKey);
                            int executeUpdate = 0;
                            try {
                                executeUpdate = preparedStatement.executeUpdate();
                            } catch (SQLException e) {
                                System.err.println(e.getMessage());
                                respondInfo.setSucceed(false);
                                respondInfo.setMessage(e.getMessage());
                            }
                            if (executeUpdate == 1) {
                                respondInfo.setSucceed(true);
                                respondInfo.setMessage("User with email address:" + requestInfo.getEmail() + " has login successfully!");
                                respondInfo.setSessionID(sessionID);
                            }
                        } catch (SQLException e) {
                            respondInfo.setSucceed(false);
                            respondInfo.setMessage("Prepared Statement Failed");
                            e.printStackTrace();
                        }

                        dbUtil.close();
                        return respondInfo;
                    }
                    break;
                }

                respondInfo.setMessage("Login Failed - username or password incorrect");
                respondInfo.setSucceed(false);
            } catch (SQLException e) {
                System.err.println(e.getMessage());
                respondInfo.setSucceed(false);
                respondInfo.setMessage(e.getMessage());
            }
        } catch (SQLException e) {
            respondInfo.setSucceed(false);
            respondInfo.setMessage("Prepared Statement Failed");
            e.printStackTrace();
        }
        dbUtil.close();
        return respondInfo;
    }

    public static RespondInfo hasNewMessages(IdentificationInfo identificationInfo) {
        RespondInfo respondInfo = new RespondInfo();
        DBUtil dbUtil = new DBUtil();
        dbUtil.connectToDB("server_admin", "sdfcldkd", "chat_server");
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = dbUtil.getConnection().prepareStatement("SELECT\n" +
                    "messages.recipient\n" +
                    "FROM\n" +
                    "messages\n" +
                    "WHERE\n" +
                    "messages.recipient = ? AND\n" +
                    "messages.is_sent = 0\n");
            preparedStatement.setString(1, identificationInfo.getEmail());
            ArrayList<Object> table = dbUtil.selectQuery(preparedStatement);
            String recipient = null;
            try {
                recipient = dbUtil.getElement(table, 0, 0, String.class);
                if (recipient != null)
                    respondInfo.setSucceed(true);
                else {
                    respondInfo.setSucceed(false);
                }
            } catch (Exception e) {
                respondInfo.setSucceed(false);
                respondInfo.setMessage(e.toString());
                e.printStackTrace();
            }
        } catch (SQLException e) {
            respondInfo.setSucceed(false);
            respondInfo.setMessage("Prepared Statement Failed");
            e.printStackTrace();
        }
        dbUtil.close();
        return respondInfo;
    }

    public static MessageInfo getMessage(IdentificationInfo identificationInfo) {
        MessageInfo messageInfo = null;
        DBUtil dbUtil = new DBUtil();
        dbUtil.connectToDB("server_admin", "sdfcldkd", "chat_server");
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = dbUtil.getConnection().prepareStatement("SELECT\n" +
                    "messages.recipient,\n" +
                    "messages.sender,\n" +
                    "messages.receive_time,\n" +
                    "messages.content,\n" +
                    "messages.attachment\n" +
                    "FROM\n" +
                    "messages\n" +
                    "WHERE\n" +
                    "messages.recipient = ? AND\n" +
                    "messages.is_sent = 0\n" +
                    "LIMIT 1");
            preparedStatement.setString(1, identificationInfo.getEmail());
            ArrayList<Object> table = dbUtil.selectQuery(preparedStatement);
            ArrayList<Object> row = dbUtil.getRow(table, 0);
            String recipientString = (String) row.get(0);
            String sender = (String) row.get(1);
            Timestamp receiveTime = (Timestamp) row.get(2);
            String content = (String) row.get(3);
            String attachment = (String) row.get(4);
            messageInfo = new MessageInfo();
            MessageInfo.Recipient recipient = new MessageInfo.Recipient();
            recipient.setrType(MessageInfo.Recipient.RecipientType.PERSON);
            recipient.setRecipient(recipientString);
            messageInfo.setRecipient(recipient);
            messageInfo.setSender(sender);
            messageInfo.setTime(receiveTime.getTime());
            messageInfo.setContent(content);
            MessageInfo.AttachmentURL attachmentURL = new MessageInfo.AttachmentURL();
            attachmentURL.setUrl(attachment);
            messageInfo.setAttachmentURL(attachmentURL);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        dbUtil.close();
        return messageInfo;
    }

    public static String[] getSessionInfo(String sessionID) {
        DBUtil dbUtil = new DBUtil();
        dbUtil.connectToDB("server_admin", "sdfcldkd", "chat_server");
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = dbUtil.getConnection().prepareStatement("SELECT\n" +
                    "sessions.person_email,\n" +
                    "sessions.session_salt\n" +
                    "FROM\n" +
                    "sessions\n" +
                    "WHERE\n" +
                    "sessions.session_id = ?\n");
            preparedStatement.setString(1, sessionID);
            ArrayList<Object> table = dbUtil.selectQuery(preparedStatement);
            String email = dbUtil.getElement(table, 0, 0, String.class);
            String salt = dbUtil.getElement(table, 0, 1, String.class);
            dbUtil.close();
            return new String[]{email, salt};
        } catch (SQLException e) {
            e.printStackTrace();
        }
        dbUtil.close();
        return null;
    }

    public static RespondInfo sentMessage(MessageInfo messageInfo) {
        RespondInfo respondInfo = new RespondInfo();
        DBUtil dbUtil = new DBUtil();
        dbUtil.connectToDB("server_admin", "sdfcldkd", "chat_server");
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = dbUtil.getConnection().prepareStatement("UPDATE messages\n" +
                    "SET messages.is_sent = 1\n" +
                    "WHERE\n" +
                    "messages.recipient = ? AND\n" +
                    "messages.sender = ? AND\n" +
                    "messages.receive_time = ? AND\n" +
                    "messages.is_sent = 0\n");
            preparedStatement.setString(1, messageInfo.getRecipient().getRecipient());
            preparedStatement.setString(2, messageInfo.getSender());
            preparedStatement.setTimestamp(3, Timestamp.from(Instant.ofEpochMilli(messageInfo.getTime())));
            int executeUpdate = 0;
            try {
                executeUpdate = preparedStatement.executeUpdate();
            } catch (SQLException e) {
                System.err.println(e.getMessage());
                respondInfo.setSucceed(false);
                respondInfo.setMessage(e.getMessage());
            }
            if (executeUpdate == 1) {
                respondInfo.setSucceed(true);
            }
        } catch (SQLException e) {
            respondInfo.setSucceed(false);
            respondInfo.setMessage("Prepared Statement Failed");
            e.printStackTrace();
        }
        dbUtil.close();
        return respondInfo;
    }

    public static RespondInfo insertMessage(MessageInfo messageInfo) {
        RespondInfo respondInfo = new RespondInfo();
        DBUtil dbUtil = new DBUtil();
        dbUtil.connectToDB("server_admin", "sdfcldkd", "chat_server");
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = dbUtil.getConnection().prepareStatement("INSERT INTO messages (\n" +
                    "recipient,\n" +
                    "sender,\n" +
                    "content,\n" +
                    "attachment)\n" +
                    "VALUES (?, ?, ?, ?);");
            preparedStatement.setString(1, messageInfo.getRecipient().getRecipient());
            preparedStatement.setString(2, messageInfo.getSender());
            preparedStatement.setString(3, messageInfo.getContent());
            if (messageInfo.getAttachmentURL() != null)
                preparedStatement.setString(4, messageInfo.getAttachmentURL().getUrl());
            else
                preparedStatement.setString(4, null);
            int executeUpdate = 0;
            try {
                executeUpdate = preparedStatement.executeUpdate();
            } catch (SQLException e) {
                System.err.println(e.getMessage());
                respondInfo.setSucceed(false);
                respondInfo.setMessage(e.getMessage());
            }
            if (executeUpdate == 1) {
                respondInfo.setSucceed(true);
                respondInfo.setMessage("message successfully added in DB!");
            }
        } catch (SQLException e) {
            respondInfo.setSucceed(false);
            respondInfo.setMessage("Prepared Statement Failed");
            e.printStackTrace();
        }
        dbUtil.close();
        return respondInfo;
    }
}

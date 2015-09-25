import javax.xml.bind.annotation.*;
import java.sql.Timestamp;
import java.util.Date;

/**
 * Created by Hamed on 9/20/2015.
 */
@XmlRootElement
public class MessageInfo extends XMLInfo {
    String sender;
    Recipient recipient;
    long time;
    String session;
    String content;
    AttachmentURL attachmentURL;

    public String getSender() {
        return sender;
    }

    @XmlElement
    public void setSender(String sender) {
        this.sender = sender;
    }

    public Recipient getRecipient() {
        return recipient;
    }

    @XmlElement
    public void setRecipient(Recipient recipient) {
        this.recipient = recipient;
    }

    public long getTime() {
        return time;
    }

    @XmlElement
    public void setTime(long time) {
        this.time = time;
    }

    public String getSession() {
        return session;
    }

    @XmlElement
    public void setSession(String session) {
        this.session = session;
    }

    public String getContent() {
        return content;
    }

    @XmlElement
    public void setContent(String content) {
        this.content = content;
    }

    public AttachmentURL getAttachmentURL() {
        return attachmentURL;
    }

    @XmlElement
    public void setAttachmentURL(AttachmentURL attachmentURL) {
        this.attachmentURL = attachmentURL;
    }

    @Override
    public String toString() {
        return "MessageInfo{" +
                "sender='" + sender + '\'' +
                ", recipient=" + recipient +
                ", time=" + time +
                ", session='" + session + '\'' +
                ", content='" + content + '\'' +
                ", attachmentURL=" + attachmentURL +
                '}';
    }

    @XmlRootElement
    static class Recipient {
        RecipientType rType;
        String recipient;

        public enum RecipientType {
            PERSON ("PERSON"), GROUP ("GROUP");

            String name;

            RecipientType(String name) {
                this.name = name;
            }

            @Override
            public String toString() {
                return "RecipientType{" +
                        name + '}';
            }
        }

        public RecipientType getrType() {
            return rType;
        }

        @XmlAttribute
        public void setrType(RecipientType rType) {
            this.rType = rType;
        }

        public String getRecipient() {
            return recipient;
        }

        @XmlValue
        public void setRecipient(String recipient) {
            this.recipient = recipient;
        }

        @Override
        public String toString() {
            return "Recipient{" +
                    "rType=" + rType +
                    ", recipient='" + recipient + '\'' +
                    '}';
        }
    }

    @XmlRootElement
    static class AttachmentURL {
        String fileType;
        String url;

        public String getFileType() {
            return fileType;
        }

        @XmlAttribute
        public void setFileType(String fileType) {
            this.fileType = fileType;
        }

        public String getUrl() {
            return url;
        }

        @XmlValue
        public void setUrl(String url) {
            this.url = url;
        }

        @Override
        public String toString() {
            return "AttachmentURL{" +
                    "fileType='" + fileType + '\'' +
                    ", url='" + url + '\'' +
                    '}';
        }
    }
}


import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;

/**
 * Created by Hamed on 9/20/2015.
 */
@XmlRootElement
public class MessageInfo extends XMLInfo {
    String sender;
    Recipient recipient;
    Date time;
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

    public Date getTime() {
        return time;
    }

    @XmlElement
    public void setTime(Date time) {
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

        @Override
        public String toString() {
            return "Recipient{" +
                    "rType=" + rType +
                    '}';
        }
    }

    @XmlRootElement
    public class AttachmentURL {
        String fileType;

        public String getFileType() {
            return fileType;
        }

        @XmlAttribute
        public void setFileType(String fileType) {
            this.fileType = fileType;
        }

        @Override
        public String toString() {
            return "AttachmentURL{" +
                    "fileType='" + fileType + '\'' +
                    '}';
        }
    }
}


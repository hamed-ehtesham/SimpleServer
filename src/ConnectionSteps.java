import java.nio.channels.SelectionKey;
import java.util.ArrayList;

public interface ConnectionSteps {
    enum Registration implements ConnectionSteps {
        PUBLIC_KEY, SYMMETRIC_KEY, REG_INFO, REG_RESPOND;

        private Object attachment;

        @Override
        public Object getAttachment() {
            return attachment;
        }

        @Override
        public void setAttachment(Object attachment) {
            this.attachment = attachment;
        }
    }

    enum Login implements ConnectionSteps {
        PUBLIC_KEY, SYMMETRIC_KEY, LOGIN_INFO, LOGIN_RESPOND;

        private Object attachment;

        @Override
        public Object getAttachment() {
            return attachment;
        }

        @Override
        public void setAttachment(Object attachment) {
            this.attachment = attachment;
        }
    }

    enum Messaging implements ConnectionSteps {
        IDENTIFICATION, SYNC_RESPOND, MESSAGE_INFO, MESSAGE_RESPOND;

        private Object attachment;

        @Override
        public Object getAttachment() {
            return attachment;
        }

        @Override
        public void setAttachment(Object attachment) {
            this.attachment = attachment;
        }
    }

    Object getAttachment();

    void setAttachment(Object attachment);

    static void attach(SelectionKey key, int ops, ConnectionSteps nextStep, Object... attachments) {
        ArrayList<Object> attachmentList = null;
        if (attachments.length > 0) {
            attachmentList = new ArrayList<Object>(attachments.length);
            for (Object attachment : attachments) {
                attachmentList.add(attachment);
            }
        }
        key.interestOps(ops);
        nextStep.setAttachment(attachmentList);
        key.attach(nextStep);
    }

    static ArrayList<Object> attachment(SelectionKey key) {
        ConnectionSteps step = (ConnectionSteps) key.attachment();
        ArrayList<Object> attachmentList = (ArrayList<Object>) step.getAttachment();
        return attachmentList;
    }
}

public class ConnectionSteps {
    public enum Registration {
        PUBLIC_KEY, SYMMETRIC_KEY, REG_INFO, REG_RESPOND;

        private Object attachment;

        public Object getAttachment() {
            return attachment;
        }

        public void setAttachment(Object attachment) {
            this.attachment = attachment;
        }
    }

    public enum Login {
        PUBLIC_KEY, SYMMETRIC_KEY, LOGIN_INFO, LOGIN_RESPOND;

        private Object attachment;

        public Object getAttachment() {
            return attachment;
        }

        public void setAttachment(Object attachment) {
            this.attachment = attachment;
        }
    }

    public enum Messaging {
        IDLE, MESSAGE_INFO, MESSAGE_RESPOND
    }
}

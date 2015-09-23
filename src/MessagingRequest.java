//import javax.crypto.SealedObject;
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.nio.ByteBuffer;
//import java.nio.channels.SelectionKey;
//import java.nio.channels.SocketChannel;
//
///**
// * Created by Hamed on 9/23/2015.
// */
//public class MessagingRequest {
//    String sessionID;
//
//    public MessagingRequest(String sessionID) {
//        this.sessionID = sessionID;
//    }
//
//    public String getSessionID() {
//        return sessionID;
//    }
//
//    public void setSessionID(String sessionID) {
//        this.sessionID = sessionID;
//    }
//
//    public static void main(String[] args) {
//        MessagingRequest request = new MessagingRequest("test session id");
//        request.request();
//    }
//
//    class request extends ClientHandler {
//        private SealedObject sealedObject;
//
//        public request(String ADDRESS, int PORT) {
//            super(ADDRESS, PORT);
//        }
//
//        @Override
//        protected void read(SelectionKey key) throws IOException {
//            ByteArrayOutputStream bos = ChannelHelper.read(key);
//            byte[] data = bos.toByteArray();
//            switch ((ConnectionSteps.Registration) key.attachment()) {
//                case PUBLIC_KEY: {
//                    KeyInfo keyInfo = XMLUtil.unmarshal(KeyInfo.class, data);
//                    System.out.println(keyInfo);
//                    encryptSymmetricKey(keyInfo);
//                    key.interestOps(SelectionKey.OP_WRITE);
//                    key.attach(ConnectionSteps.Registration.SYMMETRIC_KEY);
//                    break;
//                }
//                case REG_RESPOND: {
//                    RegistrationRespondInfo respondInfo = XMLUtil.unmarshal(RegistrationRespondInfo.class, data);
//                    setRespond(respondInfo);
//                    System.out.println(respondInfo);
//                    key.cancel();
//                    Thread.currentThread().interrupt();
//                    break;
//                }
//            }
//        }
//
//        @Override
//        protected void write(SelectionKey key) throws IOException {
//            SocketChannel channel = (SocketChannel) key.channel();
//
//            switch ((ConnectionSteps.Registration) key.attachment()) {
//                case SYMMETRIC_KEY: {
//                    if (sealedObject != null) {
//                        ChannelHelper.writeObject(channel, sealedObject);
////                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
////                        ObjectOutputStream oos = new ObjectOutputStream(bos);
////                        SealedObject[] soa = new SealedObject[]{sealedObject};
////
////                        oos.writeObject(soa);
////                        oos.flush();
////
////                        ByteBuffer buffer = ByteBuffer.wrap(bos.toByteArray());
////                        channel.write(buffer);
////                    System.out.println(new String(buffer.array()));
//                        key.attach(ConnectionSteps.Registration.REG_INFO);
//                    }
//
//                    break;
//                }
//                case REG_INFO: {
//                    RegistrationRequestInfo requestInfo = new RegistrationRequestInfo();
//                    requestInfo.setEmail(getEmail());
//                    requestInfo.setPassword(getPassword());
//                    requestInfo.setFirstName(getFirstName());
//                    requestInfo.setLastName(getLastName());
//                    requestInfo.setNickname(getNickname());
//
//                    ByteBuffer buffer = XMLUtil.marshal(requestInfo);
//                    AESEncryptionUtil aesEncryptionUtil = new AESEncryptionUtil(symmetricKey);
//                    buffer = aesEncryptionUtil.encrypt(buffer);
//                    channel.write(buffer);
////                    System.out.println(new String(buffer.array()));
//                    key.interestOps(SelectionKey.OP_READ);
//                    ConnectionSteps.Registration respond = ConnectionSteps.Registration.REG_RESPOND;
//                    key.attach(ConnectionSteps.Registration.REG_RESPOND);
//
//                    break;
//                }
//            }
//        }
//
//        private void encryptSymmetricKey(KeyInfo keyInfo) {
//            try {
//                sealedObject = RSAEncryptionUtil.encryptByPublicKey(symmetricKey, keyInfo.key);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    public void request() {
//        Thread thread = new Thread(new request("localhost", 8511));
//        thread.start();
//    }
//}

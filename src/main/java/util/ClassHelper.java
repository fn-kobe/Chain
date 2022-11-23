package util;

import java.io.*;

public class ClassHelper {
    public byte[] serializeClass(Serializable serializable) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(BufferHelper.getMaxBufferSize());
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(serializable);
            objectOutputStream.close();
            // get the byte array of the object
            byte[] serialize = byteArrayOutputStream.toByteArray();
            // DEBUG
            System.out.println("The array for write size is: " + serialize.length);
            byteArrayOutputStream.close();
            return serialize;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Excpetion in serializeClass");
        }
        return null;
    }

    public Serializable deserializeClass(byte[] data) {
        try {
            System.out.println("The array for read size is: " + data.length);
            ObjectInputStream iStream = new ObjectInputStream(new ByteArrayInputStream(data));
            Serializable obj = (Serializable) iStream.readObject();
            iStream.close();
            return obj;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

package util;

public class AddressPort {
    String address = ""; // ip
    int port = 0;
    boolean isValid = false;

    public AddressPort() {
    }

    public AddressPort(String address, int port) {
        isValid = StringHelper.isValidIP(address);
        if (isValid) {
            this.address = address;
            this.port = port;
        }
    }

    public void setAddressPort(String address, int port) {
        isValid = StringHelper.isValidIP(address);
        if (isValid) {
            this.address = address;
            this.port = port;
        }
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public boolean isValid() {
        return isValid;
    }

    public boolean isTheSame(AddressPort another){
        if (isValid != another.isValid) return false;
        if (!address.equals(another.address)) return false;
        if (!(port == another.port)) return false;

        return true;
    }

    public boolean isTheSame(String address, int port){
        if (!isValid) return false;
        if (!this.address.equals(address)) return false;
        if (!(this.port == port)) return false;

        return true;
    }
}

package com.pratishaad.homelibrarymanagement.authentication;

public class RegisterClassDB {
    String userID;
    String uname;
    String email;

    public RegisterClassDB(){

    }

    public RegisterClassDB(String userID, String uname, String email) {
        this.userID = userID;
        this.uname = uname;
        this.email = email;
    }

    public String getUserID() {
        return userID;
    }

    public String getEmail() {
        return email;
    }

    public String getUname() {
        return uname;
    }

}

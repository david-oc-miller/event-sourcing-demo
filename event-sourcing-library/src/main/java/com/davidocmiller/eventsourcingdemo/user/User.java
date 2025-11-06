package com.davidocmiller.eventsourcingdemo.user;

import lombok.Getter;

@Getter
public class User
{

    private String userId;
    private String firstName;

    public User(String userId, String firstName)
    {
        this.userId = userId;
        this.firstName = firstName;
    }    
}

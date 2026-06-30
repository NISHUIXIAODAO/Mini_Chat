package com.easychat.service;

public interface IUserEmailBloomService {
    boolean mightContain(String email);

    void add(String email);
}


package com.softserve.edu.hypercinema.exception;

public class HallNotFoundException extends ServiceException {

    public HallNotFoundException() {
    }

    public HallNotFoundException(String message) {
        super(message);
    }

}
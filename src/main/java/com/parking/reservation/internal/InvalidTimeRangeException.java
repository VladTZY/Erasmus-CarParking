package com.parking.reservation.internal;

class InvalidTimeRangeException extends RuntimeException {
    InvalidTimeRangeException(String message) {
        super(message);
    }
}

package ca.localcoin.banking.service;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) { super(message); }
}

package com.talentsaurus.matchmaking.matchmaking.service;

public class NotFoundException extends RuntimeException {
  public NotFoundException(String message) {
    super(message);
  }
}

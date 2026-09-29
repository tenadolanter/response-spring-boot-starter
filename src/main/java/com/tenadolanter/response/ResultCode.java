package com.tenadolanter.response;

/**
 * A business status code. Applications can implement this interface with their own enum.
 */
public interface ResultCode {

    int getCode();

    String getMessage();
}

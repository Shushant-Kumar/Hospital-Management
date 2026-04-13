package com.shushant.hospital_management.common.exception;

public class ResourceConflictException extends BusinessException {

    public ResourceConflictException(String message) {
        super(ErrorCode.RESOURCE_CONFLICT, message);
    }
}

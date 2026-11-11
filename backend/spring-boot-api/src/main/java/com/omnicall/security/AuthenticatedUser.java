package com.omnicall.security;

import java.util.UUID;

public record AuthenticatedUser(UUID id, String phone) {
}

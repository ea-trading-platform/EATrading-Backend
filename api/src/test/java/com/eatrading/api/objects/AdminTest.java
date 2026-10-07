package com.eatrading.api.objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

class AdminTest {

    @Test
    void testAdminConstructorAndGetters() {
        Admin admin = new Admin("Alice", "alice@example.com");
        assertEquals("Alice", admin.getName());
        assertEquals("alice@example.com", admin.getEmail());
        assertNotNull(admin.getId());
        assertNull(admin.getCreatedAt());
        assertNull(admin.getUpdatedAt());
    }

    @Test
    void testSettersUpdateValues() {
        Admin admin = new Admin("Bob", "bob@example.com");
        admin.setName("Bobby");
        admin.setEmail("bobby@example.com");
        assertEquals("Bobby", admin.getName());
        assertEquals("bobby@example.com", admin.getEmail());
    }

    // ===== EDGE CASES =====

    @Test
    void testTwoAdmins_HaveDifferentIds() {
        Admin a1 = new Admin("A", "a@example.com");
        Admin a2 = new Admin("B", "b@example.com");
        assertNotNull(a1.getId());
        assertNotNull(a2.getId());
        assertNotEquals(a1.getId(), a2.getId());
    }

    @Test
    void testSetNameAndEmailToNull_CurrentBehavior() {
        Admin a = new Admin("Name", "e@example.com");
        a.setName(null);
        a.setEmail(null);
        assertNull(a.getName());
        assertNull(a.getEmail());
        // TODO: decide whether nulls should be allowed for name/email and enforce validation if not.
    }
}

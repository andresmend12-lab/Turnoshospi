package com.example.turnoshospi.domain.logic

import com.example.turnoshospi.domain.model.UserRole
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionCheckerTest {

    @Test
    fun `canManageStaff returns true for ADMIN`() {
        assertTrue(PermissionChecker.canManageStaff(UserRole.ADMIN))
    }

    @Test
    fun `canManageStaff returns false for STAFF`() {
        assertFalse(PermissionChecker.canManageStaff(UserRole.STAFF))
    }
}

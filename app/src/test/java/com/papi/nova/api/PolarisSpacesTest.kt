package com.papi.nova.api
import org.junit.Assert.*
import org.junit.Test

@org.robolectric.annotation.Config(sdk = [33])
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
class PolarisSpacesTest {
    private val valid = """{"schema":1,"status":true,"enabled":true,"available":true,"can_switch":true,"selected_space_id":"a","spaces":[{"id":"a","name":"Alex","state":"ready","selected":true},{"id":"b","name":"Sam","state":"in_use","selected":false}]}"""
    @Test fun parsesPermissionScopedChoicesAndActivity() {
        val result = requireNotNull(PolarisSpaces.parse(valid))
        assertEquals("Alex", result.selected?.name); assertEquals("in_use", result.spaces[1].state)
        assertTrue(result.canSwitch)
    }
    @Test fun rejectsAmbiguousIdentityTypesAndState() {
        for (bad in listOf(valid.replace("\"id\":\"b\"", "\"id\":\"a\""),
            valid.replace("\"selected_space_id\":\"a\"", "\"selected_space_id\":\"b\""),
            valid.replace("\"selected\":false", "\"selected\":true"),
            valid.replace("\"state\":\"in_use\"", "\"state\":\"idle_maybe\""),
            valid.replace("\"can_switch\":true", "\"can_switch\":\"true\""),
            valid.replace("\"schema\":1", "\"schema\":1,\"schema\":1"),
            valid.replace("\"available\":true", "\"available\":false"))) assertNull(bad, PolarisSpaces.parse(bad))
    }
    @Test fun unavailableHostHasNoImplicitSelection() {
        val result = PolarisSpaces.parse("""{"schema":1,"status":true,"enabled":false,"available":false,"can_switch":false,"selected_space_id":"","spaces":[]}""")
        assertNotNull(result); assertNull(result?.selected)
    }
    @Test fun desktopChoiceRequiresAnExplicitBooleanGrant() {
        val desktop = valid.replace("\"selected_space_id\":\"a\"", "\"selected_space_id\":\"desktop\"")
            .replace("\"selected\":true", "\"selected\":false")
        assertNull(PolarisSpaces.parse(desktop))
        val allowed = desktop.replace("\"schema\":1", "\"schema\":1,\"desktop_allowed\":true")
        val parsed = requireNotNull(PolarisSpaces.parse(allowed))
        assertEquals("desktop", parsed.selectedId); assertNull(parsed.selected)
        assertNull(PolarisSpaces.parse(allowed.replace("\"desktop_allowed\":true", "\"desktop_allowed\":\"true\"")))
        assertNull(PolarisSpaces.parse(valid.replace("\"id\":\"a\"", "\"library_enabled\":\"true\",\"id\":\"a\"")))
    }

}

package com.papi.nova.nvstream

import java.lang.reflect.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoonBridgeJniSignatureTest {
    @Test
    fun workerAudioFlagKeepsTheCompiledNativeEntrypoint() {
        // Robolectric rewrites native methods. Inspect the real class without
        // initializing it or attempting to load Android native libraries.
        val bridge = Class.forName(
            "com.papi.nova.nvstream.jni.MoonBridge",
            false,
            javaClass.classLoader
        )
        val intType = Int::class.javaPrimitiveType!!
        val parameters = buildList<Class<*>> {
            repeat(4) { add(String::class.java) }
            repeat(10) { add(intType) }
            repeat(2) { add(ByteArray::class.java) }
            repeat(3) { add(intType) }
            add(Boolean::class.javaPrimitiveType!!)
        }
        val method = bridge.getDeclaredMethod("startConnection", *parameters.toTypedArray())
        assertEquals(intType, method.returnType)
        assertTrue(Modifier.isPublic(method.modifiers))
        assertTrue(Modifier.isStatic(method.modifiers))
        assertTrue(Modifier.isNative(method.modifiers))
    }
}

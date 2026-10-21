package io.github.gonzsanz.cobalttv.data.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uParserTest {

    @Test
    fun `parses attributes, groups, logos and urls`() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1 tvg-id="cine.1" tvg-name="Cine Uno" tvg-logo="https://img/1.png" group-title="Cine",Cine Uno
            https://stream.example/cine1.m3u8
            #EXTINF:-1 tvg-id="dep.2" group-title="Deportes",Deportes 24
            http://stream.example/dep2.m3u8
        """.trimIndent()

        val channels = M3uParser.parse(playlist)

        assertEquals(2, channels.size)
        assertEquals("Cine Uno", channels[0].name)
        assertEquals("Cine", channels[0].group)
        assertEquals("https://img/1.png", channels[0].logoUrl)
        assertEquals("cine.1", channels[0].tvgId)
        assertEquals("https://stream.example/cine1.m3u8", channels[0].streamUrl)
        assertEquals("Deportes", channels[1].group)
        assertNull(channels[1].logoUrl)
    }

    @Test
    fun `handles BOM, CRLF and a missing header`() {
        val playlist = "\uFEFF#EXTINF:-1 group-title=\"Noticias\",Aurora\r\n" +
            "https://stream.example/aurora.m3u8\r\n" +
            "#EXTINF:-1,Planeta\r\n" +
            "https://stream.example/planeta.m3u8\r\n"

        val channels = M3uParser.parse(playlist)

        assertEquals(2, channels.size)
        assertEquals("Aurora", channels[0].name)
        assertEquals("Noticias", channels[0].group)
        assertEquals("Planeta", channels[1].name)
        assertEquals("Sin categoría", channels[1].group)
    }

    @Test
    fun `keeps commas in quoted attributes and in the name`() {
        val playlist = """
            #EXTINF:-1 group-title="Cine, Acción" tvg-name="Uno, Dos",Canal, con coma
            https://stream.example/uno.m3u8
        """.trimIndent()

        val channel = M3uParser.parse(playlist).single()

        assertEquals("Canal, con coma", channel.name)
        assertEquals("Cine, Acción", channel.group)
    }

    @Test
    fun `ignores malformed entries without dropping valid ones`() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1 tvg-id="huerfano",Sin URL
            #EXTINF:-1 group-title="Cine",Correcto
            https://stream.example/correcto.m3u8
            # un comentario
            #EXTINF:-1,Roto
            #EXTINF:-1,Otro correcto
            https://stream.example/otro.m3u8
        """.trimIndent()

        val channels = M3uParser.parse(playlist)

        assertEquals(2, channels.size)
        assertEquals(listOf("Correcto", "Otro correcto"), channels.map { it.name })
    }

    @Test
    fun `deduplicates identical name and url`() {
        val playlist = """
            #EXTINF:-1 group-title="Cine",Repetido
            https://stream.example/repetido.m3u8
            #EXTINF:-1 group-title="Otro",Repetido
            https://stream.example/repetido.m3u8
        """.trimIndent()

        assertEquals(1, M3uParser.parse(playlist).size)
    }

    @Test
    fun `accepts bare urls without EXTINF`() {
        val playlist = """
            https://stream.example/mi_canal-uno.m3u8
            https://stream.example/dos.m3u8?token=abc
        """.trimIndent()

        val channels = M3uParser.parse(playlist)

        assertEquals(2, channels.size)
        assertEquals("mi canal uno", channels[0].name)
        assertEquals("dos", channels[1].name)
    }

    @Test
    fun `falls back to tvg-name when the title is empty`() {
        val playlist = """
            #EXTINF:-1 tvg-name="Nombre Alternativo" group-title="Series",
            https://stream.example/alternativo.m3u8
        """.trimIndent()

        assertEquals("Nombre Alternativo", M3uParser.parse(playlist).single().name)
    }

    @Test
    fun `EXTGRP overrides the group attribute`() {
        val playlist = """
            #EXTINF:-1 group-title="Cine",Canal
            #EXTGRP:Documentales
            https://stream.example/canal.m3u8
        """.trimIndent()

        assertEquals("Documentales", M3uParser.parse(playlist).single().group)
    }

    @Test
    fun `ids are stable across parses and unique per entry`() {
        val playlist = """
            #EXTINF:-1,Canal A
            https://stream.example/a.m3u8
            #EXTINF:-1,Canal B
            https://stream.example/b.m3u8
        """.trimIndent()

        val first = M3uParser.parse(playlist)
        val second = M3uParser.parse(playlist)

        assertEquals(first.map { it.id }, second.map { it.id })
        assertNotEquals(first[0].id, first[1].id)
        assertTrue(first.all { it.id.startsWith("ch-") })
    }

    @Test
    fun `empty and comment-only playlists return no channels`() {
        assertTrue(M3uParser.parse("").isEmpty())
        assertTrue(M3uParser.parse("#EXTM3U\n# solo comentarios").isEmpty())
    }
}

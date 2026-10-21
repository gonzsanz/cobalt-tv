package io.github.gonzsanz.cobalttv.data.epg

import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EpgParserTest {

    private val now = parseXmltvTime("20261021120000 +0000")!!

    private val xml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <tv>
          <channel id="cine.1"><display-name>Cine Uno</display-name></channel>
          <programme start="20261021110000 +0000" stop="20261021123000 +0000" channel="cine.1">
            <title lang="es">Película &amp; palomitas</title>
            <desc>Una película de prueba.</desc>
          </programme>
          <programme start="20261021123000 +0000" stop="20261021130000 +0000" channel="cine.1">
            <title>Próximo</title>
          </programme>
          <programme start="20261021080000 +0000" stop="20261021090000 +0000" channel="cine.1">
            <title>Antiguo</title>
          </programme>
          <programme start="20261021120000 +0000" stop="20261021130000 +0000" channel="dep.2">
            <title>Deportes</title>
          </programme>
          <programme start="20261021120000 +0000" stop="20261021130000 +0000" channel="otro">
            <title>Ignorado</title>
          </programme>
        </tv>
    """.trimIndent()

    @Test
    fun `keeps current and next programmes for wanted channels`() {
        val result = EpgParser.parse(
            input = ByteArrayInputStream(xml.toByteArray()),
            channelIds = setOf("cine.1", "dep.2"),
            nowMs = now,
        )

        assertEquals(setOf("cine.1", "dep.2"), result.keys)
        assertFalse(result.containsKey("otro"))

        val cine = result.getValue("cine.1")
        assertEquals(listOf("Película & palomitas", "Próximo"), cine.map { it.title })

        val cineNowNext = cine.nowNextAt(now)
        assertEquals("Película & palomitas", cineNowNext.now?.title)
        assertEquals("Próximo", cineNowNext.next?.title)

        val depNowNext = result.getValue("dep.2").nowNextAt(now)
        assertEquals("Deportes", depNowNext.now?.title)
        assertNull(depNowNext.next)
    }

    @Test
    fun `uses a default duration when stop is missing`() {
        val shortXml = """
            <tv>
              <programme start="20261021115900 +0000" channel="cine.1">
                <title>Sin hora de fin</title>
              </programme>
            </tv>
        """.trimIndent()

        val programme = EpgParser.parse(
            input = ByteArrayInputStream(shortXml.toByteArray()),
            channelIds = setOf("cine.1"),
            nowMs = now,
        ).getValue("cine.1").single()

        assertEquals("Sin hora de fin", programme.title)
        assertEquals(programme.startMs + 60 * 60 * 1000, programme.stopMs)
        assertNotNull(programme)
    }

    @Test
    fun `ignores every programme when no channel ids are requested`() {
        val result = EpgParser.parse(
            input = ByteArrayInputStream(xml.toByteArray()),
            channelIds = emptySet(),
            nowMs = now,
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `drops programmes outside the window and invalid time ranges`() {
        val windowXml = """
            <tv>
              <programme start="20261023120000 +0000" stop="20261023130000 +0000" channel="cine.1">
                <title>Demasiado futuro</title>
              </programme>
              <programme start="20261021130000 +0000" stop="20261021120000 +0000" channel="cine.1">
                <title>Rango inválido</title>
              </programme>
            </tv>
        """.trimIndent()

        val result = EpgParser.parse(
            input = ByteArrayInputStream(windowXml.toByteArray()),
            channelIds = setOf("cine.1"),
            nowMs = now,
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `parses XMLTV timestamps with and without an offset`() {
        assertEquals(0L, parseXmltvTime("19700101000000 +0000"))
        assertEquals(0L, parseXmltvTime("19700101020000 +0200"))
        assertEquals(0L, parseXmltvTime("19700101000000"))
        assertEquals(-7_200_000L, parseXmltvTime("19700101000000 +02:00"))
        assertNull(parseXmltvTime("not-a-date"))
        assertNull(parseXmltvTime(null))
    }
}

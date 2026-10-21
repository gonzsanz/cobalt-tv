package io.github.gonzsanz.cobalttv.data.xtream

import io.github.gonzsanz.cobalttv.data.catalog.Channel
import io.github.gonzsanz.cobalttv.data.catalog.SeriesItem
import io.github.gonzsanz.cobalttv.data.catalog.VodItem
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class XtreamJsonTest {

    private val credentials = XtreamCredentials(
        server = "http://demo.example:8080/",
        username = "user name",
        password = "p@ss",
    )
    private val base = XtreamJson.normalizeServer(credentials.server)

    @Test
    fun `normalizes server urls and builds encoded api endpoints`() {
        val normalized = XtreamJson.normalizeServer(
            "demo.example:8080/player_api.php?username=old#fragment",
        )
        assertEquals("http://demo.example:8080/", normalized.toString())

        val api = XtreamJson.apiUrl(base, credentials, "get_live_streams")
        assertEquals("/player_api.php", api.encodedPath)
        assertEquals("user name", api.queryParameter("username"))
        assertEquals("p@ss", api.queryParameter("password"))
        assertEquals("get_live_streams", api.queryParameter("action"))
        assertTrue(api.toString().contains("username=user%20name"))
        assertTrue(api.toString().contains("password=p%40ss"))
    }

    @Test
    fun `keeps server subdirectories and strips known endpoints`() {
        val subpath = XtreamJson.normalizeServer(
            "https://demo.example/iptv/player_api.php?username=old",
        )
        assertEquals("https://demo.example/iptv", subpath.toString())

        val api = XtreamJson.apiUrl(subpath, credentials, "get_live_streams")
        assertEquals("/iptv/player_api.php", api.encodedPath)
        assertEquals(
            "https://demo.example/iptv/live/user%20name/p@ss/101.m3u8",
            XtreamJson.streamUrl(subpath, "live", credentials, "101", "m3u8"),
        )
    }

    @Test
    fun `builds playback urls with encoded credentials`() {
        assertEquals(
            "http://demo.example:8080/live/user%20name/p@ss/101.m3u8",
            XtreamJson.streamUrl(base, "live", credentials, "101", "m3u8"),
        )
        assertEquals(
            "http://demo.example:8080/movie/user%20name/p@ss/55.mkv",
            XtreamJson.streamUrl(base, "movie", credentials, "55", "mkv"),
        )
    }

    @Test
    fun `reads account status and limits`() {
        val account = XtreamJson.parseLogin(
            JSONObject(
                """{"user_info":{"auth":1,"status":"Active","max_connections":"2"}}""",
            ),
        )

        assertEquals("Active", account.status)
        assertFalse(account.isTrial)
        assertEquals(2, account.maxConnections)
    }

    @Test
    fun `rejects invalid or inactive accounts`() {
        val authError = assertThrows(XtreamException::class.java) {
            XtreamJson.parseLogin(JSONObject("""{"user_info":{"auth":0}}"""))
        }
        assertEquals("Usuario o contraseña incorrectos.", authError.message)

        val expired = assertThrows(XtreamException::class.java) {
            XtreamJson.parseLogin(
                JSONObject("""{"user_info":{"auth":"1","status":"Expired"}}"""),
            )
        }
        assertTrue(expired.message!!.contains("Expired"))
    }

    @Test
    fun `normalizes live streams with categories and metadata`() {
        val categories = XtreamJson.parseCategories(
            JSONArray(
                """[
                    {"category_id":"1","category_name":"Cine"},
                    {"category_id":2,"category_name":"Deportes"}
                ]""",
            ),
        )
        val channels = XtreamJson.parseLiveStreams(
            value = JSONArray(
                """[
                    {
                        "num":3,
                        "name":"Cine Uno",
                        "stream_id":101,
                        "stream_icon":"https://img.example/cine.png",
                        "epg_channel_id":"cine.1",
                        "category_id":"1"
                    },
                    {
                        "name":"Sin categoria",
                        "stream_id":"102",
                        "category_id":"99",
                        "epg_channel_id":"null"
                    }
                ]""",
            ),
            categories = categories,
            credentials = credentials,
            base = base,
        )

        assertEquals(2, channels.size)
        assertEquals("Cine", channels[0].group)
        assertEquals("Cine Uno", channels[0].name)
        assertEquals("https://img.example/cine.png", channels[0].logoUrl)
        assertEquals("cine.1", channels[0].tvgId)
        assertEquals(3, channels[0].number)
        assertEquals(
            "http://demo.example:8080/live/user%20name/p@ss/101.m3u8",
            channels[0].streamUrl,
        )
        assertTrue(channels[0].id.startsWith("ch-"))
        assertEquals("Sin categoría", channels[1].group)
        assertNull(channels[1].tvgId)
        assertNull(channels[1].logoUrl)
    }

    @Test
    fun `honours direct sources and skips duplicate or incomplete streams`() {
        val channels = XtreamJson.parseLiveStreams(
            value = JSONArray(
                """[
                    {
                        "stream_id":201,
                        "name":"Directo",
                        "direct_source":"https://cdn.example/live.m3u8",
                        "category_id":"1"
                    },
                    {
                        "stream_id":201,
                        "name":"Duplicado",
                        "category_id":"1"
                    },
                    {"name":"Sin identificador"},
                    {
                        "stream_id":202,
                        "name":"Construido",
                        "direct_source":"null",
                        "category_id":"1"
                    }
                ]""",
            ),
            categories = mapOf("1" to "Cine"),
            credentials = credentials,
            base = base,
        )

        assertEquals(2, channels.size)
        assertEquals("https://cdn.example/live.m3u8", channels[0].streamUrl)
        assertEquals(
            "http://demo.example:8080/live/user%20name/p@ss/202.m3u8",
            channels[1].streamUrl,
        )
    }

    @Test
    fun `normalizes movies and series with fallbacks`() {
        val movies = XtreamJson.parseMovies(
            value = JSONArray(
                """[
                    {
                        "stream_id":55,
                        "name":"Película",
                        "container_extension":"mkv",
                        "category_id":"2",
                        "rating":"7.5",
                        "year":"2024",
                        "plot":"Sinopsis",
                        "stream_icon":"https://img.example/movie.png"
                    }
                ]""",
            ),
            categories = mapOf("2" to "Cine"),
            credentials = credentials,
            base = base,
        )
        val movie = movies.single()
        assertEquals("55", movie.sourceId)
        assertEquals("Cine", movie.group)
        assertEquals("mkv", movie.containerExtension)
        assertEquals("7.5", movie.rating)
        assertEquals("2024", movie.year)
        assertEquals(
            "http://demo.example:8080/movie/user%20name/p@ss/55.mkv",
            movie.streamUrl,
        )
        assertTrue(movie.id.startsWith("mv-"))

        val series = XtreamJson.parseSeries(
            value = JSONArray(
                """[
                    {
                        "series_id":77,
                        "name":"Serie",
                        "cover":"https://img.example/series.png",
                        "category_id":"3",
                        "rating":"8",
                        "year":"2023",
                        "plot":"Sinopsis"
                    }
                ]""",
            ),
            categories = mapOf("3" to "Series"),
            credentials = credentials,
            base = base,
        ).single()
        assertEquals("77", series.sourceId)
        assertEquals("Series", series.group)
        assertEquals("https://img.example/series.png", series.coverUrl)
        assertTrue(series.id.startsWith("sr-"))
    }

    @Test
    fun `tolerates missing or incomplete sections`() {
        assertTrue(XtreamJson.parseCategories(JSONObject("{}")).isEmpty())
        assertTrue(XtreamJson.parseCategories(null).isEmpty())
        assertTrue(
            XtreamJson.parseLiveStreams(
                value = JSONObject("""{"error":"no disponible"}"""),
                categories = emptyMap(),
                credentials = credentials,
                base = base,
            ).isEmpty(),
        )
        assertTrue(
            XtreamJson.parseMovies(
                value = JSONArray("""[{"name":"sin id"}]"""),
                categories = emptyMap(),
                credentials = credentials,
                base = base,
            ).isEmpty(),
        )
        assertTrue(
            XtreamJson.parseSeries(
                value = JSONArray(),
                categories = emptyMap(),
                credentials = credentials,
                base = base,
            ).isEmpty(),
        )
    }

    @Test
    fun `ids are stable across parses and unique per stream`() {
        val json = JSONArray(
            """[
                {"stream_id":1,"name":"Uno","category_id":"1"},
                {"stream_id":2,"name":"Dos","category_id":"1"}
            ]""",
        )
        val first = XtreamJson.parseLiveStreams(json, mapOf("1" to "Cine"), credentials, base)
        val second = XtreamJson.parseLiveStreams(json, mapOf("1" to "Cine"), credentials, base)

        assertEquals(first.map { it.id }, second.map { it.id })
        assertTrue(first[0].id != first[1].id)
    }

    @Test
    fun `catalog cache round trips normalized content`() {
        val catalog = XtreamCatalog(
            channels = listOf(
                Channel(
                    id = "ch-1",
                    name = "Cine",
                    streamUrl = "http://demo/live/user/pass/1.m3u8",
                    group = "Cine",
                    logoUrl = "https://img/1.png",
                    tvgId = "cine.1",
                    number = 1,
                ),
            ),
            movies = listOf(
                VodItem(
                    id = "mv-1",
                    sourceId = "55",
                    name = "Película",
                    streamUrl = "http://demo/movie/user/pass/55.mp4",
                    group = "Cine",
                    rating = "7",
                    year = "2024",
                    plot = "Sinopsis",
                    containerExtension = "mp4",
                ),
            ),
            series = listOf(
                SeriesItem(
                    id = "sr-1",
                    sourceId = "77",
                    name = "Serie",
                    group = "Series",
                    coverUrl = "https://img/series.png",
                    rating = "8",
                    year = "2023",
                    plot = "Sinopsis",
                ),
            ),
        )

        val decoded = XtreamCatalogCache.decode(XtreamCatalogCache.encode(catalog))

        assertEquals(catalog, decoded)
        assertNotNull(decoded.channels.single().number)
    }

    @Test
    fun `catalog cache rejects empty or invalid content`() {
        assertThrows(XtreamException::class.java) {
            XtreamCatalogCache.decode("no es json")
        }
        assertThrows(XtreamException::class.java) {
            XtreamCatalogCache.decode("""{"version":1,"channels":[],"movies":[],"series":[]}""")
        }
    }

    @Test
    fun `parses series info episodes ordered by season and number`() {
        val root = JSONObject(
            """
            {"info":{"plot":"Trama"},
             "episodes":{
               "2":[{"id":"30","episode_num":1,"title":"Dos uno","container_extension":"mkv",
                     "info":{"plot":"P","duration":"45:00"}}],
               "1":[{"id":"11","episode_num":2,"title":"Uno dos"},
                    {"id":"10","episode_num":"1","title":"Uno uno","container_extension":"mp4"},
                    {"id":"10","episode_num":1,"title":"Duplicado"},
                    {"title":"Sin id"}]
             }}
            """.trimIndent(),
        )
        val detail = XtreamJson.parseSeriesInfo(root, credentials, base)
        assertEquals("Trama", detail.plot)
        assertEquals(listOf("10", "11", "30"), detail.episodes.map { it.id })
        assertEquals(listOf(1, 1, 2), detail.episodes.map { it.season })
        assertEquals("Uno uno", detail.episodes[0].title)
        assertTrue(detail.episodes[2].streamUrl.endsWith("/series/user%20name/p@ss/30.mkv"))
        assertEquals("45:00", detail.episodes[2].duration)
    }

    @Test
    fun `parses series info with episodes as nested arrays`() {
        val root = JSONObject(
            """{"episodes":[[{"id":"1","episode_num":1,"title":"A"}],[{"id":"2","episode_num":1,"title":"B"}]]}""",
        )
        val detail = XtreamJson.parseSeriesInfo(root, credentials, base)
        assertEquals(listOf(1, 2), detail.episodes.map { it.season })
    }}

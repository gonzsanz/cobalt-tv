package io.github.gonzsanz.cobalttv.ui

import androidx.compose.runtime.staticCompositionLocalOf
import io.github.gonzsanz.cobalttv.data.catalog.CatalogRepository

/** Provided once by [CobaltTvApp] so screens can observe the imported list. */
val LocalCatalog = staticCompositionLocalOf<CatalogRepository> {
    error("CatalogRepository no está disponible")
}

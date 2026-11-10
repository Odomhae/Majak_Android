package com.odom.mahjongfun.data.level

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** assets에서 배치와 단계 매핑을 읽어 [LevelCatalog]를 만든다. 스플래시에서 미리 불러 두는 용도. */
@Singleton
class AssetLevelRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val mutex = Mutex()
    private var cached: LevelCatalog? = null

    suspend fun catalog(): LevelCatalog = mutex.withLock {
        cached ?: withContext(Dispatchers.IO) { load() }.also { cached = it }
    }

    private fun load(): LevelCatalog {
        val assets = context.assets
        val layouts = assets.list(LAYOUT_DIR).orEmpty()
            .filter { it.endsWith(".json") }
            .sorted()
            .map { name -> LevelJson.parseLayout(read("$LAYOUT_DIR/$name")) }
        return LevelCatalog(layouts, LevelJson.parseStages(read(LEVELS_FILE)))
    }

    private fun read(path: String): String =
        context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }

    private companion object {
        const val LAYOUT_DIR = "layouts"
        const val LEVELS_FILE = "levels.json"
    }
}

package com.odom.mahjongfun

import com.odom.mahjongfun.core.model.Layout
import com.odom.mahjongfun.data.level.LevelCatalog
import com.odom.mahjongfun.data.level.LevelJson
import java.io.File

/** JVM 단위 테스트에서 앱 assets를 파일로 직접 읽는다. (Gradle 테스트의 작업 디렉터리는 app 모듈) */
object TestAssets {
    private val root = File("src/main/assets")

    val layoutFiles: List<File>
        get() = File(root, "layouts").listFiles { f -> f.extension == "json" }.orEmpty().sortedBy { it.name }

    val layouts: List<Layout> by lazy { layoutFiles.map { LevelJson.parseLayout(it.readText(Charsets.UTF_8)) } }

    val stages: Map<Int, String> by lazy { LevelJson.parseStages(File(root, "levels.json").readText(Charsets.UTF_8)) }

    val catalog: LevelCatalog by lazy { LevelCatalog(layouts, stages) }
}

package com.github.quillraven.quillycrawler.ashley.system

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.github.quillraven.commons.ashley.component.RemoveComponent
import com.github.quillraven.commons.map.MapService
import com.github.quillraven.quillycrawler.ashley.component.GoToNextLevelComponent
import com.github.quillraven.quillycrawler.ashley.component.PlayerComponent
import com.github.quillraven.quillycrawler.ashley.component.playerCmp
import com.github.quillraven.quillycrawler.event.GameEventDispatcher
import com.github.quillraven.quillycrawler.event.MapChangeEvent
import ktx.ashley.allOf
import ktx.ashley.exclude
import ktx.log.debug
import ktx.log.error
import ktx.log.logger

class MapSystem(
  private val mapService: MapService,
  private val gameEventDispatcher: GameEventDispatcher
) : IteratingSystem(
  allOf(PlayerComponent::class, GoToNextLevelComponent::class).exclude(RemoveComponent::class).get()
) {
  private var currentMapFolder: FileHandle = FileHandle("")

  override fun addedToEngine(engine: Engine) {
    super.addedToEngine(engine)
    mapService.setMap(engine, "maps/tutorial.tmx")
  }

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val playerCmp = entity.playerCmp
    playerCmp.dungeonLevel++
    LOG.debug { "Moving to dungeon level ${playerCmp.dungeonLevel}" }

    val nextMapFilePath = nextMap(playerCmp.dungeonLevel)
    if (nextMapFilePath.isNotBlank()) {
      mapService.setMap(engine, nextMapFilePath)
    }
    gameEventDispatcher.dispatchEvent<MapChangeEvent> {
      this.entity = entity
      this.level = playerCmp.dungeonLevel
    }

    entity.remove(GoToNextLevelComponent::class.java)
  }

  private fun nextMap(dungeonLevel: Int): String {
    val folderPath = "maps/level_${dungeonLevel}"
    val mapFolder = Gdx.files.internal(folderPath)
    if (!mapFolder.exists()) {
      LOG.debug { "Map folder '$folderPath' does not exist" }
      if (currentMapFolder.path().isBlank()) {
        return ""
      }
    } else {
      LOG.debug { "Switch map folder to '${mapFolder.path()}'" }
      currentMapFolder = mapFolder
    }

    val mapFiles = currentMapFolder.list(".tmx")
    if (mapFiles.isEmpty()) {
      LOG.error { "Map folder '${currentMapFolder.path()}' has no .tmx files" }
      return ""
    }

    return mapFiles.random().path()
  }

  companion object {
    private val LOG = logger<MapSystem>()
  }
}
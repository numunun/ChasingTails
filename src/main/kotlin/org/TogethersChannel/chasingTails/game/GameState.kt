package org.TogethersChannel.chasingTails.game

enum class GameState {
    WAITING, // 대기 중
    PREPARING, // 게임 시작 대기 중
    GRACE_PERIOD,
    RUNNING, // 게임 진행 중
    ENDED    // 게임 종료
}
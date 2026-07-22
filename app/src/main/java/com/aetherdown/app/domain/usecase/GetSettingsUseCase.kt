package com.aetherdown.app.domain.usecase

import com.aetherdown.app.domain.model.AppSettings
import com.aetherdown.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    fun getSettings(): Flow<AppSettings> = settingsRepository.settings

    suspend fun getSettingsOnce(): AppSettings = settingsRepository.getSettingsOnce()
}

package com.aetherdown.app.domain.usecase

import com.aetherdown.app.domain.model.AppSettings
import com.aetherdown.app.domain.repository.SettingsRepository
import javax.inject.Inject

class UpdateSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(settings: AppSettings) {
        settingsRepository.updateSettings(settings)
    }
}

package com.customapplock.studio.ui.setup

import androidx.lifecycle.ViewModel
import com.customapplock.studio.data.LockRepository
import com.customapplock.studio.domain.LockType
import com.customapplock.studio.domain.MASTER_PKG
import com.customapplock.studio.service.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val repo: LockRepository,
    private val session: SessionManager
) : ViewModel() {
    suspend fun save(pkgs: List<String>, type: LockType, secret: String) {
        pkgs.forEach { repo.saveLock(it, type, secret) }
        if (MASTER_PKG in pkgs) session.setMasterUnlocked()
    }
}

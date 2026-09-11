package com.tradeflow.core

import android.app.Application
import com.tradeflow.core.data.Repo
import com.tradeflow.core.data.TradeFlowDb

/** App holder: single DB + Repo for UI, services and receivers. Declared in manifest. */
class TradeFlowApp : Application() {
    val db by lazy { TradeFlowDb.get(this) }
    val repo by lazy { Repo(db) }
}

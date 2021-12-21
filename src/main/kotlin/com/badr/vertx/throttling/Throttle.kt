package com.badr.vertx.throttling

import com.badr.cache.storage.Storage
import io.vertx.core.Vertx

interface Throttle {
  companion object {
    fun getThrottling(vertx: Vertx, ips: List<String>? = null, storage: Storage): ThrottleImpl {
      return ThrottleImpl(vertx, ips, storage)
    }
  }
}

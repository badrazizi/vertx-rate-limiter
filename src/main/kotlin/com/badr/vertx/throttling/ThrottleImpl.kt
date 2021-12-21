package com.badr.vertx.throttling

import com.badr.cache.storage.Storage
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import java.util.concurrent.TimeUnit

class ThrottleImpl(private val vertx: Vertx, ips: List<String>? = null, private val storage: Storage) {
  private lateinit var router: Router

  var includeHeaders: Boolean = true
    private set
  var throttlingRequest: Int = 30
    private set
  var throttlingTime: Long = 60
    private set
  var throttlingTimeUnit: TimeUnit = TimeUnit.SECONDS
    private set

  var originalIPFrom: List<String> = ips ?: arrayListOf(
    "173.245.48.0/20",
    "103.21.244.0/22",
    "103.22.200.0/22",
    "103.31.4.0/22",
    "141.101.64.0/18",
    "108.162.192.0/18",
    "190.93.240.0/20",
    "188.114.96.0/20",
    "197.234.240.0/22",
    "198.41.128.0/17",
    "162.158.0.0/15",
    "104.16.0.0/13",
    "104.24.0.0/14",
    "172.64.0.0/13",
    "131.0.72.0/22"
  )
    private set

  var ipHeaders: List<String> = arrayListOf(
    "CF-Connecting-IP",
    "True-Client-IP"
  )
    private set

  private val cidrs: MutableList<CIDRUtils> = arrayListOf()

  fun ipHeaders(headers: List<String>) = apply {
    this.ipHeaders = headers
  }

  fun includeHeaders(enabled: Boolean = true) = apply {
    this.includeHeaders = enabled
  }

  fun throttlingRequest(count: Int = 30) = apply {
    this.throttlingRequest = count
  }

  fun throttlingTime(time: Long = 60) = apply {
    this.throttlingTime = time
  }

  fun throttlingTimeUnit(timeUnit: TimeUnit = TimeUnit.SECONDS) = apply {
    this.throttlingTimeUnit = timeUnit
  }

  fun getRouter(): Router {
    this.router = Router.router(vertx)
    return getRouter(router)
  }

  fun getRouter(router: Router): Router {
    for (ip in originalIPFrom) {
      if (ip.contains("/")) {
        cidrs.add(CIDRUtils(ip))
      }
    }
    router.route().handler(throttlingHandler)

    this.router = router
    return router
  }

  private val throttlingHandler = Handler<RoutingContext> { ctx ->
    val host = ctx.request().remoteAddress().host()
    val port = ctx.request().remoteAddress().port()
    val headers = ctx.request().headers()

    var ip = ""
    if (host in originalIPFrom || cidrs.any { it.isInRange(host) }) {
      if (headers.isEmpty || ipHeaders.isEmpty()) {
        ctx.put("ip", ip)
        ctx.next()
        ctx.request().resume()
        return@Handler
      }

      for (ipHeader in ipHeaders) {
        val foundHeader = headers.names().filter { h -> h.equals(ipHeader, true) }
        if (foundHeader.isEmpty()) {
          continue
        } else {
          ip = headers[foundHeader[0]]
          break
        }
      }
    } else {
      ip = host
    }

    if (ip.isBlank()) {
      ctx.put("ip", host)
      ctx.next()
      ctx.request().resume()
      return@Handler
    }

    ctx.put("ip", ip)

    ctx.request().pause()
    storage.get<ThrottlingClients>(ip).onComplete { tcAR ->
      if (tcAR.succeeded()) {
        val found = tcAR.result()
        if (found.throttl >= throttlingRequest) {
          if (includeHeaders) {
            ctx.response().putHeader("RATE_LIMIT_MAX", throttlingRequest.toString())
            ctx.response().putHeader("RATE_LIMIT_COUNT", "${found.throttl}")
            ctx.response().putHeader("RATE_LIMIT_WAIT_TIME", "${throttlingTimeUnit.toSeconds(throttlingTime)}s")
            ctx.response().putHeader("RATE_LIMIT_IP", ip)
          }

          ctx.response().setStatusCode(429).end()
          ctx.request().resume()
          return@onComplete
        }

        found.throttl += 1

        if (includeHeaders) {
          ctx.response().putHeader("RATE_LIMIT_MAX", throttlingRequest.toString())
          ctx.response().putHeader("RATE_LIMIT_COUNT", "${found.throttl}")
          ctx.response().putHeader("RATE_LIMIT_IP", ip)
        }

        ctx.next()
        ctx.request().resume()
      } else {
        storage.add(ip, ThrottlingClients().apply {
          this.ip = ip
          this.port = port
          this.throttl = 1
        }, throttlingTime, throttlingTimeUnit)

        if (includeHeaders) {
          ctx.response().putHeader("RATE_LIMIT_MAX", throttlingRequest.toString())
          ctx.response().putHeader("RATE_LIMIT_COUNT", "1")
          ctx.response().putHeader("RATE_LIMIT_IP", ip)
        }

        ctx.next()
        ctx.request().resume()
      }
    }
  }
}

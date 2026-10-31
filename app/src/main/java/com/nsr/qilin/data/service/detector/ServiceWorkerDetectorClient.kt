package com.nsr.qilin.data.service.detector

import android.webkit.ServiceWorkerClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse

/**
 * ServiceWorkerClient that monitors ServiceWorker background requests for media manifests and streams.
 * Ported from super-video-downloader.
 */
class ServiceWorkerDetectorClient(
    private val detector: WebsiteVideoDetector
) : ServiceWorkerClient() {

    override fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? {
        val url = request.url?.toString()
        if (url != null) {
            val headers = request.requestHeaders ?: emptyMap()
            detector.inspectUrlAsync(url, headers)
        }
        return super.shouldInterceptRequest(request)
    }
}

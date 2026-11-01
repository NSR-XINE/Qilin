package com.nsr.qilin.data.service.detector

import android.webkit.JavascriptInterface

/**
 * JavaScript interface that bridges in-page HTML5 video events, play() triggers,
 * and media fetch/XHR network requests into the Android WebsiteVideoDetector.
 * Ported from super-video-downloader.
 */
class WebMediaDetectionBridge(
    private val onMediaDetected: (url: String, mimeType: String, title: String) -> Unit
) {
    companion object {
        const val BRIDGE_NAME = "QilinVideoDetector"

        val INJECT_JS = """
            (function() {
                if (window.__qilin_detector_injected__) return;
                window.__qilin_detector_injected__ = true;

                function isSegment(u) {
                    if (!u || typeof u !== 'string') return false;
                    const clean = u.split('?')[0].toLowerCase();
                    return clean.endsWith('.ts') || clean.endsWith('.m4s') || clean.endsWith('.key') ||
                           clean.endsWith('.vtt') || clean.endsWith('.srt') || clean.includes('/segment');
                }

                function notifyMedia(url, mimeType, title) {
                    if (!url || typeof url !== 'string') return;
                    if (url.startsWith('blob:') && !url.includes('http')) return;
                    if (isSegment(url)) return;
                    try {
                        if (window.QilinVideoDetector && window.QilinVideoDetector.onMediaDetected) {
                            window.QilinVideoDetector.onMediaDetected(url, mimeType || '', title || document.title || '');
                        }
                    } catch(e) {}
                }

                // 1. Hook HTMLMediaElement.prototype.play
                try {
                    const origPlay = HTMLMediaElement.prototype.play;
                    HTMLMediaElement.prototype.play = function() {
                        const src = this.currentSrc || this.src;
                        if (src) {
                            notifyMedia(src, this.type, document.title);
                        }
                        return origPlay.apply(this, arguments);
                    };
                } catch(e) {}

                // 2. Hook HTMLMediaElement.prototype.src
                try {
                    const origSrcDesc = Object.getOwnPropertyDescriptor(HTMLMediaElement.prototype, 'src');
                    if (origSrcDesc && origSrcDesc.set) {
                        Object.defineProperty(HTMLMediaElement.prototype, 'src', {
                            set: function(val) {
                                if (val) notifyMedia(val, this.type, document.title);
                                return origSrcDesc.set.apply(this, arguments);
                            },
                            get: origSrcDesc.get
                        });
                    }
                } catch(e) {}

                // 3. Hook HTMLSourceElement.prototype.src
                try {
                    const origSourceSrc = Object.getOwnPropertyDescriptor(HTMLSourceElement.prototype, 'src');
                    if (origSourceSrc && origSourceSrc.set) {
                        Object.defineProperty(HTMLSourceElement.prototype, 'src', {
                            set: function(val) {
                                if (val) notifyMedia(val, this.type, document.title);
                                return origSourceSrc.set.apply(this, arguments);
                            },
                            get: origSourceSrc.get
                        });
                    }
                } catch(e) {}

                // 4. Hook XMLHttpRequest for media URLs
                try {
                    const origXhrOpen = XMLHttpRequest.prototype.open;
                    XMLHttpRequest.prototype.open = function(method, url) {
                        if (typeof url === 'string' && !isSegment(url)) {
                            const lower = url.toLowerCase();
                            if (lower.includes('.m3u8') || lower.includes('.mpd') || lower.includes('.mp4') || lower.includes('manifest')) {
                                notifyMedia(url, '', document.title);
                            }
                        }
                        return origXhrOpen.apply(this, arguments);
                    };
                } catch(e) {}

                // 5. Hook window.fetch for media URLs
                try {
                    const origFetch = window.fetch;
                    window.fetch = function(input, init) {
                        const url = typeof input === 'string' ? input : (input && input.url ? input.url : '');
                        if (url && !isSegment(url)) {
                            const lower = url.toLowerCase();
                            if (lower.includes('.m3u8') || lower.includes('.mpd') || lower.includes('.mp4') || lower.includes('manifest')) {
                                notifyMedia(url, '', document.title);
                            }
                        }
                        return origFetch.apply(this, arguments);
                    };
                } catch(e) {}

                // 6. Scan existing DOM media elements
                function scanMediaElements() {
                    try {
                        const media = document.querySelectorAll('video, audio, source');
                        for (let i = 0; i < media.length; i++) {
                            const el = media[i];
                            const src = el.currentSrc || el.src;
                            if (src) {
                                notifyMedia(src, el.type, document.title);
                            }
                        }
                    } catch(e) {}
                }

                if (document.readyState === 'complete' || document.readyState === 'interactive') {
                    scanMediaElements();
                } else {
                    document.addEventListener('DOMContentLoaded', scanMediaElements);
                }
                window.addEventListener('load', scanMediaElements);

                // 7. Observe DOM mutations for dynamically inserted video elements
                try {
                    const observer = new MutationObserver(function(mutations) {
                        scanMediaElements();
                    });
                    observer.observe(document.documentElement || document.body, { childList: true, subtree: true });
                } catch(e) {}
            })();
        """.trimIndent()
    }

    @JavascriptInterface
    fun onMediaDetected(url: String?, mimeType: String?, title: String?) {
        if (!url.isNullOrBlank()) {
            onMediaDetected.invoke(url, mimeType.orEmpty(), title.orEmpty())
        }
    }
}

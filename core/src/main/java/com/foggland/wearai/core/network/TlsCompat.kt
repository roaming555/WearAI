package com.foggland.wearai.core.network

import java.io.InputStream
import java.security.KeyStore
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * 老系统（Android 5.x / 6.x）的 TLS 兼容层。
 *
 * Android 5.x 的内置根证书库停留在 2014 年左右，缺少 USERTrust RSA CA、
 * Sectigo Public Server Authentication Root R46、ISRG Root X1 等较新的公共根证书。
 * 访问使用这些根签发的服务（例如智谱开放平台 open.bigmodel.cn）时，
 * 系统会抛出 `Trust anchor for certification path not found` 而请求失败。
 *
 * 处理方式：把补充的公共根证书随 App 一起打包，与系统信任库**合并**使用。
 * 这里只做「额外增加信任」，不会关闭证书校验，也不会信任任意自签名证书。
 */
object TlsCompat {

    /** 随包内置的补充 CA 证书（PEM），位于 src/main/resources。 */
    private val BUNDLED_CA_FILES = listOf(
        "ca_usertrust_rsa.pem",
        "ca_sectigo_r46.pem",
    )

    /** 系统信任库与内置根证书合并后的 TrustManager。 */
    val trustManager: X509TrustManager by lazy { buildTrustManager() }

    /** 与 [trustManager] 配套的 SSLSocketFactory。 */
    val socketFactory: SSLSocketFactory by lazy {
        SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(trustManager), null)
        }.socketFactory
    }

    private fun buildTrustManager(): X509TrustManager {
        val systemTm = systemTrustManager() ?: error("无法获取系统 TrustManager")
        val bundledTm = bundledTrustManager() ?: return systemTm
        return CompositeTrustManager(systemTm, bundledTm)
    }

    private fun systemTrustManager(): X509TrustManager? = try {
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(null as KeyStore?)
        tmf.trustManagers.filterIsInstance<X509TrustManager>().firstOrNull()
    } catch (e: Exception) {
        null
    }

    private fun bundledTrustManager(): X509TrustManager? {
        return try {
            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply { load(null, null) }
            val factory = CertificateFactory.getInstance("X.509")
            var loaded = 0
            for (name in BUNDLED_CA_FILES) {
                val stream: InputStream? = TlsCompat::class.java.classLoader?.getResourceAsStream(name)
                if (stream != null) {
                    stream.use { input ->
                        factory.generateCertificates(input).forEachIndexed { index, certificate ->
                            keyStore.setCertificateEntry("$name-$index", certificate)
                            loaded++
                        }
                    }
                }
            }
            if (loaded == 0) return null
            val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            tmf.init(keyStore)
            tmf.trustManagers.filterIsInstance<X509TrustManager>().firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    /** 先按系统信任库校验，失败再尝试内置根证书。 */
    private class CompositeTrustManager(
        private val system: X509TrustManager,
        private val bundled: X509TrustManager,
    ) : X509TrustManager {

        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
            try {
                system.checkClientTrusted(chain, authType)
            } catch (systemError: CertificateException) {
                bundled.checkClientTrusted(chain, authType)
            }
        }

        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
            try {
                system.checkServerTrusted(chain, authType)
            } catch (systemError: CertificateException) {
                try {
                    bundled.checkServerTrusted(chain, authType)
                } catch (bundledError: CertificateException) {
                    // 两套信任库都无法验证时，抛出系统错误以保留原始诊断信息
                    throw systemError
                }
            }
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> =
            system.acceptedIssuers + bundled.acceptedIssuers
    }
}

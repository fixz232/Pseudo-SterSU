package me.weishu.kernelsu.ui.util

import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.Security
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

private const val ED25519_ALGORITHM = "Ed25519"

/**
 * Verifies store catalogs without relying on the platform's default provider
 * order. Some vendor ROMs expose Ed25519 through AndroidKeyStore first, whose
 * KeyFactory rejects ordinary X.509 public keys and asks callers to generate a
 * Keystore key pair instead.
 */
internal fun verifyCatalogEd25519Signature(
    payloads: Iterable<ByteArray>,
    signatureBytes: ByteArray,
    publicKeyBytes: ByteArray,
): Boolean {
    val providers = Security.getProviders().filter { isUsableCatalogCryptoProvider(it.name) }
    var lastFailure: Exception? = null
    var verificationAttempted = false

    for (keyProvider in providers) {
        val publicKey = try {
            KeyFactory.getInstance(ED25519_ALGORITHM, keyProvider)
                .generatePublic(X509EncodedKeySpec(publicKeyBytes))
        } catch (error: Exception) {
            lastFailure = error
            continue
        }

        for (signatureProvider in providers) {
            for (payload in payloads) {
                try {
                    val verifier = Signature.getInstance(ED25519_ALGORITHM, signatureProvider)
                    verifier.initVerify(publicKey)
                    verifier.update(payload)
                    verificationAttempted = true
                    if (verifier.verify(signatureBytes)) return true
                } catch (error: Exception) {
                    lastFailure = error
                }
            }
        }
    }

    if (verificationAttempted) return false
    throw NoSuchAlgorithmException("No usable Ed25519 provider is available", lastFailure)
}

internal fun isUsableCatalogCryptoProvider(providerName: String): Boolean =
    !providerName.contains("AndroidKeyStore", ignoreCase = true)

package me.weishu.kernelsu.ui.util

import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.Security
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.math.BigInteger
import java.security.MessageDigest

private val ED25519_ALGORITHMS = arrayOf("Ed25519", "EdDSA")

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
        for (algorithm in ED25519_ALGORITHMS) {
            val publicKey = try {
                KeyFactory.getInstance(algorithm, keyProvider)
                    .generatePublic(X509EncodedKeySpec(publicKeyBytes))
            } catch (error: Exception) {
                lastFailure = error
                continue
            }

            for (signatureProvider in providers) {
                for (payload in payloads) {
                    try {
                        val verifier = Signature.getInstance(algorithm, signatureProvider)
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
    }

    // A few vendor Android builds ship only AndroidKeyStore for Ed25519. That
    // provider intentionally rejects ordinary X.509 public keys. Keep the
    // catalog signed on those devices by using the small, data-only fallback
    // below instead of disabling verification or accepting unsigned content.
    val fallbackResult = runCatching {
        payloads.any { payload ->
            PureJavaEd25519.verify(signatureBytes, publicKeyBytes, payload)
        }
    }.getOrNull()
    if (fallbackResult == true) return true
    if (verificationAttempted || fallbackResult == false) return false
    throw NoSuchAlgorithmException("No usable Ed25519 provider is available", lastFailure)
}

internal fun isUsableCatalogCryptoProvider(providerName: String): Boolean =
    !providerName.contains("AndroidKeyStore", ignoreCase = true)

/** Minimal RFC 8032 Ed25519 verifier used only when Android exposes no usable provider. */
internal object PureJavaEd25519 {
    private val P = BigInteger.ONE.shiftLeft(255).subtract(BigInteger.valueOf(19))
    private val Q = BigInteger.ONE.shiftLeft(252).add(BigInteger("27742317777372353535851937790883648493"))
    private val D = BigInteger("-121665").multiply(BigInteger("121666").modInverse(P)).mod(P)
    private val I = BigInteger.valueOf(2).modPow(P.subtract(BigInteger.ONE).divide(BigInteger.valueOf(4)), P)
    private val TWO = BigInteger.valueOf(2)
    private val ZERO = BigInteger.ZERO
    private val ONE = BigInteger.ONE
    private val BY = BigInteger.valueOf(4).multiply(BigInteger.valueOf(5).modInverse(P)).mod(P)
    private val BX = requireNotNull(recoverX(BY, false))
    private val BASE = Point(BX, BY, ONE, BX.multiply(BY).mod(P))

    fun verify(signature: ByteArray, publicKey: ByteArray, message: ByteArray): Boolean {
        if (signature.size != 64) return false
        val key = publicKey.copyOfRange(publicKey.size - 32, publicKey.size)
        val a = decodePoint(key) ?: return false
        val r = decodePoint(signature.copyOfRange(0, 32)) ?: return false
        val s = fromLittleEndian(signature, 32, 32)
        if (s >= Q) return false
        val digest = MessageDigest.getInstance("SHA-512").digest(
            signature.copyOfRange(0, 32) + key + message,
        )
        val h = fromLittleEndian(digest).mod(Q)
        val left = scalarMultiply(BASE, s)
        val right = add(r, scalarMultiply(a, h))
        return encode(left).contentEquals(encode(right))
    }

    private data class Point(val x: BigInteger, val y: BigInteger, val z: BigInteger, val t: BigInteger)

    private fun add(p: Point, q: Point): Point {
        val a = mod(p.y.subtract(p.x).multiply(q.y.subtract(q.x)))
        val b = mod(p.y.add(p.x).multiply(q.y.add(q.x)))
        val c = mod(p.t.multiply(TWO).multiply(D).multiply(q.t))
        val d = mod(p.z.multiply(TWO).multiply(q.z))
        val e = mod(b.subtract(a))
        val f = mod(d.subtract(c))
        val g = mod(d.add(c))
        val h = mod(b.add(a))
        return Point(mod(e.multiply(f)), mod(g.multiply(h)), mod(f.multiply(g)), mod(e.multiply(h)))
    }

    private fun double(p: Point): Point {
        val a = mod(p.x.multiply(p.x))
        val b = mod(p.y.multiply(p.y))
        val c = mod(TWO.multiply(p.z).multiply(p.z))
        val d = mod(a.negate())
        val e = mod(p.x.add(p.y).multiply(p.x.add(p.y)).subtract(a).subtract(b))
        val g = mod(d.add(b))
        val f = mod(g.subtract(c))
        val h = mod(d.subtract(b))
        return Point(mod(e.multiply(f)), mod(g.multiply(h)), mod(f.multiply(g)), mod(e.multiply(h)))
    }

    private fun scalarMultiply(point: Point, scalar: BigInteger): Point {
        var result = Point(ZERO, ONE, ONE, ZERO)
        var addend = point
        var k = scalar
        while (k.signum() > 0) {
            if (k.testBit(0)) result = add(result, addend)
            addend = double(addend)
            k = k.shiftRight(1)
        }
        return result
    }

    private fun decodePoint(encoded: ByteArray): Point? {
        if (encoded.size != 32) return null
        val sign = (encoded[31].toInt() ushr 7) and 1
        val yBytes = encoded.copyOf()
        yBytes[31] = (yBytes[31].toInt() and 0x7f).toByte()
        val y = fromLittleEndian(yBytes)
        if (y >= P) return null
        val x = recoverX(y, sign == 1)
        return x?.let { Point(it, y, ONE, it.multiply(y).mod(P)) }
    }

    private fun recoverX(y: BigInteger, sign: Boolean): BigInteger? {
        val y2 = y.multiply(y).mod(P)
        val numerator = y2.subtract(ONE).mod(P)
        val denominator = D.multiply(y2).add(ONE).mod(P)
        val x2 = numerator.multiply(denominator.modInverse(P)).mod(P)
        var x = x2.modPow(P.add(BigInteger.valueOf(3)).divide(BigInteger.valueOf(8)), P)
        if (x.multiply(x).subtract(x2).mod(P) != ZERO) x = x.multiply(I).mod(P)
        if (x.multiply(x).subtract(x2).mod(P) != ZERO) return null
        if ((x.testBit(0)) != sign) x = P.subtract(x)
        if (x == ZERO && sign) return null
        return x
    }

    private fun encode(point: Point): ByteArray {
        val zInv = point.z.modInverse(P)
        val x = point.x.multiply(zInv).mod(P)
        val y = point.y.multiply(zInv).mod(P)
        val encoded = toLittleEndian(y, 32)
        encoded[31] = (encoded[31].toInt() or (if (x.testBit(0)) 0x80 else 0)).toByte()
        return encoded
    }

    private fun mod(value: BigInteger): BigInteger = value.mod(P)

    private fun fromLittleEndian(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size - offset): BigInteger {
        val slice = bytes.copyOfRange(offset, offset + length)
        slice.reverse()
        return BigInteger(1, slice)
    }

    private fun toLittleEndian(value: BigInteger, length: Int): ByteArray {
        val source = value.toByteArray()
        val output = ByteArray(length)
        for (index in 0 until length) {
            val sourceIndex = source.size - 1 - index
            if (sourceIndex >= 0) output[index] = source[sourceIndex]
        }
        return output
    }
}

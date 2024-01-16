package Utility

import java.util.*


object RandomUtils {
    private var seed: Long = 123
    private val random = Random(seed)
    fun setSeed(seed: Long) {
        RandomUtils.seed = seed
        random.setSeed(seed)
    }

    fun setSeedFromTime() {
        seed = System.currentTimeMillis()
        random.setSeed(seed)
    }

    fun getSeed(): Long {
        return seed
    }

    /**
     * Return the next random, uniformly distributed `double` value between `0.0` (inclusive) and `1.0` (exclusive).
     *
     * @return the next random, uniformly distributed `double` value between `0.0` (inclusive) and `1.0` (exclusive).
     */
    fun nextDouble(): Double {
        return random.nextDouble()
    }

    /**
     * Return the next random, uniformly distributed `int` value between
     * `0` (inclusive) and `upperBound` (exclusive).
     *
     * @return the next random, uniformly distributed `int` value between
     * `0` (inclusive) and `upperBound` (exclusive).
     */
    fun nextInt(upperBound: Int): Int {
        return random.nextInt(upperBound)
    }

    /**
     * Returns the next random, uniformly distributed `int` value between
     * `lowerBound` (inclusive) and `upperBound` (exclusive).
     *
     * @return the next random, uniformly distributed `int` value between
     * `lowerBound` (inclusive) and `upperBound` (exclusive).
     */
    fun nextInt(lowerBound: Int, upperBound: Int): Int {
        return lowerBound + random.nextInt(upperBound - lowerBound)
    }
}


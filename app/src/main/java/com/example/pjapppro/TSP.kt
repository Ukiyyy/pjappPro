package com.example.pjapppro

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.pow
import kotlin.math.sqrt

@OptIn(kotlin.ExperimentalStdlibApi::class)
class TSP(private val context: Context, path: String, maxEvaluations: Int) {
    enum class DistanceType {
        EUCLIDEAN,
        WEIGHTED
    }

    inner class City {
        var index = 0
        var x = 0.0
        var y = 0.0
    }

    inner class Tour {
        var distance: Double
        var dimension: Int
        private var path: Array<City?>

        constructor(tour: Tour) {
            distance = tour.distance
            dimension = tour.dimension
            path = tour.path.clone()
        }

        constructor(dimension: Int) {
            this.dimension = dimension
            path = arrayOfNulls(dimension)
            distance = Double.MAX_VALUE
        }

        fun clone(): Tour {
            return Tour(this)
        }

        fun getPath(): Array<City?> {
            return path
        }

        fun setPath(path: Array<City?>) {
            this.path = path.clone()
        }

        fun setCity(index: Int, city: City?) {
            path[index] = city
            distance = Double.MAX_VALUE
        }
    }

    var name: String? = null
    private var start: City? = null
    private var cities: List<City> = ArrayList()
    private var numberOfCities = 0
    private lateinit var weights: Array<DoubleArray>
    private var distanceType = DistanceType.EUCLIDEAN
    var numberOfEvaluations: Int
    var maxEvaluations: Int

    init {
        loadData(path)
        numberOfEvaluations = 0
        this.maxEvaluations = maxEvaluations
    }
    /*
    private fun printCityCoordinates() {
        println("City Coordinates:")
        cities.forEach { city ->
            println("City ${city.index}: x = ${city.x}, y = ${city.y}")
        }
    }
    */

    fun evaluate(tour: Tour) {
        var distance = 0.0
        for (index in 0..<numberOfCities) {
            distance += if (index + 1 < numberOfCities) calculateDistance(
                tour.getPath()[index],
                tour.getPath()[index + 1]
            ) else calculateDistance(tour.getPath()[index], start)
        }
        tour.distance = distance
        numberOfEvaluations++
        //Log.d("TSP", "Total tour distance: $distance")
    }
    private fun calculateDistance(from: City?, to: City?): Double {
        if (from == null || to == null || from.index == to.index)
            return Double.MAX_VALUE

        return when (distanceType) {
            DistanceType.EUCLIDEAN -> sqrt((to.x - from.x).pow(2) + (to.y - from.y).pow(2))
            DistanceType.WEIGHTED -> weights[from.index][to.index]
            else -> Double.MAX_VALUE
        }
    }
    fun generateTour(): Tour {
        val tour = Tour(numberOfCities)
        val shuffledCities = cities.shuffled()
        tour.setCity(0, start)
        for (i in 0..<numberOfCities) {
            //println(cities[i].x)
            if (cities[i].index == start!!.index)
                continue
            tour.setCity(i, shuffledCities[i])
        }
        //Log.d("TSP", "Generated tour: ${tour.getPath().joinToString { "${it?.index}" }}")
        return tour
    }
    private fun loadData(path: String) {
        val assetManager = context.assets
        try {
            assetManager.open(path).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { br ->
                    var line: String?
                    var edgeWeightType: String? = null
                    var edgeWeightFormat: String? = null

                    while (br.readLine().also { line = it } != null) {
                        //Log.d("TSP", "Read line: $line")
                        when {
                            line!!.startsWith("EDGE_WEIGHT_TYPE") -> edgeWeightType = line!!.split(":")[1].trim()
                            line!!.startsWith("EDGE_WEIGHT_FORMAT") -> edgeWeightFormat = line!!.split(":")[1].trim()
                            line!!.startsWith("DIMENSION") -> numberOfCities = line!!.split(":")[1].trim().toInt()
                            line!!.startsWith("EDGE_WEIGHT_SECTION") || line!!.startsWith("NODE_COORD_SECTION") -> break
                        }
                    }

                    when (edgeWeightType) {
                        "EXPLICIT" -> {
                            parseExplicitWeights(br)
                            distanceType = DistanceType.WEIGHTED
                        }
                        "EUC_2D" -> {
                            parseEuc2dCoords(br)
                            distanceType = DistanceType.EUCLIDEAN
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /*private fun parseExplicitWeights(br: BufferedReader) {
        weights = Array(numberOfCities) { DoubleArray(numberOfCities) }
        for (i in 0..<numberOfCities) {
            val line = br.readLine().trim().split("\\s+".toRegex())
            for (j in 0..<numberOfCities) {
                weights[i][j] = line[j].toDouble()
            }
        }
    }*/
    private fun parseExplicitWeights(br: BufferedReader) {
        weights = Array(numberOfCities) { DoubleArray(numberOfCities) }
        cities = ArrayList() // Ustvari seznam mest

        for (i in 0..<numberOfCities) {
            val line = br.readLine().trim().split("\\s+".toRegex())
            val city = City()
            city.index = i // Nastavite indeks mesta
            (cities as ArrayList<City>).add(city) // Dodajte mesto v seznam

            for (j in 0..<numberOfCities) {
                weights[i][j] = line[j].toDouble()
            }
        }

        start = cities[0] // Nastavite začetno mesto
    }


    private fun parseEuc2dCoords(br: BufferedReader) {
        cities = ArrayList()
        for (i in 0..<numberOfCities) {
            val line = br.readLine().trim().split("\\s+".toRegex())
            val city = City()
            city.index = line[0].toInt() - 1
            city.x = line[1].toDouble()
            city.y = line[2].toDouble()
            //println(city.index)
            (cities as ArrayList<City>).add(city)
            //println(cities[i])
            Log.d("TSP", "Parsed City ${city.index}: x = ${city.x}, y = ${city.y}")
        }
    }

    fun getCities(): List<City> {
        return cities
    }

}

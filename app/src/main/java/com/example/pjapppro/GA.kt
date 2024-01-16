package com.example.pjapppro

import Utility.RandomUtils
import Utility.RandomUtils.nextDouble

@OptIn(kotlin.ExperimentalStdlibApi::class)
class GA(
    private var popSize: Int,
    private var cr: Double,
    private var pm: Double
) {
    private var population: ArrayList<TSP.Tour?>? = null
    private var offspring: ArrayList<TSP.Tour?>? = null
    fun execute(problem: TSP): TSP.Tour? {
        population = ArrayList()
        offspring = ArrayList()
        var best: TSP.Tour? = null

        // Ustvari začetno populacijo in najdi najboljšega posameznika
        for (i in 0..<popSize) {
            val newTour = problem.generateTour()
            problem.evaluate(newTour)
            population!!.add(newTour)
            if (best == null || newTour.distance < best.distance) {
                best = newTour.clone()
            }
        }

        // Glavna zanka genetskega algoritma
        while (problem.numberOfEvaluations < problem.maxEvaluations) {
            // Elitizem - shrani najboljšega v potomce
            offspring!!.add(best!!.clone())

            // Generiranje nove populacije
            while (offspring!!.size < popSize) {
                val parent1 = tournamentSelection()
                var parent2 = tournamentSelection()
                // Preveri, da starša nista enaka
                while (parent1 == parent2) {
                    parent2 = tournamentSelection()
                }
                if (nextDouble() < cr) {
                    val children = pmx(parent1!!, parent2!!, problem)
                    offspring!!.add(children?.get(0))
                    if (offspring!!.size < popSize) offspring!!.add(children?.get(1))
                } else {
                    if (parent1 != null) {
                        offspring!!.add(parent1.clone())
                    }
                    if (offspring!!.size < popSize) if (parent2 != null) {
                        offspring!!.add(parent2.clone())
                    }
                }
            }

            // Izvedi mutacijo in ovrednoti potomce
            for (off in offspring!!) {
                if (nextDouble() < pm) {
                    swapMutation(off)
                }

                problem.evaluate(off!!)
                if (best != null) {
                    if (off.distance < best.distance) {
                        best = off.clone()
                    }
                }
            }

            // Nadomesti staro populacijo z novo
            population = ArrayList(offspring)
            offspring!!.clear()
        }

        return best
    }


    private fun swapMutation(off: TSP.Tour?) {
        val index1 = RandomUtils.nextInt(off!!.getPath().size)
        var index2 = RandomUtils.nextInt(off.getPath().size)
        while (index1 == index2) {
            index2 = RandomUtils.nextInt(off.getPath().size)
        }
        val temp = off.getPath()[index1]
        off.getPath()[index1] = off.getPath()[index2]
        off.getPath()[index2] = temp
        off.distance = Double.MAX_VALUE // Označi, da je treba ponovno izračunati razdaljo

        /*if(off!!.getPath()[100] == null)
            println("error")*/

    }

    private fun pmx(parent1: TSP.Tour, parent2: TSP.Tour, problem: TSP): Array<TSP.Tour> {

        /*if(parent1.getPath()[100] == null || parent2.getPath()[100] == null)
            println("error")*/

        val child1 = problem.Tour(parent1.dimension)
        val child2 = problem.Tour(parent2.dimension)

        // 1. Naključno izberi odsek za izmenjavo
        val crossoverPoint1 = RandomUtils.nextInt(parent1.dimension)
        val crossoverPoint2 = RandomUtils.nextInt(parent1.dimension)
        val start = Math.min(crossoverPoint1, crossoverPoint2)
        val end = Math.max(crossoverPoint1, crossoverPoint2)

        // 2. Kopiraj odsek iz starša v otroka
        for (i in start..end) {
            child1.setCity(i, parent1.getPath()[i])
            child2.setCity(i, parent2.getPath()[i])
        }

        // 3. Izpolni preostale mestne položaje za vsakega otroka
        fillRemainingCities(child1, parent2, start, end)
        fillRemainingCities(child2, parent1, start, end)

        /*if(child1.getPath()[100] == null || child2.getPath()[100] == null)
            println("error")*/

        return arrayOf(child1, child2)
    }

    private fun fillRemainingCities(child: TSP.Tour, parent: TSP.Tour, start: Int, end: Int) {
        for (i in 0..<parent.dimension) {
            if (!isCityInTour(child, parent.getPath()[i])) {
                for (j in 0..<parent.dimension) {
                    if (child.getPath()[j] == null) {
                        child.setCity(j, parent.getPath()[i])
                        break
                    }
                }
            }
        }
    }

    private fun isCityInTour(tour: TSP.Tour, city: TSP.City?): Boolean {
        return tour.getPath().any { it == city }
    }

    private fun tournamentSelection(): TSP.Tour? {
        val index1 = RandomUtils.nextInt(population!!.size)
        var index2 = RandomUtils.nextInt(population!!.size)
        while (index1 == index2) {
            index2 = RandomUtils.nextInt(population!!.size)
        }
        val tour1 = population!![index1]
        val tour2 = population!![index2]
        return if (tour1!!.distance <= tour2!!.distance) tour1.clone() else tour2.clone()
    }

}

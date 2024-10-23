/*
 * MIT License
 *
 * Copyright (c) 2018 Pablo Pallocchi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the “Software”), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 */

package com.esri.arcgismaps.kotlin.sampleviewer.model.room

import kotlin.math.log

class OkapiBM25 {

    companion object {
        /**
         * Calculates the OkapiBM25 score for a given column in an FTS4 table.
         */
        fun score(matchInfo: Array<Int>, column: Int, b: Double = 0.75, k1: Double = 1.2): Double {

            val pOffset = 0
            val cOffset = 1
            val nOffset = 2
            val aOffset = 3

            // the number of matchable phrases in the query
            val termCount = matchInfo[pOffset]
            // the number of user defined columns in the FTS table
            val colCount = matchInfo[cOffset]

            val lOffset = aOffset + colCount
            val xOffset = lOffset + colCount

            // the number of rows in the FTS4 table
            val totalDocs = matchInfo[nOffset].toDouble()
            val avgLength = matchInfo[aOffset + column].toDouble()
            val docLength = matchInfo[lOffset + column].toDouble()

            var score = 0.0

            for (i in 0 until termCount) {

                val currentX = xOffset + (3 * (column + i * colCount))

                // in the current row, the number of times the phrase appears in the column
                val termFrequency = matchInfo[currentX].toDouble()
                // the total number of rows in the FTS table for which the column contains at least one instance of the phrase.
                val docsWithTerm = matchInfo[currentX + 2].toDouble()

                val p = totalDocs - docsWithTerm + 0.5
                val q = docsWithTerm + 0.5
                val idf = log(p, q)

                val r = termFrequency * (k1 + 1)
                val s = b * (docLength / avgLength)
                val t = termFrequency + (k1 * (1 - b + s))
                val rightSide = r / t

                score += (idf * rightSide)
            }

            return score
        }
    }
}

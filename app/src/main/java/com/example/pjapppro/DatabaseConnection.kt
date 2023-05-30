package com.example.pjapppro

import java.sql.*

object DatabaseConnection {
    private const val DB_URL = "sql7.freemysqlhosting.net"
    private const val USER = "sql7620703"
    private const val PASS = "syRmuGc8Zd"

    @get:Throws(SQLException::class)
    val connection: Connection
        get() = DriverManager.getConnection(DB_URL, USER, PASS)

    @Throws(SQLException::class)
    fun insertLog(date: String, userId: Int, paketnikId: Int) {
        val query = "INSERT INTO logs (date, userid, paketnikid) VALUES (?, ?, ?)"
        val connection: Connection = DriverManager.getConnection(DB_URL, USER, PASS)

        try {
            val preparedStatement: PreparedStatement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)
            preparedStatement.setString(1, date)
            preparedStatement.setInt(2, userId)
            preparedStatement.setInt(3, paketnikId)

            preparedStatement.executeUpdate()

            preparedStatement.close()
        } finally {
            connection.close()
        }
    }
}
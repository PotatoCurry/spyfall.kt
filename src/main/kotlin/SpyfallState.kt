package io.github.potatocurry.spyfallkt

import com.sksamuel.hoplite.ConfigLoader
import io.kweb.random
import io.kweb.shoebox.Shoebox
import java.util.*

object SpyfallState {
    val locations = Locations.importLocations()
    val games = Shoebox<Game>()
    val users = Shoebox<User>()

    data class Game(
        val title: String,
        val code: String = generateCode(),
        val location: Location = locations.random(),
        var state: GameState = GameState.INACTIVE,
        val spy: User? = null
    ) {
        fun randomRole() = location.roles.random()
    }

    enum class GameState {
        INACTIVE, ACTIVE, FINISHED
    }

    data class Locations(
        val locations: List<Location>
    ) {
        companion object {
            fun importLocations(): List<Location> {
                val locationsConfig = ConfigLoader().loadConfigOrThrow<Locations>("/locations.yaml")
                return locationsConfig.locations
            }
        }
    }

    data class Location(
        val name: String,
        val roles: List<String>
    )

    data class User(
        val uid: String,
        val gameCode: String,
        val name: String
    ) {
        val game = games[gameCode]!!
        lateinit var role: String
    }

    private fun generateCode(): String {
        return UUID.randomUUID().toString().take(4).toUpperCase() // TODO: Come up with better system or collision detection
    }

    fun createGame(title: String): Game {
        val game = Game(title)
        games[game.code] = game
        return game
    }

    fun createUser(name: String, gameCode: String): User {
        val user = User(generateUid(), gameCode, name)
        users[user.uid] = user
        return user
    }

    fun usersByGame(game: Game) = users.view("usersByGame", User::gameCode).orderedSet(game.code) // TODO: Can this be done with a simple get?

    private fun generateUid() = random.nextInt(100_000_000).toString(16)
}
